package io.kdmtracker.data

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.CampaignSnapshot
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.transfer.CampaignDto
import io.kdmtracker.core.transfer.HybridLogicalClockDto
import io.kdmtracker.core.transfer.SettlementDto
import io.kdmtracker.core.transfer.SyncMetaDto
import io.kdmtracker.core.transfer.TransferMapper
import io.kdmtracker.data.db.KdmDatabase
import kotlinx.serialization.json.Json
import io.kdmtracker.data.db.Campaign as CampaignRow
import io.kdmtracker.data.db.Outbox as OutboxRow
import io.kdmtracker.data.db.Settlement as SettlementRow

/**
 * [CampaignStore] backed by SQLDelight.
 *
 * Each method maps between generated row types and the `:core` domain aggregates. Rows are
 * keyed by their ULID, so `INSERT OR REPLACE` transparently upserts. Ruleset overrides are
 * stored as a JSON blob; the remaining columns map one-to-one onto the aggregates, and the
 * four sync columns rebuild [SyncMeta]. A campaign's `settlementIds` is derived from the
 * settlements loaded for it, since ownership lives in `settlement.campaignId`.
 *
 * Every local mutation also lands in the `outbox`, inside the *same* transaction as the domain
 * row (Review Focus 2). The outbox is the client-side change log the sync engine pushes; its
 * `(campaignId, entityType, entityId)` primary key makes replay idempotent — re-upserting an
 * entity replaces its pending entry instead of appending a second one. The outbox `payload` is the
 * entity's versioned transfer DTO (`CampaignDto`/`SettlementDto`) encoded as JSON, so a future
 * [io.kdmtracker.core.sync.SyncStore] can map a row straight onto a [ChangeRecord].
 */
class SqlDelightCampaignStore(
    private val database: KdmDatabase,
    private val log: (String) -> Unit = { println(it) },
) : CampaignStore {
    override fun loadSnapshots(): List<CampaignSnapshot> =
        database.campaignQueries
            .selectAllCampaigns()
            .executeAsList()
            .map { row ->
                val settlements =
                    database.settlementQueries
                        .selectSettlementsByCampaign(row.id)
                        .executeAsList()
                        .map { it.toDomain() }
                CampaignSnapshot(
                    campaign = row.toDomain(settlements.map { it.id }),
                    settlements = settlements,
                )
            }

    override fun loadRecords(campaignId: String): List<SettlementRecord> =
        database.settlementQueries
            .selectSettlementsByCampaign(campaignId)
            .executeAsList()
            .map { it.toRecord() }

    override fun upsertCampaign(campaign: Campaign) {
        val sync = campaign.sync
        database.transaction {
            database.campaignQueries.upsertCampaign(
                id = campaign.id.value,
                name = campaign.name,
                rulesetId = campaign.rulesetId,
                rulesetOverridesJson = json.encodeToString(RulesetOverrides.serializer(), campaign.rulesetOverrides),
                updatedAtMillis = sync.updatedAt.millis,
                updatedAtCounter = sync.updatedAt.counter.toLong(),
                rev = sync.rev,
                deletedAtMillis = sync.deletedAt?.millis,
                deletedAtCounter = sync.deletedAt?.counter?.toLong(),
            )
            recordOutbox(campaign.toOutboxRecord())
        }
    }

    override fun upsertSettlement(settlement: Settlement) {
        database.transaction {
            writeSettlement(settlement, storedState(settlement.id.value))
        }
    }

    override fun upsertSettlement(record: SettlementRecord) {
        database.transaction {
            writeSettlement(record.settlement, record.state)
        }
    }

    /**
     * Writes the settlement scalars and [state] as one row, recording the matching outbox entry.
     *
     * The outbox payload is the settlement's versioned transfer DTO, so it carries [state] and a
     * later sync push cannot drop it. Callers run this inside a `database.transaction {}`.
     */
    private fun writeSettlement(
        settlement: Settlement,
        state: SettlementState,
    ) {
        val sync = settlement.sync
        database.settlementQueries.upsertSettlement(
            id = settlement.id.value,
            campaignId = settlement.campaignId.value,
            name = settlement.name,
            lanternYear = settlement.lanternYear.toLong(),
            phase = settlement.phase.name,
            currentStep = settlement.currentStep.toLong(),
            endeavors = settlement.endeavors.toLong(),
            population = settlement.population.toLong(),
            settlementEvent = settlement.settlementEvent,
            updatedAtMillis = sync.updatedAt.millis,
            updatedAtCounter = sync.updatedAt.counter.toLong(),
            rev = sync.rev,
            deletedAtMillis = sync.deletedAt?.millis,
            deletedAtCounter = sync.deletedAt?.counter?.toLong(),
            stateJson = json.encodeToString(SettlementState.serializer(), state),
        )
        recordOutbox(settlement.toOutboxRecord(state))
    }

    /** The state already stored for [id], or an empty state when the row is new. */
    private fun storedState(id: String): SettlementState =
        database.settlementQueries
            .selectSettlementById(id)
            .executeAsOneOrNull()
            ?.let { decodeState(it.stateJson) }
            ?: SettlementState()

    /**
     * Decodes [stateJson] into a [SettlementState], tolerating blobs this build cannot read.
     *
     * A newer build may have written fields this build does not know about; a downgraded device
     * must not crash on launch, so an unknown or malformed blob degrades to an empty state (and is
     * logged) instead of throwing out of [loadRecords].
     */
    private fun decodeState(stateJson: String): SettlementState =
        runCatching { json.decodeFromString(SettlementState.serializer(), stateJson) }
            .getOrElse { error ->
                log("SqlDelightCampaignStore: ignoring undecodable settlement stateJson: ${error.message}")
                SettlementState()
            }

    override fun <T> transaction(block: () -> T): T = database.transactionWithResult { block() }

    /**
     * Records [record] as a pending change.
     *
     * Upserting is idempotent by the outbox primary key, so replaying the same mutation (or a
     * crash-retried write) leaves exactly one entry. When called inside a `database.transaction {}`
     * the write joins that transaction; the entity upserts above rely on this to keep the domain row
     * and its outbox entry atomic.
     */
    fun recordOutbox(record: ChangeRecord) {
        database.outboxQueries.upsertOutbox(
            campaignId = record.campaignId,
            entityType = record.entityType,
            entityId = record.entityId,
            op = record.op.name,
            payload = record.payload,
            updatedAtMillis = record.updatedAtMillis,
            updatedAtCounter = record.updatedAtCounter.toLong(),
        )
    }

    /** Local changes for [campaignId] not yet acknowledged by the hub, oldest edit first. */
    fun pending(campaignId: String): List<ChangeRecord> =
        database.outboxQueries
            .selectOutboxByCampaign(campaignId)
            .executeAsList()
            .map { it.toChangeRecord() }

    /** The revision [campaignId] last synced through; `0` when it has never synced. */
    fun cursor(campaignId: String): Long =
        database.syncCursorQueries
            .selectCursor(campaignId)
            .executeAsOneOrNull()
            ?: 0L

    /** Persists [campaignId]'s new sync cursor. */
    fun setCursor(
        campaignId: String,
        rev: Long,
    ) {
        database.syncCursorQueries.upsertCursor(campaignId = campaignId, rev = rev)
    }

    /** Removes exactly [records] from [campaignId]'s outbox once the hub acknowledges them. */
    fun clearAcknowledged(
        campaignId: String,
        records: List<ChangeRecord>,
    ) {
        database.transaction {
            records.forEach { record ->
                database.outboxQueries.deleteOutbox(
                    campaignId = campaignId,
                    entityType = record.entityType,
                    entityId = record.entityId,
                    updatedAtMillis = record.updatedAtMillis,
                    updatedAtCounter = record.updatedAtCounter.toLong(),
                )
            }
        }
    }

    private fun CampaignRow.toDomain(settlementIds: List<Ulid>): Campaign =
        Campaign(
            id = Ulid.parse(id),
            name = name,
            rulesetId = rulesetId,
            rulesetOverrides = json.decodeFromString(RulesetOverrides.serializer(), rulesetOverridesJson),
            settlementIds = settlementIds,
            sync =
                syncMeta(
                    updatedAtMillis = updatedAtMillis,
                    updatedAtCounter = updatedAtCounter,
                    rev = rev,
                    deletedAtMillis = deletedAtMillis,
                    deletedAtCounter = deletedAtCounter,
                ),
        )

    private fun SettlementRow.toDomain(): Settlement =
        Settlement(
            id = Ulid.parse(id),
            campaignId = Ulid.parse(campaignId),
            name = name,
            lanternYear = lanternYear.toInt(),
            phase = SettlementPhase.valueOf(phase),
            currentStep = currentStep.toInt(),
            endeavors = endeavors.toInt(),
            population = population.toInt(),
            settlementEvent = settlementEvent,
            sync =
                syncMeta(
                    updatedAtMillis = updatedAtMillis,
                    updatedAtCounter = updatedAtCounter,
                    rev = rev,
                    deletedAtMillis = deletedAtMillis,
                    deletedAtCounter = deletedAtCounter,
                ),
        )

    private fun SettlementRow.toRecord(): SettlementRecord =
        SettlementRecord(
            settlement = toDomain(),
            state = decodeState(stateJson),
        )

    private fun syncMeta(
        updatedAtMillis: Long,
        updatedAtCounter: Long,
        rev: Long,
        deletedAtMillis: Long?,
        deletedAtCounter: Long?,
    ): SyncMeta =
        SyncMeta(
            updatedAt = HybridLogicalClock(updatedAtMillis, updatedAtCounter.toInt()),
            rev = rev,
            deletedAt =
                deletedAtMillis?.let { millis ->
                    HybridLogicalClock(millis, (deletedAtCounter ?: 0L).toInt())
                },
        )

    private fun Campaign.toOutboxRecord(): ChangeRecord =
        ChangeRecord(
            campaignId = id.value,
            entityType = ENTITY_TYPE_CAMPAIGN,
            entityId = id.value,
            op = sync.changeOp(),
            baseRev = null,
            payload = json.encodeToString(CampaignDto.serializer(), TransferMapper.toDto(this, emptyList())),
            updatedAtMillis = sync.updatedAt.millis,
            updatedAtCounter = sync.updatedAt.counter,
            rev = 0,
        )

    private fun Settlement.toOutboxRecord(state: SettlementState): ChangeRecord =
        ChangeRecord(
            campaignId = campaignId.value,
            entityType = ENTITY_TYPE_SETTLEMENT,
            entityId = id.value,
            op = sync.changeOp(),
            baseRev = null,
            payload = json.encodeToString(SettlementDto.serializer(), toPayloadDto(state)),
            updatedAtMillis = sync.updatedAt.millis,
            updatedAtCounter = sync.updatedAt.counter,
            rev = 0,
        )

    private fun SyncMeta.changeOp(): ChangeOp = if (deletedAt != null) ChangeOp.DELETE else ChangeOp.UPSERT

    private fun Settlement.toPayloadDto(state: SettlementState): SettlementDto =
        SettlementDto(
            id = id.value,
            campaignId = campaignId.value,
            name = name,
            lanternYear = lanternYear,
            phase = phase,
            currentStep = currentStep,
            endeavors = endeavors,
            population = population,
            settlementEvent = settlementEvent,
            sync = sync.toPayloadDto(),
            state = state,
        )

    private fun SyncMeta.toPayloadDto(): SyncMetaDto =
        SyncMetaDto(
            updatedAt = updatedAt.toPayloadDto(),
            rev = rev,
            deletedAt = deletedAt?.toPayloadDto(),
        )

    private fun HybridLogicalClock.toPayloadDto(): HybridLogicalClockDto =
        HybridLogicalClockDto(millis = millis, counter = counter)

    private fun OutboxRow.toChangeRecord(): ChangeRecord =
        ChangeRecord(
            campaignId = campaignId,
            entityType = entityType,
            entityId = entityId,
            op = ChangeOp.valueOf(op),
            baseRev = null,
            payload = payload,
            updatedAtMillis = updatedAtMillis,
            updatedAtCounter = updatedAtCounter.toInt(),
            rev = 0,
        )

    private companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private const val ENTITY_TYPE_CAMPAIGN = "campaign"
        private const val ENTITY_TYPE_SETTLEMENT = "settlement"
    }
}

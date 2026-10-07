package io.kdmtracker.data

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.sync.CampaignSummary
import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.MembershipStore
import io.kdmtracker.core.sync.SyncStore
import io.kdmtracker.core.transfer.CampaignDto
import io.kdmtracker.core.transfer.HybridLogicalClockDto
import io.kdmtracker.core.transfer.SettlementDto
import io.kdmtracker.core.transfer.SyncMetaDto
import io.kdmtracker.core.transfer.TransferMapper
import io.kdmtracker.data.db.KdmDatabase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.kdmtracker.data.db.Campaign as CampaignRow
import io.kdmtracker.data.db.Campaign_membership as MembershipRow
import io.kdmtracker.data.db.Outbox as OutboxRow
import io.kdmtracker.data.db.Settlement as SettlementRow

/**
 * [SyncStore] backed by SQLDelight.
 *
 * Maps the `outbox` rows to the `:core` [ChangeRecord] wire type and applies winning merged
 * records back into the canonical domain tables. The outbox `payload` is the entity's versioned
 * transfer DTO (`CampaignDto`/`SettlementDto`) encoded as JSON (see [SqlDelightCampaignStore]), so
 * the same shape written locally decodes straight back into a domain row here.
 *
 * [applyMerged] is the Ruling-Y seam: applying a remote winner upserts the domain row **and**
 * removes any pending outbox entry for the same `(campaignId, entityType, entityId)` in one
 * transaction. That drop is what stops a superseded local edit from being re-pushed, and it is why
 * the store exposes a triple-only delete rather than the HLC-guarded one used by
 * [clearAcknowledged]. The upsert makes a repeated merge (the engine's merge loop is not itself
 * transactional) idempotent.
 *
 * [local] reads the canonical domain row (not the outbox), so the engine can merge against an edit
 * that the hub already acknowledged (Ruling AD). An unsupported [ChangeRecord.entityType] is
 * skipped and logged instead of throwing, so a single unrecognised record cannot wedge the pass
 * (Ruling AF).
 */
class SqlDelightSyncStore(
    private val database: KdmDatabase,
    private val log: (String) -> Unit = { println(it) },
) : SyncStore,
    MembershipStore {
    override fun cursor(campaignId: String): Long =
        database.syncCursorQueries
            .selectCursor(campaignId)
            .executeAsOneOrNull()
            ?: 0L

    override fun setCursor(
        campaignId: String,
        rev: Long,
    ) {
        database.syncCursorQueries.upsertCursor(campaignId = campaignId, rev = rev)
    }

    override fun pending(campaignId: String): List<ChangeRecord> =
        database.outboxQueries
            .selectOutboxByCampaign(campaignId)
            .executeAsList()
            .map { it.toChangeRecord() }

    override fun local(
        campaignId: String,
        entityType: String,
        entityId: String,
    ): ChangeRecord? =
        when (entityType) {
            ENTITY_TYPE_CAMPAIGN ->
                database.campaignQueries
                    .selectCampaignById(entityId)
                    .executeAsOneOrNull()
                    ?.toChangeRecord()
            ENTITY_TYPE_SETTLEMENT ->
                database.settlementQueries
                    .selectSettlementById(entityId)
                    .executeAsOneOrNull()
                    ?.toChangeRecord()
            else -> null
        }

    override fun enqueue(
        campaignId: String,
        record: ChangeRecord,
    ) {
        database.outboxQueries.upsertOutbox(
            campaignId = campaignId,
            entityType = record.entityType,
            entityId = record.entityId,
            op = record.op.name,
            payload = record.payload,
            updatedAtMillis = record.updatedAtMillis,
            updatedAtCounter = record.updatedAtCounter.toLong(),
        )
    }

    override fun applyMerged(
        campaignId: String,
        record: ChangeRecord,
    ) {
        when (record.entityType) {
            ENTITY_TYPE_CAMPAIGN ->
                database.transaction {
                    upsertCampaign(record.payload, record.rev)
                    dropPending(campaignId, record)
                }
            ENTITY_TYPE_SETTLEMENT ->
                database.transaction {
                    upsertSettlement(record.payload, record.rev)
                    dropPending(campaignId, record)
                }
            else ->
                log(
                    "Skipping sync record with unsupported entity type " +
                        "'${record.entityType}' (entityId=${record.entityId})",
                )
        }
    }

    override fun clearAcknowledged(
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

    /**
     * Stores the caller's membership summary for [summary]'s campaign, replacing any prior row for
     * the same campaign id so a re-listed campaign updates instead of piling up duplicates.
     */
    override fun upsertMembership(summary: CampaignSummary) {
        database.membershipQueries.upsertMembership(
            campaignId = summary.id,
            name = summary.name,
            role = summary.role,
            ownerUsername = summary.ownerUsername,
            sharedWithJson = json.encodeToString(summary.sharedWith),
        )
    }

    /** Every stored membership summary, for the offline Home list. */
    override fun memberships(): List<CampaignSummary> =
        database.membershipQueries
            .selectAllMemberships()
            .executeAsList()
            .map { it.toCampaignSummary() }

    /** The stored membership summary for [campaignId], or `null` when the campaign is unknown. */
    override fun membership(campaignId: String): CampaignSummary? =
        database.membershipQueries
            .selectMembershipById(campaignId)
            .executeAsOneOrNull()
            ?.toCampaignSummary()

    private fun MembershipRow.toCampaignSummary(): CampaignSummary =
        CampaignSummary(
            id = campaignId,
            name = name,
            role = role,
            ownerUsername = ownerUsername,
            sharedWith = json.decodeFromString(sharedWithJson),
        )

    private fun dropPending(
        campaignId: String,
        record: ChangeRecord,
    ) {
        database.outboxQueries.deleteOutboxByKey(
            campaignId = campaignId,
            entityType = record.entityType,
            entityId = record.entityId,
        )
    }

    private fun upsertCampaign(
        payload: String,
        rev: Long,
    ) {
        val (campaign, _) = TransferMapper.toDomain(json.decodeFromString(CampaignDto.serializer(), payload))
        val sync = campaign.sync
        database.campaignQueries.upsertCampaign(
            id = campaign.id.value,
            name = campaign.name,
            rulesetId = campaign.rulesetId,
            rulesetOverridesJson = json.encodeToString(RulesetOverrides.serializer(), campaign.rulesetOverrides),
            updatedAtMillis = sync.updatedAt.millis,
            updatedAtCounter = sync.updatedAt.counter.toLong(),
            rev = rev,
            deletedAtMillis = sync.deletedAt?.millis,
            deletedAtCounter = sync.deletedAt?.counter?.toLong(),
            enabledPacksJson = json.encodeToString(campaign.enabledPacks),
        )
    }

    private fun upsertSettlement(
        payload: String,
        rev: Long,
    ) {
        val dto = json.decodeFromString(SettlementDto.serializer(), payload)
        val settlement = dto.toDomain()
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
            rev = rev,
            deletedAtMillis = sync.deletedAt?.millis,
            deletedAtCounter = sync.deletedAt?.counter?.toLong(),
            stateJson = json.encodeToString(SettlementState.serializer(), dto.state),
        )
    }

    private fun SettlementDto.toDomain(): Settlement =
        Settlement(
            id = Ulid.parse(id),
            campaignId = Ulid.parse(campaignId),
            name = name,
            lanternYear = lanternYear,
            phase = phase,
            currentStep = currentStep,
            endeavors = endeavors,
            population = population,
            settlementEvent = settlementEvent,
            sync =
                SyncMeta(
                    updatedAt = HybridLogicalClock(sync.updatedAt.millis, sync.updatedAt.counter),
                    rev = sync.rev,
                    deletedAt = sync.deletedAt?.let { HybridLogicalClock(it.millis, it.counter) },
                ),
        )

    private fun OutboxRow.toChangeRecord(): ChangeRecord =
        ChangeRecord(
            campaignId = campaignId,
            entityType = entityType,
            entityId = entityId,
            op = runCatching { ChangeOp.valueOf(op) }.getOrDefault(ChangeOp.UPSERT),
            baseRev = null,
            payload = payload,
            updatedAtMillis = updatedAtMillis,
            updatedAtCounter = updatedAtCounter.toInt(),
            rev = 0,
        )

    private fun CampaignRow.toChangeRecord(): ChangeRecord {
        val campaign =
            Campaign(
                id = Ulid.parse(id),
                name = name,
                rulesetId = rulesetId,
                rulesetOverrides = json.decodeFromString(RulesetOverrides.serializer(), rulesetOverridesJson),
                settlementIds = emptyList(),
                sync =
                    SyncMeta(
                        updatedAt = HybridLogicalClock(updatedAtMillis, updatedAtCounter.toInt()),
                        rev = rev,
                        deletedAt = deletedAtMillis?.let { HybridLogicalClock(it, (deletedAtCounter ?: 0L).toInt()) },
                    ),
            )
        return ChangeRecord(
            campaignId = id,
            entityType = ENTITY_TYPE_CAMPAIGN,
            entityId = id,
            op = if (deletedAtMillis != null) ChangeOp.DELETE else ChangeOp.UPSERT,
            baseRev = null,
            payload = json.encodeToString(CampaignDto.serializer(), TransferMapper.toDto(campaign, emptyList())),
            updatedAtMillis = updatedAtMillis,
            updatedAtCounter = updatedAtCounter.toInt(),
            rev = rev,
        )
    }

    private fun SettlementRow.toChangeRecord(): ChangeRecord {
        val dto =
            SettlementDto(
                id = id,
                campaignId = campaignId,
                name = name,
                lanternYear = lanternYear.toInt(),
                phase = SettlementPhase.valueOf(phase),
                currentStep = currentStep.toInt(),
                endeavors = endeavors.toInt(),
                population = population.toInt(),
                settlementEvent = settlementEvent,
                sync =
                    SyncMetaDto(
                        updatedAt = HybridLogicalClockDto(updatedAtMillis, updatedAtCounter.toInt()),
                        rev = rev,
                        deletedAt =
                            deletedAtMillis?.let {
                                HybridLogicalClockDto(
                                    it,
                                    (deletedAtCounter ?: 0L).toInt(),
                                )
                            },
                    ),
                state = json.decodeFromString(SettlementState.serializer(), stateJson),
            )
        return ChangeRecord(
            campaignId = campaignId,
            entityType = ENTITY_TYPE_SETTLEMENT,
            entityId = id,
            op = if (deletedAtMillis != null) ChangeOp.DELETE else ChangeOp.UPSERT,
            baseRev = null,
            payload = json.encodeToString(SettlementDto.serializer(), dto),
            updatedAtMillis = updatedAtMillis,
            updatedAtCounter = updatedAtCounter.toInt(),
            rev = rev,
        )
    }

    private companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private const val ENTITY_TYPE_CAMPAIGN = "campaign"
        private const val ENTITY_TYPE_SETTLEMENT = "settlement"
    }
}

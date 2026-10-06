package io.kdmtracker.data

import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.SyncStore
import io.kdmtracker.core.transfer.CampaignDto
import io.kdmtracker.core.transfer.SettlementDto
import io.kdmtracker.core.transfer.TransferMapper
import io.kdmtracker.data.db.KdmDatabase
import kotlinx.serialization.json.Json
import io.kdmtracker.data.db.Outbox as OutboxRow

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
 */
class SqlDelightSyncStore(
    private val database: KdmDatabase,
) : SyncStore {
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

    override fun applyMerged(
        campaignId: String,
        record: ChangeRecord,
    ) {
        database.transaction {
            when (record.entityType) {
                ENTITY_TYPE_CAMPAIGN -> upsertCampaign(record.payload)
                ENTITY_TYPE_SETTLEMENT -> upsertSettlement(record.payload)
                else -> throw IllegalArgumentException("Unsupported sync entity type: ${record.entityType}")
            }
            database.outboxQueries.deleteOutboxByKey(
                campaignId = campaignId,
                entityType = record.entityType,
                entityId = record.entityId,
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

    private fun upsertCampaign(payload: String) {
        val (campaign, _) = TransferMapper.toDomain(json.decodeFromString(CampaignDto.serializer(), payload))
        val sync = campaign.sync
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
    }

    private fun upsertSettlement(payload: String) {
        val settlement = json.decodeFromString(SettlementDto.serializer(), payload).toDomain()
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

    private companion object {
        private val json = Json
        private const val ENTITY_TYPE_CAMPAIGN = "campaign"
        private const val ENTITY_TYPE_SETTLEMENT = "settlement"
    }
}

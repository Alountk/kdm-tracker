package io.kdmtracker.data

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.CampaignSnapshot
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.data.db.KdmDatabase
import kotlinx.serialization.json.Json
import io.kdmtracker.data.db.Campaign as CampaignRow
import io.kdmtracker.data.db.Settlement as SettlementRow

/**
 * [CampaignStore] backed by SQLDelight.
 *
 * Each method maps between generated row types and the `:core` domain aggregates. Rows are
 * keyed by their ULID, so `INSERT OR REPLACE` transparently upserts. Ruleset overrides are
 * stored as a JSON blob; the remaining columns map one-to-one onto the aggregates, and the
 * four sync columns rebuild [SyncMeta]. A campaign's `settlementIds` is derived from the
 * settlements loaded for it, since ownership lives in `settlement.campaignId`.
 */
class SqlDelightCampaignStore(
    private val database: KdmDatabase,
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

    override fun upsertCampaign(campaign: Campaign) {
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

    override fun upsertSettlement(settlement: Settlement) {
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

    override fun <T> transaction(block: () -> T): T = database.transactionWithResult { block() }

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

    private companion object {
        private val json = Json
    }
}

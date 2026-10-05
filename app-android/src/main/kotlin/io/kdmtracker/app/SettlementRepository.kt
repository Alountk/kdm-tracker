package io.kdmtracker.app

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.CampaignSnapshot
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.persistence.SyncClock
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.RulesetOverrides

/**
 * Holder of the campaign and settlement the UI currently renders, backed by a [CampaignStore].
 *
 * On construction it loads the first stored snapshot. If the store is empty — or a campaign exists
 * with no live settlement, which can only result from an interrupted seed — it seeds a single
 * "Walking Skeleton" campaign and its first settlement atomically through the store. [update]
 * stamps the incoming settlement with a fresh [SyncClock] value and an incremented revision before
 * writing, so last-write-wins has an ordering signal. No rules live here; the controller drives the
 * loop.
 */
class SettlementRepository(
    private val store: CampaignStore,
    private val clock: SyncClock = SyncClock.system(),
) {
    val campaign: Campaign

    var settlement: Settlement
        private set

    init {
        val existing = store.loadSnapshots().firstOrNull()
        val snapshot = if (existing == null || existing.settlements.isEmpty()) seed() else existing
        campaign = snapshot.campaign
        settlement = snapshot.settlements.first()
    }

    fun update(next: Settlement) {
        val previous = settlement.sync
        val stamped =
            next.copy(
                sync =
                    next.sync.copy(
                        updatedAt = clock.tick(previous.updatedAt),
                        rev = previous.rev + 1,
                    ),
            )
        store.upsertSettlement(stamped)
        settlement = stamped
    }

    /**
     * Writes the sample campaign and settlement in one transaction and returns them as the initial
     * snapshot.
     *
     * The campaign and its settlement commit together, so a crash cannot leave a campaign row with
     * no settlement behind. A later construction that finds such an orphan reseeds over it rather
     * than crashing.
     */
    private fun seed(): CampaignSnapshot {
        val campaignId = Ulid.generate(0L) { 0 }
        val settlementId = Ulid.generate(1L) { 1 }
        val campaign =
            Campaign(
                id = campaignId,
                name = "Walking Skeleton",
                rulesetId = Core16Ruleset.id,
                rulesetOverrides = RulesetOverrides(),
                settlementIds = listOf(settlementId),
                sync = SyncMeta(clock.tick(null), rev = 1),
            )
        val settlement =
            Settlement
                .new(id = settlementId, campaignId = campaignId, name = "New Settlement")
                .copy(sync = SyncMeta(clock.tick(null), rev = 1))
        store.transaction {
            store.upsertCampaign(campaign)
            store.upsertSettlement(settlement)
        }
        return CampaignSnapshot(campaign, listOf(settlement))
    }
}

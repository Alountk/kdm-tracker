package io.kdmtracker.app

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.CampaignSnapshot
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.RulesetOverrides

/**
 * Holder of the campaign and settlement the UI currently renders, backed by a [CampaignStore].
 *
 * On construction it loads the first stored snapshot. If the store is empty it seeds a single
 * "Walking Skeleton" campaign and its first settlement and writes them through the store, so a
 * fresh device starts with state the UI can draw. [update] writes the new settlement through the
 * store and exposes it. No rules or sync logic live here; the controller drives the loop.
 */
class SettlementRepository(
    private val store: CampaignStore,
) {
    val campaign: Campaign

    var settlement: Settlement
        private set

    init {
        val existing = store.loadSnapshots().firstOrNull() ?: seed()
        campaign = existing.campaign
        settlement = existing.settlements.first()
    }

    fun update(next: Settlement) {
        store.upsertSettlement(next)
        settlement = next
    }

    /**
     * Writes the sample campaign and settlement and returns them as the initial snapshot.
     *
     * Only called when the store has no snapshots, so the write is idempotent across runs: a
     * second construction over the same store loads the persisted state instead of reseeding.
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
                sync = SyncMeta(HybridLogicalClock(0, 0)),
            )
        val settlement =
            Settlement.new(
                id = settlementId,
                campaignId = campaignId,
                name = "New Settlement",
            )
        store.upsertCampaign(campaign)
        store.upsertSettlement(settlement)
        return CampaignSnapshot(campaign, listOf(settlement))
    }
}

package io.kdmtracker.app

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.RulesetOverrides

/**
 * In-memory holder of the campaign and settlement the UI currently renders.
 *
 * It is seeded with a single "Walking Skeleton" campaign and its first settlement so the UI has
 * state to draw before any persistence layer exists. [update] swaps the settlement; no mutation,
 * persistence or sync logic lives here yet.
 */
class SettlementRepository {
    private val campaignId: Ulid = Ulid.generate(0L) { 0 }
    private val settlementId: Ulid = Ulid.generate(1L) { 1 }

    val campaign: Campaign =
        Campaign(
            id = campaignId,
            name = "Walking Skeleton",
            rulesetId = Core16Ruleset.id,
            rulesetOverrides = RulesetOverrides(),
            settlementIds = listOf(settlementId),
            sync = SyncMeta(HybridLogicalClock(0, 0)),
        )

    var settlement: Settlement =
        Settlement.new(
            id = settlementId,
            campaignId = campaign.id,
            name = "New Settlement",
        )

    fun update(next: Settlement) {
        settlement = next
    }
}

package io.kdmtracker.core.persistence

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Settlement

/**
 * A campaign together with the settlements it owns.
 *
 * [loadSnapshots] returns one of these per stored campaign, with the campaign's
 * settlements embedded in no particular order.
 */
data class CampaignSnapshot(
    val campaign: Campaign,
    val settlements: List<Settlement>,
)

/**
 * Persistence port for campaigns and their settlements.
 *
 * Synchronous by design: SQLDelight exposes blocking queries, and the consumers of
 * this port run their own scheduling. A `Flow`-based, reactive variant is a later
 * concern; this port intentionally does not leak `:data` or Android types into `:core`.
 *
 * Implementations must key records by their [io.kdmtracker.core.domain.Ulid], so
 * upserting an existing campaign or settlement replaces the prior value in place.
 */
interface CampaignStore {
    /** Returns every stored campaign with its settlements. */
    fun loadSnapshots(): List<CampaignSnapshot>

    /** Inserts [campaign] or replaces the stored campaign sharing its id. */
    fun upsertCampaign(campaign: Campaign)

    /** Inserts [settlement] or replaces the stored settlement sharing its id. */
    fun upsertSettlement(settlement: Settlement)
}

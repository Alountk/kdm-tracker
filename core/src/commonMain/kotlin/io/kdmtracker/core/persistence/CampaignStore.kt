package io.kdmtracker.core.persistence

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementRecord

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

    /**
     * Inserts [record] or replaces the stored settlement sharing its id, carrying the full
     * [io.kdmtracker.core.domain.SettlementState] alongside the settlement scalars.
     *
     * This is the canonical write. The scalar [upsertSettlement] overload preserves whatever
     * state is already stored for the settlement, so it can never clobber a record's state.
     */
    fun upsertSettlement(record: SettlementRecord)

    /**
     * Returns every live settlement owned by [campaignId] together with its stored state.
     *
     * [loadSnapshots] remains the settlement-scalars-only view; this is the state-carrying read
     * path the phase steps use.
     */
    fun loadRecords(campaignId: String): List<SettlementRecord>

    /**
     * Runs [block] as one atomic unit: every write it performs commits together, and a failure
     * rolls the whole unit back so no partially written aggregate is ever observable.
     */
    fun <T> transaction(block: () -> T): T
}

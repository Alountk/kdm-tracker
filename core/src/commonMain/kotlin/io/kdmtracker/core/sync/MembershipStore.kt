package io.kdmtracker.core.sync

/**
 * Local, offline cache of the signed-in user's campaign memberships.
 *
 * Read/write port over a [CampaignSummary] per campaign: the app's Home list joins stored
 * campaigns with these summaries to compute a sharing badge, and creating a campaign caches the
 * caller's fresh `OWNER` membership. The server stays authoritative for role/ownership; this port
 * only mirrors it for offline display and is backfilled on every successful
 * [io.kdmtracker.core.sync.SyncApi.listCampaigns].
 */
interface MembershipStore {
    /** Every cached membership summary, in no particular order. */
    fun memberships(): List<CampaignSummary>

    /** The cached membership summary for [campaignId], or `null` when the campaign is unknown. */
    fun membership(campaignId: String): CampaignSummary?

    /** Inserts [summary] or replaces the cached summary sharing its campaign id. */
    fun upsertMembership(summary: CampaignSummary)
}

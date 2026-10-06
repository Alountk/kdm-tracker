package io.kdmtracker.core.sync

/**
 * In-memory [MembershipStore] shared by the app tests.
 *
 * Upserts replace in place by campaign id, matching the SQLDelight store's `PRIMARY KEY` semantics.
 */
class InMemoryMembershipStore : MembershipStore {
    private val stored = LinkedHashMap<String, CampaignSummary>()

    override fun memberships(): List<CampaignSummary> = stored.values.toList()

    override fun membership(campaignId: String): CampaignSummary? = stored[campaignId]

    override fun upsertMembership(summary: CampaignSummary) {
        stored[summary.id] = summary
    }
}

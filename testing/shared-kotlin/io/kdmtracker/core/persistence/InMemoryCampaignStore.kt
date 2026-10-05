package io.kdmtracker.core.persistence

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Settlement

/**
 * In-memory [CampaignStore] shared by the contract suite and the app/`:data` tests.
 *
 * Records are keyed by ULID so an upsert replaces in place, and each campaign's
 * `settlementIds` is derived from the settlements loaded for it rather than the stored value,
 * matching the SQLDelight store. [transaction] runs [block] directly: an in-memory write cannot
 * fail halfway.
 */
class InMemoryCampaignStore : CampaignStore {
    private val campaigns = LinkedHashMap<String, Campaign>()
    private val settlements = LinkedHashMap<String, Settlement>()

    override fun loadSnapshots(): List<CampaignSnapshot> =
        campaigns.values.map { campaign ->
            val owned = settlements.values.filter { it.campaignId == campaign.id }
            CampaignSnapshot(
                campaign = campaign.copy(settlementIds = owned.map { it.id }),
                settlements = owned,
            )
        }

    override fun upsertCampaign(campaign: Campaign) {
        campaigns[campaign.id.value] = campaign
    }

    override fun upsertSettlement(settlement: Settlement) {
        settlements[settlement.id.value] = settlement
    }

    override fun <T> transaction(block: () -> T): T = block()
}

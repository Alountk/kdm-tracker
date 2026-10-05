package io.kdmtracker.app

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.persistence.CampaignSnapshot
import io.kdmtracker.core.persistence.CampaignStore

/**
 * In-memory [CampaignStore] for JVM tests: no SQLite, no Android driver.
 *
 * Mirrors the observable semantics of the SQLDelight store: records are keyed by ULID so an
 * upsert replaces in place, and a campaign's `settlementIds` is derived from the settlements
 * loaded for it rather than the stored value.
 */
class FakeCampaignStore : CampaignStore {
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
}

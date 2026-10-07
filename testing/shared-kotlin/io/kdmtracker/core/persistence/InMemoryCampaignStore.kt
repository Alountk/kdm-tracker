package io.kdmtracker.core.persistence

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState

/**
 * In-memory [CampaignStore] shared by the contract suite and the app/`:data` tests.
 *
 * Records are keyed by ULID so an upsert replaces in place, and each campaign's
 * `settlementIds` is derived from the settlements loaded for it rather than the stored value,
 * matching the SQLDelight store. [transaction] runs [block] directly: an in-memory write cannot
 * fail halfway.
 *
 * The store holds [SettlementRecord]s: [upsertSettlement] with a bare [Settlement] preserves
 * whatever state is already stored, so a scalar write can never clobber a record's state.
 */
class InMemoryCampaignStore : CampaignStore {
    private val campaigns = LinkedHashMap<String, Campaign>()
    private val settlements = LinkedHashMap<String, SettlementRecord>()

    override fun loadSnapshots(): List<CampaignSnapshot> =
        campaigns.values.map { campaign ->
            val owned = settlements.values.filter { it.settlement.campaignId == campaign.id }
            CampaignSnapshot(
                campaign = campaign.copy(settlementIds = owned.map { it.settlement.id }),
                settlements = owned.map { it.settlement },
            )
        }

    override fun upsertCampaign(campaign: Campaign) {
        campaigns[campaign.id.value] = campaign
    }

    override fun upsertSettlement(settlement: Settlement) {
        val existing = settlements[settlement.id.value]
        settlements[settlement.id.value] = SettlementRecord(settlement, existing?.state ?: SettlementState())
    }

    override fun upsertSettlement(record: SettlementRecord) {
        settlements[record.settlement.id.value] = record
    }

    override fun loadRecords(campaignId: String): List<SettlementRecord> =
        settlements.values.filter { it.settlement.campaignId.value == campaignId }

    override fun <T> transaction(block: () -> T): T = block()
}

package io.kdmtracker.core.persistence

/**
 * Runs the shared [CampaignStoreContract] against the in-memory executable specification.
 *
 * The SQLDelight store in `:data` runs the identical suite over a temp-file database, so the two
 * implementations cannot drift apart unnoticed.
 */
class CampaignStoreContractTest : CampaignStoreContract() {
    override fun createStore(): CampaignStore = InMemoryCampaignStore()
}

package io.kdmtracker.app

import io.kdmtracker.core.content.Core16Content
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.RulesetOverrides
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins that the app composes a [io.kdmtracker.core.content.ContentCatalog] from the held
 * campaign's enabled packs and exposes it through the controller's [UiState].
 *
 * An empty pack set is a valid campaign (an empty catalog), while an unknown pack id is a
 * surfaced load error rather than a crash. The existing loop behavior is untouched.
 */
class SettlementCatalogTest {
    @Test
    fun composesTheCatalogFromTheEnabledCorePack() {
        val store = InMemoryCampaignStore()
        seed(store, name = "Core", enabledPacks = listOf(Core16Content.pack.id))

        val repository = SettlementRepository(store)

        assertTrue(repository.catalog.locationDefinitions.isNotEmpty())
        assertTrue(repository.catalog.locationDefinitions.any { it.id == LocationId("lantern_hoard") })
        assertTrue(repository.catalog.innovationDefinitions.isNotEmpty())
        assertNull(repository.catalogError)
    }

    @Test
    fun emptyPackSetYieldsAnEmptyCatalogAndNoError() {
        val store = InMemoryCampaignStore()
        seed(store, name = "Empty", enabledPacks = emptyList())

        val repository = SettlementRepository(store)

        assertTrue(repository.catalog.locationDefinitions.isEmpty())
        assertTrue(repository.catalog.innovationDefinitions.isEmpty())
        assertNull(repository.catalogError)
    }

    @Test
    fun unknownPackIdYieldsAnEmptyCatalogAndSurfacesAnError() {
        val store = InMemoryCampaignStore()
        seed(store, name = "Unknown", enabledPacks = listOf("not-a-pack"))

        val repository = SettlementRepository(store)

        assertTrue(repository.catalog.locationDefinitions.isEmpty())
        assertTrue(repository.catalog.innovationDefinitions.isEmpty())
        assertNotNull(repository.catalogError)
    }

    @Test
    fun controllerPublishesTheRecordAndCatalogView() {
        val store = InMemoryCampaignStore()
        seed(store, name = "Core", enabledPacks = listOf(Core16Content.pack.id))
        val controller = LanternYearController(SettlementRepository(store))

        val state = controller.state.value

        assertNotNull(state.record)
        assertTrue(state.catalogLocations.isNotEmpty())
        assertTrue(state.innovationDefinitions.isNotEmpty())
        assertEquals(4, state.endeavorBreakdown.base)
        assertEquals(4, state.endeavorBreakdown.total)
    }

    @Test
    fun controllerProjectsStorageLinesFromTheRecordLedger() {
        val store = InMemoryCampaignStore()
        seed(store, name = "Core", enabledPacks = listOf(Core16Content.pack.id))
        val repository = SettlementRepository(store)
        repository.update(
            repository.record.copy(
                state =
                    repository.record.state.copy(
                        resourceEntries = listOf(ResourceEntry(ResourceId("Bone"), 3)),
                    ),
            ),
        )

        val line =
            LanternYearController(repository)
                .state.value.storageLines
                .single()

        assertEquals("Bone", line.id.value)
        assertEquals(3, line.quantity)
    }

    @Test
    fun controllerStillAdvancesTheLoop() {
        val store = InMemoryCampaignStore()
        seed(store, name = "Core", enabledPacks = listOf(Core16Content.pack.id))
        val controller = LanternYearController(SettlementRepository(store))

        controller.advance()

        assertEquals(2, controller.state.value.currentStep)
    }

    @Test
    fun controllerSelectCampaignRepublishesTheCatalogView() {
        val store = InMemoryCampaignStore()
        val core = seed(store, name = "Core", enabledPacks = listOf(Core16Content.pack.id))
        val empty = seed(store, name = "Empty", seedValue = 3, enabledPacks = emptyList())
        val controller = LanternYearController(SettlementRepository(store))

        assertTrue(
            controller.state.value.catalogLocations
                .isNotEmpty(),
            "holds the first campaign",
        )

        assertTrue(controller.selectCampaign(empty))

        assertTrue(
            controller.state.value.catalogLocations
                .isEmpty(),
            "the selected empty campaign wins",
        )
        assertEquals(
            empty,
            controller.state.value.record
                ?.settlement
                ?.campaignId
                ?.value,
        )

        assertTrue(controller.selectCampaign(core))
        assertTrue(
            controller.state.value.catalogLocations
                .isNotEmpty(),
        )
    }

    private fun seed(
        store: CampaignStore,
        name: String,
        seedValue: Long = 1L,
        enabledPacks: List<String>,
    ): String {
        val campaignId = Ulid.generate(seedValue) { seedValue.toInt() }
        val settlementId = Ulid.generate(seedValue + 1) { seedValue.toInt() + 1 }
        store.transaction {
            store.upsertCampaign(
                Campaign(
                    id = campaignId,
                    name = name,
                    rulesetId = Core16Ruleset.id,
                    rulesetOverrides = RulesetOverrides(),
                    settlementIds = listOf(settlementId),
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                    enabledPacks = enabledPacks,
                ),
            )
            store.upsertSettlement(
                Settlement.new(id = settlementId, campaignId = campaignId, name = name),
            )
        }
        return campaignId.value
    }
}

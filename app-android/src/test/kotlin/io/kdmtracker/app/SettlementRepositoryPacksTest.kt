package io.kdmtracker.app

import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.content.Core16Content
import io.kdmtracker.core.content.Legacy15Content
import io.kdmtracker.core.content.PackRegistry
import io.kdmtracker.core.content.RegisteredPack
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettlementRepositoryPacksTest {
    private val expansion =
        ContentPack(id = "test.expansion", version = "1", name = "Test", requires = listOf(Core16Content.pack.id))

    private val dependent =
        ContentPack(id = "test.dependent", version = "1", name = "Dependent", requires = listOf(expansion.id))

    private val registry =
        PackRegistry(
            listOf(
                RegisteredPack(Core16Content.pack, base = true),
                RegisteredPack(Legacy15Content.pack, base = true),
                RegisteredPack(expansion),
                RegisteredPack(dependent),
            ),
        )

    private val store = InMemoryCampaignStore()
    private val repository = SettlementRepository(store, packs = registry)

    private fun stored() = store.loadSnapshots().single().campaign

    @Test
    fun enablingAnExpansionStoresItAndTheCatalogStillComposes() {
        repository.changePacks(registry.baseIds + expansion.id)

        assertEquals(registry.baseIds + expansion.id, repository.campaign.enabledPacks)
        assertEquals(repository.campaign.enabledPacks, stored().enabledPacks)
        assertNull(repository.catalogError)
    }

    @Test
    fun aChangeIsStampedSoSyncOrdersItAfterTheCampaignItReplaces() {
        val before = repository.campaign.sync

        repository.changePacks(registry.baseIds + expansion.id)

        val after = stored().sync
        assertEquals(before.rev + 1, after.rev)
        assertTrue(after.updatedAt > before.updatedAt)
    }

    @Test
    fun disablingAPackTheCampaignHasIsRefusedAndNothingIsWritten() {
        repository.changePacks(registry.baseIds + expansion.id)
        val before = stored()

        val failure = assertFailsWith<IllegalArgumentException> { repository.changePacks(registry.baseIds) }

        assertEquals("content pack '${expansion.id}' is already enabled and cannot be disabled", failure.message)
        assertEquals(before, stored())
        assertEquals(before.enabledPacks, repository.campaign.enabledPacks)
    }

    @Test
    fun anExpansionWhoseRequirementIsMissingIsRefusedAndNothingIsWritten() {
        val before = stored()

        val failure =
            assertFailsWith<IllegalArgumentException> { repository.changePacks(registry.baseIds + dependent.id) }

        assertEquals("content pack '${dependent.id}' requires '${expansion.id}'", failure.message)
        assertEquals(before, stored())
    }

    @Test
    fun anUnknownPackIsRefused() {
        val failure = assertFailsWith<IllegalArgumentException> { repository.changePacks(registry.baseIds + "nope") }

        assertEquals("unknown content pack 'nope'", failure.message)
    }

    @Test
    fun askingForTheSamePacksWritesNothing() {
        val before = stored()

        repository.changePacks(registry.baseIds)

        assertEquals(before, stored())
    }
}

package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15Innovations
import io.kdmtracker.core.content.generated.Core15Resources
import io.kdmtracker.core.domain.ResourceCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Legacy15ContentTest {
    private val resources = Legacy15Content.pack.bundle.resources

    @Test
    fun packComposes() {
        val result = ContentCatalogComposer.compose(listOf(Legacy15Content.pack))

        assertTrue(result is CatalogResult.Ok, "expected the 1.5 pack to compose, was $result")
    }

    @Test
    fun packIsTheLegacy15Edition() {
        assertEquals("core-1.5", Legacy15Content.pack.id)
        assertEquals("1.5", Legacy15Content.pack.version)
    }

    @Test
    fun packCarriesTheGeneratedInnovations() {
        val innovations = Legacy15Content.pack.bundle.innovations

        assertTrue(innovations.isNotEmpty(), "expected generated innovations")
        assertEquals(Core15Innovations.bundle.innovations, innovations)
    }

    @Test
    fun packCarriesTheGeneratedResources() {
        assertTrue(resources.isNotEmpty(), "expected generated resources")
        assertEquals(
            Core15Resources.bundle.resources.map { it.id to it.category },
            resources.map { it.id to it.category },
        )
        assertTrue(resources.all { it.source.isNotBlank() }, "every resource carries a source")
    }

    @Test
    fun resourcesHaveTheExpectedCardCount() {
        assertEquals(56, resources.size)
    }

    @Test
    fun monsterBoneIsABasicResource() {
        // "Hueso de Monstruo" (Monster Bone) is a card of the "Recursos Básicos" deck, so BASIC.
        val hueso = resources.first { it.names.es == "Hueso de Monstruo" }

        assertEquals(ResourceCategory.BASIC, hueso.category)
    }

    @Test
    fun whiteLionPeltIsAMonsterResource() {
        // "Pelaje Blanco" (White Lion Pelt) is a card of the "León Blanco" quarry deck, so MONSTER.
        val pelaje = resources.first { it.names.es == "Pelaje Blanco" }

        assertEquals(ResourceCategory.MONSTER, pelaje.category)
    }

    @Test
    fun strangeIronIsASpecialResource() {
        // "Hierro" (Iron) is a card of the "Recursos Extraños" (Strange Resources) deck, so SPECIAL.
        val hierro = resources.first { it.names.es == "Hierro" }

        assertEquals(ResourceCategory.SPECIAL, hierro.category)
    }

    @Test
    fun alimanaVerminDeckIsDeliberatelySpecial() {
        // Alimañas (vermin) have no VERMIN enum member; their deck ships the "strange"
        // consumables, so SPECIAL is the deliberate choice. Pinned so it stays deliberate.
        val cucaracha = resources.first { it.names.es == "Cucaracha Silbante" }

        assertEquals(ResourceCategory.SPECIAL, cucaracha.category)
    }

    @Test
    fun resourceNamesAreSorted() {
        val names = resources.map { it.names.es }

        assertEquals(names.sorted(), names)
    }
}

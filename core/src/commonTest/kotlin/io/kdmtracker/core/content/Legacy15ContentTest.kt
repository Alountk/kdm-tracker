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
        assertEquals(Core15Resources.bundle.resources, resources)
    }

    @Test
    fun resourcesHaveTheExpectedCardCount() {
        assertEquals(56, resources.size)
    }

    @Test
    fun monsterBoneIsABasicResource() {
        // "Hueso de Monstruo" (Monster Bone) is a card of the "Recursos Básicos" deck, so BASIC.
        val hueso = resources.first { it.name == "Hueso de Monstruo" }

        assertEquals(ResourceCategory.BASIC, hueso.category)
    }

    @Test
    fun whiteLionPeltIsAMonsterResource() {
        // "Pelaje Blanco" (White Lion Pelt) is a card of the "León Blanco" quarry deck, so MONSTER.
        val pelaje = resources.first { it.name == "Pelaje Blanco" }

        assertEquals(ResourceCategory.MONSTER, pelaje.category)
    }

    @Test
    fun strangeIronIsASpecialResource() {
        // "Hierro" (Iron) is a card of the "Recursos Extraños" (Strange Resources) deck, so SPECIAL.
        val hierro = resources.first { it.name == "Hierro" }

        assertEquals(ResourceCategory.SPECIAL, hierro.category)
    }

    @Test
    fun resourceNamesAreSorted() {
        val names = resources.map { it.name }

        assertEquals(names.sorted(), names)
    }
}

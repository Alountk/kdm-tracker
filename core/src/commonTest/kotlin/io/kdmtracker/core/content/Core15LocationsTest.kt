package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15Locations
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.SettlementLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the committed `core-1.5` settlement-location bundle against the source cards.
 *
 * The costs are keyed by a specific resource **card** name as an approximation of the
 * card's material *type* (see the content-ingest README); this test locks the extraction
 * so the approximation cannot drift silently.
 */
class Core15LocationsTest {
    private val locations = Core15Locations.bundle.locations

    private fun byName(name: String): SettlementLocation = locations.first { it.name == name }

    @Test
    fun packCarriesTheThirteenBaseLocations() {
        assertEquals(13, locations.size)
        assertEquals(
            listOf(
                "Armería",
                "Artesano de Máscaras",
                "Cirujano Barbero",
                "Curtiduría",
                "Círculo de Piedra",
                "Cúmulo de Linternas",
                "Cúmulo de Linternas Extinto",
                "Felinarium",
                "Herrería",
                "Molino de Órganos",
                "Peletería",
                "Plumería",
                "Taller de Huesos",
            ),
            locations.map { it.name },
        )
    }

    @Test
    fun armeriaRequiresTheBoneSmithAndCostsThreeBoneOneHide() {
        val armeria = byName("Armería")

        assertEquals(listOf("Taller de Huesos"), armeria.requires)
        assertEquals(1, armeria.buildEndeavors)
        assertEquals(mapOf("Hueso de Monstruo" to 3, "Piel de Monstruo" to 1), armeria.buildCost)
    }

    @Test
    fun circuloDePiedraCostsThreeOrganOneHide() {
        val circulo = byName("Círculo de Piedra")

        assertEquals(listOf("Molino de Órganos"), circulo.requires)
        assertEquals(mapOf("Órgano de Monstruo" to 3, "Piel de Monstruo" to 1), circulo.buildCost)
    }

    @Test
    fun lanternHoardInnovatesOncePerPhaseForBoneOrganHide() {
        val action = byName("Cúmulo de Linternas").actions.single()

        assertEquals("Innovar", action.name)
        assertEquals(ActionKind.INNOVATE, action.kind)
        assertEquals(1, action.endeavorCost)
        assertEquals(
            mapOf("Hueso de Monstruo" to 1, "Órgano de Monstruo" to 1, "Piel de Monstruo" to 1),
            action.resourceCost,
        )
        assertTrue(action.oncePerPhase, "Innovate is once per Settlement Phase")
        assertEquals(2, action.draw)
        assertEquals(1, action.keep)
    }

    @Test
    fun locationsAreSortedByName() {
        assertEquals(locations.map { it.name }.sorted(), locations.map { it.name })
    }

    @Test
    fun packIncludesTheGeneratedLocations() {
        assertEquals(Core15Locations.bundle.locations, Legacy15Content.pack.bundle.locations)
    }
}

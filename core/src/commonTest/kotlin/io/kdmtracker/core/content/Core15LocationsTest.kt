package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15Locations
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.SettlementLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the committed `core-1.5` settlement-location bundle against the source cards.
 *
 * The costs are keyed by a specific resource **card** id, resolved from the card name through
 * the identity CSV (see the content-ingest README); this test locks the extraction so it
 * cannot drift silently.
 */
class Core15LocationsTest {
    private val locations = Core15Locations.bundle.locations

    private fun byName(name: String): SettlementLocation = locations.first { it.names.es == name }

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
            locations.map { it.names.es },
        )
    }

    @Test
    fun armeriaRequiresTheBoneSmithAndCostsThreeBoneOneHide() {
        val armeria = byName("Armería")

        assertEquals(LocationId("weapon_crafter"), armeria.id)
        assertEquals(listOf(LocationId("bone_smith")), armeria.requires)
        assertEquals(1, armeria.buildEndeavors)
        assertEquals(
            mapOf(ResourceId("bone.monster") to 3, ResourceId("hide.monster") to 1),
            armeria.buildCost,
        )
    }

    @Test
    fun circuloDePiedraCostsThreeOrganOneHide() {
        val circulo = byName("Círculo de Piedra")

        assertEquals(listOf(LocationId("organ_grinder")), circulo.requires)
        assertEquals(
            mapOf(ResourceId("organ.monster") to 3, ResourceId("hide.monster") to 1),
            circulo.buildCost,
        )
    }

    @Test
    fun lanternHoardInnovatesOncePerPhaseForBoneOrganHide() {
        val action = byName("Cúmulo de Linternas").actions.single()

        assertEquals("Innovar", action.names.es)
        assertEquals("Innovate", action.names.en)
        assertEquals(ActionKind.INNOVATE, action.kind)
        assertEquals(1, action.endeavorCost)
        assertEquals(
            mapOf(
                ResourceId("bone.monster") to 1,
                ResourceId("organ.monster") to 1,
                ResourceId("hide.monster") to 1,
            ),
            action.resourceCost,
        )
        assertTrue(action.oncePerPhase, "Innovate is once per Settlement Phase")
        assertEquals(2, action.draw)
        assertEquals(1, action.keep)
    }

    @Test
    fun locationsAreSortedByName() {
        assertEquals(locations.map { it.names.es }.sorted(), locations.map { it.names.es })
    }

    @Test
    fun packIncludesTheGeneratedLocations() {
        assertTrue(
            Legacy15Content.pack.bundle.locations
                .containsAll(Core15Locations.bundle.locations),
        )
    }
}

package io.kdmtracker.app

import io.kdmtracker.app.ui.settlement.InnovationsModel
import io.kdmtracker.app.ui.settlement.LocationsModel
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.Core16Content
import io.kdmtracker.core.content.Legacy15Content
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.SettlementAction
import io.kdmtracker.core.domain.SettlementLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the Develop step's two panels: the innovation draw text must come from the ruleset (so a
 * house rule changes it) and the locations must split into built and buildable by their requirement.
 */
class DevelopPanelsTest {
    @Test
    fun theDrawLabelFollowsTheRulesetNotALiteral() {
        assertEquals("Robar 2, elegir 1", InnovationsModel.of(emptyList(), emptyList(), draw = 2, keep = 1).drawLabel)
        assertEquals("Robar 4, elegir 1", InnovationsModel.of(emptyList(), emptyList(), draw = 4, keep = 1).drawLabel)
    }

    @Test
    fun theDeckSizeCountsTheCards() {
        assertEquals(14, InnovationsModel.of(List(14) { "i$it" }, emptyList(), 2, 1).deckSize)
    }

    @Test
    fun aLocationWithNoBuildEndeavorsIsAlreadyBuilt() {
        val model =
            LocationsModel.of(
                built = listOf("El Cúmulo de Linternas"),
                catalog =
                    listOf(
                        SettlementLocation(LocationId("lantern_hoard"), LocalizedText(es = "El Cúmulo de Linternas")),
                        SettlementLocation(
                            LocationId("bone_smith"),
                            LocalizedText(es = "Taller de Huesos"),
                            buildEndeavors = 1,
                        ),
                    ),
            )

        assertEquals(listOf("El Cúmulo de Linternas"), model.built.map { it.name })
        assertEquals(listOf("Taller de Huesos"), model.buildable.map { it.name })
        assertEquals(1, model.buildable.single().endeavors)
    }

    @Test
    fun aBuiltLocationShowsItsActionNotConstruir() {
        // S2-plan T4, deferred here: the built section shows the location's own action; the
        // buildable section's button reads "Construir"; a built location with no action gets
        // no button at all rather than a bogus "Construir".
        val lantern =
            SettlementLocation(
                LocationId("lantern_hoard"),
                LocalizedText(es = "Cúmulo de Linternas"),
                actions =
                    listOf(
                        SettlementAction(names = LocalizedText(es = "Innovar"), kind = ActionKind.INNOVATE),
                    ),
            )
        val taller =
            SettlementLocation(LocationId("bone_smith"), LocalizedText(es = "Taller de Huesos"), buildEndeavors = 1)

        val mixed = LocationsModel.of(built = listOf("Cúmulo de Linternas"), catalog = listOf(lantern, taller))

        assertEquals("Innovar", mixed.built.single().buttonLabel)
        assertEquals("Construir", mixed.buildable.single().buttonLabel)

        val onlyBuilt = LocationsModel.of(built = listOf("Taller de Huesos"), catalog = listOf(taller))

        assertNull(onlyBuilt.built.single().buttonLabel)
    }

    @Test
    fun thePanelCountsTheMergedCatalogsLocationsOnce() {
        // S2 collapses the two packs' duplicates: the panel's "N Opciones" total is the merged
        // catalog's size (19), not the pre-S2 32.
        val catalog = ContentCatalog(listOf(Core16Content.pack, Legacy15Content.pack)).locationDefinitions

        val model = LocationsModel.of(built = emptyList(), catalog = catalog)

        assertEquals(19, model.built.size + model.buildable.size)
    }

    @Test
    fun aPrincipleSlotShowsTheChoiceLabelNotTheCategoryPrefix() {
        // The controller used to hand-build "DEATH: …"; S2 removed the prefix at the source and
        // the slot passes the chosen label through unchanged (the model expects the chosen
        // principles — three slots max, empties for the rest).
        val model = InnovationsModel.of(emptyList(), listOf("The First Harvest → Cannibalize"), draw = 2, keep = 1)

        assertEquals(listOf("The First Harvest → Cannibalize"), model.principleSlots)
        assertEquals(2, model.emptySlots)
    }
}

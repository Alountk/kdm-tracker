package io.kdmtracker.app

import io.kdmtracker.app.ui.settlement.ResourceColour
import io.kdmtracker.app.ui.settlement.SettlementSummaryModel
import io.kdmtracker.app.ui.settlement.SummaryResource
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.ResourceMaterial
import io.kdmtracker.core.domain.StorageLine
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the summary's block: the four basic resources in the design's order with its short labels and
 * colours, the endeavors pair carrying the derived maximum, and a resource the design does not name
 * still rendering.
 */
class SettlementSummaryTest {
    @Test
    fun theFourBasicResourcesKeepTheDesignsOrderLabelsAndColours() {
        val model =
            SettlementSummaryModel.of(
                survivalLimit = 1,
                endeavors = 4,
                maxEndeavors = 4,
                resources =
                    listOf(
                        SummaryResource(ResourceId("bone"), "Hueso de Monstruo", "Hueso", 1, ResourceColour.BONE),
                        SummaryResource(ResourceId("organ"), "Órgano de Monstruo", "Órgano", 1, ResourceColour.CORAL),
                        SummaryResource(ResourceId("hide"), "Piel de Monstruo", "Pellejo", 1, ResourceColour.GOLD),
                        SummaryResource(ResourceId("scrap"), "Scrap", "Chatarra", 0, ResourceColour.DIM),
                    ),
            )

        assertEquals(listOf("Hueso", "Órgano", "Pellejo", "Chatarra"), model.resources.map { it.label })
        assertEquals(
            listOf(ResourceColour.BONE, ResourceColour.CORAL, ResourceColour.GOLD, ResourceColour.DIM),
            model.resources.map { it.colour },
        )
        assertEquals(4, model.resources.size, "a resource with none still renders its cell")
        assertEquals(0, model.resources.last().quantity)
    }

    @Test
    fun theCellsDrawTheIconsTheNewestDesignNames() {
        assertEquals("nutrition", ResourceColour.BONE.icon)
        assertEquals("psychology", ResourceColour.CORAL.icon)
        assertEquals("layers", ResourceColour.GOLD.icon)
        assertEquals("hardware", ResourceColour.DIM.icon)
    }

    @Test
    fun theEndeavorsPairCarriesTheDerivedMaximum() {
        val model = SettlementSummaryModel.of(1, endeavors = 3, maxEndeavors = 4, resources = emptyList())

        assertEquals(3, model.endeavors)
        assertEquals(4, model.maxEndeavors)
    }

    private fun basic(
        id: String,
        material: ResourceMaterial,
    ) = ResourceDefinition(
        id = ResourceId(id),
        names = LocalizedText(es = id),
        category = ResourceCategory.BASIC,
        materials = setOf(material),
    )

    private val catalog =
        listOf(
            basic("scrap", ResourceMaterial.SCRAP),
            basic("skull.monster", ResourceMaterial.BONE),
            basic("hide.monster", ResourceMaterial.HIDE),
            basic("organ.monster", ResourceMaterial.ORGAN),
            basic("bone.monster", ResourceMaterial.BONE),
        )

    @Test
    fun theGridHasOneCellPerMaterialInTheDesignsOrderWhateverTheCatalogOrder() {
        val resources = SettlementSummaryModel.basicResources(catalog, emptyList())

        assertEquals(listOf("Hueso", "Órgano", "Pellejo", "Chatarra"), resources.map { it.label })
        assertEquals(
            listOf("bone.monster", "organ.monster", "hide.monster", "scrap"),
            resources.map { it.id.value },
        )
        assertEquals(
            listOf(ResourceColour.BONE, ResourceColour.CORAL, ResourceColour.GOLD, ResourceColour.DIM),
            resources.map { it.colour },
        )
    }

    @Test
    fun aCellCountsItsMaterialsBasicResourceAndNotTheOtherBonesInTheCatalog() {
        val storage =
            listOf(
                StorageLine(ResourceId("bone.monster"), 1),
                StorageLine(ResourceId("skull.monster"), 4),
                StorageLine(ResourceId("scrap"), 2),
            )

        val resources = SettlementSummaryModel.basicResources(catalog, storage)

        assertEquals(listOf(1, 0, 0, 2), resources.map { it.quantity })
    }

    @Test
    fun aMaterialWhoseBasicResourceTheCatalogLacksDropsOut() {
        val resources =
            SettlementSummaryModel.basicResources(
                definitions = catalog.filterNot { it.id.value == "scrap" },
                storage = emptyList(),
            )

        assertEquals(listOf("Hueso", "Órgano", "Pellejo"), resources.map { it.label })
    }
}

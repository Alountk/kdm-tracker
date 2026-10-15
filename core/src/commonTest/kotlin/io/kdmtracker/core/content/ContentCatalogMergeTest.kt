package io.kdmtracker.core.content

import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.SettlementAction
import io.kdmtracker.core.domain.SettlementEvent
import io.kdmtracker.core.domain.SettlementLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentCatalogMergeTest {
    private fun pack(
        id: String,
        locations: List<SettlementLocation>,
    ) = ContentPack(
        id = id,
        version = "1.0.0",
        name = id,
        bundle = ContentBundle(locations = locations),
    )

    private fun ok(result: CatalogResult): ContentCatalog {
        assertTrue(result is CatalogResult.Ok, "expected Ok but was $result")
        return result.catalog
    }

    private fun packWithInnovations(vararg innovations: Innovation) =
        ContentPack(
            id = "innovations-${innovations.size}",
            version = "1.0.0",
            name = "innovations",
            bundle = ContentBundle(innovations = innovations.toList()),
        )

    private fun packWithSettlementEvents(vararg events: SettlementEvent) =
        ContentPack(
            id = "events-${events.size}",
            version = "1.0.0",
            name = "events",
            bundle = ContentBundle(settlementEvents = events.toList()),
        )

    @Test
    fun settlementEventsMergeAcrossPacksSortedAndDeduped() {
        val a = packWithSettlementEvents(SettlementEvent("Plaga", "plaga text"))
        val b =
            packWithSettlementEvents(
                SettlementEvent("Rivalidad", "rivalidad text"),
                SettlementEvent("Plaga", "plaga text"),
            )
        val catalog = ok(ContentCatalogComposer.compose(listOf(a, b)))

        assertEquals(listOf("Plaga", "Rivalidad"), catalog.settlementEvents)
        assertEquals(2, catalog.settlementEventDefinitions.size)
    }

    @Test
    fun modeledLocationWinsOverBareRegardlessOfPackOrder() {
        val bare = SettlementLocation(LocationId("weapon_crafter"), LocalizedText(es = "Armería"))
        val modeled =
            SettlementLocation(
                LocationId("weapon_crafter"),
                LocalizedText(es = "Armería"),
                buildCost =
                    mapOf(
                        ResourceId("bone.monster") to 3,
                    ),
            )

        val forward =
            ok(ContentCatalogComposer.compose(listOf(pack("bare", listOf(bare)), pack("modeled", listOf(modeled)))))
        val reversed =
            ok(ContentCatalogComposer.compose(listOf(pack("modeled", listOf(modeled)), pack("bare", listOf(bare)))))

        assertEquals(
            mapOf(ResourceId("bone.monster") to 3),
            forward.locationDefinitions
                .single {
                    it.names.es ==
                        "Armería"
                }.buildCost,
        )
        assertEquals(
            mapOf(ResourceId("bone.monster") to 3),
            reversed.locationDefinitions
                .single {
                    it.names.es ==
                        "Armería"
                }.buildCost,
        )
    }

    @Test
    fun aCategoryFillsInFromWhicheverPackCarriesOne() {
        val bare = Innovation(InnovationId("pintura"), LocalizedText(es = "Pintura"), "Pintura ritual.")
        val withCategory =
            Innovation(InnovationId("pintura"), LocalizedText(es = "Pintura"), "Pintura ritual.", category = "Arte")

        val seedFirst =
            ok(
                ContentCatalogComposer.compose(
                    listOf(packWithInnovations(bare), packWithInnovations(withCategory)),
                ),
            )
        val pipelineFirst =
            ok(
                ContentCatalogComposer.compose(
                    listOf(packWithInnovations(withCategory), packWithInnovations(bare)),
                ),
            )

        assertEquals("Arte", seedFirst.innovationDefinitions.single().category)
        assertEquals("Arte", pipelineFirst.innovationDefinitions.single().category)
    }

    @Test
    fun aLocationsAuthoredDescriptionSurvivesTheMergeInEitherPackOrder() {
        val bare = SettlementLocation(LocationId("bone_smith"), LocalizedText(es = "Taller de Huesos"))
        val withProse =
            SettlementLocation(
                LocationId("bone_smith"),
                LocalizedText(es = "Taller de Huesos"),
                description = LocalizedText(es = "Talla armas de hueso."),
            )

        val seedFirst =
            ok(ContentCatalogComposer.compose(listOf(pack("seed", listOf(bare)), pack("pipeline", listOf(withProse)))))
        val pipelineFirst =
            ok(ContentCatalogComposer.compose(listOf(pack("pipeline", listOf(withProse)), pack("seed", listOf(bare)))))

        assertEquals(LocalizedText(es = "Talla armas de hueso."), seedFirst.locationDefinitions.single().description)
        assertEquals(
            LocalizedText(es = "Talla armas de hueso."),
            pipelineFirst.locationDefinitions.single().description,
        )
    }

    @Test
    fun modeledActionsAlsoWinOverBare() {
        val bare = SettlementLocation(LocationId("weapon_crafter"), LocalizedText(es = "Armería"))
        val modeled =
            SettlementLocation(
                LocationId("weapon_crafter"),
                LocalizedText(es = "Armería"),
                actions = listOf(SettlementAction(names = LocalizedText(es = "Build"))),
            )

        val catalog =
            ok(ContentCatalogComposer.compose(listOf(pack("bare", listOf(bare)), pack("modeled", listOf(modeled)))))

        assertTrue(
            catalog.locationDefinitions
                .single { it.names.es == "Armería" }
                .actions
                .isNotEmpty(),
        )
    }

    @Test
    fun bareOverlapKeepsFirstOccurrenceByPackOrder() {
        val first = SettlementLocation(LocationId("weapon_crafter"), LocalizedText(es = "Armería"), buildEndeavors = 1)
        val second = SettlementLocation(LocationId("weapon_crafter"), LocalizedText(es = "Armería"), buildEndeavors = 2)

        val forward = ok(ContentCatalogComposer.compose(listOf(pack("a", listOf(first)), pack("b", listOf(second)))))
        val reversed = ok(ContentCatalogComposer.compose(listOf(pack("b", listOf(second)), pack("a", listOf(first)))))

        assertEquals(1, forward.locationDefinitions.single { it.names.es == "Armería" }.buildEndeavors)
        assertEquals(2, reversed.locationDefinitions.single { it.names.es == "Armería" }.buildEndeavors)
    }
}

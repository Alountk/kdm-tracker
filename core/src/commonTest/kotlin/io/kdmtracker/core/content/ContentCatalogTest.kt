package io.kdmtracker.core.content

import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.SettlementLocation
import io.kdmtracker.core.domain.TimelineEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentCatalogTest {
    private val core =
        ContentPack(
            id = "core",
            version = "1.0.0",
            name = "Core",
            bundle =
                ContentBundle(
                    innovations =
                        listOf(
                            Innovation(name = "pictographs", description = "…"),
                            Innovation(name = "ammonia", description = "…"),
                            Innovation(name = "pictographs", description = "duplicate name"),
                        ),
                    locations = listOf(SettlementLocation("lantern-hoard")),
                    resources =
                        listOf(
                            ResourceDefinition("hide", ResourceCategory.BASIC),
                            ResourceDefinition("bone", ResourceCategory.BASIC),
                        ),
                    timeline = listOf(TimelineEntry(lanternYear = 4, label = "Story Event")),
                    quarries = listOf("white-lion", "screaming-antelope"),
                    nemeses = listOf("butcher"),
                ),
        )

    private val expansion =
        ContentPack(
            id = "expansion",
            version = "1.0.0",
            name = "Expansion",
            requires = listOf("core"),
            bundle =
                ContentBundle(
                    innovations =
                        listOf(
                            Innovation(name = "forbidden-dance", description = "…"),
                            Innovation(name = "ammonia", description = "…"),
                        ),
                    quarries = listOf("gorm", "white-lion"),
                    nemeses = listOf("the-hand"),
                    resources =
                        listOf(
                            ResourceDefinition("bone", ResourceCategory.BASIC),
                            ResourceDefinition("scrap", ResourceCategory.SPECIAL),
                        ),
                ),
        )

    private fun ok(result: CatalogResult): ContentCatalog {
        assertTrue(result is CatalogResult.Ok, "expected Ok but was $result")
        return result.catalog
    }

    @Test
    fun composeIsOrderIndependent() {
        val forward = ok(ContentCatalogComposer.compose(listOf(core, expansion)))
        val reversed = ok(ContentCatalogComposer.compose(listOf(expansion, core)))

        assertEquals(forward.innovations, reversed.innovations)
        assertEquals(forward.locations, reversed.locations)
        assertEquals(forward.quarries, reversed.quarries)
        assertEquals(forward.nemeses, reversed.nemeses)
        assertEquals(forward.resources, reversed.resources)
    }

    @Test
    fun typedDefinitionsAreMergedFromTheBundles() {
        val catalog = ok(ContentCatalogComposer.compose(listOf(core, expansion)))

        assertEquals(3, catalog.innovationDefinitions.size)
        assertEquals("lantern-hoard", catalog.locationDefinitions.single().name)
        assertEquals(ResourceCategory.BASIC, catalog.resourceDefinitions.first { it.name == "bone" }.category)
        assertEquals(
            ResourceCategory.SPECIAL,
            catalog.resourceDefinitions.first { it.name == "scrap" }.category,
        )
        assertEquals(listOf(TimelineEntry(lanternYear = 4, label = "Story Event")), catalog.timeline)
    }

    @Test
    fun missingRequirementReturnsMissingRequirement() {
        val orphan =
            ContentPack(
                id = "orphan",
                version = "1.0.0",
                name = "Orphan",
                requires = listOf("core"),
            )

        val result = ContentCatalogComposer.compose(listOf(orphan))

        assertEquals(CatalogResult.MissingRequirement(packId = "orphan", missing = "core"), result)
    }

    @Test
    fun satisfiedRequirementPasses() {
        val result = ContentCatalogComposer.compose(listOf(core, expansion))

        assertTrue(result is CatalogResult.Ok)
    }

    @Test
    fun unionDedupsAndSorts() {
        val catalog = ok(ContentCatalogComposer.compose(listOf(core, expansion)))

        assertEquals(listOf("ammonia", "forbidden-dance", "pictographs"), catalog.innovations)
        assertEquals(listOf("lantern-hoard"), catalog.locations)
        assertEquals(listOf("gorm", "screaming-antelope", "white-lion"), catalog.quarries)
        assertEquals(listOf("butcher", "the-hand"), catalog.nemeses)
        assertEquals(listOf("bone", "hide", "scrap"), catalog.resources)
    }
}

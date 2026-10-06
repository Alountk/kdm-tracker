package io.kdmtracker.core.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentCatalogTest {
    private val core =
        ContentPack(
            id = "core",
            version = "1.0.0",
            name = "Core",
            provideInnovations = listOf("pictographs", "ammonia", "pictographs"),
            provideLocations = listOf("lantern-hoard"),
            provideQuarries = listOf("white-lion", "screaming-antelope"),
            provideNemeses = listOf("butcher"),
            provideResources = listOf("hide", "bone", "hide"),
        )

    private val expansion =
        ContentPack(
            id = "expansion",
            version = "1.0.0",
            name = "Expansion",
            requires = listOf("core"),
            provideInnovations = listOf("forbidden-dance", "ammonia"),
            provideQuarries = listOf("gorm", "white-lion"),
            provideNemeses = listOf("the-hand"),
            provideResources = listOf("bone", "scrap"),
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

package io.kdmtracker.core.content

import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceDefinition
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResourceGroupsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun sourceOfMapsKnownNamesAndFallsBackToOtros() {
        assertEquals("León Blanco", ResourceSources.sourceOf("Garra de León"))
        assertEquals("Fénix", ResourceSources.sourceOf("Pico de Ave"))
        assertEquals("Antílope Bramador", ResourceSources.sourceOf("Cuerno en Espiral"))
        assertEquals("Básicos", ResourceSources.sourceOf("Hueso de Monstruo"))
        assertEquals("Otros", ResourceSources.sourceOf("Nombre Desconocido"))
    }

    @Test
    fun resourceGroupsKeepsEveryResource() {
        val catalog =
            ContentCatalog(
                listOf(
                    ContentPack(
                        id = "p",
                        version = "1",
                        name = "p",
                        bundle =
                            ContentBundle(
                                resources =
                                    listOf(
                                        ResourceDefinition("Garra de León", ResourceCategory.MONSTER),
                                        ResourceDefinition("Nombre Desconocido", ResourceCategory.SPECIAL),
                                    ),
                            ),
                    ),
                ),
            )

        val groups = catalog.resourceGroups()

        assertTrue("León Blanco" in groups)
        assertTrue("Otros" in groups)
        assertEquals(2, groups.values.sumOf { it.size })
    }

    @Test
    fun legacyResourceDefinitionJsonWithoutSourceDecodesEmpty() {
        val definition = json.decodeFromString<ResourceDefinition>("""{"name":"X","category":"BASIC"}""")

        assertEquals("", definition.source)
    }
}

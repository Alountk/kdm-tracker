package io.kdmtracker.core.content

import io.kdmtracker.core.domain.ResourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ContentIdentityTest {
    private val catalog = ContentCatalog(listOf(Core16Content.pack, Legacy15Content.pack))

    @Test
    fun everyCatalogResourceNameResolvesToAnId() {
        catalog.resourceDefinitions.forEach { definition ->
            assertNotNull(
                ContentIdentity.resourceIdFor(definition.names.es),
                "no content id for '${definition.names.es}'",
            )
        }
    }

    @Test
    fun theCatalogCarriesEachResourceExactlyOnce() {
        assertEquals(
            catalog.resourceDefinitions.size,
            catalog.resourceDefinitions.distinctBy { it.id }.size,
        )
    }

    @Test
    fun bothPacksAgreeOnTheFourBasicsSoTheyDedupeIntoOne() {
        // 56 generated 1.5 cards, plus the 1.6 seed's "Scrap" — which no 1.5 deck carries, so it
        // is genuinely a 57th resource rather than a duplicate.
        assertEquals(57, catalog.resourceDefinitions.size)

        // The three basics the seed names in English are the *same* resources the 1.5 pack names
        // in Spanish, so they collapse to one definition each rather than double-counting.
        listOf("bone.monster", "hide.monster", "organ.monster").forEach { id ->
            assertEquals(1, catalog.resourceDefinitions.count { it.id.value == id }, "id $id")
        }

        assertEquals(ResourceId("bone.monster"), ContentIdentity.resourceIdFor("Hueso de Monstruo"))
        assertEquals(ResourceId("bone.monster"), ContentIdentity.resourceIdFor("Bone"))
        assertEquals(ResourceId("hide.monster"), ContentIdentity.resourceIdFor("Piel de Monstruo"))
        assertEquals(ResourceId("organ.monster"), ContentIdentity.resourceIdFor("Órgano de Monstruo"))
        assertEquals(ResourceId("scrap"), ContentIdentity.resourceIdFor("Scrap"))
    }
}

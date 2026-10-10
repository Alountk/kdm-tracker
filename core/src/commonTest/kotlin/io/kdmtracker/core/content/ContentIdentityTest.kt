package io.kdmtracker.core.content

import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
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

    @Test
    fun everyCatalogLocationNameResolvesToAnId() {
        catalog.locationDefinitions.forEach { location ->
            assertNotNull(
                ContentIdentity.locationIdFor(location.names.es),
                "no content id for '${location.names.es}'",
            )
            location.names.en?.let { en ->
                assertNotNull(ContentIdentity.locationIdFor(en), "no content id for '$en'")
            }
        }
    }

    @Test
    fun everyPackLocationCarriesTheIdTheIdentityTableGivesIt() {
        catalog.locationDefinitions.forEach { location ->
            assertEquals(
                ContentIdentity.locationIdFor(location.names.es),
                location.id,
                location.names.es,
            )
        }
    }

    @Test
    fun theSameEntityUnderTwoLanguagesMergesIntoOne() {
        // "Lantern Hoard" (seed) and "Cúmulo de Linternas" (1.5) are one location, so the merged
        // catalog carries it once and the panel's option count collapses.
        assertEquals(1, catalog.locationDefinitions.count { it.id == LocationId("lantern_hoard") })
    }

    @Test
    fun theMergedLanternHoardCarriesBothLanguages() {
        val hoard = catalog.locationDefinitions.single { it.id == LocationId("lantern_hoard") }
        assertEquals(LocalizedText(es = "Cúmulo de Linternas", en = "Lantern Hoard"), hoard.names)
    }

    @Test
    fun everyCatalogInnovationNameResolvesToAnId() {
        catalog.innovationDefinitions.forEach { innovation ->
            assertNotNull(
                ContentIdentity.innovationIdFor(innovation.names.es),
                "no content id for '${innovation.names.es}'",
            )
            innovation.names.en?.let { en ->
                assertNotNull(ContentIdentity.innovationIdFor(en), "no content id for '$en'")
            }
        }
    }

    @Test
    fun everyPackInnovationCarriesTheIdTheIdentityTableGivesIt() {
        catalog.innovationDefinitions.forEach { innovation ->
            assertEquals(
                ContentIdentity.innovationIdFor(innovation.names.es),
                innovation.id,
                innovation.names.es,
            )
        }
    }

    @Test
    fun everyInnovationConsequenceResolvesToAnId() {
        // Consequences name innovations; a string the identity table does not cover would make
        // its card unreachable in the unlock tree.
        catalog.innovationDefinitions.flatMap { innovation ->
            innovation.consequences.map { consequence ->
                assertNotNull(
                    ContentIdentity.innovationIdFor(consequence),
                    "no content id for consequence '$consequence'",
                )
            }
        }
    }

    @Test
    fun languageAndLenguajeMergeIntoOneInnovationCarryingBothConsequences() {
        val language = catalog.innovationDefinitions.single { it.id == InnovationId("language") }

        assertEquals(LocalizedText(es = "Lenguaje", en = "Language"), language.names)
        val seed =
            Core16Content.pack.bundle.innovations
                .first { it.id == InnovationId("language") }
        val legacy =
            Legacy15Content.pack.bundle.innovations
                .first { it.id == InnovationId("language") }
        assertEquals((seed.consequences + legacy.consequences).distinct(), language.consequences)
    }
}

package io.kdmtracker.core.domain

import io.kdmtracker.core.content.resourceDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageTest {
    private val bone = ResourceId("bone.monster")
    private val organ = ResourceId("organ.monster")

    @Test
    fun gainsAndSpendsProjectToTheNetQuantity() {
        val entries =
            listOf(
                ResourceEntry(id = bone, delta = 3),
                ResourceEntry(id = bone, delta = -1),
            )

        assertEquals(
            listOf(StorageLine(id = bone, quantity = 2)),
            Storage.project(entries),
        )
    }

    @Test
    fun zeroQuantityLinesAreDropped() {
        val entries =
            listOf(
                ResourceEntry(id = bone, delta = 2),
                ResourceEntry(id = bone, delta = -2),
            )

        assertEquals(emptyList(), Storage.project(entries))
    }

    @Test
    fun twoVariantsOfOneMaterialAreSeparateLines() {
        val entries =
            listOf(
                ResourceEntry(id = ResourceId("hide.white_lion"), delta = 1),
                ResourceEntry(id = ResourceId("hide.monster"), delta = 2),
            )

        assertEquals(
            listOf(
                StorageLine(id = ResourceId("hide.monster"), quantity = 2),
                StorageLine(id = ResourceId("hide.white_lion"), quantity = 1),
            ),
            Storage.project(entries),
        )
    }

    @Test
    fun byMaterialSumsEveryVariantOfTheSameMaterialIntoOneBucket() {
        val entries =
            listOf(
                ResourceEntry(id = ResourceId("hide.white_lion"), delta = 1),
                ResourceEntry(id = ResourceId("hide.monster"), delta = 2),
                ResourceEntry(id = bone, delta = 5),
            )
        val definitions =
            listOf(
                resourceDefinition("hide.white_lion", ResourceCategory.MONSTER, ResourceMaterial.HIDE),
                resourceDefinition("hide.monster", ResourceCategory.BASIC, ResourceMaterial.HIDE),
                resourceDefinition("bone.monster", ResourceCategory.BASIC, ResourceMaterial.BONE),
            )

        assertEquals(
            mapOf(ResourceMaterial.HIDE to 3, ResourceMaterial.BONE to 5),
            Storage.byMaterial(entries, definitions),
        )
    }

    @Test
    fun byMaterialIgnoresResourcesThatFeedNoBucket() {
        val entries = listOf(ResourceEntry(id = ResourceId("herb.black_lichen"), delta = 4))
        val definitions =
            listOf(resourceDefinition("herb.black_lichen", ResourceCategory.SPECIAL, material = null))

        assertEquals(emptyMap(), Storage.byMaterial(entries, definitions))
    }

    @Test
    fun byMaterialIgnoresAnIdTheCatalogDoesNotCarry() {
        val entries = listOf(ResourceEntry(id = ResourceId("legacy.gone"), delta = 4))

        assertEquals(emptyMap(), Storage.byMaterial(entries, emptyList()))
    }

    @Test
    fun byMaterialDoesNotCountAWildcardInAnyBucket() {
        // The `???` card pays a bone, a hide or an organ. While it sits in storage it is committed
        // to none of them, so counting it in all three would make the four cells add up to more
        // stock than the settlement actually holds.
        val entries = listOf(ResourceEntry(id = ResourceId("unknown.monster_part"), delta = 2))
        val wildcard =
            ResourceDefinition(
                id = ResourceId("unknown.monster_part"),
                names = LocalizedText(es = "???"),
                category = ResourceCategory.BASIC,
                materials =
                    setOf(
                        ResourceMaterial.BONE,
                        ResourceMaterial.HIDE,
                        ResourceMaterial.ORGAN,
                    ),
            )

        assertEquals(emptyMap(), Storage.byMaterial(entries, listOf(wildcard)))
    }

    @Test
    fun quantityOfReturnsTheNetForOneId() {
        val entries =
            listOf(
                ResourceEntry(id = bone, delta = 3),
                ResourceEntry(id = organ, delta = 1),
            )

        assertEquals(3, Storage.quantityOf(entries, bone))
        assertEquals(0, Storage.quantityOf(entries, ResourceId("scrap")))
    }

    @Test
    fun anEmptyLedgerProjectsToEmptyStorage() {
        assertEquals(emptyList(), Storage.project(emptyList()))
        assertEquals(0, Storage.total(emptyList()))
    }

    @Test
    fun aNegativeNetIsKeptAndVisible() {
        val entries = listOf(ResourceEntry(id = bone, delta = -5))

        assertEquals(listOf(StorageLine(id = bone, quantity = -5)), Storage.project(entries))
    }

    @Test
    fun projectSortsByIdSoTheOutputIsDeterministic() {
        val entries =
            listOf(
                ResourceEntry(id = organ, delta = 1),
                ResourceEntry(id = bone, delta = 1),
                ResourceEntry(id = ResourceId("scrap"), delta = 1),
            )

        assertEquals(
            listOf(bone, organ, ResourceId("scrap")),
            Storage.project(entries).map { it.id },
        )
    }

    @Test
    fun totalSumsEveryNetQuantity() {
        val entries =
            listOf(
                ResourceEntry(id = bone, delta = 3),
                ResourceEntry(id = organ, delta = -1),
            )

        assertEquals(2, Storage.total(entries))
    }
}

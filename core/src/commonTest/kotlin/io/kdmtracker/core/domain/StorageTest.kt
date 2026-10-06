package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class StorageTest {
    @Test
    fun gainsAndSpendsProjectToTheNetQuantity() {
        val entries =
            listOf(
                ResourceEntry(name = "Bone", category = ResourceCategory.BASIC, delta = 3),
                ResourceEntry(name = "Bone", category = ResourceCategory.BASIC, delta = -1),
            )

        assertEquals(
            listOf(StorageLine(name = "Bone", category = ResourceCategory.BASIC, quantity = 2)),
            Storage.project(entries),
        )
    }

    @Test
    fun zeroQuantityLinesAreDropped() {
        val entries =
            listOf(
                ResourceEntry(name = "Bone", category = ResourceCategory.BASIC, delta = 2),
                ResourceEntry(name = "Bone", category = ResourceCategory.BASIC, delta = -2),
            )

        assertEquals(emptyList(), Storage.project(entries))
    }

    @Test
    fun theSameNameInDifferentCategoriesStaysSeparate() {
        val entries =
            listOf(
                ResourceEntry(name = "Fang", category = ResourceCategory.BASIC, delta = 2),
                ResourceEntry(name = "Fang", category = ResourceCategory.MONSTER, delta = 5),
            )

        assertEquals(
            listOf(
                StorageLine(name = "Fang", category = ResourceCategory.BASIC, quantity = 2),
                StorageLine(name = "Fang", category = ResourceCategory.MONSTER, quantity = 5),
            ),
            Storage.project(entries),
        )
    }

    @Test
    fun forCategoryReturnsOnlyThatCategory() {
        val entries =
            listOf(
                ResourceEntry(name = "Bone", category = ResourceCategory.BASIC, delta = 2),
                ResourceEntry(name = "Gizzard", category = ResourceCategory.MONSTER, delta = 1),
            )

        assertEquals(
            listOf(StorageLine(name = "Gizzard", category = ResourceCategory.MONSTER, quantity = 1)),
            Storage.forCategory(entries, ResourceCategory.MONSTER),
        )
    }

    @Test
    fun anEmptyLedgerProjectsToEmptyStorage() {
        assertEquals(emptyList(), Storage.project(emptyList()))
        assertEquals(0, Storage.total(emptyList()))
    }

    @Test
    fun aNegativeNetIsKeptAndVisible() {
        val entries = listOf(ResourceEntry(name = "Bone", category = ResourceCategory.BASIC, delta = -5))

        assertEquals(
            listOf(StorageLine(name = "Bone", category = ResourceCategory.BASIC, quantity = -5)),
            Storage.project(entries),
        )
    }

    @Test
    fun projectSortsByCategoryThenName() {
        val entries =
            listOf(
                ResourceEntry(name = "Hide", category = ResourceCategory.SPECIAL, delta = 1),
                ResourceEntry(name = "Organ", category = ResourceCategory.MONSTER, delta = 1),
                ResourceEntry(name = "Skull", category = ResourceCategory.BASIC, delta = 1),
                ResourceEntry(name = "Bone", category = ResourceCategory.BASIC, delta = 1),
            )

        assertEquals(
            listOf(
                StorageLine(name = "Bone", category = ResourceCategory.BASIC, quantity = 1),
                StorageLine(name = "Skull", category = ResourceCategory.BASIC, quantity = 1),
                StorageLine(name = "Hide", category = ResourceCategory.SPECIAL, quantity = 1),
                StorageLine(name = "Organ", category = ResourceCategory.MONSTER, quantity = 1),
            ),
            Storage.project(entries),
        )
    }

    @Test
    fun totalSumsEveryNetQuantity() {
        val entries =
            listOf(
                ResourceEntry(name = "Bone", category = ResourceCategory.BASIC, delta = 3),
                ResourceEntry(name = "Gizzard", category = ResourceCategory.MONSTER, delta = -1),
            )

        assertEquals(2, Storage.total(entries))
    }
}

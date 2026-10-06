package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class StorageTest {
    @Test
    fun gainsAndSpendsProjectToTheNetQuantity() {
        val entries =
            listOf(
                ResourceEntry(name = "Bone", category = "Bone", delta = 3),
                ResourceEntry(name = "Bone", category = "Bone", delta = -1),
            )

        assertEquals(
            listOf(StorageLine(name = "Bone", category = "Bone", quantity = 2)),
            Storage.project(entries),
        )
    }

    @Test
    fun zeroQuantityLinesAreDropped() {
        val entries =
            listOf(
                ResourceEntry(name = "Bone", category = "Bone", delta = 2),
                ResourceEntry(name = "Bone", category = "Bone", delta = -2),
            )

        assertEquals(emptyList(), Storage.project(entries))
    }

    @Test
    fun theSameNameInDifferentCategoriesStaysSeparate() {
        val entries =
            listOf(
                ResourceEntry(name = "Fang", category = "Bone", delta = 2),
                ResourceEntry(name = "Fang", category = "Other", delta = 5),
            )

        assertEquals(
            listOf(
                StorageLine(name = "Fang", category = "Bone", quantity = 2),
                StorageLine(name = "Fang", category = "Other", quantity = 5),
            ),
            Storage.project(entries),
        )
    }

    @Test
    fun forCategoryReturnsOnlyThatCategory() {
        val entries =
            listOf(
                ResourceEntry(name = "Bone", category = "Bone", delta = 2),
                ResourceEntry(name = "Gizzard", category = "Organ", delta = 1),
            )

        assertEquals(
            listOf(StorageLine(name = "Gizzard", category = "Organ", quantity = 1)),
            Storage.forCategory(entries, "Organ"),
        )
    }

    @Test
    fun anEmptyLedgerProjectsToEmptyStorage() {
        assertEquals(emptyList(), Storage.project(emptyList()))
        assertEquals(0, Storage.total(emptyList()))
    }

    @Test
    fun aNegativeNetIsKeptAndVisible() {
        val entries = listOf(ResourceEntry(name = "Bone", category = "Bone", delta = -5))

        assertEquals(
            listOf(StorageLine(name = "Bone", category = "Bone", quantity = -5)),
            Storage.project(entries),
        )
    }

    @Test
    fun projectSortsByCategoryThenName() {
        val entries =
            listOf(
                ResourceEntry(name = "Hide", category = "Other", delta = 1),
                ResourceEntry(name = "Organ", category = "Organ", delta = 1),
                ResourceEntry(name = "Skull", category = "Bone", delta = 1),
                ResourceEntry(name = "Bone", category = "Bone", delta = 1),
            )

        assertEquals(
            listOf(
                StorageLine(name = "Bone", category = "Bone", quantity = 1),
                StorageLine(name = "Skull", category = "Bone", quantity = 1),
                StorageLine(name = "Organ", category = "Organ", quantity = 1),
                StorageLine(name = "Hide", category = "Other", quantity = 1),
            ),
            Storage.project(entries),
        )
    }

    @Test
    fun totalSumsEveryNetQuantity() {
        val entries =
            listOf(
                ResourceEntry(name = "Bone", category = "Bone", delta = 3),
                ResourceEntry(name = "Gizzard", category = "Organ", delta = -1),
            )

        assertEquals(2, Storage.total(entries))
    }
}

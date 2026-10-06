package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettlementLocationTest {
    private val lanternHoard = SettlementLocation(name = "Lantern Hoard")
    private val boneSmith = SettlementLocation(name = "Bone Smith", requires = listOf("Lantern Hoard"))

    @Test
    fun locationWithoutPrerequisitesCanBeBuilt() {
        val set = LocationSet()

        assertTrue(set.canBuild(lanternHoard))
        assertEquals(listOf("Lantern Hoard"), set.with(lanternHoard).built)
    }

    @Test
    fun locationWithUnmetPrerequisiteCannotBeBuilt() {
        val set = LocationSet()

        assertFalse(set.canBuild(boneSmith))
        assertEquals(set, set.with(boneSmith))
    }

    @Test
    fun locationBuildsOnceItsPrerequisitesAreBuilt() {
        val set = LocationSet().with(lanternHoard)

        assertTrue(set.canBuild(boneSmith))
        assertEquals(listOf("Lantern Hoard", "Bone Smith"), set.with(boneSmith).built)
    }

    @Test
    fun buildingTwiceIsIdempotent() {
        val once = LocationSet().with(lanternHoard).with(boneSmith)

        assertEquals(once, once.with(lanternHoard))
        assertEquals(once, once.with(boneSmith))
    }

    @Test
    fun allPrerequisitesAreRequired() {
        val smithy = SettlementLocation(name = "Smithy", requires = listOf("Bone Smith", "Organ Grinder"))
        val partial = LocationSet().with(lanternHoard).with(boneSmith)

        assertFalse(partial.canBuild(smithy))
        assertEquals(partial, partial.with(smithy))
    }
}

package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettlementLocationTest {
    private val lanternHoard = SettlementLocation(LocationId("lantern_hoard"), LocalizedText(es = "Lantern Hoard"))
    private val boneSmith =
        SettlementLocation(
            LocationId("bone_smith"),
            LocalizedText(es = "Bone Smith"),
            requires = listOf(LocationId("lantern_hoard")),
        )

    @Test
    fun locationWithoutPrerequisitesCanBeBuilt() {
        val set = LocationSet()

        assertTrue(set.canBuild(lanternHoard))
        assertEquals(listOf(LocationId("lantern_hoard")), set.with(lanternHoard).built)
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
        assertEquals(listOf(LocationId("lantern_hoard"), LocationId("bone_smith")), set.with(boneSmith).built)
    }

    @Test
    fun buildingTwiceIsIdempotent() {
        val once = LocationSet().with(lanternHoard).with(boneSmith)

        assertEquals(once, once.with(lanternHoard))
        assertEquals(once, once.with(boneSmith))
    }

    @Test
    fun allPrerequisitesAreRequired() {
        val smithy =
            SettlementLocation(
                LocationId("smithy"),
                LocalizedText(es = "Smithy"),
                requires = listOf(LocationId("bone_smith"), LocationId("organ_grinder")),
            )
        val partial = LocationSet().with(lanternHoard).with(boneSmith)

        assertFalse(partial.canBuild(smithy))
        assertEquals(partial, partial.with(smithy))
    }
}

package io.kdmtracker.core.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackRelationsTest {
    private fun pack(id: String) =
        ContentPack(
            id = id,
            version = "1",
            name = id,
            requires =
                if (id ==
                    "base"
                ) {
                    emptyList()
                } else {
                    listOf("base")
                },
        )

    private fun registry(vararg relations: PackRelation) =
        PackRegistry(
            listOf(
                RegisteredPack(pack("base"), base = true),
                RegisteredPack(pack("a")),
                RegisteredPack(pack("b")),
                RegisteredPack(pack("c")),
            ),
            relations.toList(),
        )

    private val chartClash = PackRelation("a", "b", RelationKind.INCOMPATIBLE, RelationSource.CHART)
    private val officialClash = PackRelation("a", "b", RelationKind.INCOMPATIBLE, RelationSource.OFFICIAL)

    @Test
    fun anIncompatibilityNobodyConfirmedWarnsAndStillEnablesBoth() {
        val selection = registry(chartClash).select(listOf("a", "b"))

        assertEquals(PackSelection.Ok(listOf("base", "a", "b"), listOf(chartClash)), selection)
    }

    @Test
    fun anOfficialIncompatibilityIsRefused() {
        assertEquals(
            PackSelection.Blocked("content pack 'a' is incompatible with 'b'"),
            registry(officialClash).select(listOf("a", "b")),
        )
    }

    @Test
    fun theRefusalNamesThePacksInRegistryOrderWhicheverWayTheRelationIsWritten() {
        val reversed = PackRelation("b", "a", RelationKind.INCOMPATIBLE, RelationSource.OFFICIAL)

        assertEquals(
            PackSelection.Blocked("content pack 'a' is incompatible with 'b'"),
            registry(reversed).select(listOf("a", "b")),
        )
    }

    @Test
    fun aCompatibilityIssueWarnsEvenWhenOfficial() {
        val issue = PackRelation("a", "b", RelationKind.COMPATIBILITY_ISSUES, RelationSource.OFFICIAL)

        assertEquals(
            PackSelection.Ok(listOf("base", "a", "b"), listOf(issue)),
            registry(issue).select(listOf("a", "b")),
        )
    }

    @Test
    fun aRelationWithAPackThatIsNotEnabledIsIgnored() {
        assertEquals(PackSelection.Ok(listOf("base", "a")), registry(officialClash).select(listOf("a")))
    }

    @Test
    fun theSamePairWrittenInBothDirectionsWarnsOnce() {
        val reversed = PackRelation("b", "a", RelationKind.INCOMPATIBLE, RelationSource.CHART)

        val selection = registry(chartClash, reversed).select(listOf("a", "b"))

        assertTrue(selection is PackSelection.Ok)
        assertEquals(1, selection.warnings.size)
    }

    @Test
    fun withoutRelationsThereAreNoWarnings() {
        val selection = registry().select(listOf("a", "b"))

        assertTrue(selection is PackSelection.Ok)
        assertTrue(selection.warnings.isEmpty())
    }

    @Test
    fun aChangeWarnsOnlyAboutTheRelationsOfThePackItAdds() {
        val withC = PackRelation("c", "a", RelationKind.COMPATIBILITY_ISSUES, RelationSource.CHART)

        val change = registry(chartClash, withC).change(listOf("base", "a", "b"), listOf("base", "a", "b", "c"))

        assertEquals(PackSelection.Ok(listOf("base", "a", "b", "c"), listOf(withC)), change)
    }

    @Test
    fun aChangeRefusesAnOfficialIncompatibilityWithAPackTheCampaignHas() {
        assertEquals(
            PackSelection.Blocked("content pack 'a' is incompatible with 'b'"),
            registry(officialClash).change(listOf("base", "a"), listOf("base", "a", "b")),
        )
    }

    @Test
    fun aChangeLeavesAnExistingIncompatiblePairAlone() {
        val change = registry(officialClash).change(listOf("base", "a", "b"), listOf("base", "a", "b", "c"))

        assertEquals(PackSelection.Ok(listOf("base", "a", "b", "c")), change)
    }
}

package io.kdmtracker.core.content

import kotlin.test.Test
import kotlin.test.assertEquals

class NodeContestTest {
    private fun pack(
        id: String,
        vararg monsters: NodeMonster,
    ) = ContentPack(
        id = id,
        version = "1",
        name = id,
        requires = if (id == "base") emptyList() else listOf("base"),
        bundle = ContentBundle(nodeMonsters = monsters.toList()),
    )

    private val registry =
        PackRegistry(
            listOf(
                RegisteredPack(
                    pack(
                        "base",
                        NodeMonster("White Lion", MonsterNode.NQ1),
                        NodeMonster("The Butcher", MonsterNode.NN1),
                    ),
                    base = true,
                ),
                RegisteredPack(pack("gorm", NodeMonster("Gorm", MonsterNode.NQ1))),
                RegisteredPack(pack("flower", NodeMonster("Flower Knight", MonsterNode.NQ1))),
                RegisteredPack(pack("spider", NodeMonster("Spidicules", MonsterNode.NQ2))),
            ),
        )

    @Test
    fun anAddedMonsterOnATakenNodeContestsItWithTheNewOneAsDefault() {
        val contests = registry.contests(current = listOf("base"), wanted = listOf("base", "gorm"))

        assertEquals(listOf(NodeContest(MonsterNode.NQ1, listOf("White Lion", "Gorm"), "Gorm")), contests)
    }

    @Test
    fun anAddedMonsterAloneOnItsNodeContestsNothing() {
        assertEquals(emptyList(), registry.contests(listOf("base"), listOf("base", "spider")))
    }

    @Test
    fun aNodeTheCampaignAlreadyContestedIsOfferedAgainWhenAnotherPackJoinsIt() {
        val contests = registry.contests(listOf("base", "gorm"), listOf("base", "gorm", "flower"))

        assertEquals(
            listOf(NodeContest(MonsterNode.NQ1, listOf("White Lion", "Gorm", "Flower Knight"), "Flower Knight")),
            contests,
        )
    }

    @Test
    fun withNothingAddedThereIsNothingToChoose() {
        assertEquals(emptyList(), registry.contests(listOf("base", "gorm"), listOf("base", "gorm")))
    }

    @Test
    fun choosingTwoNewPacksAtOnceListsEachContestedNodeOnce() {
        val contests = registry.contests(listOf("base"), listOf("base", "gorm", "flower"))

        assertEquals(
            listOf(NodeContest(MonsterNode.NQ1, listOf("White Lion", "Gorm", "Flower Knight"), "Flower Knight")),
            contests,
        )
    }
}

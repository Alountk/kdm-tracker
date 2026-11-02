package io.kdmtracker.core.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeOccupancyTest {
    private fun pack(
        id: String,
        vararg monsters: NodeMonster,
    ) = ContentPack(id = id, version = "1", name = id, bundle = ContentBundle(nodeMonsters = monsters.toList()))

    private val lion = NodeMonster("White Lion", MonsterNode.NQ1)
    private val gorm = NodeMonster("Gorm", MonsterNode.NQ1)
    private val butcher = NodeMonster("The Butcher", MonsterNode.NN1)
    private val base = pack("base", lion, butcher)
    private val gormPack = pack("gorm", gorm)

    private fun catalog(
        packs: List<ContentPack>,
        choices: Map<MonsterNode, String> = emptyMap(),
    ) = ContentCatalog(packs, choices)

    @Test
    fun aMonsterAloneOnItsNodeIsListedAccordingToTheKindOfNode() {
        val catalog = catalog(listOf(base))

        assertEquals(listOf("White Lion"), catalog.quarries)
        assertEquals(listOf("The Butcher"), catalog.nemeses)
    }

    @Test
    fun theCoreFoeAndTheFinaleAreNemeses() {
        val core =
            pack("core", NodeMonster("The Watcher", MonsterNode.CO), NodeMonster("Golden Smoke Knight", MonsterNode.FI))

        assertEquals(listOf("Golden Smoke Knight", "The Watcher"), catalog(listOf(core)).nemeses)
    }

    @Test
    fun anExpansionMonsterReplacesTheOneOnItsNodeByDefault() {
        val catalog = catalog(listOf(base, gormPack))

        assertEquals(listOf("Gorm"), catalog.quarries)
        assertEquals(mapOf(MonsterNode.NQ1 to "Gorm", MonsterNode.NN1 to "The Butcher"), catalog.occupants)
    }

    @Test
    fun theStoredChoiceWinsOverTheDefault() {
        val catalog = catalog(listOf(base, gormPack), mapOf(MonsterNode.NQ1 to "White Lion"))

        assertEquals(listOf("White Lion"), catalog.quarries)
    }

    @Test
    fun aChoiceNamingAMonsterThatIsNotOnTheNodeIsIgnored() {
        val catalog = catalog(listOf(base, gormPack), mapOf(MonsterNode.NQ1 to "Phoenix"))

        assertEquals(listOf("Gorm"), catalog.quarries)
    }

    @Test
    fun theOccupantDoesNotDependOnTheOrderOfTheChoiceMap() {
        val choices = mapOf(MonsterNode.NN1 to "The Butcher", MonsterNode.NQ1 to "White Lion")

        assertEquals(
            catalog(listOf(base, gormPack), choices).occupants,
            catalog(listOf(base, gormPack), choices.entries.reversed().associate { it.toPair() }).occupants,
        )
    }

    @Test
    fun theMonsterNamedByAListAndNotByANodeIsStillListed() {
        val plain = ContentPack("plain", "1", "plain", bundle = ContentBundle(quarries = listOf("Dragon")))

        val catalog = catalog(listOf(base, plain))

        assertEquals(listOf("Dragon", "White Lion"), catalog.quarries)
        assertTrue(catalog.occupants.keys.none { it == MonsterNode.NQ2 })
    }

    @Test
    fun theCoreSeedDeclaresItsEightMonstersOnTheirNodes() {
        val seed = ContentCatalog(listOf(Core16Content.pack))

        assertEquals(
            mapOf(
                MonsterNode.NQ1 to "White Lion",
                MonsterNode.NQ2 to "Screaming Antelope",
                MonsterNode.NQ3 to "Phoenix",
                MonsterNode.NN1 to "The Butcher",
                MonsterNode.NN2 to "King's Man",
                MonsterNode.NN3 to "The Hand",
                MonsterNode.CO to "The Watcher",
                MonsterNode.FI to "Golden Smoke Knight",
            ),
            seed.occupants,
        )
    }
}

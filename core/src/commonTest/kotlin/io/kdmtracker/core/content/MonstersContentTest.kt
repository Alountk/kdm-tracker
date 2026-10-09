package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15Monsters
import io.kdmtracker.core.domain.MonsterCardKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonstersContentTest {
    private val monsterCards = Legacy15Content.pack.bundle.monsterCards

    @Test
    fun monsterCardsAreNotEmpty() {
        assertTrue(monsterCards.isNotEmpty(), "expected generated 1.5 monster cards")
    }

    @Test
    fun whiteLionHasAiAndHitLocations() {
        val lion = monsterCards.filter { it.monster == "White Lion" }
        assertTrue(lion.any { it.kind == MonsterCardKind.AI }, "expected AI cards")
        assertTrue(lion.any { it.kind == MonsterCardKind.HIT_LOCATION }, "expected hit locations")
    }

    @Test
    fun packCarriesTheGeneratedMonsterCards() {
        assertEquals(Core15Monsters.bundle.monsterCards, monsterCards)
    }
}

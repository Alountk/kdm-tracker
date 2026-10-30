package io.kdmtracker.app.ui.showdown

import io.kdmtracker.core.domain.MonsterCard
import io.kdmtracker.core.domain.MonsterCardKind
import io.kdmtracker.core.domain.ShowdownState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShowdownBoardModelTest {
    private val claw =
        MonsterCard(
            name = "Zarpazo",
            text =
                "Selección de Objetivo amenaza más cercana, encarada, a alcance sin objetivo: olfateo " +
                    "Mover & Atacar Objetivo Velocidad Precisión Daño Activación 2 2+ 1 Tras Dañar",
            kind = MonsterCardKind.AI,
            monster = "White Lion",
            rank = "B",
        )
    private val trait =
        MonsterCard(
            name = "Golpe Ceniciento",
            text = "Ejecuta Ennegrecer.",
            kind = MonsterCardKind.AI,
            monster = "White Lion",
            rank = "A",
        )
    private val reflex =
        MonsterCard(
            name = "Cola de la Bestia",
            text = "Reflejo Haz un movimiento completo del monstruo adelante en línea recta.",
            kind = MonsterCardKind.HIT_LOCATION,
            monster = "White Lion",
        )
    private val plain =
        MonsterCard(
            name = "Bajo Vientre",
            text = "Un golpe debilitante en el bajo vientre. Herida Crítica Ganas un recurso básico.",
            kind = MonsterCardKind.HIT_LOCATION,
            monster = "White Lion",
        )
    private val otherMonster =
        MonsterCard("Cuerno", "Fallo Otro monstruo.", MonsterCardKind.HIT_LOCATION, "Phoenix")
    private val cards = listOf(claw, trait, reflex, plain, otherMonster)

    private fun showdown(
        aiDeck: List<String> = listOf("Zarpazo", "Golpe Ceniciento"),
        discard: List<String> = emptyList(),
        wounds: List<String> = emptyList(),
        lastDrawn: String? = null,
    ) = ShowdownState(
        monster = "White Lion",
        level = 2,
        aiDeck = aiDeck,
        discard = discard,
        wounds = wounds,
        lastDrawn = lastDrawn,
    )

    @Test
    fun healthCountsTheCardsNotYetWounded() {
        val model =
            ShowdownBoardModel.of(
                showdown(aiDeck = listOf("A"), discard = listOf("B", "C"), wounds = listOf("D")),
                cards,
            )

        assertEquals(3, model.health)
        assertEquals(4, model.deckSize)
        assertEquals(1, model.wounds)
    }

    @Test
    fun theHeaderCarriesTheMonsterAndItsLevel() {
        val model = ShowdownBoardModel.of(showdown(), cards)

        assertEquals("White Lion", model.monster)
        assertEquals(2, model.level)
    }

    @Test
    fun noCardIsShownBeforeTheFirstDraw() {
        assertNull(ShowdownBoardModel.of(showdown(), cards).drawn)
    }

    @Test
    fun theDrawnCardReadsItsStatsFromTheCardText() {
        val drawn = ShowdownBoardModel.of(showdown(lastDrawn = "Zarpazo"), cards).drawn

        assertEquals("Zarpazo", drawn?.name)
        assertEquals("B", drawn?.rank)
        assertEquals(CardStats(speed = 2, accuracy = "2+", damage = 1), drawn?.stats)
    }

    @Test
    fun theTextDropsTheStatsBlockItAlreadyShowsAsNumbers() {
        val drawn = ShowdownBoardModel.of(showdown(lastDrawn = "Zarpazo"), cards).drawn

        assertEquals(
            "Selección de Objetivo amenaza más cercana, encarada, a alcance sin objetivo: olfateo Tras Dañar",
            drawn?.text,
        )
    }

    @Test
    fun aCardWithoutAMoveAndAttackBlockHasNoStats() {
        val drawn = ShowdownBoardModel.of(showdown(lastDrawn = "Golpe Ceniciento"), cards).drawn

        assertNull(drawn?.stats)
        assertEquals("Ejecuta Ennegrecer.", drawn?.text)
    }

    @Test
    fun aDrawnNameMissingFromTheCatalogStillShowsItsName() {
        val drawn = ShowdownBoardModel.of(showdown(lastDrawn = "Carta Perdida"), cards).drawn

        assertEquals("Carta Perdida", drawn?.name)
        assertEquals("", drawn?.text)
        assertNull(drawn?.rank)
    }

    @Test
    fun hitLocationsAreTheMonstersOwnAndFlagReactions() {
        val locations = ShowdownBoardModel.of(showdown(), cards).hitLocations

        assertEquals(listOf("Cola de la Bestia", "Bajo Vientre"), locations.map { it.name })
        assertTrue(locations[0].reaction)
        assertFalse(locations[1].reaction)
    }
}

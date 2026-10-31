package io.kdmtracker.app

import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.domain.WoundState
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Plays the first lantern year through the real controller and checks, step by step, what the
 * rulebook's settlement-phase table states (KDM 1.5 ES, "Fase de Asentamiento", pp. 34-35 and
 * 81-85): returning survivors heal, each returning survivor earns an endeavor, the timeline moves
 * the year on, deaths are counted, unspent endeavors are lost at the end, and the year closes with
 * the hunt and the showdown.
 */
class SettlementYearRulesFlowTest {
    private val controller = LanternYearController(SettlementRepository(InMemoryCampaignStore()))

    private val state get() = controller.state.value

    private fun survivor(id: Ulid) = state.survivors.single { it.id == id }

    private fun advanceTo(step: Int) {
        while (state.currentStep < step) controller.flow.advance()
    }

    @Test
    fun theFirstLanternYearPlaysTheElevenStepsInRulebookOrder() {
        // Step 1, Preparación: the phase opens on the settlement board with an empty pool.
        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
        assertEquals(1, state.currentStep)
        assertEquals(1, state.lanternYear)
        assertEquals(0, state.endeavors)

        // Step 2, Supervivientes Regresan: injuries heal and tokens clear; survival and insanity stay.
        val hurt = state.survivors.first()
        controller.survivors.updateSurvivor(
            hurt.copy(
                wounds = mapOf(HitLocation.HEAD to WoundState(light = true, heavy = true)),
                tokens = setOf("bleeding"),
                survival = 3,
                insanity = 2,
            ),
        )
        advanceTo(2)
        controller.survivors.healSurvivors()
        val healed = survivor(hurt.id)
        assertTrue(healed.wounds.values.none { it.light || it.heavy }, "light and heavy injuries heal")
        assertTrue(healed.tokens.isEmpty(), "temporary tokens clear")
        assertEquals(3, healed.survival, "survival is kept")
        assertEquals(2, healed.insanity, "insanity is kept")

        // Step 3, Ganar Esfuerzos: each returning survivor earns one endeavor, once per phase.
        advanceTo(3)
        assertEquals(4, state.endeavors)
        controller.flow.previous()
        controller.flow.selectStep(3)
        assertEquals(4, state.endeavors, "revisiting the step does not earn them twice")

        // Step 4, Línea de Tiempo: leaving it moves the lantern year on by one.
        advanceTo(4)
        assertEquals(1, state.lanternYear)
        advanceTo(5)
        assertEquals(2, state.lanternYear)

        // Step 5, Contador de Muertes: the death is counted and the population shrinks.
        controller.settlement.recordDeaths(1)
        assertEquals(1, state.deaths)
        assertEquals(3, state.population)

        // Step 8, Preparar Partida: the living survivors depart (at most four).
        advanceTo(8)
        val party = state.survivors.filter { it.status != SurvivorStatus.DEAD }.map { it.id }
        controller.survivors.markDeparting(party)
        assertEquals(
            party.toSet(),
            state.survivors
                .filter { it.status == SurvivorStatus.DEPARTING }
                .map { it.id }
                .toSet(),
        )

        // Step 11, Fin de Fase: the endeavors nobody spent are lost as the phase commits to the hunt.
        advanceTo(11)
        assertEquals(4, state.endeavors)
        controller.flow.advance()
        assertEquals(SettlementPhase.HUNT, state.phase)
        assertEquals(0, state.endeavors)

        // The hunt ends on the quarry, the showdown is won and the next settlement phase opens.
        controller.hunt.setUpHunt("White Lion", 1, party.size)
        controller.flow.finishHunt(HuntStatus.QUARRY_FOUND)
        assertEquals(SettlementPhase.SHOWDOWN, state.phase)
        controller.showdown.setUpShowdown("White Lion", 1, listOf("A", "B"))
        controller.flow.finishShowdown(ShowdownStatus.VICTORY)
        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
        assertEquals(1, state.currentStep)
        assertEquals(2, state.lanternYear, "the year only moves on at the timeline step")
        assertEquals(0, state.endeavors)
    }

    @Test
    fun walkingBackAndForthInsideTheYearNeverEarnsOrCountsTwice() {
        advanceTo(5)
        assertEquals(4, state.endeavors)
        assertEquals(2, state.lanternYear)

        // Births at step 5 enlarge the population after the endeavors were earned.
        controller.settlement.recordBirths(2)
        repeat(4) { controller.flow.previous() }
        assertEquals(1, state.currentStep)
        advanceTo(5)

        assertEquals(6, state.population)
        assertEquals(4, state.endeavors, "the earlier survivors count, not the newborns, and only once")
        assertEquals(2, state.lanternYear, "crossing the timeline step again keeps the year")
    }

    @Test
    fun aWipedPartyEarnsNoEndeavorsBecauseNobodyReturnsFromTheHunt() {
        advanceTo(11)
        controller.chooseTheDepartingParty()
        controller.flow.advance()
        controller.hunt.setUpHunt("White Lion", 1, 4)

        controller.flow.finishHunt(HuntStatus.FAILED)

        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
        assertEquals(4, state.deaths, "the party died with the hunt")
        assertEquals(0, state.population)
        advanceTo(3)
        assertEquals(0, state.endeavors)
    }

    @Test
    fun theSurvivorsWhoStayedHomeStillEarnTheirEndeavorWhenThePartyIsWiped() {
        advanceTo(8)
        controller.survivors.markDeparting(state.survivors.take(2).map { it.id })
        advanceTo(11)
        controller.flow.advance()
        controller.hunt.setUpHunt("White Lion", 1, 2)

        controller.flow.finishHunt(HuntStatus.FAILED)

        assertEquals(2, state.deaths)
        assertEquals(2, state.population)
        advanceTo(3)
        assertEquals(2, state.endeavors)
    }
}

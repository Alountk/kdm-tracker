package io.kdmtracker.app

import io.kdmtracker.app.ui.addToDeparting
import io.kdmtracker.app.ui.departingIds
import io.kdmtracker.app.ui.departingLimit
import io.kdmtracker.app.ui.emptySlotCount
import io.kdmtracker.app.ui.gearGridLabel
import io.kdmtracker.app.ui.swapDepartingSlot
import io.kdmtracker.app.ui.swapPickerCandidates
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * S8: the step-8 body's pure derivations — the party limit, the departing slot list, the swap
 * picker's candidate list, the swap/add list algebra, the empty-slot count and the gear-grid count
 * label. Verbatim design copy is pinned separately in `SettlementStepBodiesTest`; these tests only
 * pin the derivations' behavior.
 */
class DepartingSlotsModelTest {
    private fun survivor(
        seed: Long,
        name: String,
        status: SurvivorStatus = SurvivorStatus.ALIVE,
    ): Survivor =
        Survivor(
            id = Ulid.generate(seed) { 0 },
            name = name,
            status = status,
        )

    // -- the party limit: population capped at four --

    @Test
    fun thePartyLimitIsThePopulationCappedAtFour() {
        assertEquals(4, departingLimit(12))
        assertEquals(4, departingLimit(4))
    }

    @Test
    fun aPopulationBelowFourKeepsThePartyAtThePopulation() {
        assertEquals(3, departingLimit(3))
        assertEquals(1, departingLimit(1))
        assertEquals(0, departingLimit(0))
    }

    // -- the departing slot list: roster order, departing only --

    @Test
    fun departingIdsFollowRosterOrderAndSkipTheOthers() {
        val alive = survivor(1, "Erza")
        val first = survivor(2, "Marko", SurvivorStatus.DEPARTING)
        val dead = survivor(3, "Erlinda", SurvivorStatus.DEAD)
        val second = survivor(4, "Albar", SurvivorStatus.DEPARTING)
        assertEquals(listOf(first.id, second.id), departingIds(listOf(alive, first, dead, second)))
    }

    @Test
    fun anEmptyRosterYieldsNoDepartingIds() {
        assertEquals(emptyList(), departingIds(emptyList()))
    }

    // -- the swap picker's candidates: alive, never dead or already departing --

    @Test
    fun swapPickerCandidatesExcludeTheDeadAndTheAlreadyDeparting() {
        val alive = survivor(1, "Erza")
        val departing = survivor(2, "Marko", SurvivorStatus.DEPARTING)
        val dead = survivor(3, "Erlinda", SurvivorStatus.DEAD)
        assertEquals(listOf(alive), swapPickerCandidates(listOf(alive, departing, dead)))
    }

    @Test
    fun swapPickerCandidatesKeepRosterOrderAndIncludeEveryAliveSurvivor() {
        val first = survivor(1, "Erza")
        val second = survivor(2, "Marko")
        assertEquals(listOf(first, second), swapPickerCandidates(listOf(first, second)))
    }

    // -- swap/add: the new full departing list, order preserved --

    @Test
    fun swappingReplacesTheSlotInPlacePreservingOrder() {
        val a = Ulid.generate(1) { 0 }
        val b = Ulid.generate(2) { 0 }
        val c = Ulid.generate(3) { 0 }
        val x = Ulid.generate(9) { 0 }
        assertEquals(listOf(a, x, c), swapDepartingSlot(listOf(a, b, c), 1, x))
    }

    @Test
    fun swappingAnUnknownSlotLeavesTheListUnchanged() {
        val a = Ulid.generate(1) { 0 }
        val x = Ulid.generate(9) { 0 }
        assertEquals(listOf(a), swapDepartingSlot(listOf(a), 4, x))
    }

    @Test
    fun addingAppendsTheSurvivorAfterTheCurrentSlots() {
        val a = Ulid.generate(1) { 0 }
        val b = Ulid.generate(2) { 0 }
        val x = Ulid.generate(9) { 0 }
        assertEquals(listOf(a, b, x), addToDeparting(listOf(a, b), x))
        assertEquals(listOf(x), addToDeparting(emptyList(), x))
    }

    // -- the derived empty-slot cards --

    @Test
    fun emptySlotCountFillsThePartyUpToTheLimit() {
        assertEquals(0, emptySlotCount(4, 4))
        assertEquals(3, emptySlotCount(4, 1))
        assertEquals(2, emptySlotCount(2, 0))
    }

    @Test
    fun emptySlotCountNeverGoesNegative() {
        assertEquals(0, emptySlotCount(3, 4))
    }

    // -- the gear strip's count label --

    @Test
    fun theGearGridLabelCountsAssignedGearAgainstNine() {
        assertEquals("Cuadrícula 2/9", gearGridLabel(listOf("Tela", "Piedra de Fundador")))
        assertEquals("Cuadrícula 0/9", gearGridLabel(emptyList()))
    }
}

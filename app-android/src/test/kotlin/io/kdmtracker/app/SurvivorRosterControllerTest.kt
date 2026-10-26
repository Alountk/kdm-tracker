package io.kdmtracker.app

import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.domain.WoundState
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the controller's survivor-roster views and actions: the seeded roster, healing, departing
 * selection (with the cap), gear assignment, and the roster-aware death/birth recording.
 */
class SurvivorRosterControllerTest {
    private fun controller(
        repository: SettlementRepository = SettlementRepository(InMemoryCampaignStore()),
    ): LanternYearController {
        var n = 0L
        return LanternYearController(repository, newSurvivorId = { Ulid.generate(++n) { n.toInt() } })
    }

    @Test
    fun exposesTheSeededRoster() {
        val controller = controller()

        assertEquals(4, controller.state.value.survivors.size)
    }

    @Test
    fun healingSurvivorsClearsWoundsAndTokens() {
        val repository = SettlementRepository(InMemoryCampaignStore())
        val wounded =
            repository.record.state.survivors.first().copy(
                wounds = mapOf(HitLocation.HEAD to WoundState(heavy = true)),
                tokens = setOf("bleeding"),
            )
        repository.update(
            repository.record.copy(
                state =
                    repository.record.state.copy(
                        survivors =
                            listOf(wounded) +
                                repository.record.state.survivors
                                    .drop(1),
                    ),
            ),
        )
        val controller = controller(repository)

        controller.survivors.healSurvivors()

        val healed =
            controller.state.value.survivors
                .first { it.id == wounded.id }
        assertFalse(healed.wounds.getValue(HitLocation.HEAD).heavy)
        assertTrue(healed.tokens.isEmpty())
    }

    @Test
    fun markingDepartingUpdatesWhoDeparts() {
        val controller = controller()
        val ids =
            controller.state.value.survivors
                .take(2)
                .map { it.id }

        controller.survivors.markDeparting(ids)

        assertEquals(
            2,
            controller.state.value.survivors
                .count { it.status == SurvivorStatus.DEPARTING },
        )
    }

    @Test
    fun markingMoreThanFourIsBlocked() {
        val controller = controller()
        controller.settlement.recordBirths(1)
        val ids =
            controller.state.value.survivors
                .filter { it.status != SurvivorStatus.DEAD }
                .map { it.id }

        controller.survivors.markDeparting(ids)

        assertNotNull(controller.state.value.actionError)
        assertTrue(
            controller.state.value.survivors
                .none { it.status == SurvivorStatus.DEPARTING },
        )
    }

    @Test
    fun assigningGearPersists() {
        val controller = controller()
        val id =
            controller.state.value.survivors
                .first()
                .id

        controller.survivors.assignGear(id, "Hacha")

        assertTrue(
            controller.state.value.survivors
                .first { it.id == id }
                .gear
                .contains("Hacha"),
        )
    }

    @Test
    fun recordingADeathShrinksTheRoster() {
        val controller = controller()

        controller.settlement.recordDeaths(1)

        assertEquals(
            3,
            controller.state.value.survivors
                .count { it.status != SurvivorStatus.DEAD },
        )
    }

    @Test
    fun recordingABirthGrowsTheRoster() {
        val controller = controller()

        controller.settlement.recordBirths(1)

        assertEquals(
            5,
            controller.state.value.survivors
                .count { it.status != SurvivorStatus.DEAD },
        )
    }
}

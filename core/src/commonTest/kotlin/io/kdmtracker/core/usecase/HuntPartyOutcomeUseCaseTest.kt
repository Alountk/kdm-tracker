package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HuntPartyOutcomeUseCaseTest {
    private val survivorId = Ulid.generate(1L) { 0 }

    private fun record(players: Int = 3): SettlementRecord {
        val base =
            SettlementRecord(
                settlement =
                    Settlement(
                        id = Ulid.generate(90L) { 0 },
                        campaignId = Ulid.generate(91L) { 0 },
                        name = "Kingsport",
                        lanternYear = 3,
                        phase = SettlementPhase.HUNT,
                        currentStep = 1,
                        endeavors = 0,
                        population = 4,
                        settlementEvent = null,
                        sync = SyncMeta(HybridLogicalClock(0, 0)),
                    ),
                state = SettlementState(survivors = listOf(Survivor(id = survivorId))),
            )
        return assertIs<StepResult.Ok<SettlementRecord>>(SetUpHunt.run(base, "White Lion", 1, players)).value
    }

    @Test
    fun assignPlayerIsBoundedToThePlayerCount() {
        val ok = assertIs<StepResult.Ok<SettlementRecord>>(AssignPlayer.run(record(players = 3), survivorId, 2))
        assertEquals(2, ok.value.state.hunt.assignments[survivorId])

        assertIs<StepResult.Blocked>(AssignPlayer.run(record(players = 3), survivorId, 4))
        assertIs<StepResult.Blocked>(AssignPlayer.run(record(players = 3), survivorId, 0))
    }

    @Test
    fun assignPlayerRejectsAnUnknownSurvivor() {
        val result = AssignPlayer.run(record(), Ulid.generate(7L) { 0 }, 1)

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun assignHuntEventAppendsToASpaceAndResolveMarksIt() {
        val assigned =
            assertIs<StepResult.Ok<SettlementRecord>>(AssignHuntEvent.run(record(), index = 2, "Evento Aleatorio"))
        assertTrue(
            "Evento Aleatorio" in
                assigned.value.state.hunt.spaces[2]
                    .events,
        )

        val resolved =
            assertIs<StepResult.Ok<SettlementRecord>>(
                ResolveHuntEvent.run(assigned.value, index = 2, "Evento Aleatorio"),
            )
        assertTrue(
            "Evento Aleatorio" in
                resolved.value.state.hunt.spaces[2]
                    .resolved,
        )
    }

    @Test
    fun resolveRejectsAnEventThatWasNotAssigned() {
        val result = ResolveHuntEvent.run(record(), index = 2, "Nope")

        assertIs<StepResult.Blocked>(result)
    }

    @Test
    fun setHuntOutcomeStoresATerminalStatusOnly() {
        val ok = assertIs<StepResult.Ok<SettlementRecord>>(SetHuntOutcome.run(record(), HuntStatus.QUARRY_FOUND))
        assertEquals(HuntStatus.QUARRY_FOUND, ok.value.state.hunt.status)

        assertIs<StepResult.Blocked>(SetHuntOutcome.run(record(), HuntStatus.ACTIVE))
    }
}

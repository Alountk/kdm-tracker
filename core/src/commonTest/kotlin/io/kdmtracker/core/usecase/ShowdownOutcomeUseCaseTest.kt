package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ShowdownOutcomeUseCaseTest {
    private val first = Ulid.generate(1L) { 0 }
    private val second = Ulid.generate(2L) { 0 }

    private fun record(): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(90L) { 0 },
                    campaignId = Ulid.generate(91L) { 0 },
                    name = "Kingsport",
                    lanternYear = 3,
                    phase = SettlementPhase.SHOWDOWN,
                    currentStep = 1,
                    endeavors = 0,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state = SettlementState(survivors = listOf(Survivor(id = first, huntXp = 2), Survivor(id = second))),
        )

    @Test
    fun setShowdownOutcomeStoresTerminalStatusesOnly() {
        val ok = assertIs<StepResult.Ok<SettlementRecord>>(SetShowdownOutcome.run(record(), ShowdownStatus.VICTORY))
        assertEquals(ShowdownStatus.VICTORY, ok.value.state.showdown.status)

        assertIs<StepResult.Blocked>(SetShowdownOutcome.run(record(), ShowdownStatus.ACTIVE))
    }

    @Test
    fun setShowdownOutcomeRejectsAReEntry() {
        val finished =
            assertIs<StepResult.Ok<SettlementRecord>>(
                SetShowdownOutcome.run(record(), ShowdownStatus.VICTORY),
            ).value

        assertIs<StepResult.Blocked>(SetShowdownOutcome.run(finished, ShowdownStatus.DEFEAT))
    }

    @Test
    fun recordHuntXpGrantsOneToEachListedSurvivor() {
        val ok = assertIs<StepResult.Ok<SettlementRecord>>(RecordHuntXp.run(record(), listOf(first)))

        val byId =
            ok.value.state.survivors
                .associateBy { it.id }
        assertEquals(3, byId.getValue(first).huntXp)
        assertEquals(0, byId.getValue(second).huntXp)
    }

    @Test
    fun recordHuntXpRejectsUnknownIds() {
        assertIs<StepResult.Blocked>(RecordHuntXp.run(record(), listOf(Ulid.generate(9L) { 0 })))
    }
}

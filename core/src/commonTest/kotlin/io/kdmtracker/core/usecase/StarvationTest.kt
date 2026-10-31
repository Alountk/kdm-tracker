package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.domain.storage
import io.kdmtracker.core.rules.StepResult
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Starvation (rulebook "Fase de Caza", pp. 61-63): the settlement loses a d5 of its stored resources.
 * The roll is owed; the player pays it by choosing, one unit at a time, what to discard.
 */
class StarvationTest {
    private val bone = ResourceId("bone.monster")
    private val hide = ResourceId("hide.monster")

    private fun settlementWith(vararg stored: Pair<ResourceId, Int>): SettlementRecord {
        val record =
            SettlementRecord(
                settlement =
                    Settlement(
                        id = Ulid.generate(201L) { 0 },
                        campaignId = Ulid.generate(202L) { 0 },
                        name = "Kingsport",
                        lanternYear = 1,
                        phase = SettlementPhase.HUNT,
                        currentStep = 1,
                        endeavors = 0,
                        population = 4,
                        settlementEvent = null,
                        sync = SyncMeta(HybridLogicalClock(0, 0)),
                    ),
            )
        return record.copy(
            state = record.state.copy(resourceEntries = stored.map { (id, quantity) -> ResourceEntry(id, quantity) }),
        )
    }

    private fun ok(result: StepResult<SettlementRecord>) = assertIs<StepResult.Ok<SettlementRecord>>(result).value

    @Test
    fun theRollIsOwedInFull() {
        val starved = ok(Starve.run(settlementWith(bone to 10), roll = 4))

        assertEquals(4, starved.state.starvationOwed)
        assertEquals(10, starved.state.storage().sumOf { it.quantity }, "nothing is discarded on its own")
    }

    @Test
    fun aSmallStorageOnlyOwesWhatItHolds() {
        val starved = ok(Starve.run(settlementWith(bone to 2), roll = 5))

        assertEquals(2, starved.state.starvationOwed)
    }

    @Test
    fun anEmptyStorageOwesNothing() {
        assertEquals(0, ok(Starve.run(settlementWith(), roll = 3)).state.starvationOwed)
    }

    @Test
    fun aRollOutsideOneToFiveIsRefused() {
        assertIs<StepResult.Blocked>(Starve.run(settlementWith(bone to 9), roll = 0))
        assertIs<StepResult.Blocked>(Starve.run(settlementWith(bone to 9), roll = 6))
    }

    @Test
    fun theDiceAlwaysRollOneToFive() {
        val random = Random(7)
        repeat(200) { assertTrue(Starve.roll(random) in 1..5) }
    }

    @Test
    fun aSecondStarvationAddsToWhatIsStillOwed() {
        val once = ok(Starve.run(settlementWith(bone to 10), roll = 3))

        val twice = ok(Starve.run(once, roll = 4))

        assertEquals(7, twice.state.starvationOwed)
    }

    @Test
    fun theOwedAmountNeverExceedsWhatTheStorageStillHolds() {
        val once = ok(Starve.run(settlementWith(bone to 4), roll = 3))

        val twice = ok(Starve.run(once, roll = 5))

        assertEquals(4, twice.state.starvationOwed)
    }

    @Test
    fun thePlayerPaysOneChosenUnitAtATime() {
        val owed = ok(Starve.run(settlementWith(bone to 3, hide to 3), roll = 2))

        val first = ok(DiscardForStarvation.run(owed, hide))
        val second = ok(DiscardForStarvation.run(first, bone))

        assertEquals(0, second.state.starvationOwed)
        assertEquals(listOf(bone to 2, hide to 2), second.state.storage().map { it.id to it.quantity })
    }

    @Test
    fun nothingIsDiscardedWhenNothingIsOwed() {
        val record = settlementWith(bone to 3)

        assertEquals(StepResult.Blocked("no starvation owed"), DiscardForStarvation.run(record, bone))
    }

    @Test
    fun aResourceTheStorageLacksCannotPayAndLeavesTheDebtUntouched() {
        val owed = ok(Starve.run(settlementWith(bone to 3), roll = 2))

        assertIs<StepResult.Blocked>(DiscardForStarvation.run(owed, hide))
        assertEquals(2, owed.state.starvationOwed)
    }
}

package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import io.kdmtracker.core.ruleset.Core16Ruleset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EndeavorAndLedgerUseCaseTest {
    private fun record(endeavors: Int = 0): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(1L) { 0 },
                    campaignId = Ulid.generate(2L) { 0 },
                    name = "Kingsport",
                    lanternYear = 1,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 1,
                    endeavors = endeavors,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
        )

    @Test
    fun gainingEndeavorsAddsTheReturneeBreakdownToThePool() {
        val result = GainEndeavors.run(record(endeavors = 0), 4, Core16Ruleset)

        assertEquals(4, result.settlement.endeavors)
    }

    @Test
    fun gainingEndeavorsWithNoReturneesLeavesThePoolUnchanged() {
        val result = GainEndeavors.run(record(endeavors = 3), 0, Core16Ruleset)

        assertEquals(3, result.settlement.endeavors)
    }

    @Test
    fun spendingMoreThanTheLedgerHoldsIsBlocked() {
        val result = AddResourceEntry.run(record(), ResourceEntry("Bone", ResourceCategory.BASIC, -2))

        val blocked = assertIs<StepResult.Blocked>(result)
        assertEquals("insufficient Bone", blocked.reason)
    }

    @Test
    fun spendingWithinTheLedgerAppendsAndProjectsTheRemainder() {
        val gained =
            assertIs<StepResult.Ok<SettlementRecord>>(
                AddResourceEntry.run(record(), ResourceEntry("Bone", ResourceCategory.BASIC, 3)),
            )
        val spent =
            assertIs<StepResult.Ok<SettlementRecord>>(
                AddResourceEntry.run(gained.value, ResourceEntry("Bone", ResourceCategory.BASIC, -2)),
            )

        val line = spent.value.storage().single { it.name == "Bone" }
        assertEquals(1, line.quantity)
    }
}

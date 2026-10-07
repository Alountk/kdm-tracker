package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SpendEndeavorUseCaseTest {
    private fun record(endeavors: Int = 0): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(1L) { 0 },
                    campaignId = Ulid.generate(2L) { 0 },
                    name = "Kingsport",
                    lanternYear = 1,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 5,
                    endeavors = endeavors,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
        )

    @Test
    fun spendingAnEndeavorWithNoneIsBlocked() {
        val result = SpendEndeavor.run(record(endeavors = 0))

        val blocked = assertIs<StepResult.Blocked>(result)
        assertEquals("no endeavors", blocked.reason)
    }

    @Test
    fun spendingAnEndeavorDecrementsThePool() {
        val result = SpendEndeavor.run(record(endeavors = 2), count = 1)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(1, ok.value.settlement.endeavors)
    }
}

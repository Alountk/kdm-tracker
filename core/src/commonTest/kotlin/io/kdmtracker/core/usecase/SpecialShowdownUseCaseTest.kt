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

class SpecialShowdownUseCaseTest {
    private fun record(): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(1L) { 0 },
                    campaignId = Ulid.generate(2L) { 0 },
                    name = "Kingsport",
                    lanternYear = 9,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 9,
                    endeavors = 0,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
        )

    @Test
    fun declaringTogglesTheFlag() {
        val declared = assertIs<StepResult.Ok<SettlementRecord>>(SetSpecialShowdown.run(record(), true))
        assertEquals(true, declared.value.state.specialShowdown)

        val cleared = assertIs<StepResult.Ok<SettlementRecord>>(SetSpecialShowdown.run(declared.value, false))
        assertEquals(false, cleared.value.state.specialShowdown)
    }
}

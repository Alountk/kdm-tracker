package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RosterLifecycleUseCaseTest {
    private fun record(
        living: Int,
        dead: Int = 0,
    ): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(1L) { 0 },
                    campaignId = Ulid.generate(2L) { 0 },
                    name = "Kingsport",
                    lanternYear = 1,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 5,
                    endeavors = 0,
                    population = living,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state =
                SettlementState(
                    survivors =
                        List(living) { Survivor(id = Ulid.generate(it.toLong() + 10) { 0 }) } +
                            List(dead) {
                                Survivor(id = Ulid.generate(100L + it) { 0 }, status = SurvivorStatus.DEAD)
                            },
                ),
        )

    @Test
    fun recordDeathsMarksLivingSurvivorsDead() {
        val result = RecordDeaths.run(record(living = 4), count = 2)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(2, ok.value.state.deaths)
        assertEquals(2, ok.value.population)
        assertEquals(
            2,
            ok.value.state.survivors
                .count { it.status == SurvivorStatus.DEAD },
        )
    }

    @Test
    fun recordDeathsCapsAtTheLivingCount() {
        val result = RecordDeaths.run(record(living = 2), count = 5)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(5, ok.value.state.deaths)
        assertEquals(0, ok.value.population)
    }

    @Test
    fun recordBirthsAppendsUnnamedSurvivors() {
        var n = 0L
        val result = RecordBirths.run(record(living = 4), count = 3) { Ulid.generate(++n) { n.toInt() } }

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(3, ok.value.state.births)
        assertEquals(7, ok.value.population)
        assertEquals(7, ok.value.state.survivors.size)
    }

    @Test
    fun recordDeathsRejectsNonPositive() {
        val result = RecordDeaths.run(record(living = 4), count = 0)

        assertEquals("invalid death count", assertIs<StepResult.Blocked>(result).reason)
    }

    @Test
    fun recordBirthsRejectsNonPositive() {
        val result = RecordBirths.run(record(living = 4), count = -1) { Ulid.generate(1L) { 0 } }

        assertEquals("invalid birth count", assertIs<StepResult.Blocked>(result).reason)
    }
}

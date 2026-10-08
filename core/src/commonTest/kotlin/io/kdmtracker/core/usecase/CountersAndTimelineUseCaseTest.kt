package io.kdmtracker.core.usecase

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

class CountersAndTimelineUseCaseTest {
    private fun record(
        population: Int = 4,
        deaths: Int = 0,
        births: Int = 0,
        lanternYear: Int = 1,
        phaseActionsUsed: Set<String> = emptySet(),
    ): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(1L) { 0 },
                    campaignId = Ulid.generate(2L) { 0 },
                    name = "Kingsport",
                    lanternYear = lanternYear,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 5,
                    endeavors = 0,
                    population = population,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state =
                SettlementState(
                    deaths = deaths,
                    births = births,
                    phaseActionsUsed = phaseActionsUsed,
                    survivors = List(population) { Survivor(id = Ulid.generate(it.toLong() + 10) { 0 }) },
                ),
        )

    @Test
    fun recordDeathsAddsToCounterAndLowersPopulation() {
        val result = RecordDeaths.run(record(), count = 2)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(2, ok.value.state.deaths)
        assertEquals(2, ok.value.population)
    }

    @Test
    fun recordDeathsFloorsPopulationAtZero() {
        val result = RecordDeaths.run(record(population = 1), count = 3)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(3, ok.value.state.deaths)
        assertEquals(0, ok.value.population)
    }

    @Test
    fun recordDeathsRejectsNonPositiveAndLeavesTheRecordUnchanged() {
        val source = record()

        val result = RecordDeaths.run(source, count = 0)

        val blocked = assertIs<StepResult.Blocked>(result)
        assertEquals("invalid death count", blocked.reason)
        assertEquals(0, source.state.deaths)
        assertEquals(4, source.population)
    }

    @Test
    fun recordBirthsAddsToCounterAndRaisesPopulation() {
        var n = 0L
        val result = RecordBirths.run(record(), count = 3) { Ulid.generate(++n) { n.toInt() } }

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(3, ok.value.state.births)
        assertEquals(7, ok.value.population)
    }

    @Test
    fun recordBirthsRejectsNonPositive() {
        val result = RecordBirths.run(record(), count = -1) { Ulid.generate(1L) { 0 } }

        assertEquals("invalid birth count", assertIs<StepResult.Blocked>(result).reason)
    }

    @Test
    fun advanceTimelineIncrementsOnceThenIsIdempotent() {
        val once = AdvanceTimeline.run(record(lanternYear = 1))

        assertEquals(2, once.settlement.lanternYear)

        val twice = AdvanceTimeline.run(once)

        assertEquals(2, twice.settlement.lanternYear)
    }

    @Test
    fun advanceTimelineRunsAgainAfterMarkersReset() {
        val once = AdvanceTimeline.run(record(lanternYear = 1))
        val reset = once.copy(state = once.state.copy(phaseActionsUsed = emptySet()))

        val twice = AdvanceTimeline.run(reset)

        assertEquals(3, twice.settlement.lanternYear)
    }
}

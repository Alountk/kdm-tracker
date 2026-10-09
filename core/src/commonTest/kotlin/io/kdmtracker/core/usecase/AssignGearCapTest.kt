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
import kotlin.test.assertTrue

class AssignGearCapTest {
    private val id = Ulid.generate(1L) { 0 }

    private fun record(gear: List<String>): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(90L) { 0 },
                    campaignId = Ulid.generate(91L) { 0 },
                    name = "Kingsport",
                    lanternYear = 1,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 8,
                    endeavors = 0,
                    population = 1,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state = SettlementState(survivors = listOf(Survivor(id = id, gear = gear))),
        )

    private fun nineItems(): List<String> = List(9) { "Gear $it" }

    @Test
    fun assigningUpToNineItemsSucceeds() {
        val result = AssignGear.run(record(nineItems().dropLast(1)), id, "Gear 8")

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(
            9,
            ok.value.state.survivors
                .single()
                .gear.size,
        )
    }

    @Test
    fun assigningATenthDistinctItemIsBlocked() {
        val source = record(nineItems())

        val result = AssignGear.run(source, id, "Gear 9")

        assertEquals("gear grid is full", assertIs<StepResult.Blocked>(result).reason)
        assertEquals(
            9,
            source.state.survivors
                .single()
                .gear.size,
        )
    }

    @Test
    fun reassigningAnAlreadyCarriedNameIsANoOp() {
        val result = AssignGear.run(record(nineItems()), id, "Gear 0")

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(
            9,
            ok.value.state.survivors
                .single()
                .gear.size,
        )
        assertTrue(
            "Gear 0" in
                ok.value.state.survivors
                    .single()
                    .gear,
        )
    }
}

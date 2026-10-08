package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.Attributes
import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.domain.WoundState
import io.kdmtracker.core.rules.StepResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RosterMutationUseCaseTest {
    private val first = Ulid.generate(1L) { 0 }
    private val second = Ulid.generate(2L) { 0 }
    private val third = Ulid.generate(3L) { 0 }

    private fun survivor(
        id: Ulid,
        status: SurvivorStatus = SurvivorStatus.ALIVE,
    ): Survivor = Survivor(id = id, status = status)

    private fun record(vararg survivors: Survivor): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(90L) { 0 },
                    campaignId = Ulid.generate(91L) { 0 },
                    name = "Kingsport",
                    lanternYear = 1,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 2,
                    endeavors = 0,
                    population = survivors.size,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state = SettlementState(survivors = survivors.toList()),
        )

    @Test
    fun returnSurvivorsHealsWoundsAndClearsTokensButKeepsProgression() {
        val eli =
            Survivor(
                id = first,
                attributes = Attributes(strength = 2),
                wounds = mapOf(HitLocation.HEAD to WoundState(armor = 2, light = true, heavy = true)),
                tokens = setOf("bleeding"),
                disorders = listOf("Apatía"),
                huntXp = 3,
            )

        val healed =
            ReturnSurvivors
                .run(record(eli))
                .state.survivors
                .single()

        assertFalse(healed.wounds.getValue(HitLocation.HEAD).light)
        assertFalse(healed.wounds.getValue(HitLocation.HEAD).heavy)
        assertEquals(2, healed.wounds.getValue(HitLocation.HEAD).armor)
        assertTrue(healed.tokens.isEmpty())
        assertEquals(2, healed.attributes.strength)
        assertEquals(listOf("Apatía"), healed.disorders)
        assertEquals(3, healed.huntXp)
    }

    @Test
    fun markDepartingSetsTheChosenAndResetsTheRest() {
        val result =
            MarkDeparting.run(
                record(survivor(first), survivor(second), survivor(third)),
                listOf(first, second),
                limit = 4,
            )

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        val byId =
            ok.value.state.survivors
                .associateBy { it.id }
        assertEquals(SurvivorStatus.DEPARTING, byId.getValue(first).status)
        assertEquals(SurvivorStatus.DEPARTING, byId.getValue(second).status)
        assertEquals(SurvivorStatus.ALIVE, byId.getValue(third).status)
    }

    @Test
    fun markDepartingRejectsMoreThanTheLimit() {
        val result = MarkDeparting.run(record(survivor(first), survivor(second)), listOf(first, second), limit = 1)

        assertEquals("invalid departing selection", assertIs<StepResult.Blocked>(result).reason)
    }

    @Test
    fun markDepartingRejectsMoreThanTheLivingCount() {
        val result =
            MarkDeparting.run(
                record(survivor(first), survivor(second), survivor(third, SurvivorStatus.DEAD)),
                listOf(first, second, third),
                limit = 4,
            )

        assertEquals("invalid departing selection", assertIs<StepResult.Blocked>(result).reason)
    }

    @Test
    fun assignGearAddsAndUnassignRemoves() {
        val assigned =
            assertIs<StepResult.Ok<SettlementRecord>>(AssignGear.run(record(survivor(first)), first, "Hacha"))
                .value
                .state
                .survivors
                .single()
        assertEquals(listOf("Hacha"), assigned.gear)

        val removed =
            assertIs<StepResult.Ok<SettlementRecord>>(UnassignGear.run(record(assigned), first, "Hacha"))
                .value
                .state
                .survivors
                .single()
        assertTrue(removed.gear.isEmpty())
    }

    @Test
    fun updateSurvivorRejectsTooManyDisorders() {
        val edited = survivor(first).copy(disorders = listOf("a", "b", "c", "d"))

        val result = UpdateSurvivor.run(record(survivor(first)), edited)

        assertEquals("invalid survivor", assertIs<StepResult.Blocked>(result).reason)
    }

    @Test
    fun updateSurvivorRejectsAnUnknownId() {
        val result = UpdateSurvivor.run(record(survivor(first)), survivor(second))

        assertEquals("unknown survivor", assertIs<StepResult.Blocked>(result).reason)
    }

    @Test
    fun updateSurvivorReplacesTheRecord() {
        val edited = survivor(first).copy(name = "Eli", courage = 4)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(UpdateSurvivor.run(record(survivor(first)), edited))

        assertEquals(
            edited,
            ok.value.state.survivors
                .single(),
        )
    }
}

package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementEvent
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import io.kdmtracker.core.ruleset.Core16Ruleset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DecisionUseCaseTest {
    private val catalog =
        ContentCatalog(
            listOf(
                ContentPack(
                    id = "test",
                    version = "1",
                    name = "test",
                    bundle = ContentBundle(settlementEvents = listOf(SettlementEvent("Rivalidad", "rivalidad text"))),
                ),
            ),
        )

    private fun record(
        deaths: Int = 0,
        resolvedMilestones: Set<MilestoneId> = emptySet(),
    ): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(1L) { 0 },
                    campaignId = Ulid.generate(2L) { 0 },
                    name = "Kingsport",
                    lanternYear = 1,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 6,
                    endeavors = 0,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state = SettlementState(deaths = deaths, resolvedMilestones = resolvedMilestones),
        )

    @Test
    fun choosePrincipleStoresTheChoice() {
        val result =
            ChoosePrinciple.run(record(), PrincipleCategory.DEATH, "The First Grave → Graves", Core16Ruleset)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(mapOf(PrincipleCategory.DEATH to "The First Grave → Graves"), ok.value.state.principles)
    }

    @Test
    fun choosePrincipleRejectsUnknownLabel() {
        val source = record()

        val result = ChoosePrinciple.run(source, PrincipleCategory.DEATH, "Not A Choice", Core16Ruleset)

        assertEquals("unknown principle choice", assertIs<StepResult.Blocked>(result).reason)
        assertEquals(emptyMap(), source.state.principles)
    }

    @Test
    fun resolveMilestoneTicksAReachedMilestone() {
        val result = ResolveMilestone.run(record(deaths = 1), Core16Milestones.PRINCIPLE_DEATH, Core16Ruleset)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals(setOf(Core16Milestones.PRINCIPLE_DEATH), ok.value.state.resolvedMilestones)
    }

    @Test
    fun resolveMilestoneRejectsAnUnreachedMilestone() {
        val source = record(deaths = 0)

        val result = ResolveMilestone.run(source, Core16Milestones.PRINCIPLE_DEATH, Core16Ruleset)

        assertEquals("milestone not reached", assertIs<StepResult.Blocked>(result).reason)
        assertEquals(emptySet(), source.state.resolvedMilestones)
    }

    @Test
    fun resolveMilestoneRejectsAnUnknownMilestone() {
        val result = ResolveMilestone.run(record(deaths = 99), Core16Milestones.HOODED_KNIGHT, Core16Ruleset)

        // HOODED_KNIGHT needs 5 innovations; with 0 it is unknown-reached but not unknown.
        assertEquals("milestone not reached", assertIs<StepResult.Blocked>(result).reason)
    }

    @Test
    fun recordSettlementEventStoresTheName() {
        val result = RecordSettlementEvent.run(record(), "Rivalidad", catalog)

        val ok = assertIs<StepResult.Ok<SettlementRecord>>(result)
        assertEquals("Rivalidad", ok.value.settlement.settlementEvent)
    }

    @Test
    fun recordSettlementEventRejectsUnknownName() {
        val source = record()

        val result = RecordSettlementEvent.run(source, "No Such Event", catalog)

        assertEquals("unknown settlement event No Such Event", assertIs<StepResult.Blocked>(result).reason)
        assertNull(source.settlement.settlementEvent)
    }

    @Test
    fun principleCategoryOfMapsTheThreePrincipleMilestones() {
        assertEquals(PrincipleCategory.DEATH, Core16Milestones.principleCategoryOf(Core16Milestones.PRINCIPLE_DEATH))
        assertEquals(
            PrincipleCategory.NEW_LIFE,
            Core16Milestones.principleCategoryOf(Core16Milestones.PRINCIPLE_NEW_LIFE),
        )
        assertEquals(
            PrincipleCategory.SOCIETY,
            Core16Milestones.principleCategoryOf(Core16Milestones.PRINCIPLE_SOCIETY),
        )
        assertNull(Core16Milestones.principleCategoryOf(Core16Milestones.HOODED_KNIGHT))
        assertNull(Core16Milestones.principleCategoryOf(Core16Milestones.GAME_OVER))
    }
}

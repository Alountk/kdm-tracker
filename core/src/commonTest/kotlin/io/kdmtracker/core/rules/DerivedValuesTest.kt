package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.ruleset.applyOverrides
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DerivedValuesTest {
    private val id = Ulid.generate(1L) { 0 }
    private val campaignId = Ulid.generate(2L) { 0 }

    private fun settlement(endeavors: Int): Settlement =
        Settlement(
            id = id,
            campaignId = campaignId,
            name = "Kingsport",
            lanternYear = 1,
            phase = SettlementPhase.SETTLEMENT,
            currentStep = 1,
            endeavors = endeavors,
            population = 4,
            settlementEvent = null,
            sync = SyncMeta(HybridLogicalClock(0, 0)),
        )

    @Test
    fun endeavorsForUsesReturnedSurvivorsAsBase() {
        val breakdown = DerivedValues.endeavorsFor(3, Core16Ruleset)
        assertEquals(3, breakdown.base)
        assertEquals(3, breakdown.total)
        assertTrue(breakdown.bonuses.isEmpty())
    }

    @Test
    fun endeavorsForUsesTheRulesetRatePerReturnee() {
        val ruleset = Core16Ruleset.applyOverrides(RulesetOverrides(endeavorsPerReturnee = 2))
        val breakdown = DerivedValues.endeavorsFor(3, ruleset)
        assertEquals(6, breakdown.base)
        assertEquals(6, breakdown.total)
    }

    @Test
    fun recomputeDiscardsStaleEndeavorsAndUsesReturnedSurvivors() {
        val recomputed = recompute(settlement(endeavors = 99), returnedSurvivors = 2, ruleset = Core16Ruleset)
        assertEquals(2, recomputed.endeavors)
    }

    @Test
    fun recomputeChangesWhenEarlierInputChanges() {
        val two = recompute(settlement(endeavors = 0), returnedSurvivors = 2, ruleset = Core16Ruleset)
        val five = recompute(settlement(endeavors = 0), returnedSurvivors = 5, ruleset = Core16Ruleset)
        assertEquals(2, two.endeavors)
        assertEquals(5, five.endeavors)
    }

    @Test
    fun recomputeLeavesUnrelatedFieldsUntouched() {
        val original = settlement(endeavors = 99)
        val recomputed = recompute(original, returnedSurvivors = 2, ruleset = Core16Ruleset)
        assertEquals(original.copy(endeavors = 2), recomputed)
    }

    @Test
    fun survivalLimitAddsInnovationBonusToRulesetBase() {
        val ruleset = Core16Ruleset.applyOverrides(RulesetOverrides(survivalLimit = 4))
        assertEquals(4, DerivedValues.survivalLimit(ruleset))
        assertEquals(7, DerivedValues.survivalLimit(ruleset, innovationBonus = 3))
    }
}

package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.DerivedValues
import io.kdmtracker.core.ruleset.Ruleset

/**
 * Grants the endeavors earned by the survivors who returned from the hunt.
 *
 * This is the Settlement Phase's Gains Endeavors step: it *adds* the earned total to
 * the record's existing pool rather than overwriting it, so any endeavors carried into
 * the step remain spendable. The ruleset supplies the earning rate through
 * [DerivedValues.endeavorsFor], so the mechanism carries no literal.
 */
object GainEndeavors {
    /**
     * Returns a copy of [record] whose settlement pool has the [returnedSurvivors]'
     * breakdown total added to it.
     */
    fun run(
        record: SettlementRecord,
        returnedSurvivors: Int,
        ruleset: Ruleset,
    ): SettlementRecord {
        val earned = DerivedValues.endeavorsFor(returnedSurvivors, ruleset).total
        return record.copy(
            settlement = record.settlement.copy(endeavors = record.settlement.endeavors + earned),
        )
    }
}

package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult
import io.kdmtracker.core.ruleset.Ruleset

/**
 * Records the settlement's chosen principle for a milestone story event (steps 5/6).
 *
 * The label must be one the [ruleset] declares for [category]; an unknown choice is refused
 * and leaves the record untouched. The choice is stored per [PrincipleCategory] and
 * overwrites a previous choice in that category (the last word wins).
 */
object ChoosePrinciple {
    /**
     * Returns [StepResult.Ok] with the choice persisted, or [StepResult.Blocked] with
     * `"unknown principle choice"` when [label] is not one of the ruleset's choices for
     * [category].
     */
    fun run(
        record: SettlementRecord,
        category: PrincipleCategory,
        label: String,
        ruleset: Ruleset,
    ): StepResult<SettlementRecord> {
        val known = ruleset.principles[category].orEmpty().any { it.label == label }
        if (!known) {
            return StepResult.Blocked("unknown principle choice")
        }
        return StepResult.Ok(
            record.copy(
                state = record.state.copy(principles = record.state.principles + (category to label)),
            ),
        )
    }
}

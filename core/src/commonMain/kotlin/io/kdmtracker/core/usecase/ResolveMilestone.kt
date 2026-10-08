package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult
import io.kdmtracker.core.ruleset.Ruleset

/**
 * Ticks a reached milestone as resolved (Settlement Phase step 6, Check Milestones).
 *
 * Guarded twice: the id must be a milestone the [ruleset] declares, and it must actually be
 * reached for the record's counters. An unknown or unreached id is refused and leaves the
 * record untouched. Resolution is idempotent — ticking an already-resolved milestone is a
 * no-op success.
 */
object ResolveMilestone {
    /**
     * Returns [StepResult.Ok] with the milestone marked resolved, or [StepResult.Blocked]
     * with `"unknown milestone"` when [id] is not in the ruleset, or `"milestone not reached"`
     * when the record's counters have not reached it.
     */
    fun run(
        record: SettlementRecord,
        id: MilestoneId,
        ruleset: Ruleset,
    ): StepResult<SettlementRecord> {
        if (ruleset.milestones.none { it.id == id }) {
            return StepResult.Blocked("unknown milestone")
        }
        if (id !in record.reachedMilestones(ruleset)) {
            return StepResult.Blocked("milestone not reached")
        }
        return StepResult.Ok(
            record.copy(
                state = record.state.copy(resolvedMilestones = record.state.resolvedMilestones + id),
            ),
        )
    }
}

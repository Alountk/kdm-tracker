package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Edits the kind of a hunt-board space: Start / normal / "E" (monster event) / Overwhelming
 * Darkness / Starvation / monster.
 *
 * The board is player-set because the per-monster diagram is an image. The space count never
 * changes; an out-of-range index is refused.
 */
object MarkSpaceKind {
    /**
     * Returns [StepResult.Ok] with the edited space, or [StepResult.Blocked] with
     * `"invalid space"` when [index] is out of range.
     */
    fun run(
        record: SettlementRecord,
        index: Int,
        kind: HuntSpaceKind,
    ): StepResult<SettlementRecord> {
        val hunt = record.state.hunt
        if (index !in hunt.spaces.indices) {
            return StepResult.Blocked("invalid space")
        }
        val spaces =
            hunt.spaces.toMutableList().also { list ->
                list[index] = list[index].copy(kind = kind)
            }
        return StepResult.Ok(record.copy(state = record.state.copy(hunt = hunt.copy(spaces = spaces))))
    }
}

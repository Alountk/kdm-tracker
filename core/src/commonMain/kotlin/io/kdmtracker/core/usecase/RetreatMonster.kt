package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Moves the monster's miniature one space **away** from the survivor party along the hunt board.
 *
 * Some hunt events make the beast retreat (e.g. `Asustadizo`: "el Antílope Bramador se mueve 1
 * espacio alejándose de los supervivientes"). This is the counterpart of [MoveMonster], which only
 * steps the beast *toward* the party.
 *
 * Blocks when the board is not set up or the hunt is already over. The step is floored at the Start
 * space (index 0): the beast never retreats past it, so a retreat at index 0 is a no-op that still
 * returns [StepResult.Ok].
 */
object RetreatMonster {
    /** Returns [StepResult.Ok] with the beast one space further from the party, or a [StepResult.Blocked]. */
    fun run(record: SettlementRecord): StepResult<SettlementRecord> {
        val hunt = record.state.hunt
        if (hunt.spaces.isEmpty()) {
            return StepResult.Blocked("hunt not set up")
        }
        if (hunt.status != HuntStatus.ACTIVE) {
            return StepResult.Blocked("hunt is over")
        }
        // Retreat means moving *away* from the party, never past the Start space (index 0).
        val party = hunt.position.coerceIn(0, hunt.spaces.lastIndex)
        val next =
            when {
                hunt.monsterPosition < party -> hunt.monsterPosition + 1 // beast is behind: step away
                hunt.monsterPosition > party -> (hunt.monsterPosition - 1).coerceAtLeast(0)
                else -> hunt.monsterPosition.coerceAtLeast(0) // same space: step back toward Start
            }
        return StepResult.Ok(
            record.copy(
                state =
                    record.state.copy(
                        hunt = hunt.copy(monsterPosition = next),
                    ),
            ),
        )
    }
}

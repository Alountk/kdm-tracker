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
 * Blocks when the board is not set up or the hunt is already over. The step is clamped to the board:
 * the beast never leaves it, so a retreat at the last space is a no-op that still returns
 * [StepResult.Ok].
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
        // The beast waits ahead of the party, so away from it is further along the board; a beast
        // behind the party (older saves) steps back toward Start. Either way it stays on the board.
        val party = hunt.position.coerceIn(0, hunt.spaces.lastIndex)
        val step = if (hunt.monsterPosition >= party) 1 else -1
        val next = (hunt.monsterPosition + step).coerceIn(0, hunt.spaces.lastIndex)
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

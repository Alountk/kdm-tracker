package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult
import kotlin.math.absoluteValue

/**
 * Moves the monster's miniature one space toward the survivor party along the hunt board.
 *
 * Per the hunt rules (`external-assets/06_caza_asentamiento_y_terreno.md`, §2 Progresión de la
 * Caza): the monster steps toward the party, and when it reaches the party's space it **ambushes**
 * them — the survivors lose their first turn in the coming combat — so the hunt resolves to
 * [HuntStatus.AMBUSH] on contact.
 *
 * Blocks when the board is not set up or the hunt is already over. The movement is a single step
 * toward [HuntState.position]; it never overshoots the party's space (it lands on it and ambushes).
 */
object MoveMonster {
    /** Returns [StepResult.Ok] with the monster one space closer (ambushed on contact), or a [StepResult.Blocked]. */
    fun run(record: SettlementRecord): StepResult<SettlementRecord> {
        val hunt = record.state.hunt
        if (hunt.spaces.isEmpty()) {
            return StepResult.Blocked("hunt not set up")
        }
        if (hunt.status != HuntStatus.ACTIVE) {
            return StepResult.Blocked("hunt is over")
        }
        val party = hunt.position.coerceIn(0, hunt.spaces.lastIndex)
        val wasAdjacent = (hunt.monsterPosition - party).absoluteValue == 1
        val next =
            when {
                hunt.monsterPosition > party -> hunt.monsterPosition - 1
                hunt.monsterPosition < party -> hunt.monsterPosition + 1
                else -> hunt.monsterPosition
            }
        // The monster stepped onto the party's space from an adjacent space: ambush.
        val ambushed = next == party && wasAdjacent
        return StepResult.Ok(
            record.copy(
                state =
                    record.state.copy(
                        hunt =
                            hunt.copy(
                                monsterPosition = next,
                                status = if (ambushed) HuntStatus.AMBUSH else hunt.status,
                            ),
                    ),
            ),
        )
    }
}

package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HuntSpace
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntState
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Sets up the Hunting Phase for the current lantern year.
 *
 * Seeds the thirteen-space board with the Start space at index 0 and the monster on the last space;
 * the player marks Overwhelming Darkness / Starvation / the "E" spaces later with
 * [MarkSpaceKind], because the per-monster board diagram is not machine-readable.
 */
object SetUpHunt {
    /** The hunt board has thirteen spaces. */
    const val BOARD_SPACES = 13

    /**
     * Returns [StepResult.Ok] with the seeded board, or [StepResult.Blocked] on a blank quarry, a
     * level outside `1..3`, or a player count outside `1..4`.
     */
    fun run(
        record: SettlementRecord,
        quarry: String,
        level: Int,
        players: Int,
    ): StepResult<SettlementRecord> {
        if (quarry.isBlank()) {
            return StepResult.Blocked("unknown quarry")
        }
        if (level !in 1..3) {
            return StepResult.Blocked("invalid level")
        }
        if (players !in 1..4) {
            return StepResult.Blocked("invalid players")
        }
        val spaces =
            List(BOARD_SPACES) { index ->
                HuntSpace(
                    kind =
                        when (index) {
                            0 -> HuntSpaceKind.START
                            BOARD_SPACES - 1 -> HuntSpaceKind.MONSTER
                            else -> HuntSpaceKind.NORMAL
                        },
                )
            }
        val hunt =
            HuntState(
                quarry = quarry.trim(),
                level = level,
                players = players,
                spaces = spaces,
                position = 0,
                monsterPosition = spaces.lastIndex,
                status = HuntStatus.ACTIVE,
            )
        return StepResult.Ok(record.copy(state = record.state.copy(hunt = hunt)))
    }
}

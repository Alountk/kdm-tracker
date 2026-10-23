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
 * Seeds the thirteen-space board with the **base** layout from the manual (`kdm_parte1.md` 9.1 /
 * rulebook p.61): Start at index 0, **Overwhelming Darkness** in the center (index 6), and
 * **Starvation** at the end (index 12); every other space starts as an ordinary event space.
 *
 * The per-quarry special-"E" positions (and the monster's starting space by level) live only in
 * each monster's hunt-board **diagram** — an image in the rulebook with no extractable text — so
 * they are not invented here; the player marks them later with [MarkSpaceKind].
 */
object SetUpHunt {
    /** The hunt board has thirteen spaces. */
    const val BOARD_SPACES = 13

    /** Index of the central Overwhelming Darkness space (13 spaces: 0..12, center = 6). */
    const val OVERWHELMING_DARKNESS_INDEX = 6

    /** Index of the final Starvation space. */
    const val STARVATION_INDEX = BOARD_SPACES - 1

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
                HuntSpace(kind = baseLayoutKind(index))
            }
        val hunt =
            HuntState(
                quarry = quarry.trim(),
                level = level,
                players = players,
                spaces = spaces,
                position = 0,
                monsterPosition = STARVATION_INDEX,
                status = HuntStatus.ACTIVE,
            )
        return StepResult.Ok(record.copy(state = record.state.copy(hunt = hunt)))
    }

    /** The base role of [index] on the standard thirteen-space board (manual 9.1). */
    private fun baseLayoutKind(index: Int): HuntSpaceKind =
        when (index) {
            0 -> HuntSpaceKind.START
            OVERWHELMING_DARKNESS_INDEX -> HuntSpaceKind.OVERWHELMING_DARKNESS
            STARVATION_INDEX -> HuntSpaceKind.STARVATION
            else -> HuntSpaceKind.NORMAL
        }
}

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
 * Seeds the thirteen-space board from the canonical hunt-track table in
 * `external-assets/25_tableros_de_caza_de_presas_le_n_blanco_f_nix_y_ant_lope_bramador.md`, whose
 * spaces are numbered **0-12** (section 2, aligned with this app array).
 * The fixed spaces (`START` @0, `OVERWHELMING_DARKNESS` @6, `STARVATION` @12) hold for every
 * quarry; each quarry also has its own monster-event (`E`) spaces and a beast starting space that
 * depends on the level. Layout is code-hardcoded (the per-monster diagrams are images).
 *
 * `MarkSpaceKind` still works for manual adjustment.
 */
object SetUpHunt {
    /** The hunt board has thirteen spaces. */
    const val BOARD_SPACES = 13

    /** Index of the central Overwhelming Darkness space (md 6). */
    const val OVERWHELMING_DARKNESS_INDEX = 6

    /** Index of the final Starvation space (md 12). */
    const val STARVATION_INDEX = BOARD_SPACES - 1

    /**
     * The quarry's monster-event (`E`) spaces — md `25_tableros` section 2, 0-based indices 0-12.
     */
    private val MONSTER_EVENT_INDICES =
        mapOf(
            "White Lion" to listOf(1, 2, 5, 8, 9),
            "Phoenix" to listOf(1, 4, 7, 9),
            "Screaming Antelope" to listOf(1, 2, 7, 9),
        )

    /**
     * Per-quarry, per-level beast starting-space index (md section 2, 0-based 0-12):
     * White Lion L1/L2/L3 = 4/7/11; Phoenix = 5/8/11; Screaming Antelope = 4/8/10.
     */
    private val BEAST_HOME_BY_LEVEL =
        mapOf(
            "White Lion" to mapOf(1 to 4, 2 to 7, 3 to 11),
            "Phoenix" to mapOf(1 to 5, 2 to 8, 3 to 11),
            "Screaming Antelope" to mapOf(1 to 4, 2 to 8, 3 to 10),
        )

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
        val monsterEventIndices = MONSTER_EVENT_INDICES[quarry.trim()].orEmpty()
        val spaces =
            List(BOARD_SPACES) { index ->
                HuntSpace(kind = layoutKind(index, monsterEventIndices))
            }
        val hunt =
            HuntState(
                quarry = quarry.trim(),
                level = level,
                players = players,
                spaces = spaces,
                position = 0,
                monsterPosition = BEAST_HOME_BY_LEVEL[quarry.trim()]?.get(level) ?: STARVATION_INDEX,
                status = HuntStatus.ACTIVE,
            )
        return StepResult.Ok(record.copy(state = record.state.copy(hunt = hunt)))
    }

    /** The role of [index] for this quarry's board: fixed, a monster-event `E`, or ordinary. */
    private fun layoutKind(
        index: Int,
        monsterEventIndices: List<Int>,
    ): HuntSpaceKind =
        when {
            index == 0 -> HuntSpaceKind.START
            index == OVERWHELMING_DARKNESS_INDEX -> HuntSpaceKind.OVERWHELMING_DARKNESS
            index == STARVATION_INDEX -> HuntSpaceKind.STARVATION
            index in monsterEventIndices -> HuntSpaceKind.MONSTER_E
            else -> HuntSpaceKind.NORMAL
        }
}

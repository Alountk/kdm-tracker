package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntState

/**
 * One rendered space of the hunt board: its [index], a material [icon] name, a short [label], and
 * whether it is the survivor party's current space.
 */
data class HuntSpaceView(
    val index: Int,
    val icon: String,
    val label: String,
    val isParty: Boolean,
)

/**
 * The pure 13-space hunt board frame: the per-space views plus the range header and the two
 * position markers. Extracted from the `HuntBoard` composable so the render is testable without a
 * Compose harness; the composable consumes this view.
 */
data class HuntBoardFrame(
    val spaces: List<HuntSpaceView>,
    val rangeLabel: String,
    val partyMarker: String,
    val monsterMarker: String,
) {
    companion object {
        /** Builds the frame for [hunt], naming the range `Casillas 0 - <last>`. */
        fun from(hunt: HuntState): HuntBoardFrame {
            val spaces =
                hunt.spaces.mapIndexed { index, space ->
                    HuntSpaceView(
                        index = index,
                        icon = space.kind.materialIcon(index, hunt),
                        label = space.kind.cellLabel(index, hunt),
                        isParty = index == hunt.position,
                    )
                }
            val last = hunt.spaces.lastIndex
            return HuntBoardFrame(
                spaces = spaces,
                rangeLabel = "Casillas 0 - $last",
                partyMarker = "Supervivientes (Casilla ${hunt.position})",
                monsterMarker = "Monstruo (Casilla ${hunt.monsterPosition})",
            )
        }

        private fun HuntSpaceKind.materialIcon(
            index: Int,
            hunt: HuntState,
        ): String =
            when (this) {
                // Fixed spaces keep their own mark, never the party's star.
                HuntSpaceKind.START -> "home"
                HuntSpaceKind.OVERWHELMING_DARKNESS -> "wb_twilight"
                HuntSpaceKind.STARVATION -> "restaurant"
                HuntSpaceKind.MONSTER -> "pets" // the beast: a paw, not the lair skull
                HuntSpaceKind.MONSTER_E -> "visibility" // the quarry's gold special event
                HuntSpaceKind.NORMAL -> if (index == hunt.position) "flare" else "E"
            }

        private fun HuntSpaceKind.cellLabel(
            index: Int,
            hunt: HuntState,
        ): String =
            when (this) {
                HuntSpaceKind.START -> "Asent."
                HuntSpaceKind.OVERWHELMING_DARKNESS -> "Oscuridad Abrumadora"
                HuntSpaceKind.STARVATION -> "Hambruna"
                // The beast's space names the quarry below the paw (wraps for long names).
                HuntSpaceKind.MONSTER -> hunt.quarry ?: "Bestia"
                HuntSpaceKind.MONSTER_E -> "Evento"
                HuntSpaceKind.NORMAL -> if (index == hunt.position) "Grupo" else "Evento"
            }
    }
}

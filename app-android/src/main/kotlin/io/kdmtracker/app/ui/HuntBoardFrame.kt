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
            when {
                this == HuntSpaceKind.START -> "home"
                index == hunt.position -> "flare"
                this == HuntSpaceKind.MONSTER -> "skull"
                this == HuntSpaceKind.MONSTER_E -> "E"
                this == HuntSpaceKind.NORMAL -> "E"
                this == HuntSpaceKind.OVERWHELMING_DARKNESS -> "wb_twilight"
                this == HuntSpaceKind.STARVATION -> "restaurant"
                else -> "help"
            }

        private fun HuntSpaceKind.cellLabel(
            index: Int,
            hunt: HuntState,
        ): String =
            when {
                this == HuntSpaceKind.START -> "Asent."
                index == hunt.position -> "Grupo"
                this == HuntSpaceKind.MONSTER -> "Guarida"
                this == HuntSpaceKind.MONSTER_E -> "Evento"
                this == HuntSpaceKind.NORMAL -> "Evento"
                this == HuntSpaceKind.OVERWHELMING_DARKNESS -> "Oscuridad Abrumadora"
                this == HuntSpaceKind.STARVATION -> "Hambruna"
                else -> ""
            }
    }
}

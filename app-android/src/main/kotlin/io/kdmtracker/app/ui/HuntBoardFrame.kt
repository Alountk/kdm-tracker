package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntState

/**
 * The colour tone a hunt-board space renders in, mirroring the design's per-space anatomy
 * (`.superpowers/design` + the Stitch `estados` screen): each fixed/role space has its own accent
 * border, medallion fill and label colour.
 */
enum class SpaceTone { OUTLINE, GOLD, VIOLET, AMBER, BLOOD, SURFACE }

/**
 * One rendered space of the hunt board: its [index], a material [icon] name, a short [label], a
 * small [subLabel] caption below it, its colour [tone], whether it is the survivor party's current
 * space, and whether the beast currently stands here.
 */
data class HuntSpaceView(
    val index: Int,
    val icon: String,
    val label: String,
    val subLabel: String = "",
    val tone: SpaceTone = SpaceTone.OUTLINE,
    val isParty: Boolean,
    val isBeast: Boolean = false,
)

/**
 * The pure 13-space hunt board frame: the per-space views plus the range header and the two
 * position markers. Extracted from the `HuntBoard` composable so the render is testable without a
 * Compose harness; the composable consumes this view.
 *
 * A space's mark is resolved by, in order: the **beast** (its current position paints a paw and the
 * quarry's name), the **party** (its current space paints the filled star `Grupo`), then the fixed
 * space kind (Start / Overwhelming Darkness / Starvation / the quarry's special event); a plain
 * space shows the grey `E`. Fixed spaces are never repainted as `Grupo`.
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
                    val isBeast = index == hunt.monsterPosition && hunt.monsterPosition >= 0
                    val isParty = index == hunt.position && !isBeast
                    HuntSpaceView(
                        index = index,
                        icon = markIcon(space.kind, isParty, isBeast),
                        label = markLabel(space.kind, hunt, isParty, isBeast),
                        subLabel = markSubLabel(space.kind, isParty, isBeast),
                        tone = markTone(space.kind, isParty, isBeast),
                        isParty = isParty,
                        isBeast = isBeast,
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

        private fun markIcon(
            kind: HuntSpaceKind,
            isParty: Boolean,
            isBeast: Boolean,
        ): String {
            // Fixed spaces always keep their own mark — never the party's star (S2).
            val fixed =
                when (kind) {
                    HuntSpaceKind.START -> "foundation"
                    HuntSpaceKind.OVERWHELMING_DARKNESS -> "bedtime"
                    HuntSpaceKind.STARVATION -> "no_meals"
                    else -> null
                }
            if (fixed != null) return fixed
            return when {
                isBeast -> "cruelty_free" // the beast: a paw, wherever it currently stands
                isParty -> "groups" // the party's filled group marker
                kind == HuntSpaceKind.MONSTER -> "cruelty_free" // a marked beast space (legacy/manual)
                kind == HuntSpaceKind.MONSTER_E -> "visibility" // the quarry's gold special event
                else -> "E"
            }
        }

        private fun markLabel(
            kind: HuntSpaceKind,
            hunt: HuntState,
            isParty: Boolean,
            isBeast: Boolean,
        ): String {
            // Fixed spaces always keep their own label — never the party's Grupo (S2).
            val fixed =
                when (kind) {
                    HuntSpaceKind.START -> "Asent."
                    HuntSpaceKind.OVERWHELMING_DARKNESS -> "Oscuridad Abrumadora"
                    HuntSpaceKind.STARVATION -> "Hambruna"
                    else -> null
                }
            if (fixed != null) return fixed
            return when {
                // The beast's space names the quarry below the paw (wraps for long names).
                isBeast -> hunt.quarry ?: "Bestia"
                isParty -> "Grupo"
                kind == HuntSpaceKind.MONSTER -> hunt.quarry ?: "Bestia"
                else -> "Evento"
            }
        }

        /** The design's small caption under the label (e.g. `Inicio`, `Tablero`, `Activo`). */
        private fun markSubLabel(
            kind: HuntSpaceKind,
            isParty: Boolean,
            isBeast: Boolean,
        ): String =
            when {
                isParty -> "Activo"
                isBeast -> "En Acecho"
                kind == HuntSpaceKind.START -> "Inicio"
                kind == HuntSpaceKind.OVERWHELMING_DARKNESS -> "Divisoria"
                kind == HuntSpaceKind.STARVATION -> "Desgaste"
                kind == HuntSpaceKind.MONSTER_E -> "Ojo"
                else -> "Tablero"
            }

        /** The design's accent tone per space role — drives border, medallion and label colour. */
        private fun markTone(
            kind: HuntSpaceKind,
            isParty: Boolean,
            isBeast: Boolean,
        ): SpaceTone =
            when {
                isBeast -> SpaceTone.BLOOD
                isParty -> SpaceTone.GOLD
                kind == HuntSpaceKind.MONSTER -> SpaceTone.BLOOD
                kind == HuntSpaceKind.MONSTER_E -> SpaceTone.GOLD
                kind == HuntSpaceKind.OVERWHELMING_DARKNESS -> SpaceTone.VIOLET
                kind == HuntSpaceKind.STARVATION -> SpaceTone.AMBER
                kind == HuntSpaceKind.START -> SpaceTone.SURFACE
                else -> SpaceTone.OUTLINE
            }
    }
}

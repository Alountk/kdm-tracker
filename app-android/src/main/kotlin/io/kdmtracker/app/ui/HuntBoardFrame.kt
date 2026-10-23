package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntState

/**
 * The colour tone a hunt-board space renders in, mirroring the design's per-space anatomy
 * (`.superpowers/design` + the Stitch `estados` screen): each fixed/role space has its own accent
 * border, medallion fill and label colour.
 */
enum class SpaceTone { OUTLINE, GOLD, VIOLET, AMBER, BLOOD, SURFACE, GREEN }

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
    /** The small type badge at the step's top-right (S1): `flag`/`swords`/`nights_stay`/`warning`/`skull`/`star`. */
    val badge: String = "",
    /** True on the party's active step only: renders the animated ping dot in place of [badge] (L5b). */
    val partyPing: Boolean = false,
    /** False for the start (00) and final (12) steps, which render no number (S1). */
    val showNumber: Boolean = true,
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
            val last = hunt.spaces.lastIndex
            val spaces =
                hunt.spaces.mapIndexed { index, space ->
                    val isBeast = index == hunt.monsterPosition && hunt.monsterPosition >= 0
                    val isParty = index == hunt.position && !isBeast
                    HuntSpaceView(
                        index = index,
                        icon = markIcon(space.kind, isParty, isBeast),
                        label = markLabel(space.kind, hunt, isParty, isBeast),
                        subLabel = markSubLabel(space.kind, hunt, isParty, isBeast),
                        tone = markTone(space.kind, isParty, isBeast),
                        isParty = isParty,
                        isBeast = isBeast,
                        badge = markBadge(space.kind, isBeast, isParty),
                        partyPing = isParty && !isBeast, // the ping dot replaces the badge on the party's step (L5b)
                        showNumber = index != 0 && index != last, // start + final steps carry no number (S1)
                    )
                }
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
                // S4: a quarry special-event step is `E. <Animal>` (e.g. `E. León`).
                kind == HuntSpaceKind.MONSTER_E -> "E. ${quarryAnimal(hunt.quarry)}"
                else -> "Evento"
            }
        }

        /** The short Spanish animal for a quarry (S4): `White Lion` → `León`, etc. */
        private fun quarryAnimal(quarry: String?): String =
            when (quarry?.trim()) {
                "White Lion" -> "León"
                "Phoenix" -> "Fénix"
                "Screaming Antelope" -> "Antílope"
                else -> quarry ?: "Bestia"
            }

        /**
         * The design's top-right type badge (S1/L5e). The **beast wins** over the fixed kind:
         * wherever the beast stands, the step is a confrontation and shows `swords`.
         *
         * The **party's step** shows no type badge — it carries the ping dot instead (L5b).
         * An ordinary event step shows **no mark at all** (L5e); a quarry special event shows a
         * `star` (L5e) — the eye lives only in the medallion centre.
         */
        private fun markBadge(
            kind: HuntSpaceKind,
            isBeast: Boolean,
            isParty: Boolean,
        ): String {
            if (isParty) return "" // the party's step uses the ping dot, never a type badge (L5b)
            if (isBeast) return "swords"
            return when (kind) {
                HuntSpaceKind.START -> "flag"
                HuntSpaceKind.OVERWHELMING_DARKNESS -> "nights_stay"
                HuntSpaceKind.STARVATION -> "warning"
                HuntSpaceKind.MONSTER -> "skull" // final step when the beast is elsewhere
                HuntSpaceKind.MONSTER_E -> "star" // quarry special event: a star, not the eye (L5e)
                else -> "" // ordinary event step: no top-right mark (L5e)
            }
        }

        /** The design's small caption under the label (e.g. `Inicio`, `Tablero`, `Activo`). */
        private fun markSubLabel(
            kind: HuntSpaceKind,
            hunt: HuntState,
            isParty: Boolean,
            isBeast: Boolean,
        ): String =
            when {
                isParty -> "Activo"
                isBeast -> "En Acecho"
                kind == HuntSpaceKind.START -> "Inicio"
                kind == HuntSpaceKind.OVERWHELMING_DARKNESS -> "Divisoria"
                kind == HuntSpaceKind.STARVATION -> "Desgaste"
                kind == HuntSpaceKind.MONSTER_E -> "Ojo ${quarryAnimal(hunt.quarry)}" // `Ojo León`
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
                isParty -> SpaceTone.GREEN // party / active tone is emerald (design decision L2)
                kind == HuntSpaceKind.MONSTER -> SpaceTone.BLOOD
                kind == HuntSpaceKind.MONSTER_E -> SpaceTone.GOLD
                kind == HuntSpaceKind.OVERWHELMING_DARKNESS -> SpaceTone.VIOLET
                kind == HuntSpaceKind.STARVATION -> SpaceTone.AMBER
                kind == HuntSpaceKind.START -> SpaceTone.SURFACE
                else -> SpaceTone.OUTLINE
            }
    }
}

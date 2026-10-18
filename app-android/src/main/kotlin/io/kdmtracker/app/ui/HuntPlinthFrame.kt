package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.HuntState

/**
 * The quarry-plinth data the `QuarryPlinth` composable renders 1:1 from the Stitch design.
 *
 * Only the derived values live here: the `Presa Activa`/`Nivel N` label, the quarry (falling back
 * to the design's example `León Blanco` before setup), the `Variante Primera Historia` variant and
 * the `Distancia` in casillas. The plinth's layout and colours are styling, not data, and are
 * verified by emulator capture.
 */
data class HuntPlinthFrame(
    val eyebrow: String,
    val levelLabel: String,
    val quarry: String,
    val variant: String,
    val distance: String,
) {
    companion object {
        /** The design's example quarry shown before the hunt is set up. */
        private const val EXAMPLE_QUARRY = "León Blanco"

        /** The design's example variant line, which is not quarry-derived. */
        private const val VARIANT = "Variante Primera Historia"

        fun from(hunt: HuntState): HuntPlinthFrame =
            HuntPlinthFrame(
                eyebrow = "Presa Activa",
                levelLabel = "Nivel ${hunt.level}",
                quarry = hunt.quarry ?: EXAMPLE_QUARRY,
                variant = VARIANT,
                distance = (hunt.monsterPosition - hunt.position).coerceAtLeast(0).toString(),
            )
    }
}

package io.kdmtracker.app.ui.settlement

/**
 * The Innovations panel's values.
 *
 * [drawLabel] is built from the ruleset's draw/keep, never a literal, so a campaign's house rule
 * ("draw 4, keep 1") changes the text without touching the panel.
 */
data class InnovationsModel(
    val deckSize: Int,
    val principleSlots: List<String>,
    val emptySlots: Int,
    val drawLabel: String,
) {
    companion object {
        /** The design's three slots: the chosen principle plus two to discover. */
        const val SLOT_COUNT = 3

        fun of(
            deck: List<String>,
            principles: List<String>,
            draw: Int,
            keep: Int,
        ): InnovationsModel =
            InnovationsModel(
                deckSize = deck.size,
                principleSlots = principles,
                emptySlots = (SLOT_COUNT - principles.size).coerceAtLeast(0),
                drawLabel = "Robar $draw, elegir $keep",
            )
    }
}

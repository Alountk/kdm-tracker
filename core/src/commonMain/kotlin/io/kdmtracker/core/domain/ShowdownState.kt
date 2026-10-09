package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/** The Showdown's outcome. */
enum class ShowdownStatus { ACTIVE, VICTORY, DEFEAT }

/**
 * The Showdown Phase's tracked state for the current lantern year.
 *
 * The monster's AI deck is its health: [aiDeck] holds the cards still face-down, [discard] the cards
 * drawn this cycle (the monster's turns), and [wounds] the cards removed by wounds. Drawing moves a
 * card from [aiDeck] to [discard]; wounding moves one from [aiDeck] to [wounds]. When [aiDeck] is
 * empty the next action reshuffles [discard] back into it. The monster dies when every AI card is in
 * [wounds] — [aiDeck] and [discard] both empty.
 *
 * @property lastDrawn the most recently drawn card, shown to the table.
 */
@Serializable
data class ShowdownState(
    val monster: String = "",
    val level: Int = 1,
    val aiDeck: List<String> = emptyList(),
    val discard: List<String> = emptyList(),
    val wounds: List<String> = emptyList(),
    val lastDrawn: String? = null,
    val status: ShowdownStatus = ShowdownStatus.ACTIVE,
)

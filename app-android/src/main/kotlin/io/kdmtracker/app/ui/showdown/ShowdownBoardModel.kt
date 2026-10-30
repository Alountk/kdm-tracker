package io.kdmtracker.app.ui.showdown

import io.kdmtracker.core.domain.MonsterCard
import io.kdmtracker.core.domain.MonsterCardKind
import io.kdmtracker.core.domain.ShowdownState

/** The Move & Attack numbers an AI card prints: speed, accuracy (`2+`, `Auto`) and damage. */
data class CardStats(
    val speed: Int,
    val accuracy: String,
    val damage: Int,
)

/** The monster's last drawn AI card; [stats] is `null` for a card with no Move & Attack block. */
data class DrawnCard(
    val name: String,
    val rank: String?,
    val text: String,
    val stats: CardStats?,
)

/** One hit location; [reaction] marks the cards whose text opens with a miss or reflex trigger. */
data class HitLocationRow(
    val name: String,
    val text: String,
    val reaction: Boolean,
)

/**
 * What the showdown board renders, derived from the stored encounter and the monster's cards.
 *
 * The AI deck is the monster's health: [health] counts the cards not yet in the wound stack out of
 * the [deckSize] the encounter started with.
 */
data class ShowdownBoardModel(
    val monster: String,
    val level: Int,
    val health: Int,
    val deckSize: Int,
    val wounds: Int,
    val drawn: DrawnCard?,
    val hitLocations: List<HitLocationRow>,
) {
    companion object {
        private val STATS =
            Regex(
                """(?:Mover & Atacar Objetivo )?Velocidad Precisión Daño(?: Activación)? (\d+) (\d\+|Auto|-) (\d+)\b\s*""",
            )
        private val REACTION = Regex("""^(Fallo|Reflejo)\b""")

        fun of(
            showdown: ShowdownState,
            cards: List<MonsterCard>,
        ): ShowdownBoardModel {
            val own = cards.filter { it.monster == showdown.monster }
            val remaining = showdown.aiDeck.size + showdown.discard.size
            return ShowdownBoardModel(
                monster = showdown.monster,
                level = showdown.level,
                health = remaining,
                deckSize = remaining + showdown.wounds.size,
                wounds = showdown.wounds.size,
                drawn = showdown.lastDrawn?.let { name -> drawnCard(name, own) },
                hitLocations =
                    own
                        .filter { it.kind == MonsterCardKind.HIT_LOCATION }
                        .map { HitLocationRow(it.name, it.text, REACTION.containsMatchIn(it.text)) },
            )
        }

        private fun drawnCard(
            name: String,
            own: List<MonsterCard>,
        ): DrawnCard {
            val card = own.firstOrNull { it.kind == MonsterCardKind.AI && it.name == name }
            val printed = card?.text.orEmpty()
            val match = STATS.find(printed)
            val stats =
                match?.destructured?.let { (speed, accuracy, damage) ->
                    CardStats(speed.toInt(), accuracy, damage.toInt())
                }
            val text = if (match == null) printed else printed.replace(match.value, "").trim()
            return DrawnCard(name = name, rank = card?.rank, text = text, stats = stats)
        }
    }
}

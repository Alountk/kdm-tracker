package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * An innovation the settlement can unlock.
 *
 * @property id the innovation's stable, language-neutral identity.
 * @property names the innovation's localized display names.
 * @property description the rules text shown on the innovation card.
 * @property consequences the names of the cards the innovation adds to the deck.
 * @property survivalLimitBonus the survival limit increase the innovation grants.
 */
@Serializable
data class Innovation(
    val id: InnovationId,
    val names: LocalizedText,
    val description: String,
    val consequences: List<String> = emptyList(),
    val survivalLimitBonus: Int = 0,
)

/**
 * The innovations a settlement has unlocked, deduped by [Innovation.id].
 *
 * The collection holds content only; anything derived from it — such as the
 * [InnovationDeck] — lives on the deck, never hardcoded here.
 */
@Serializable
data class InnovationCollection(
    val innovations: List<Innovation> = emptyList(),
) {
    /**
     * Returns a collection that also holds [innovation], unless an innovation
     * with the same [Innovation.id] is already present. The first-seen
     * innovation wins; the later one is ignored.
     */
    fun with(innovation: Innovation): InnovationCollection =
        if (innovations.any { it.id == innovation.id }) this else copy(innovations = innovations + innovation)

    /** The sum of every unlocked innovation's survival limit bonus. */
    fun survivalLimitBonus(): Int = innovations.sumOf { it.survivalLimitBonus }

    companion object {
        /**
         * Resolves [names] against [catalog].
         *
         * A name resolves through either language, so a caller may pass what it displays
         * (`Lenguaje` on a Spanish screen, `Language` on an English one) and reach the same
         * innovation. Unknown-name rule: a name absent from [catalog] is ignored, never
         * invented and never an error. The result keeps the order [names] gives, and a
         * repeated name resolves to the same innovation only once.
         */
        fun of(
            names: Collection<String>,
            catalog: List<Innovation>,
        ): InnovationCollection =
            names
                .mapNotNull { name -> catalog.firstOrNull { it.names.es == name || it.names.en == name } }
                .fold(InnovationCollection()) { collection, innovation -> collection.with(innovation) }
    }
}

/**
 * The deck a settlement draws from when it innovates: exactly the
 * [Innovation.consequences] of the unlocked innovations, deduped and sorted.
 * Innovation names are not cards and never appear in the deck.
 */
data class InnovationDeck(
    val cards: List<String>,
) {
    companion object {
        fun of(collection: InnovationCollection): InnovationDeck =
            InnovationDeck(
                collection.innovations
                    .flatMap { it.consequences }
                    .distinct()
                    .sorted(),
            )
    }
}

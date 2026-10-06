package io.kdmtracker.core.domain

/**
 * An innovation the settlement can unlock.
 *
 * @property name the innovation's unique, human-readable name.
 * @property description the rules text shown on the innovation card.
 * @property consequences the names of the cards the innovation adds to the deck.
 * @property survivalLimitBonus the survival limit increase the innovation grants.
 */
data class Innovation(
    val name: String,
    val description: String,
    val consequences: List<String> = emptyList(),
    val survivalLimitBonus: Int = 0,
)

/**
 * The innovations a settlement has unlocked, deduped by [Innovation.name].
 *
 * The collection holds content only; anything derived from it — such as the
 * [InnovationDeck] — lives on the deck, never hardcoded here.
 */
data class InnovationCollection(
    val innovations: List<Innovation> = emptyList(),
) {
    /**
     * Returns a collection that also holds [innovation], unless an innovation
     * with the same [Innovation.name] is already present. The first-seen
     * innovation wins; the later one is ignored.
     */
    fun with(innovation: Innovation): InnovationCollection =
        if (innovations.any { it.name == innovation.name }) this else copy(innovations = innovations + innovation)

    /** The sum of every unlocked innovation's survival limit bonus. */
    fun survivalLimitBonus(): Int = innovations.sumOf { it.survivalLimitBonus }

    companion object {
        /**
         * Resolves [names] against [catalog].
         *
         * Unknown-name rule: a name absent from [catalog] is ignored, never
         * invented and never an error. The result keeps the order [names] gives,
         * and a repeated name resolves to the same innovation only once.
         */
        fun of(
            names: Collection<String>,
            catalog: List<Innovation>,
        ): InnovationCollection {
            val byName = catalog.associateBy { it.name }
            return names
                .mapNotNull { byName[it] }
                .fold(InnovationCollection()) { collection, innovation -> collection.with(innovation) }
        }
    }
}

/**
 * The deck a settlement draws from: every unlocked innovation name plus that
 * innovation's [Innovation.consequences], deduped and sorted.
 */
data class InnovationDeck(
    val cards: List<String>,
) {
    companion object {
        fun of(collection: InnovationCollection): InnovationDeck =
            InnovationDeck(
                collection.innovations
                    .flatMap { listOf(it.name) + it.consequences }
                    .distinct()
                    .sorted(),
            )
    }
}

package io.kdmtracker.domain

/**
 * Step-6 principle categories with their two official choices each
 * (research §2 step 6, from Fandom Principle: Death / New Life / Society).
 * A choice is permanent once saved — one row per (settlement, category).
 */
enum class PrincipleCategory(
    val label: String,
    val choices: List<String>,
) {
    DEATH(
        "Principle: Death",
        listOf("The First Harvest — Cannibalize", "The First Grave — Graves"),
    ),
    NEW_LIFE(
        "Principle: New Life",
        listOf("Without Mercy — Survival of the Fittest", "With Affection — Protect the Young"),
    ),
    SOCIETY(
        "Principle: Society",
        listOf("Collective Toil", "Accept Darkness"),
    ),
}

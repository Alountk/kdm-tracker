package io.kdmtracker.domain

/**
 * The five official milestone story events from the Settlement Record Sheet
 * ("Milestone Story Events"; research §2 step 6). Titles and conditions are the
 * official sheet's, verbatim.
 */
enum class MilestoneKey(
    val title: String,
    val condition: String,
) {
    PRINCIPLE_DEATH("Principle: Death", "1 death"),
    PRINCIPLE_NEW_LIFE("Principle: New Life", "first child is born"),
    PRINCIPLE_SOCIETY("Principle: Society", "population reaches 15"),
    HOODED_KNIGHT("Hooded Knight", "settlement has 5 innovations"),
    GAME_OVER("Game Over", "population reaches 0"),
}

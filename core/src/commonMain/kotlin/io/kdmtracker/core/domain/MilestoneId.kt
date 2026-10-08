package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A stable, open identifier for a milestone story event.
 *
 * The id is a value class over a string so content packs can introduce milestones
 * (Strain, Patterns, …) without editing `:core`. The shipped 1.6 ids live in
 * [Core16Milestones]; nothing about a milestone is closed to this enumeration.
 */
@Serializable
@JvmInline
value class MilestoneId(
    val value: String,
)

/** The five official Kingdom Death: Monster 1.6 milestone ids. */
object Core16Milestones {
    val PRINCIPLE_DEATH: MilestoneId = MilestoneId("principle-death")
    val PRINCIPLE_NEW_LIFE: MilestoneId = MilestoneId("principle-new-life")
    val PRINCIPLE_SOCIETY: MilestoneId = MilestoneId("principle-society")
    val HOODED_KNIGHT: MilestoneId = MilestoneId("hooded-knight")
    val GAME_OVER: MilestoneId = MilestoneId("game-over")

    val ALL: List<MilestoneId> =
        listOf(PRINCIPLE_DEATH, PRINCIPLE_NEW_LIFE, PRINCIPLE_SOCIETY, HOODED_KNIGHT, GAME_OVER)

    /**
     * The [PrincipleCategory] a principle milestone resolves into, or `null` for a milestone
     * that is not a principle event (Hooded Knight, Game Over).
     */
    fun principleCategoryOf(id: MilestoneId): PrincipleCategory? =
        when (id) {
            PRINCIPLE_DEATH -> PrincipleCategory.DEATH
            PRINCIPLE_NEW_LIFE -> PrincipleCategory.NEW_LIFE
            PRINCIPLE_SOCIETY -> PrincipleCategory.SOCIETY
            else -> null
        }
}

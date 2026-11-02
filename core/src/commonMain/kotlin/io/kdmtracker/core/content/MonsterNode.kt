package io.kdmtracker.core.content

import kotlinx.serialization.Serializable

/**
 * The slots of the monster campaign, as the official "Monster Nodes" page names them. Monsters sharing a
 * node replace one another: a settlement has one monster per node.
 */
@Serializable
enum class MonsterNode(
    val quarry: Boolean,
) {
    NQ1(quarry = true),
    NQ2(quarry = true),
    NQ3(quarry = true),
    NQ4(quarry = true),
    NN1(quarry = false),
    NN2(quarry = false),
    NN3(quarry = false),
    CO(quarry = false),
    FI(quarry = false),
}

/** A monster a pack puts on [node]. */
@Serializable
data class NodeMonster(
    val name: String,
    val node: MonsterNode,
)

/** A node several enabled packs put a monster on, offered to the players when an expansion joins it. */
data class NodeContest(
    val node: MonsterNode,
    val candidates: List<String>,
    val default: String,
)

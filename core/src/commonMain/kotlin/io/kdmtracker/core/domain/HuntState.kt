package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/** The kind of a hunt-board space: the special spaces plus normal event spaces. */
enum class HuntSpaceKind { START, NORMAL, MONSTER_E, OVERWHELMING_DARKNESS, STARVATION, MONSTER }

/**
 * One space of the 13-space hunt board.
 *
 * @property kind the space's role; the layout is player-set (the per-monster diagram is an image).
 * @property events the hunt-event names placed on this space.
 * @property resolved the subset of [events] already resolved.
 * @property roll the 2d10 result recorded the first time the party lands here (S6), or `null`
 *   before any visit. Per-step and persistent: moving back shows this step's own roll, never the
 *   same one. The player may edit it by hand.
 */
@Serializable
data class HuntSpace(
    val kind: HuntSpaceKind = HuntSpaceKind.NORMAL,
    val events: List<String> = emptyList(),
    val resolved: List<String> = emptyList(),
    val roll: Int? = null,
)

/**
 * The Hunting Phase's tracked state for the current lantern year.
 *
 * The hunt board has thirteen spaces; the party is the Step-8 departing survivors, optionally
 * assigned to players (1..[players]). The d100 hunt-event table lives on the board, so "random
 * events" are recorded by hand — they are not [events].
 *
 * @property quarry the chosen quarry (a catalog quarry name), or `null` before setup.
 * @property level the monster level (1–3).
 * @property players the number of players (1–4).
 * @property assignments the party's player assignment, keyed by survivor id (1-based player index).
 * @property spaces the thirteen board spaces once set up.
 * @property position the current space index of the survivor party.
 * @property monsterPosition the current space index of the monster's miniature; starts on the last
 *   space (the `MONSTER` space) once set up and steps toward the party with
 *   [io.kdmtracker.core.usecase.MoveMonster]. `-1` when no board is set up.
 * @property status the hunt outcome; [HuntStatus.ACTIVE] while in progress.
 * @property drawnEvent the hunt-event card most recently drawn on advance, or `null` before the
 *   first advance (S5). A transient view of the active card: the player resolves it by hand; the
 *   resolved history lives in [HuntSpace.resolved], not here.
 */
@Serializable
data class HuntState(
    val quarry: String? = null,
    val level: Int = 1,
    val players: Int = 1,
    val assignments: Map<Ulid, Int> = emptyMap(),
    val spaces: List<HuntSpace> = emptyList(),
    val position: Int = 0,
    val monsterPosition: Int = spaces.lastIndex,
    val status: HuntStatus = HuntStatus.ACTIVE,
    val drawnEvent: HuntEvent? = null,
)

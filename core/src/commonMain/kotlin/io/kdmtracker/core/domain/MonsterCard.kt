package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/** Which part of a monster's showdown content a card belongs to. */
enum class MonsterCardKind { AI, BASIC_ACTION, HIT_LOCATION }

/**
 * A monster showdown card: an AI card, the Basic Action card, or a hit location.
 *
 * @property monster the catalog monster the card belongs to (the quarry/nemesis name).
 * @property rank an AI card's rank letter (`B`/`A`/`L`/`S`), or `null` when not an AI card or unreadable.
 */
@Serializable
data class MonsterCard(
    val name: String,
    val text: String,
    val kind: MonsterCardKind,
    val monster: String,
    val rank: String? = null,
)

package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/** Which deck a hunt event belongs to. */
enum class HuntEventKind { BASIC, SPECIAL, MONSTER }

/**
 * A Hunt Event card: the deck it belongs to ([kind]), its display [name] and its rules [text].
 *
 * A [HuntEventKind.MONSTER] event carries the [monster] it belongs to (the catalog's quarry name).
 * The d100 hunt-event table lives on the hunt board, not in these decks, so a "random event" is
 * recorded by hand — it is not a [HuntEvent].
 */
@Serializable
data class HuntEvent(
    val name: String,
    val text: String,
    val kind: HuntEventKind,
    val monster: String? = null,
)

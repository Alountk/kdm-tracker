package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A Settlement Event card: a stable display [name] and its rules [text].
 *
 * Used for the Settlement Event deck drawn during Settlement Phase step 4; the
 * event's text is shown to the table, never evaluated.
 *
 * @property name the card's unique, human-readable name.
 * @property text the card's rules text.
 */
@Serializable
data class SettlementEvent(
    val name: String,
    val text: String,
)

package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A craftable gear card.
 *
 * @property name the gear's unique, human-readable name.
 * @property keywords the card's keywords (e.g. `arma`, `melé`, `hueso`).
 * @property rules the card's rules text, shown to the player; never evaluated.
 * @property modifiers the gear's structured bonuses. Stats that are icon-only on the source
 *   card are not modeled here.
 */
@Serializable
data class Gear(
    val name: String,
    val keywords: List<String> = emptyList(),
    val rules: String = "",
    val modifiers: List<Modifier> = emptyList(),
)

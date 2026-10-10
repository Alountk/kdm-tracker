package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A craftable gear card.
 *
 * @property id the gear's stable, language-neutral identity.
 * @property names the gear's localized display names.
 * @property keywords the card's keywords (e.g. `arma`, `melé`, `hueso`).
 * @property rules the card's rules text, shown to the player; never evaluated.
 * @property modifiers the gear's structured bonuses. Stats that are icon-only on the source
 *   card are not modeled here.
 */
@Serializable
data class Gear(
    val id: GearId,
    val names: LocalizedText,
    val keywords: List<String> = emptyList(),
    val rules: String = "",
    val modifiers: List<Modifier> = emptyList(),
)

package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A minimal named content card: a display [name] and its rules [description].
 *
 * Used for content that carries no extra typed structure beyond its text — the
 * fighting arts and the disorders. When a card needs more than name + description,
 * it earns its own typed definition (see [Innovation], [ResourceDefinition]).
 *
 * @property name the card's unique, human-readable name.
 * @property description the rules text shown on the card.
 */
@Serializable
data class ContentEntry(
    val name: String,
    val description: String,
)

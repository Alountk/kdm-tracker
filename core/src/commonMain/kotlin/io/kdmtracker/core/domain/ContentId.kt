package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A resource's stable, language-neutral identity.
 *
 * The value is a human-chosen slug (for example `hide.white_lion`), never a display name: it is
 * what recipes, ledger entries and storage lines key on, so re-translating a card can never orphan
 * the data that refers to it. The type supplies the namespace, so the slug carries none.
 */
@JvmInline
@Serializable
value class ResourceId(
    val value: String,
)

/** A settlement location's stable, language-neutral identity (for example `bone_smith`). */
@JvmInline
@Serializable
value class LocationId(
    val value: String,
)

/** A gear card's stable, language-neutral identity (for example `bone_dagger`). */
@JvmInline
@Serializable
value class GearId(
    val value: String,
)

/** An innovation's stable, language-neutral identity (for example `language`). */
@JvmInline
@Serializable
value class InnovationId(
    val value: String,
)

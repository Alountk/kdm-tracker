package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A location's recipe for crafting one gear.
 *
 * @property gearName the name of the [Gear] this recipe produces.
 * @property resourceCost the resources required, keyed by resource name.
 * @property requires the names of the innovations that must already be unlocked.
 */
@Serializable
data class GearRecipe(
    val gearName: String,
    val resourceCost: Map<String, Int> = emptyMap(),
    val requires: List<String> = emptyList(),
)

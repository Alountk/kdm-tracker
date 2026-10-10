package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A location's recipe for crafting one gear.
 *
 * @property gearId the id of the [Gear] this recipe produces.
 * @property resourceCost the resources required, keyed by resource id.
 * @property requires the ids of the innovations that must already be unlocked.
 * @property requiresText the card's prerequisite label when it is **not** an innovation name
 *   (e.g. `Calor`, a state granted by an innovation). Display only — it does not gate crafting.
 */
@Serializable
data class GearRecipe(
    val gearId: GearId,
    val resourceCost: Map<ResourceId, Int> = emptyMap(),
    val requires: List<InnovationId> = emptyList(),
    val requiresText: String? = null,
)

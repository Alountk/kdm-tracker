package io.kdmtracker.core.content

import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.ResourceMaterial

/**
 * A [ResourceDefinition] for tests, with the fixture's name doubling as both the id and the
 * Spanish name.
 *
 * Fixtures care about identity, grouping and merge behaviour, not about a card's real slug — so
 * the name is the shortest thing that keeps an assertion like `catalog.resources contains "bone"`
 * meaningful. Real ids come from the identity CSV, never from here. A fixture that needs a
 * wildcard passes [materials] as a set of several.
 */
fun resourceDefinition(
    name: String,
    category: ResourceCategory,
    material: ResourceMaterial? = null,
): ResourceDefinition =
    ResourceDefinition(
        id = ResourceId(name),
        names = LocalizedText(es = name),
        category = category,
        materials = material?.let { setOf(it) } ?: emptySet(),
    )

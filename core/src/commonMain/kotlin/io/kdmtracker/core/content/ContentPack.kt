package io.kdmtracker.core.content

import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.SettlementLocation
import io.kdmtracker.core.domain.TimelineEntry
import kotlinx.serialization.Serializable

/**
 * The typed definitions a pack contributes to a campaign.
 *
 * Packs carry the definitions the domain reads — innovations with their consequences,
 * locations with their prerequisites, resources with their category, timeline entries,
 * and the quarry/nemesis name lists — not bare strings. The catalog derives the
 * display-name lists from these.
 */
@Serializable
data class ContentBundle(
    val innovations: List<Innovation> = emptyList(),
    val locations: List<SettlementLocation> = emptyList(),
    val resources: List<ResourceDefinition> = emptyList(),
    val timeline: List<TimelineEntry> = emptyList(),
    val quarries: List<String> = emptyList(),
    val nemeses: List<String> = emptyList(),
)

@Serializable
data class ContentPack(
    val id: String,
    val version: String,
    val name: String,
    val requires: List<String> = emptyList(),
    val bundle: ContentBundle = ContentBundle(),
)

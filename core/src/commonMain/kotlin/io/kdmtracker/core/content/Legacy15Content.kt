package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15FightingArts
import io.kdmtracker.core.content.generated.Core15Innovations
import io.kdmtracker.core.content.generated.Core15Locations
import io.kdmtracker.core.content.generated.Core15Resources

/**
 * The `core-1.5` content pack — the Spanish 1.5 card content ingested by
 * `tools/content-ingest/`.
 *
 * It assembles the generated bundles (innovations with their consequence graph,
 * resources with their typed category, fighting arts and disorders with their effect
 * text, and the settlement locations) plus the hand-curated [Core15GearSeed] into a single
 * [ContentPack]. The generated Kotlin is the source of truth for the generated parts; the
 * gear seed is curated (see its KDoc).
 */
object Legacy15Content {
    val pack: ContentPack =
        ContentPack(
            id = "core-1.5",
            version = "1.5",
            name = "Kingdom Death 1.5 (ES)",
            bundle =
                ContentBundle(
                    innovations = Core15Innovations.bundle.innovations,
                    locations = Core15Locations.bundle.locations + Core15GearSeed.bundle.locations,
                    gear = Core15GearSeed.bundle.gear,
                    resources = Core15Resources.bundle.resources,
                    fightingArts = Core15FightingArts.bundle.fightingArts,
                    disorders = Core15FightingArts.bundle.disorders,
                ),
        )
}

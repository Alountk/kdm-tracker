package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15Innovations
import io.kdmtracker.core.content.generated.Core15Resources

/**
 * The `core-1.5` content pack — the Spanish 1.5 card content ingested by
 * `tools/content-ingest/`.
 *
 * It assembles the generated bundles (innovations with their consequence graph,
 * resources with their typed category) into a single [ContentPack]. The generated
 * Kotlin is the source of truth; this object only wires it up, so the pack has no
 * hand-transcribed content of its own.
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
                    resources = Core15Resources.bundle.resources,
                ),
        )
}

package io.kdmtracker.app

import io.kdmtracker.core.content.CatalogResult
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentCatalogComposer
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.content.Core16Content
import io.kdmtracker.core.content.Legacy15Content

/** Resolves a campaign's enabled content packs into a catalog. */
internal object PackCatalogs {
    private val knownPacks: Map<String, ContentPack> =
        listOf(Core16Content.pack, Legacy15Content.pack).associateBy { it.id }

    /** A catalog with no packs, used when composition fails so the screens render nothing. */
    val EMPTY = ContentCatalog(emptyList())

    /** An unknown pack id has nothing to compose, so it is reported as its own missing requirement. */
    fun compose(enabled: List<String>): CatalogResult {
        val unknown = enabled.firstOrNull { it !in knownPacks }
        return if (unknown != null) {
            CatalogResult.MissingRequirement(packId = unknown, missing = unknown)
        } else {
            ContentCatalogComposer.compose(enabled.map(knownPacks::getValue))
        }
    }

    /** Why a composition left the catalog empty, or `null` when it composed cleanly. */
    fun errorOf(result: CatalogResult): String? {
        val missing = result as? CatalogResult.MissingRequirement ?: return null
        return if (missing.packId == missing.missing) {
            "Cannot compose the campaign catalog: unknown content pack '${missing.packId}'"
        } else {
            "Cannot compose the campaign catalog: content pack '${missing.packId}' requires '${missing.missing}'"
        }
    }
}

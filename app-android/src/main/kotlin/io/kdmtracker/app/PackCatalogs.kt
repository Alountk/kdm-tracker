package io.kdmtracker.app

import io.kdmtracker.core.content.CatalogResult
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentCatalogComposer
import io.kdmtracker.core.content.PackRegistry

/** Resolves a campaign's enabled content packs into a catalog. */
internal object PackCatalogs {
    /** A catalog with no packs, used when composition fails so the screens render nothing. */
    val EMPTY = ContentCatalog(emptyList())

    /** An unknown pack id has nothing to compose, so it is reported as its own missing requirement. */
    fun compose(
        enabled: List<String>,
        registry: PackRegistry = PackRegistry.shipped,
    ): CatalogResult {
        val packs =
            enabled.map {
                registry.find(it)
                    ?: return CatalogResult.MissingRequirement(packId = it, missing = it)
            }
        return ContentCatalogComposer.compose(packs)
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

package io.kdmtracker.core.content

sealed interface CatalogResult {
    data class Ok(
        val catalog: ContentCatalog,
    ) : CatalogResult

    data class MissingRequirement(
        val packId: String,
        val missing: String,
    ) : CatalogResult
}

data class ContentCatalog(
    val packs: List<ContentPack>,
) {
    val innovations: List<String> = union { it.provideInnovations }
    val locations: List<String> = union { it.provideLocations }
    val quarries: List<String> = union { it.provideQuarries }
    val nemeses: List<String> = union { it.provideNemeses }
    val resources: List<String> = union { it.provideResources }

    private inline fun union(provide: (ContentPack) -> List<String>): List<String> =
        packs.flatMap(provide).distinct().sorted()
}

object ContentCatalogComposer {
    fun compose(packs: List<ContentPack>): CatalogResult {
        val available = packs.mapTo(mutableSetOf()) { it.id }
        for (pack in packs) {
            val missing = pack.requires.firstOrNull { it !in available } ?: continue
            return CatalogResult.MissingRequirement(packId = pack.id, missing = missing)
        }
        return CatalogResult.Ok(ContentCatalog(packs))
    }
}

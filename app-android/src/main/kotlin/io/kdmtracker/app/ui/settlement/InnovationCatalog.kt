package io.kdmtracker.app.ui.settlement

import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.rules.InnovationStatus
import io.kdmtracker.core.rules.InnovationView

/**
 * The catalogue browser's acquisition filter — the maintainer's three chips (`Todas`,
 * `Adquiridas`, `No adquiridas`). They are the first group the design renders; categories
 * (spec decision 4) come only after them.
 */
enum class Acquisition(
    val label: String,
) {
    ALL("Todas"),
    ACQUIRED("Adquiridas"),
    NOT_ACQUIRED("No adquiridas"),
    ;

    /** Whether [card] survives this acquisition chip. */
    fun accepts(card: CatalogCard): Boolean =
        when (this) {
            ALL -> true
            ACQUIRED -> card.status == InnovationStatus.UNLOCKED
            NOT_ACQUIRED -> card.status != InnovationStatus.UNLOCKED
        }
}

/** One card of the catalogue browser: the classified view plus the design's status footer. */
data class CatalogCard(
    val id: InnovationId,
    val name: String,
    val category: String,
    val effect: String,
    val status: InnovationStatus,
    val requires: String?,
    val footer: String,
)

/** One chip of either group: `label (count)`, and whether the browser's filter selected it. */
data class CatalogChip(
    val label: String,
    val count: Int,
    val selected: Boolean,
)

/** One chip row of the browser. The acquisition group renders first, always. */
data class CatalogChipGroup(
    val title: String,
    val chips: List<CatalogChip>,
)

/**
 * The catalogue view's values (the design's estados B and C), derived from the full card list and
 * the two filters. Every count observes the *other* group's filter — with `No adquiridas` active
 * the category chips drop the acquired card, exactly as `Arte (11)` becomes `Arte (10)` in the
 * design. The hand is never an input: filters shape the browser only.
 */
data class CatalogBrowser(
    val cards: List<CatalogCard>,
    val groups: List<CatalogChipGroup>,
    val countBar: String?,
    val subtitle: String,
    val summary: String,
)

/**
 * Builds the catalogue browser over the campaign's classified innovations.
 *
 * Ordering is [InnovationStatus] (acquired, then available, then no access) and, within a status,
 * by name — the order the design's grids draw. Nothing here touches the offer: the browser is a
 * read-only view of what the settlement owns and could still discover.
 */
object InnovationCatalog {
    const val ACQUISITION_TITLE = "ESTADO DE POSESIÓN"
    const val CATEGORY_TITLE = "CATEGORÍAS TEMÁTICAS"

    /** The catalogue in display order: acquired first, then available, then locked, by name. */
    fun cards(views: List<InnovationView>): List<CatalogCard> =
        views
            .map { view ->
                CatalogCard(
                    id = view.innovation.id,
                    name = view.innovation.names.es,
                    category = view.innovation.category,
                    effect = view.innovation.description,
                    status = view.status,
                    requires = view.missingParent,
                    footer = footerFor(view.status),
                )
            }.sortedWith(compareBy({ it.status.ordinal }, { it.name }))

    /**
     * The browser for one filter pair. [acquisition] picks the first chip group, [category] the
     * second (`null` = no category selected); each group counts over the other group's filter.
     */
    fun browser(
        cards: List<CatalogCard>,
        acquisition: Acquisition,
        category: String?,
    ): CatalogBrowser {
        val inCategory = cards.filter { category == null || it.category == category }
        val visible = inCategory.filter { acquisition.accepts(it) }

        val acquisitionChips =
            Acquisition.entries.map { candidate ->
                CatalogChip(
                    label = candidate.label,
                    count = inCategory.count { candidate.accepts(it) },
                    selected = candidate == acquisition,
                )
            }
        val categoryChips =
            cards
                .map { it.category }
                .distinct()
                .sorted()
                .map { candidate ->
                    CatalogChip(
                        label = candidate,
                        count = cards.count { it.category == candidate && acquisition.accepts(it) },
                        selected = candidate == category,
                    )
                }

        val filtered = acquisition != Acquisition.ALL || category != null
        return CatalogBrowser(
            cards = visible,
            groups =
                listOf(
                    CatalogChipGroup(ACQUISITION_TITLE, acquisitionChips),
                    CatalogChipGroup(CATEGORY_TITLE, categoryChips),
                ),
            countBar =
                if (filtered) {
                    "${visible.size} de ${cards.size} cartas — desliza para ver más"
                } else {
                    null
                },
            subtitle =
                when (acquisition) {
                    Acquisition.ALL -> "Catálogo general abierto (${cards.size} totales)"
                    Acquisition.ACQUIRED -> "Mostrando cartas adquiridas"
                    Acquisition.NOT_ACQUIRED -> "Mostrando cartas por desbloquear"
                },
            summary = summaryFor(inCategory, acquisition),
        )
    }

    private fun footerFor(status: InnovationStatus): String =
        when (status) {
            InnovationStatus.UNLOCKED -> "ADQUIRIDA"
            InnovationStatus.AVAILABLE -> "DISPONIBLE"
            InnovationStatus.NO_ACCESS -> "SIN ACCESO"
        }

    /** The design's right-hand line of `ESTADO DE POSESIÓN`, over the category-filtered set. */
    private fun summaryFor(
        inCategory: List<CatalogCard>,
        acquisition: Acquisition,
    ): String {
        val acquired = inCategory.count { it.status == InnovationStatus.UNLOCKED }
        return when (acquisition) {
            Acquisition.ALL -> "$acquired / ${inCategory.size} Activas"
            Acquisition.ACQUIRED -> "$acquired Adquiridas"
            Acquisition.NOT_ACQUIRED -> "${inCategory.size - acquired} Por Desbloquear"
        }
    }
}

package io.kdmtracker.app

import io.kdmtracker.app.ui.settlement.Acquisition
import io.kdmtracker.app.ui.settlement.InnovationCatalog
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.rules.InnovationStatus
import io.kdmtracker.core.rules.InnovationView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the catalogue browser behind the deck row (the design's `Innovaciones: Catálogo — entrada y
 * consulta`, cached at `.superpowers/design/innovar-flow/catalogo.html`).
 *
 * The chips are two groups and the acquisition group renders first (spec decision 4 — the
 * maintainer's words lead, categories follow). Each group counts over the *other* group's active
 * filter, which is what the design shows: with `No adquiridas` active the category counts drop the
 * acquired card (`Arte (11)` → `Arte (10)`). The hand is never an input here — filters only shape
 * the browser.
 */
class InnovationCatalogTest {
    private fun innovation(
        id: String,
        es: String,
        category: String,
        description: String = "Efecto de $es.",
    ) = Innovation(InnovationId(id), LocalizedText(es = es), description, emptyList(), 0, category)

    private fun view(
        card: Innovation,
        status: InnovationStatus,
        missingParent: String? = null,
    ) = InnovationView(card, status, missingParent = missingParent)

    private val pintura = view(innovation("pintura", "Pintura", "Arte"), InnovationStatus.UNLOCKED)
    private val escultura = view(innovation("escultura", "Escultura", "Arte"), InnovationStatus.AVAILABLE)
    private val lenguaje = view(innovation("lenguaje", "Lenguaje", "Sociedad"), InnovationStatus.AVAILABLE)
    private val amoniaco =
        view(innovation("amoniaco", "Amoníaco", "Ciencia"), InnovationStatus.NO_ACCESS, missingParent = "Pintura")
    private val cantos =
        view(innovation("cantos", "Cantos", "Creencia"), InnovationStatus.NO_ACCESS, missingParent = "Lenguaje")

    /** Shuffled on purpose: the browser imposes its own order. */
    private val views = listOf(cantos, escultura, pintura, amoniaco, lenguaje)

    private val cards by lazy { InnovationCatalog.cards(views) }

    private fun browser(
        acquisition: Acquisition = Acquisition.ALL,
        category: String? = null,
    ) = InnovationCatalog.browser(cards, acquisition, category)

    @Test
    fun theAcquisitionChipsComeFirstWithCounts() {
        val groups = browser().groups

        assertEquals(2, groups.size)
        assertEquals("ESTADO DE POSESIÓN", groups[0].title)
        assertEquals(
            listOf("Todas" to 5, "Adquiridas" to 1, "No adquiridas" to 4),
            groups[0].chips.map { it.label to it.count },
        )
        assertEquals("CATEGORÍAS TEMÁTICAS", groups[1].title)
    }

    @Test
    fun theAcquisitionChipsAreTheOnlyOnesActiveByDefault() {
        val acquisition = browser().groups[0].chips

        assertTrue(acquisition.single { it.label == "Todas" }.selected)
        assertTrue(acquisition.none { it.label != "Todas" && it.selected })
    }

    @Test
    fun theCategoryChipsCountOverTheAcquisitionFilter() {
        val categories = browser(acquisition = Acquisition.NOT_ACQUIRED).groups[1].chips

        // Pintura is acquired, so Arte loses one — the design's `Arte (11)` → `Arte (10)`.
        assertEquals(
            listOf("Arte" to 1, "Ciencia" to 1, "Creencia" to 1, "Sociedad" to 1),
            categories.map { it.label to it.count },
        )
    }

    @Test
    fun theAcquisitionChipsCountOverTheCategoryFilter() {
        val acquisition = browser(category = "Arte").groups[0].chips

        assertEquals(
            listOf("Todas" to 2, "Adquiridas" to 1, "No adquiridas" to 1),
            acquisition.map { it.label to it.count },
        )
    }

    @Test
    fun theSelectedChipsAreMarked() {
        val browser = browser(acquisition = Acquisition.NOT_ACQUIRED, category = "Ciencia")

        assertEquals(
            listOf(false, false, true),
            browser.groups[0].chips.map { it.selected },
        )
        assertEquals(
            listOf("Arte" to false, "Ciencia" to true, "Creencia" to false, "Sociedad" to false),
            browser.groups[1].chips.map { it.label to it.selected },
        )
    }

    @Test
    fun theCatalogIsSortedAcquiredFirstThenByName() {
        assertEquals(
            listOf("Pintura", "Escultura", "Lenguaje", "Amoníaco", "Cantos"),
            cards.map { it.name },
        )
    }

    @Test
    fun everyCardCarriesItsAccessState() {
        val byName = cards.associateBy { it.name }

        assertEquals("ADQUIRIDA", byName.getValue("Pintura").footer)
        assertEquals("DISPONIBLE", byName.getValue("Escultura").footer)
        assertEquals("SIN ACCESO", byName.getValue("Cantos").footer)
        assertEquals("Lenguaje", byName.getValue("Cantos").requires)
        assertNull(byName.getValue("Escultura").requires)
    }

    @Test
    fun theBrowserFiltersByAcquisitionAndCategory() {
        assertEquals(
            listOf("Escultura"),
            browser(acquisition = Acquisition.NOT_ACQUIRED, category = "Arte").cards.map { it.name },
        )
        assertEquals(
            listOf("Pintura"),
            browser(acquisition = Acquisition.ACQUIRED).cards.map { it.name },
        )
        assertEquals(
            emptyList(),
            browser(acquisition = Acquisition.ACQUIRED, category = "Ciencia").cards.map { it.name },
        )
    }

    @Test
    fun theCountBarAppearsOnlyWhileFiltered() {
        assertNull(browser().countBar)
        assertEquals(
            "4 de 5 cartas — desliza para ver más",
            browser(acquisition = Acquisition.NOT_ACQUIRED).countBar,
        )
        assertEquals(
            "2 de 5 cartas — desliza para ver más",
            browser(category = "Arte").countBar,
        )
    }

    @Test
    fun theDeckRowSubtitleNamesTheAcquisitionState() {
        assertEquals("Catálogo general abierto (5 totales)", browser().subtitle)
        assertEquals("Mostrando cartas por desbloquear", browser(acquisition = Acquisition.NOT_ACQUIRED).subtitle)
        assertEquals("Mostrando cartas adquiridas", browser(acquisition = Acquisition.ACQUIRED).subtitle)
    }

    @Test
    fun theSummaryCountsThePossessionState() {
        assertEquals("1 / 5 Activas", browser().summary)
        assertEquals("4 Por Desbloquear", browser(acquisition = Acquisition.NOT_ACQUIRED).summary)
        assertEquals("1 Adquiridas", browser(acquisition = Acquisition.ACQUIRED).summary)
    }
}

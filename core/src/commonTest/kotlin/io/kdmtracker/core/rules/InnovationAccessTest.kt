package io.kdmtracker.core.rules

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.SettlementState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InnovationAccessTest {
    private val language =
        Innovation(
            InnovationId("lenguaje"),
            LocalizedText(es = "Lenguaje"),
            "…",
            consequences = listOf("Choza", "Pintura"),
        )
    private val choza =
        Innovation(InnovationId("choza"), LocalizedText(es = "Choza"), "…", consequences = listOf("Cama"))
    private val pintura = Innovation(InnovationId("pintura"), LocalizedText(es = "Pintura"), "…")
    private val cama = Innovation(InnovationId("cama"), LocalizedText(es = "Cama"), "…")

    private val catalog =
        ContentCatalog(
            listOf(
                ContentPack(
                    id = "p",
                    version = "1",
                    name = "p",
                    bundle = ContentBundle(innovations = listOf(language, choza, pintura, cama)),
                ),
            ),
        )

    private fun state(unlocked: List<String>): SettlementState =
        SettlementState(innovations = InnovationCollection.of(unlocked, listOf(language, choza, pintura, cama)))

    private fun statusOf(
        views: List<InnovationView>,
        name: String,
    ): InnovationView = views.single { it.innovation.names.es == name }

    @Test
    fun unlockedAndAvailableAndNoAccessAreClassified() {
        val views = InnovationAccess.view(state(listOf("Lenguaje")), catalog)

        assertEquals(InnovationStatus.UNLOCKED, statusOf(views, "Lenguaje").status)
        assertNull(statusOf(views, "Lenguaje").missingParent)
        assertEquals(InnovationStatus.AVAILABLE, statusOf(views, "Choza").status)
        assertEquals(InnovationStatus.AVAILABLE, statusOf(views, "Pintura").status)
        assertEquals(InnovationStatus.NO_ACCESS, statusOf(views, "Cama").status)
        assertEquals("Choza", statusOf(views, "Cama").missingParent)
    }

    @Test
    fun unlockingAParentMakesItsConsequenceAvailable() {
        val views = InnovationAccess.view(state(listOf("Lenguaje", "Choza")), catalog)

        assertEquals(InnovationStatus.UNLOCKED, statusOf(views, "Choza").status)
        assertEquals(InnovationStatus.AVAILABLE, statusOf(views, "Cama").status)
    }
}

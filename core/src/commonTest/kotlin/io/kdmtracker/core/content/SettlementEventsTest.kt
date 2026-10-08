package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15SettlementEvents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementEventsTest {
    private val settlementEvents = Legacy15Content.pack.bundle.settlementEvents

    @Test
    fun settlementEventsAreNotEmpty() {
        assertTrue(settlementEvents.isNotEmpty(), "expected generated 1.5 settlement events")
    }

    @Test
    fun settlementEventsHaveTheExpectedCardCount() {
        assertEquals(23, settlementEvents.size)
    }

    @Test
    fun knownEventIsPresentWithNonBlankText() {
        val rivalidad = settlementEvents.first { it.name == "Rivalidad" }

        assertTrue(rivalidad.text.isNotBlank(), "expected the Rivalidad event text")
    }

    @Test
    fun packCarriesTheGeneratedSettlementEvents() {
        assertEquals(Core15SettlementEvents.bundle.settlementEvents, settlementEvents)
    }

    @Test
    fun namesAreSorted() {
        assertEquals(settlementEvents.map { it.name }.sorted(), settlementEvents.map { it.name })
    }
}

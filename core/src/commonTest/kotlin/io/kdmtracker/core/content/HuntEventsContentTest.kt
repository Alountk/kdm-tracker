package io.kdmtracker.core.content

import io.kdmtracker.core.content.generated.Core15HuntEvents
import io.kdmtracker.core.domain.HuntEventKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HuntEventsContentTest {
    private val huntEvents = Legacy15Content.pack.bundle.huntEvents

    @Test
    fun huntEventsAreNotEmpty() {
        assertTrue(huntEvents.isNotEmpty(), "expected generated 1.5 hunt events")
    }

    @Test
    fun basicSpecialAndMonsterArePresent() {
        assertTrue(huntEvents.any { it.kind == HuntEventKind.BASIC }, "expected a basic event")
        assertTrue(huntEvents.any { it.kind == HuntEventKind.SPECIAL }, "expected a special event")
        assertTrue(
            huntEvents.any { it.kind == HuntEventKind.MONSTER && it.monster == "White Lion" },
            "expected a White Lion monster event",
        )
    }

    @Test
    fun randomEventDeduplicatesToOne() {
        assertEquals(1, huntEvents.count { it.name == "Evento Aleatorio" })
    }

    @Test
    fun packCarriesTheGeneratedHuntEvents() {
        assertEquals(Core15HuntEvents.bundle.huntEvents, huntEvents)
    }
}

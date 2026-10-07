package io.kdmtracker.core.domain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettlementStatePhaseMarkersTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun aStateWithAMarkerReportsItAsUsed() {
        val state = SettlementState().withPhaseAction("Lantern Hoard")

        assertTrue(state.usedInPhase("Lantern Hoard"))
    }

    @Test
    fun aFreshStateReportsNoMarkers() {
        assertFalse(SettlementState().usedInPhase("Lantern Hoard"))
    }

    @Test
    fun withPhaseActionIsIdempotent() {
        val once = SettlementState().withPhaseAction("Lantern Hoard")
        val twice = once.withPhaseAction("Lantern Hoard")

        assertEquals(once, twice)
        assertEquals(setOf("Lantern Hoard"), twice.phaseActionsUsed)
    }

    @Test
    fun legacyJsonWithoutPhaseActionsUsedDecodesWithAnEmptySet() {
        val legacy =
            """
            {
              "deaths": 1,
              "births": 2
            }
            """.trimIndent()

        val decoded = json.decodeFromString<SettlementState>(legacy)

        assertEquals(emptySet(), decoded.phaseActionsUsed)
        assertEquals(1, decoded.deaths)
        assertEquals(2, decoded.births)
    }
}

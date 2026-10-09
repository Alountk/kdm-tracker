package io.kdmtracker.core.domain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShowdownStateTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val populated =
        ShowdownState(
            monster = "White Lion",
            level = 2,
            aiDeck = listOf("Zarpazo", "Bocado"),
            discard = listOf("Evaluar"),
            wounds = listOf("Atrapar"),
            lastDrawn = "Evaluar",
            status = ShowdownStatus.VICTORY,
        )

    @Test
    fun defaultShowdownStateRoundTripsThroughJson() {
        val decoded = json.decodeFromString<ShowdownState>(json.encodeToString(ShowdownState()))

        assertEquals(ShowdownState(), decoded)
    }

    @Test
    fun populatedShowdownStateRoundTrips() {
        val decoded = json.decodeFromString<ShowdownState>(json.encodeToString(populated))

        assertEquals(populated, decoded)
    }

    @Test
    fun jsonMissingShowdownFieldDecodesWithDefaults() {
        val decoded = json.decodeFromString<SettlementState>("""{ "deaths": 1 }""")

        assertEquals(ShowdownState(), decoded.showdown)
        assertEquals(ShowdownStatus.ACTIVE, decoded.showdown.status)
        assertTrue(decoded.showdown.aiDeck.isEmpty())
    }

    @Test
    fun stateWithShowdownRoundTrips() {
        val state = SettlementState(showdown = populated)

        val decoded = json.decodeFromString<SettlementState>(json.encodeToString(state))

        assertEquals(state, decoded)
    }
}

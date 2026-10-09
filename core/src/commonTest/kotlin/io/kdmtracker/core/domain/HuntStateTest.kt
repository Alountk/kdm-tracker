package io.kdmtracker.core.domain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HuntStateTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val populated =
        HuntState(
            quarry = "White Lion",
            level = 2,
            players = 3,
            assignments = mapOf(Ulid.generate(1L) { 0 } to 1, Ulid.generate(2L) { 0 } to 2),
            spaces =
                listOf(
                    HuntSpace(HuntSpaceKind.START),
                    HuntSpace(
                        HuntSpaceKind.NORMAL,
                        events = listOf("Evento Aleatorio"),
                        resolved = listOf("Evento Aleatorio"),
                    ),
                    HuntSpace(HuntSpaceKind.MONSTER_E, events = listOf("Mar de Hierba Dorada")),
                    HuntSpace(HuntSpaceKind.OVERWHELMING_DARKNESS),
                    HuntSpace(HuntSpaceKind.MONSTER),
                ),
            position = 2,
            status = HuntStatus.QUARRY_FOUND,
        )

    @Test
    fun defaultHuntStateRoundTripsThroughJson() {
        val decoded = json.decodeFromString<HuntState>(json.encodeToString(HuntState()))

        assertEquals(HuntState(), decoded)
    }

    @Test
    fun populatedHuntStateRoundTrips() {
        val decoded = json.decodeFromString<HuntState>(json.encodeToString(populated))

        assertEquals(populated, decoded)
    }

    @Test
    fun jsonMissingHuntFieldDecodesWithDefaults() {
        val decoded = json.decodeFromString<SettlementState>("""{ "deaths": 1 }""")

        assertEquals(HuntState(), decoded.hunt)
        assertEquals(HuntStatus.ACTIVE, decoded.hunt.status)
        assertTrue(decoded.hunt.spaces.isEmpty())
    }

    @Test
    fun stateWithHuntRoundTrips() {
        val state = SettlementState(hunt = populated)

        val decoded = json.decodeFromString<SettlementState>(json.encodeToString(state))

        assertEquals(state, decoded)
    }
}

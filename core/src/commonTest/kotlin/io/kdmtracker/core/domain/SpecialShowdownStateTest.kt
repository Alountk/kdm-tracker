package io.kdmtracker.core.domain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpecialShowdownStateTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun record(vararg entries: TimelineEntry): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(1L) { 0 },
                    campaignId = Ulid.generate(2L) { 0 },
                    name = "Kingsport",
                    lanternYear = 16,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 11,
                    endeavors = 0,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state = SettlementState(timeline = Timeline(entries = entries.toList())),
        )

    @Test
    fun nemesisOfReturnsTheNemesisName() {
        val record = record(TimelineEntry(16, "Nemesis: The Butcher Lvl 2"))

        assertEquals("The Butcher", record.nemesisOf(16))
    }

    @Test
    fun nemesisOfReturnsNullForAPlainYear() {
        val record = record(TimelineEntry(16, "Story Event"))

        assertNull(record.nemesisOf(16))
        assertNull(record.nemesisOf(11))
    }

    @Test
    fun specialShowdownDefaultsFalseAndRoundTrips() {
        val state = SettlementState(specialShowdown = true)

        val decoded = json.decodeFromString<SettlementState>(json.encodeToString(state))

        assertEquals(state, decoded)
    }

    @Test
    fun missingFieldDefaultsSpecialShowdownToFalse() {
        val decoded = json.decodeFromString<SettlementState>("""{ "deaths": 1 }""")

        assertEquals(false, decoded.specialShowdown)
    }

    @Test
    fun nemesisOfIgnoresALabelWithoutTheColon() {
        val record = record(TimelineEntry(16, "Nemesis"))

        assertNull(record.nemesisOf(16))
    }

    @Test
    fun nemesisOfIgnoresAnEmptyName() {
        val record = record(TimelineEntry(16, "Nemesis:"))

        assertNull(record.nemesisOf(16))
    }

    @Test
    fun nemesisOfPicksTheNemesisAmongOtherEntries() {
        val record =
            record(
                TimelineEntry(16, "Story Event"),
                TimelineEntry(16, "Nemesis: The Hand Lvl 3"),
                TimelineEntry(16, "Settlement Event"),
            )

        assertEquals("The Hand", record.nemesisOf(16))
    }
}

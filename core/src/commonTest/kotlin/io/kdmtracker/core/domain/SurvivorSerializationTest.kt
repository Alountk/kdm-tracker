package io.kdmtracker.core.domain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurvivorSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val populated =
        Survivor(
            id = Ulid.generate(1L) { 0 },
            name = "Eli",
            sex = Sex.FEMALE,
            nameBonusApplied = true,
            retired = false,
            survival = 2,
            insanity = 3,
            brainTrauma = true,
            attributes = Attributes(movement = 5, accuracy = 2, strength = 1, evasion = 0, luck = 1, speed = 2),
            wounds =
                mapOf(
                    HitLocation.HEAD to WoundState(armor = 1, light = true, heavy = false),
                    HitLocation.TORSO to WoundState(armor = 2, heavy = true),
                ),
            huntXp = 3,
            weapon = WeaponProficiency(type = "Hacha", xp = 2, specialist = true),
            courage = 4,
            understanding = 1,
            disorders = listOf("Apatía"),
            fightingArts = listOf("Duro"),
            notes = "lleva el farol",
            tokens = setOf("bleeding"),
            gear = listOf("Hacha de Contrapeso"),
            status = SurvivorStatus.DEPARTING,
        )

    @Test
    fun populatedSurvivorRoundTripsThroughJson() {
        val decoded = json.decodeFromString<Survivor>(json.encodeToString(populated))

        assertEquals(populated, decoded)
    }

    @Test
    fun defaultSurvivorRoundTripsThroughJson() {
        val survivor = Survivor(id = Ulid.generate(2L) { 0 })

        val decoded = json.decodeFromString<Survivor>(json.encodeToString(survivor))

        assertEquals(survivor, decoded)
    }

    @Test
    fun jsonMissingFieldsDecodesWithDefaults() {
        val id = Ulid.generate(3L) { 0 }

        val decoded = json.decodeFromString<Survivor>("""{ "id": "${id.value}" }""")

        assertEquals(SurvivorStatus.ALIVE, decoded.status)
        assertTrue(decoded.wounds.isEmpty())
        assertTrue(decoded.disorders.isEmpty())
        assertTrue(decoded.gear.isEmpty())
    }

    @Test
    fun stateWithSurvivorsRoundTrips() {
        val state = SettlementState(survivors = listOf(populated))

        val decoded = json.decodeFromString<SettlementState>(json.encodeToString(state))

        assertEquals(state, decoded)
    }
}

package io.kdmtracker.core.domain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementActionTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun locationDefaultsToNoBuildCostOrActions() {
        val location = SettlementLocation("X")

        assertEquals(0, location.buildEndeavors)
        assertTrue(location.buildCost.isEmpty())
        assertTrue(location.actions.isEmpty())
    }

    @Test
    fun actionRoundTripsThroughJson() {
        val action =
            SettlementAction(
                name = "Innovate",
                kind = ActionKind.INNOVATE,
                resourceCost = mapOf("Bone" to 1),
                oncePerPhase = true,
                draw = 2,
                keep = 1,
            )

        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<SettlementAction>(encoded)

        assertEquals(action, decoded)
        assertEquals(ActionKind.INNOVATE, decoded.kind)
        assertEquals(mapOf("Bone" to 1), decoded.resourceCost)
        assertTrue(decoded.oncePerPhase)
        assertEquals(2, decoded.draw)
        assertEquals(1, decoded.keep)
    }

    @Test
    fun legacyLocationJsonDecodesWithNewFieldsDefaulted() {
        val legacy = """{"name":"Lantern Hoard","requires":[]}"""

        val decoded = json.decodeFromString<SettlementLocation>(legacy)

        assertEquals("Lantern Hoard", decoded.name)
        assertTrue(decoded.requires.isEmpty())
        assertEquals(0, decoded.buildEndeavors)
        assertTrue(decoded.buildCost.isEmpty())
        assertTrue(decoded.actions.isEmpty())
    }
}

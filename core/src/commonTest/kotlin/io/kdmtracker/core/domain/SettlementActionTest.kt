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
        val location = SettlementLocation(LocationId("x"), LocalizedText(es = "X"))

        assertEquals(0, location.buildEndeavors)
        assertTrue(location.buildCost.isEmpty())
        assertTrue(location.actions.isEmpty())
    }

    @Test
    fun actionRoundTripsThroughJson() {
        val action =
            SettlementAction(
                names = LocalizedText(es = "Innovar", en = "Innovate"),
                kind = ActionKind.INNOVATE,
                resourceCost = mapOf(ResourceId("bone.monster") to 1),
                oncePerPhase = true,
                draw = 2,
                keep = 1,
            )

        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<SettlementAction>(encoded)

        assertEquals(action, decoded)
        assertEquals(ActionKind.INNOVATE, decoded.kind)
        assertEquals(mapOf(ResourceId("bone.monster") to 1), decoded.resourceCost)
        assertTrue(decoded.oncePerPhase)
        assertEquals(2, decoded.draw)
        assertEquals(1, decoded.keep)
    }

    @Test
    fun locationJsonDecodesWithTheOtherFieldsDefaulted() {
        val shaped = """{"id":"lantern_hoard","names":{"es":"Cúmulo de Linternas"}}"""

        val decoded = json.decodeFromString<SettlementLocation>(shaped)

        assertEquals(LocationId("lantern_hoard"), decoded.id)
        assertEquals(LocalizedText(es = "Cúmulo de Linternas"), decoded.names)
        assertTrue(decoded.requires.isEmpty())
        assertEquals(0, decoded.buildEndeavors)
        assertTrue(decoded.buildCost.isEmpty())
        assertTrue(decoded.actions.isEmpty())
    }
}

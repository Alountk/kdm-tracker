package io.kdmtracker.core.domain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementStateSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val populated =
        SettlementState(
            timeline =
                Timeline(
                    entries =
                        listOf(
                            TimelineEntry(1, "Returning Survivors"),
                            TimelineEntry(4, "Story Event"),
                            TimelineEntry(16, "Nemesis: The Butcher Lvl 2"),
                        ),
                    length = 40,
                ),
            innovations =
                InnovationCollection(
                    listOf(
                        Innovation(
                            id = InnovationId("language"),
                            names = LocalizedText(es = "Language"),
                            description = "You can talk.",
                            consequences = listOf("Language"),
                            survivalLimitBonus = 1,
                        ),
                        Innovation(
                            id = InnovationId("paint"),
                            names = LocalizedText(es = "Paint"),
                            description = "Face paint.",
                            consequences = listOf("Paint", "Ammonia"),
                            survivalLimitBonus = 1,
                        ),
                    ),
                ),
            locations = LocationSet(built = listOf(LocationId("lantern_hoard"), LocationId("bone_smith"))),
            resourceEntries =
                listOf(
                    ResourceEntry(ResourceId("Bone"), delta = 3),
                    ResourceEntry(ResourceId("Bone"), delta = -1),
                    ResourceEntry(ResourceId("Hide"), delta = 2),
                    ResourceEntry(ResourceId("Skull"), delta = -2),
                ),
            deaths = 2,
            births = 3,
        )

    @Test
    fun populatedStateRoundTripsThroughJson() {
        val encoded = json.encodeToString(populated)
        val decoded = json.decodeFromString<SettlementState>(encoded)

        assertEquals(populated, decoded)
    }

    @Test
    fun emptyStateRoundTripsThroughJson() {
        val encoded = json.encodeToString(SettlementState())
        val decoded = json.decodeFromString<SettlementState>(encoded)

        assertEquals(SettlementState(), decoded)
    }

    @Test
    fun nonDefaultTimelineLengthRoundTripsThroughJson() {
        val state = SettlementState(timeline = Timeline.core16(length = 30))
        val encoded = json.encodeToString(state)
        val decoded = json.decodeFromString<SettlementState>(encoded)

        assertEquals(30, decoded.timeline.length)
        assertEquals(state, decoded)
    }

    @Test
    fun jsonMissingFieldsDecodesWithDefaults() {
        val older =
            """
            {
              "timeline": {
                "entries": [
                  { "lanternYear": 4, "label": "Story Event" }
                ]
              },
              "deaths": 1
            }
            """.trimIndent()

        val decoded = json.decodeFromString<SettlementState>(older)

        assertEquals(listOf(TimelineEntry(4, "Story Event")), decoded.timeline.entries)
        assertEquals(40, decoded.timeline.length)
        assertEquals(1, decoded.deaths)
        assertTrue(decoded.innovations.innovations.isEmpty())
        assertTrue(decoded.locations.built.isEmpty())
        assertTrue(decoded.resourceEntries.isEmpty())
        assertEquals(0, decoded.births)
    }

    @Test
    fun principlesAndResolvedMilestonesRoundTrip() {
        val state =
            SettlementState(
                principles = mapOf(PrincipleCategory.DEATH to "The First Grave → Graves"),
                resolvedMilestones = setOf(Core16Milestones.PRINCIPLE_DEATH),
            )

        val decoded = json.decodeFromString<SettlementState>(json.encodeToString(state))

        assertEquals(state, decoded)
        assertEquals("The First Grave → Graves", decoded.principles[PrincipleCategory.DEATH])
    }

    @Test
    fun aLegacyBuiltLocationDecodesThroughTheIdentityTable() {
        val legacy = """{"built":["Lantern Hoard"]}"""

        val decoded = json.decodeFromString(LocationSet.serializer(), legacy)

        assertEquals(listOf(LocationId("lantern_hoard")), decoded.built)
    }

    @Test
    fun anUnmappedLegacyLocationSurvivesUnderASynthesizedId() {
        val legacy = """{"built":["Sitio Que No Existe"]}"""

        val decoded = json.decodeFromString(LocationSet.serializer(), legacy)

        assertTrue(
            decoded.built
                .single()
                .value
                .startsWith("unknown."),
        )
    }

    @Test
    fun theNewShapeEncodesBuiltLocationsAsIdObjects() {
        val set = LocationSet(built = listOf(LocationId("lantern_hoard")))

        assertEquals("""{"built":[{"id":"lantern_hoard"}]}""", json.encodeToString(set))
    }

    @Test
    fun aLegacyGearListDecodesThroughTheIdentityTable() {
        val legacy = """{"gear":["Daga de Hueso"]}"""

        val decoded = json.decodeFromString<SettlementState>(legacy)

        assertEquals(listOf(GearId("daga_de_hueso")), decoded.gear)
    }

    @Test
    fun anUnmappedLegacyGearSurvivesUnderASynthesizedId() {
        val legacy = """{"gear":["Equipo Fantasma"]}"""

        val decoded = json.decodeFromString<SettlementState>(legacy)

        assertTrue(
            decoded.gear
                .single()
                .value
                .startsWith("unknown."),
        )
    }

    @Test
    fun aLegacyInnovationDecodesWithItsNames() {
        val legacy =
            """
            {"innovations":{"innovations":[
              {"name":"Lenguaje","description":"Se pueden hablar.","consequences":["Narración"],
               "survivalLimitBonus":1}
            ]}}
            """.trimIndent()

        val decoded = json.decodeFromString<SettlementState>(legacy)
        val innovation = decoded.innovations.innovations.single()

        assertEquals(InnovationId("language"), innovation.id)
        assertEquals(LocalizedText(es = "Lenguaje"), innovation.names)
        assertEquals("Se pueden hablar.", innovation.description)
        assertEquals(listOf("Narración"), innovation.consequences)
        assertEquals(1, innovation.survivalLimitBonus)
    }

    @Test
    fun anUnmappedLegacyInnovationSurvivesUnderASynthesizedId() {
        val legacy =
            """{"innovations":{"innovations":[{"name":"Invención Extraña","description":"…"}]}}"""

        val decoded = json.decodeFromString<SettlementState>(legacy)

        assertTrue(
            decoded.innovations.innovations
                .single()
                .id.value
                .startsWith("unknown."),
        )
    }

    @Test
    fun jsonMissingNewFieldsDecodesWithDefaults() {
        val decoded = json.decodeFromString<SettlementState>("""{ "deaths": 1 }""")

        assertTrue(decoded.principles.isEmpty())
        assertTrue(decoded.resolvedMilestones.isEmpty())
    }
}

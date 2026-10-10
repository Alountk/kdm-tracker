package io.kdmtracker.core.domain

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The ledger's decode path must keep reading a settlement an older build persisted, where a
 * resource was identified by its display name — and it must never drop an entry, because a dropped
 * entry is a silently lost count.
 */
class ResourceEntrySerializerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun aNewEntryRoundTripsUnchanged() {
        val entry = ResourceEntry(ResourceId("bone.monster"), 2)

        assertEquals(
            entry,
            json.decodeFromString(ResourceEntry.serializer(), json.encodeToString(ResourceEntry.serializer(), entry)),
        )
    }

    @Test
    fun aLegacyEntryDecodesThroughTheIdentityTable() {
        val legacy = """{"name":"Hueso de Monstruo","category":"BASIC","delta":1}"""

        val entry = json.decodeFromString(ResourceEntry.serializer(), legacy)

        assertEquals(ResourceId("bone.monster"), entry.id)
        assertEquals(1, entry.delta)
    }

    @Test
    fun aLegacyEnglishNameDecodesThroughTheSameTable() {
        val legacy = """{"name":"Bone","category":"BASIC","delta":3}"""

        assertEquals(ResourceId("bone.monster"), json.decodeFromString(ResourceEntry.serializer(), legacy).id)
    }

    @Test
    fun anUnmappedLegacyNameSurvivesUnderASynthesizedId() {
        val legacy = """{"name":"Recurso Que No Existe","category":"BASIC","delta":3}"""

        val entry = json.decodeFromString(ResourceEntry.serializer(), legacy)

        assertEquals(3, entry.delta)
        assertTrue(entry.id.value.startsWith("unknown."), "was ${entry.id.value}")
    }

    @Test
    fun aLegacyEntryIsWrittenBackInTheNewShape() {
        val legacy = """{"name":"Hueso de Monstruo","category":"BASIC","delta":1}"""
        val entry = json.decodeFromString(ResourceEntry.serializer(), legacy)

        val encoded = json.encodeToString(ResourceEntry.serializer(), entry)

        assertEquals("""{"id":"bone.monster","delta":1}""", encoded)
    }
}

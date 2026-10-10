package io.kdmtracker.core.domain

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ContentIdTest {
    @Test
    fun resourceIdComparesByItsSlug() {
        assertEquals(ResourceId("bone.monster"), ResourceId("bone.monster"))
        assertNotEquals(ResourceId("bone.monster"), ResourceId("hide.white_lion"))
    }

    @Test
    fun aResourceIdIsStoredAsItsBareSlug() {
        assertEquals("\"hide.white_lion\"", Json.encodeToString(ResourceId.serializer(), ResourceId("hide.white_lion")))
    }

    @Test
    fun everyIdTypeSurvivesAJsonRoundTrip() {
        assertEquals(
            ResourceId("hide.white_lion"),
            Json.decodeFromString(
                ResourceId.serializer(),
                Json.encodeToString(ResourceId.serializer(), ResourceId("hide.white_lion")),
            ),
        )
        assertEquals(
            LocationId("bone_smith"),
            Json.decodeFromString(
                LocationId.serializer(),
                Json.encodeToString(LocationId.serializer(), LocationId("bone_smith")),
            ),
        )
        assertEquals(
            GearId("bone_dagger"),
            Json.decodeFromString(GearId.serializer(), Json.encodeToString(GearId.serializer(), GearId("bone_dagger"))),
        )
        assertEquals(
            InnovationId("language"),
            Json.decodeFromString(
                InnovationId.serializer(),
                Json.encodeToString(InnovationId.serializer(), InnovationId("language")),
            ),
        )
    }
}

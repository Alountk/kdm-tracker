package io.kdmtracker.core.sync

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChangeSetTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val upsert =
        ChangeRecord(
            campaignId = "campaign-1",
            entityType = "settlement",
            entityId = "settlement-1",
            op = ChangeOp.UPSERT,
            baseRev = 4L,
            payload = """{"name":"First Cradle"}""",
            updatedAtMillis = 1_700_000_000_000L,
            updatedAtCounter = 2,
            rev = 5L,
        )

    private val delete =
        ChangeRecord(
            campaignId = "campaign-1",
            entityType = "settlement",
            entityId = "settlement-2",
            op = ChangeOp.DELETE,
            baseRev = null,
            payload = "{}",
            updatedAtMillis = 1_700_000_000_100L,
            updatedAtCounter = 0,
            rev = 6L,
        )

    @Test
    fun pullResponseRoundTripsThroughJson() {
        val response =
            PullResponse(
                latestRev = 6L,
                records = listOf(upsert, delete),
            )

        val encoded = json.encodeToString(response)
        val decoded = json.decodeFromString<PullResponse>(encoded)

        assertEquals(response, decoded)
    }

    @Test
    fun latestRevAndRevSurviveRoundTrip() {
        val response = PullResponse(latestRev = 42L, records = listOf(upsert))

        val decoded = json.decodeFromString<PullResponse>(json.encodeToString(response))

        assertEquals(42L, decoded.latestRev)
        assertEquals(5L, decoded.records.single().rev)
    }

    @Test
    fun nullBaseRevRoundTrips() {
        val decoded =
            json.decodeFromString<PullResponse>(
                json.encodeToString(PullResponse(latestRev = 6L, records = listOf(delete))),
            )

        assertNull(decoded.records.single().baseRev)
    }

    @Test
    fun revDefaultsToZero() {
        val record =
            ChangeRecord(
                campaignId = "campaign-1",
                entityType = "settlement",
                entityId = "settlement-3",
                op = ChangeOp.UPSERT,
                baseRev = null,
                payload = "{}",
                updatedAtMillis = 1_700_000_000_200L,
                updatedAtCounter = 0,
            )

        assertEquals(0L, record.rev)
    }
}

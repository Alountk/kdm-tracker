package io.kdmtracker.sync

import io.kdmtracker.auth.CampaignRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Regression test for the pull cursor race: if the high-water mark were read *after* the records, a
 * push committing in between would make `latestRev` exceed the returned records and the client's
 * cursor would jump over a change it never received (silent data loss).
 */
class PullCursorRaceTest {
    @Test
    fun returnedRecordsNeverExceedTheReportedLatestRevWhenAPushInterleaves() {
        val campaignId = "01J0CURSOR000000000000000A"
        val campaignRepository = mock(CampaignRepository::class.java)
        val records = mock(ChangeRecordRepository::class.java)

        // The pull reads the high-water mark first: maxRev() sees rev 1.
        `when`(records.maxRev(campaignId)).thenReturn(1L)
        // A concurrent push lands after that read, so the records query now also returns rev 2.
        `when`(records.findByCampaignIdAndRevGreaterThanOrderByRevAsc(campaignId, 0L)).thenReturn(
            listOf(row(campaignId, "settlement-1", 1), row(campaignId, "settlement-2", 2)),
        )

        val response = ChangeRecordService(records, campaignRepository).pull(campaignId, 0)

        // The cursor cannot outrun the records: latestRev stays 1 and rev 2 is withheld for the next
        // pull, so the client re-requests from rev 1 and eventually receives it.
        assertEquals(1L, response.latestRev)
        assertEquals(listOf(1L), response.records.map { it.rev })
        assertTrue(response.records.all { it.rev <= response.latestRev })
    }

    private fun row(
        campaignId: String,
        entityId: String,
        rev: Long,
    ) = ChangeRecordEntity(
        campaignId = campaignId,
        entityType = "settlement",
        entityId = entityId,
        op = "UPSERT",
        payload = """{"name":"$entityId"}""",
        updatedAtMillis = 1_700_000_000_000,
        updatedAtCounter = rev.toInt(),
        baseRev = null,
        rev = rev,
    )
}

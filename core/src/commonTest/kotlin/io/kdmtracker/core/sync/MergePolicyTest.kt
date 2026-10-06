package io.kdmtracker.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MergePolicyTest {
    private fun record(
        entityId: String = "settlement-1",
        op: ChangeOp = ChangeOp.UPSERT,
        updatedAtMillis: Long = 1_700_000_000_000L,
        updatedAtCounter: Int = 0,
        payload: String = """{"name":"First Cradle"}""",
    ) = ChangeRecord(
        campaignId = "campaign-1",
        entityType = "settlement",
        entityId = entityId,
        op = op,
        baseRev = null,
        payload = payload,
        updatedAtMillis = updatedAtMillis,
        updatedAtCounter = updatedAtCounter,
    )

    @Test
    fun nullLocalTakesRemote() {
        val remote = record(updatedAtMillis = 1_700_000_000_100L)

        val result = MergePolicy.merge(local = null, remote = remote)

        assertEquals(remote, result.record)
        assertFalse(result.localWins)
    }

    @Test
    fun newerRemoteOverwritesLocal() {
        val local = record(updatedAtMillis = 1_700_000_000_100L)
        val remote = record(updatedAtMillis = 1_700_000_000_200L, payload = """{"name":"Remote"}""")

        val result = MergePolicy.merge(local, remote)

        assertEquals(remote, result.record)
        assertFalse(result.localWins)
    }

    @Test
    fun newerLocalSurvivesRemote() {
        val local = record(updatedAtMillis = 1_700_000_000_200L, payload = """{"name":"Local"}""")
        val remote = record(updatedAtMillis = 1_700_000_000_100L)

        val result = MergePolicy.merge(local, remote)

        assertEquals(local, result.record)
        assertTrue(result.localWins)
    }

    @Test
    fun counterBreaksTiesWithinTheSameMillisecond() {
        val local = record(updatedAtMillis = 1_700_000_000_100L, updatedAtCounter = 0)
        val remote = record(updatedAtMillis = 1_700_000_000_100L, updatedAtCounter = 1)

        val result = MergePolicy.merge(local, remote)

        assertEquals(remote, result.record)
        assertFalse(result.localWins)
    }

    @Test
    fun equalClockKeepsLocalEdit() {
        val local = record(updatedAtMillis = 1_700_000_000_100L, updatedAtCounter = 3)
        val remote =
            record(updatedAtMillis = 1_700_000_000_100L, updatedAtCounter = 3, payload = """{"name":"Remote"}""")

        val result = MergePolicy.merge(local, remote)

        assertEquals(local, result.record)
        assertTrue(result.localWins)
    }

    @Test
    fun newerDeleteWinsOverOlderUpsert() {
        val local = record(updatedAtMillis = 1_700_000_000_100L, op = ChangeOp.UPSERT)
        val remote = record(updatedAtMillis = 1_700_000_000_200L, op = ChangeOp.DELETE, payload = "{}")

        val result = MergePolicy.merge(local, remote)

        assertEquals(remote, result.record)
        assertEquals(ChangeOp.DELETE, result.record?.op)
        assertFalse(result.localWins)
    }

    @Test
    fun olderDeleteLosesToNewerUpsert() {
        val local = record(updatedAtMillis = 1_700_000_000_100L, op = ChangeOp.DELETE, payload = "{}")
        val remote =
            record(updatedAtMillis = 1_700_000_000_200L, op = ChangeOp.UPSERT, payload = """{"name":"Reborn"}""")

        val result = MergePolicy.merge(local, remote)

        assertEquals(remote, result.record)
        assertEquals(ChangeOp.UPSERT, result.record?.op)
        assertFalse(result.localWins)
    }

    @Test
    fun equalClockDeleteKeepsLocalTombstone() {
        val local =
            record(updatedAtMillis = 1_700_000_000_100L, updatedAtCounter = 5, op = ChangeOp.DELETE, payload = "{}")
        val remote = record(updatedAtMillis = 1_700_000_000_100L, updatedAtCounter = 5, op = ChangeOp.UPSERT)

        val result = MergePolicy.merge(local, remote)

        assertEquals(local, result.record)
        assertTrue(result.localWins)
    }

    @Test
    fun equalClockRemoteDeleteKeepsLocalUpsert() {
        val local = record(updatedAtMillis = 1_700_000_000_100L, updatedAtCounter = 5, op = ChangeOp.UPSERT)
        val remote =
            record(updatedAtMillis = 1_700_000_000_100L, updatedAtCounter = 5, op = ChangeOp.DELETE, payload = "{}")

        val result = MergePolicy.merge(local, remote)

        assertEquals(local, result.record)
        assertEquals(ChangeOp.UPSERT, result.record?.op)
        assertTrue(result.localWins)
    }
}

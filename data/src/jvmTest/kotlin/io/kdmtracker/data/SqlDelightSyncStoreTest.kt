package io.kdmtracker.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.transfer.CampaignDto
import io.kdmtracker.core.transfer.HybridLogicalClockDto
import io.kdmtracker.core.transfer.SettlementDto
import io.kdmtracker.core.transfer.SyncMetaDto
import io.kdmtracker.data.db.KdmDatabase
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlDelightSyncStoreTest {
    private val json = Json

    private fun ulid(seed: Long): Ulid = Ulid.generate(seed) { 0 }

    private fun inMemoryDatabase(): KdmDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KdmDatabase.Schema.create(driver)
        return KdmDatabase(driver)
    }

    private fun campaign(
        seed: Long,
        name: String = "Grey Havens",
        updatedAt: HybridLogicalClock = HybridLogicalClock(1_700_000_000_000, 3),
        rev: Long = 7,
    ): Campaign =
        Campaign(
            id = ulid(seed),
            name = name,
            rulesetId = "core-1.6",
            rulesetOverrides = RulesetOverrides(survivalLimit = 12),
            settlementIds = emptyList(),
            sync = SyncMeta(updatedAt = updatedAt, rev = rev),
        )

    private fun settlement(
        seed: Long,
        campaignId: Ulid,
        name: String = "First Lantern",
        updatedAt: HybridLogicalClock = HybridLogicalClock(1_700_000_000_500, 4),
        rev: Long = 11,
    ): Settlement =
        Settlement(
            id = ulid(seed),
            campaignId = campaignId,
            name = name,
            lanternYear = 3,
            phase = SettlementPhase.HUNT,
            currentStep = 5,
            endeavors = 2,
            population = 9,
            settlementEvent = "Murder",
            sync = SyncMeta(updatedAt = updatedAt, rev = rev),
        )

    private fun campaignRecord(
        campaignId: Ulid,
        name: String,
        updatedAt: HybridLogicalClock,
        op: ChangeOp = ChangeOp.UPSERT,
        rev: Long = 0,
    ): ChangeRecord {
        val dto =
            CampaignDto(
                id = campaignId.value,
                name = name,
                rulesetId = "core-1.6",
                sync =
                    SyncMetaDto(
                        updatedAt = HybridLogicalClockDto(updatedAt.millis, updatedAt.counter),
                        rev = rev,
                        deletedAt = tombstone(op, updatedAt),
                    ),
            )
        return ChangeRecord(
            campaignId = campaignId.value,
            entityType = "campaign",
            entityId = campaignId.value,
            op = op,
            baseRev = null,
            payload = json.encodeToString(CampaignDto.serializer(), dto),
            updatedAtMillis = updatedAt.millis,
            updatedAtCounter = updatedAt.counter,
            rev = rev,
        )
    }

    private fun settlementRecord(
        campaignId: Ulid,
        settlementId: Ulid,
        name: String,
        updatedAt: HybridLogicalClock,
        op: ChangeOp = ChangeOp.UPSERT,
        rev: Long = 0,
    ): ChangeRecord {
        val dto =
            SettlementDto(
                id = settlementId.value,
                campaignId = campaignId.value,
                name = name,
                lanternYear = 3,
                phase = SettlementPhase.HUNT,
                currentStep = 5,
                endeavors = 2,
                population = 9,
                settlementEvent = "Murder",
                sync =
                    SyncMetaDto(
                        updatedAt = HybridLogicalClockDto(updatedAt.millis, updatedAt.counter),
                        rev = rev,
                        deletedAt = tombstone(op, updatedAt),
                    ),
            )
        return ChangeRecord(
            campaignId = campaignId.value,
            entityType = "settlement",
            entityId = settlementId.value,
            op = op,
            baseRev = null,
            payload = json.encodeToString(SettlementDto.serializer(), dto),
            updatedAtMillis = updatedAt.millis,
            updatedAtCounter = updatedAt.counter,
            rev = rev,
        )
    }

    private fun tombstone(
        op: ChangeOp,
        updatedAt: HybridLogicalClock,
    ): HybridLogicalClockDto? =
        if (op == ChangeOp.DELETE) HybridLogicalClockDto(updatedAt.millis, updatedAt.counter) else null

    @Test
    fun pendingRoundTripsTheOutboxRows() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1)
        val child = settlement(2, parent.id)
        campaignStore.upsertCampaign(parent)
        campaignStore.upsertSettlement(child)

        val pending = syncStore.pending(parent.id.value)

        assertEquals(2, pending.size)
        val campaignEntry = pending.single { it.entityType == "campaign" }
        assertEquals(parent.id.value, campaignEntry.campaignId)
        assertEquals(parent.id.value, campaignEntry.entityId)
        assertEquals(ChangeOp.UPSERT, campaignEntry.op)
        assertEquals(parent.sync.updatedAt.millis, campaignEntry.updatedAtMillis)
        assertEquals(parent.sync.updatedAt.counter, campaignEntry.updatedAtCounter)
        assertEquals(0L, campaignEntry.rev)
        assertTrue(campaignEntry.payload.isNotBlank())
        assertEquals(child.id.value, pending.single { it.entityType == "settlement" }.entityId)
    }

    @Test
    fun applyMergedUpsertsTheSettlementAndDropsThePendingOutboxEntry() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1)
        val local = settlement(2, parent.id, name = "Local")
        campaignStore.upsertCampaign(parent)
        campaignStore.upsertSettlement(local)
        assertEquals(2, syncStore.pending(parent.id.value).size)

        val remote =
            settlementRecord(
                campaignId = parent.id,
                settlementId = local.id,
                name = "Remote",
                updatedAt = HybridLogicalClock(1_700_000_999_000, 5),
                rev = 42,
            )
        syncStore.applyMerged(parent.id.value, remote)

        val rows = database.settlementQueries.selectSettlementsByCampaign(parent.id.value).executeAsList()
        assertEquals(1, rows.size)
        assertEquals("Remote", rows.single().name)
        assertEquals(1_700_000_999_000, rows.single().updatedAtMillis)
        assertEquals(42, rows.single().rev)
        assertEquals(
            listOf("campaign"),
            syncStore.pending(parent.id.value).map { it.entityType },
            "the superseded settlement must be dropped from the outbox while the campaign stays pending",
        )
    }

    @Test
    fun applyMergedUpsertsACampaignAndDropsThePendingOutboxEntry() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1, name = "Local")
        campaignStore.upsertCampaign(parent)

        val remote =
            campaignRecord(
                campaignId = parent.id,
                name = "Remote",
                updatedAt = HybridLogicalClock(1_700_000_999_500, 2),
                rev = 43,
            )
        syncStore.applyMerged(parent.id.value, remote)

        val rows = database.campaignQueries.selectAllCampaigns().executeAsList()
        assertEquals(1, rows.size)
        assertEquals("Remote", rows.single().name)
        assertEquals(43, rows.single().rev)
        assertTrue(syncStore.pending(parent.id.value).isEmpty())
    }

    @Test
    fun applyMergedTwiceIsIdempotent() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1)
        val local = settlement(2, parent.id, name = "Local")
        campaignStore.upsertCampaign(parent)
        campaignStore.upsertSettlement(local)
        val remote =
            settlementRecord(
                campaignId = parent.id,
                settlementId = local.id,
                name = "Remote",
                updatedAt = HybridLogicalClock(1_700_000_999_000, 5),
                rev = 42,
            )

        syncStore.applyMerged(parent.id.value, remote)
        syncStore.applyMerged(parent.id.value, remote)

        val rows = database.settlementQueries.selectSettlementsByCampaign(parent.id.value).executeAsList()
        assertEquals(1, rows.size)
        assertEquals("Remote", rows.single().name)
        assertEquals(listOf("campaign"), syncStore.pending(parent.id.value).map { it.entityType })
    }

    @Test
    fun applyMergedRollsBackWhenThePayloadCannotBeDecoded() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1)
        val local = settlement(2, parent.id, name = "Local")
        campaignStore.upsertCampaign(parent)
        campaignStore.upsertSettlement(local)
        val corrupt =
            ChangeRecord(
                campaignId = parent.id.value,
                entityType = "settlement",
                entityId = local.id.value,
                op = ChangeOp.UPSERT,
                baseRev = null,
                payload = "not json",
                updatedAtMillis = 1_700_000_999_000,
                updatedAtCounter = 5,
            )

        assertFailsWith<IllegalArgumentException> { syncStore.applyMerged(parent.id.value, corrupt) }

        assertEquals(
            2,
            syncStore.pending(parent.id.value).size,
            "a failed merge must leave the outbox intact",
        )
        assertEquals(
            "Local",
            database.settlementQueries
                .selectSettlementsByCampaign(parent.id.value)
                .executeAsList()
                .single()
                .name,
            "a failed merge must not touch the domain row",
        )
    }

    @Test
    fun clearAcknowledgedRemovesExactlyTheAcknowledgedEntries() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1)
        val child = settlement(2, parent.id)
        campaignStore.upsertCampaign(parent)
        campaignStore.upsertSettlement(child)
        val all = syncStore.pending(parent.id.value)
        assertEquals(2, all.size)

        val acknowledged = all.filter { it.entityType == "campaign" }
        syncStore.clearAcknowledged(parent.id.value, acknowledged)

        assertEquals(listOf("settlement"), syncStore.pending(parent.id.value).map { it.entityType })
    }

    @Test
    fun clearAcknowledgedLeavesANewerEditForTheSameEntityPending() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1)
        campaignStore.upsertCampaign(parent)
        val acknowledged = syncStore.pending(parent.id.value)

        campaignStore.upsertCampaign(
            parent.copy(
                name = "Renamed mid-push",
                sync = SyncMeta(HybridLogicalClock(1_700_000_500_000, 5), rev = 8),
            ),
        )
        syncStore.clearAcknowledged(parent.id.value, acknowledged)

        val remaining = syncStore.pending(parent.id.value)
        assertEquals(1, remaining.size)
        assertEquals(1_700_000_500_000, remaining.single().updatedAtMillis)
    }

    @Test
    fun theCursorRoundTripsAndIsScopedPerCampaign() {
        val syncStore = SqlDelightSyncStore(inMemoryDatabase())

        assertEquals(0L, syncStore.cursor("first"))

        syncStore.setCursor("first", 42L)
        assertEquals(42L, syncStore.cursor("first"))
        assertEquals(0L, syncStore.cursor("second"))

        syncStore.setCursor("first", 43L)
        assertEquals(43L, syncStore.cursor("first"))
    }

    @Test
    fun localReadsTheCanonicalSettlementRowEvenWhenItIsNotPending() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1)
        val child = settlement(2, parent.id, name = "Canonical")
        campaignStore.upsertCampaign(parent)
        campaignStore.upsertSettlement(child)
        syncStore.clearAcknowledged(parent.id.value, syncStore.pending(parent.id.value))

        val local = syncStore.local(parent.id.value, "settlement", child.id.value)

        assertNotNull(local)
        assertEquals(child.id.value, local.entityId)
        assertEquals(child.campaignId.value, local.campaignId)
        assertEquals(child.sync.rev, local.rev)
        assertEquals("Canonical", json.decodeFromString(SettlementDto.serializer(), local.payload).name)
        assertTrue(syncStore.pending(parent.id.value).isEmpty(), "local() must not depend on the outbox")
    }

    @Test
    fun localReadsTheCanonicalCampaignRow() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1, name = "Grey Havens")
        campaignStore.upsertCampaign(parent)

        val local = syncStore.local(parent.id.value, "campaign", parent.id.value)

        assertNotNull(local)
        assertEquals(parent.id.value, local.entityId)
        assertEquals(parent.name, json.decodeFromString(CampaignDto.serializer(), local.payload).name)
    }

    @Test
    fun localReturnsNullForAnUnknownEntityTypeOrMissingRow() {
        val syncStore = SqlDelightSyncStore(inMemoryDatabase())

        assertEquals(null, syncStore.local("c1", "survivor", "s1"))
        assertEquals(null, syncStore.local("c1", "settlement", "missing"))
    }

    @Test
    fun enqueueAddsAPendingOutboxEntry() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1)
        val child = settlement(2, parent.id)
        campaignStore.upsertCampaign(parent)
        syncStore.clearAcknowledged(parent.id.value, syncStore.pending(parent.id.value))
        assertTrue(syncStore.pending(parent.id.value).isEmpty())

        val enqueued =
            settlementRecord(
                campaignId = parent.id,
                settlementId = child.id,
                name = "Re-enqueued",
                updatedAt = HybridLogicalClock(1_700_000_777_000, 1),
                rev = 5,
            )
        syncStore.enqueue(parent.id.value, enqueued)

        val pending = syncStore.pending(parent.id.value)
        assertEquals(listOf(child.id.value), pending.map { it.entityId })
        assertEquals(1_700_000_777_000, pending.single().updatedAtMillis)
    }

    @Test
    fun applyMergedPersistsTheRecordRevRatherThanThePayloadRev() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1)
        val local = settlement(2, parent.id, name = "Local")
        campaignStore.upsertCampaign(parent)
        campaignStore.upsertSettlement(local)

        // The payload still carries rev = 7; the authoritative server rev on the record is 99.
        val remote =
            settlementRecord(
                campaignId = parent.id,
                settlementId = local.id,
                name = "Remote",
                updatedAt = HybridLogicalClock(1_700_000_999_000, 5),
                rev = 7,
            ).copy(rev = 99)
        syncStore.applyMerged(parent.id.value, remote)

        assertEquals(
            99,
            database.settlementQueries
                .selectSettlementById(local.id.value)
                .executeAsOne()
                .rev,
            "the domain rev must come from ChangeRecord.rev, not the payload's sync.rev",
        )
    }

    @Test
    fun applyMergedSkipsAnUnknownEntityTypeInsteadOfThrowing() {
        val database = inMemoryDatabase()
        val skipped = mutableListOf<String>()
        val syncStore = SqlDelightSyncStore(database, log = skipped::add)
        val record =
            ChangeRecord(
                campaignId = "c1",
                entityType = "survivor",
                entityId = "s1",
                op = ChangeOp.UPSERT,
                baseRev = null,
                payload = "{}",
                updatedAtMillis = 1L,
                updatedAtCounter = 0,
            )

        syncStore.applyMerged("c1", record)

        assertTrue(skipped.single().contains("survivor"), "the skipped type must be logged")
    }

    @Test
    fun applyMergedIgnoresUnknownPayloadKeys() {
        val database = inMemoryDatabase()
        val campaignStore = SqlDelightCampaignStore(database)
        val syncStore = SqlDelightSyncStore(database)
        val parent = campaign(1)
        val local = settlement(2, parent.id, name = "Local")
        campaignStore.upsertCampaign(parent)
        campaignStore.upsertSettlement(local)

        val remote =
            settlementRecord(
                campaignId = parent.id,
                settlementId = local.id,
                name = "Remote",
                updatedAt = HybridLogicalClock(1_700_000_999_000, 5),
                rev = 42,
            )
        val withExtraKey =
            remote.copy(payload = remote.payload.dropLast(1) + ""","serverOnlyField":true}""")
        syncStore.applyMerged(parent.id.value, withExtraKey)

        assertEquals(
            "Remote",
            database.settlementQueries
                .selectSettlementById(local.id.value)
                .executeAsOne()
                .name,
        )
    }

    @Test
    fun pendingFallsBackToUpsertForAnUnknownOperation() {
        val database = inMemoryDatabase()
        database.outboxQueries.upsertOutbox(
            campaignId = "c1",
            entityType = "settlement",
            entityId = "s1",
            op = "TELEPORT",
            payload = "{}",
            updatedAtMillis = 10L,
            updatedAtCounter = 0L,
        )

        val pending = SqlDelightSyncStore(database).pending("c1")

        assertEquals(ChangeOp.UPSERT, pending.single().op)
    }
}

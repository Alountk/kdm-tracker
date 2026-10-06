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
import io.kdmtracker.data.db.KdmDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OutboxStoreTest {
    private fun ulid(seed: Long): Ulid = Ulid.generate(seed) { 0 }

    private fun inMemoryDatabase(): KdmDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KdmDatabase.Schema.create(driver)
        return KdmDatabase(driver)
    }

    private fun campaign(
        seed: Long,
        name: String = "Grey Havens",
        sync: SyncMeta = SyncMeta(HybridLogicalClock(1_700_000_000_000, 3), rev = 7),
    ): Campaign =
        Campaign(
            id = ulid(seed),
            name = name,
            rulesetId = "core-1.6",
            rulesetOverrides = RulesetOverrides(survivalLimit = 12),
            settlementIds = emptyList(),
            sync = sync,
        )

    private fun settlement(
        seed: Long,
        campaignId: Ulid,
        name: String = "First Lantern",
        sync: SyncMeta = SyncMeta(HybridLogicalClock(1_700_000_000_500, 4), rev = 11),
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
            sync = sync,
        )

    @Test
    fun aFreshSchemaCreatesTheOutboxAndCursorTables() {
        val store = SqlDelightCampaignStore(inMemoryDatabase())

        assertEquals(emptyList(), store.pending("campaign"))
        assertEquals(0L, store.cursor("campaign"))
    }

    @Test
    fun upsertCampaignRecordsExactlyOneOutboxEntry() {
        val store = SqlDelightCampaignStore(inMemoryDatabase())
        val campaign = campaign(1)

        store.upsertCampaign(campaign)

        val pending = store.pending(campaign.id.value)
        assertEquals(1, pending.size)
        val entry = pending.single()
        assertEquals(campaign.id.value, entry.campaignId)
        assertEquals("campaign", entry.entityType)
        assertEquals(campaign.id.value, entry.entityId)
        assertEquals(ChangeOp.UPSERT, entry.op)
        assertEquals(campaign.sync.updatedAt.millis, entry.updatedAtMillis)
        assertEquals(campaign.sync.updatedAt.counter, entry.updatedAtCounter)
        assertTrue(entry.payload.isNotBlank())
    }

    @Test
    fun upsertSettlementRecordsExactlyOneOutboxEntry() {
        val store = SqlDelightCampaignStore(inMemoryDatabase())
        val parent = campaign(1)
        val settlement = settlement(2, parent.id)

        store.upsertSettlement(settlement)

        val pending = store.pending(parent.id.value)
        assertEquals(1, pending.size)
        val entry = pending.single()
        assertEquals("settlement", entry.entityType)
        assertEquals(settlement.id.value, entry.entityId)
        assertEquals(settlement.sync.updatedAt.millis, entry.updatedAtMillis)
        assertTrue(entry.payload.isNotBlank())
    }

    @Test
    fun replayingTheSameUpsertUpdatesRatherThanDuplicatesTheEntry() {
        val store = SqlDelightCampaignStore(inMemoryDatabase())
        val campaign = campaign(1)
        store.upsertCampaign(campaign)

        val later =
            campaign.copy(
                name = "Renamed",
                sync = SyncMeta(HybridLogicalClock(1_700_000_999_000, 9), rev = 8),
            )
        store.upsertCampaign(later)

        val pending = store.pending(campaign.id.value)
        assertEquals(1, pending.size)
        assertEquals(1_700_000_999_000, pending.single().updatedAtMillis)
        assertEquals(9, pending.single().updatedAtCounter)
    }

    @Test
    fun clearAcknowledgedRemovesExactlyTheAcknowledgedEntries() {
        val store = SqlDelightCampaignStore(inMemoryDatabase())
        val parent = campaign(1)
        val child = settlement(2, parent.id)
        store.upsertCampaign(parent)
        store.upsertSettlement(child)
        val all = store.pending(parent.id.value)
        assertEquals(2, all.size)

        val acknowledged = all.filter { it.entityType == "campaign" }
        store.clearAcknowledged(parent.id.value, acknowledged)

        assertEquals(listOf("settlement"), store.pending(parent.id.value).map { it.entityType })
    }

    @Test
    fun clearAcknowledgedLeavesANewerEditForTheSameEntityPending() {
        val store = SqlDelightCampaignStore(inMemoryDatabase())
        val campaign = campaign(1)
        store.upsertCampaign(campaign)
        val acknowledged = store.pending(campaign.id.value)

        val newer =
            campaign.copy(
                name = "Renamed mid-push",
                sync = SyncMeta(HybridLogicalClock(1_700_000_500_000, 5), rev = 8),
            )
        store.upsertCampaign(newer)
        store.clearAcknowledged(campaign.id.value, acknowledged)

        val remaining = store.pending(campaign.id.value)
        assertEquals(1, remaining.size)
        assertEquals(1_700_000_500_000, remaining.single().updatedAtMillis)
    }

    @Test
    fun theCursorRoundTripsAndIsScopedPerCampaign() {
        val store = SqlDelightCampaignStore(inMemoryDatabase())
        assertEquals(0L, store.cursor("first"))

        store.setCursor("first", 42L)
        assertEquals(42L, store.cursor("first"))
        assertEquals(0L, store.cursor("second"))

        store.setCursor("first", 43L)
        assertEquals(43L, store.cursor("first"))
    }

    @Test
    fun migrateAddsTheOutboxAndCursorTablesToAV1ShapedDatabase() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, V1_CAMPAIGN, 0)
        driver.execute(null, V1_SETTLEMENT, 0)
        driver.execute(null, "PRAGMA user_version = 1;", 0)

        KdmDatabase.Schema.migrate(driver, 1L, KdmDatabase.Schema.version)

        val database = KdmDatabase(driver)
        assertEquals(2L, KdmDatabase.Schema.version)
        assertTrue(
            database.outboxQueries
                .selectOutboxByCampaign("campaign")
                .executeAsList()
                .isEmpty(),
        )
        assertNull(database.syncCursorQueries.selectCursor("campaign").executeAsOneOrNull())
    }

    private companion object {
        private const val V1_CAMPAIGN =
            """
            CREATE TABLE campaign (
              id TEXT NOT NULL PRIMARY KEY,
              name TEXT NOT NULL,
              rulesetId TEXT NOT NULL,
              rulesetOverridesJson TEXT NOT NULL,
              updatedAtMillis INTEGER NOT NULL,
              updatedAtCounter INTEGER NOT NULL,
              rev INTEGER NOT NULL,
              deletedAtMillis INTEGER,
              deletedAtCounter INTEGER
            );
            """

        private const val V1_SETTLEMENT =
            """
            CREATE TABLE settlement (
              id TEXT NOT NULL PRIMARY KEY,
              campaignId TEXT NOT NULL,
              name TEXT NOT NULL,
              lanternYear INTEGER NOT NULL,
              phase TEXT NOT NULL,
              currentStep INTEGER NOT NULL,
              endeavors INTEGER NOT NULL,
              population INTEGER NOT NULL,
              settlementEvent TEXT,
              updatedAtMillis INTEGER NOT NULL,
              updatedAtCounter INTEGER NOT NULL,
              rev INTEGER NOT NULL,
              deletedAtMillis INTEGER,
              deletedAtCounter INTEGER
            );
            """
    }
}

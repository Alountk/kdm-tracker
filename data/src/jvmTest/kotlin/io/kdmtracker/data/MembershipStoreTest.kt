package io.kdmtracker.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kdmtracker.core.sync.CampaignSummary
import io.kdmtracker.data.db.KdmDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MembershipStoreTest {
    private fun inMemoryDatabase(): KdmDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KdmDatabase.Schema.create(driver)
        return KdmDatabase(driver)
    }

    private fun summary(
        id: String = "campaign-1",
        name: String = "Grey Havens",
        role: String = "OWNER",
        ownerUsername: String = "alountk",
        sharedWith: List<String> = listOf("mia", "tom"),
    ): CampaignSummary = CampaignSummary(id, name, role, ownerUsername, sharedWith)

    @Test
    fun membershipRoundTripsIncludingSharedWith() {
        val store = SqlDelightSyncStore(inMemoryDatabase())
        val stored = summary()

        store.upsertMembership(stored)

        assertEquals(listOf(stored), store.memberships())
        assertEquals(stored, store.membership(stored.id))
    }

    @Test
    fun upsertingTheSameCampaignUpdatesInsteadOfDuplicating() {
        val store = SqlDelightSyncStore(inMemoryDatabase())
        store.upsertMembership(summary())
        val updated =
            summary(
                name = "Grey Havens Reloaded",
                role = "EDITOR",
                sharedWith = emptyList(),
            )

        store.upsertMembership(updated)

        assertEquals(listOf(updated), store.memberships())
    }

    @Test
    fun membershipIsNullForAnUnknownCampaign() {
        val store = SqlDelightSyncStore(inMemoryDatabase())

        assertNull(store.membership("missing"))
    }

    @Test
    fun migrateAddsTheMembershipTableToAV2ShapedDatabase() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, V1_CAMPAIGN, 0)
        driver.execute(null, V1_SETTLEMENT, 0)
        driver.execute(null, "PRAGMA user_version = 1;", 0)
        KdmDatabase.Schema.migrate(driver, 1L, 2L)

        KdmDatabase.Schema.migrate(driver, 2L, KdmDatabase.Schema.version)

        val database = KdmDatabase(driver)
        assertEquals(4L, KdmDatabase.Schema.version)
        assertTrue(
            database.membershipQueries
                .selectAllMemberships()
                .executeAsList()
                .isEmpty(),
        )
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

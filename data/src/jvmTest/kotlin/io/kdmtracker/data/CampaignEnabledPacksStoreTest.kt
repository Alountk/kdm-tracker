package io.kdmtracker.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.data.db.KdmDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins that a campaign's [Campaign.enabledPacks] survive the local store round-trip, so the
 * composed content catalog is not empty after the app reloads a campaign from the database.
 */
class CampaignEnabledPacksStoreTest {
    private fun ulid(seed: Long): Ulid = Ulid.generate(seed) { 0 }

    private fun inMemoryDatabase(): KdmDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KdmDatabase.Schema.create(driver)
        return KdmDatabase(driver)
    }

    private fun campaign(enabledPacks: List<String>): Campaign =
        Campaign(
            id = ulid(1),
            name = "Grey Havens",
            rulesetId = "core-1.6",
            rulesetOverrides = RulesetOverrides(),
            settlementIds = emptyList(),
            sync = SyncMeta(HybridLogicalClock(1_700_000_000_000, 3), rev = 7),
            enabledPacks = enabledPacks,
        )

    @Test
    fun enabledPacksSurviveAWriteAndRead() {
        val store = SqlDelightCampaignStore(inMemoryDatabase())

        store.upsertCampaign(campaign(listOf("core-1.6", "core-1.5")))

        assertEquals(
            listOf("core-1.6", "core-1.5"),
            store
                .loadSnapshots()
                .single()
                .campaign.enabledPacks,
        )
    }

    @Test
    fun aCampaignWithoutPacksRoundTripsAsEmpty() {
        val store = SqlDelightCampaignStore(inMemoryDatabase())

        store.upsertCampaign(campaign(emptyList()))

        assertEquals(
            emptyList(),
            store
                .loadSnapshots()
                .single()
                .campaign.enabledPacks,
        )
    }

    @Test
    fun migrateAddsEnabledPacksToAV4ShapedDatabase() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, V4_CAMPAIGN, 0)
        driver.execute(
            null,
            "INSERT INTO campaign VALUES ('00000000000000000000000000','N','core-1.6','{}',0,0,1,NULL,NULL);",
            0,
        )
        driver.execute(null, "PRAGMA user_version = 4;", 0)

        KdmDatabase.Schema.migrate(driver, 4L, KdmDatabase.Schema.version)

        val database = KdmDatabase(driver)
        assertEquals(5L, KdmDatabase.Schema.version)
        assertEquals(
            "[]",
            database.campaignQueries
                .selectAllCampaigns()
                .executeAsOne()
                .enabledPacksJson,
        )
    }

    private companion object {
        private const val V4_CAMPAIGN =
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
    }
}

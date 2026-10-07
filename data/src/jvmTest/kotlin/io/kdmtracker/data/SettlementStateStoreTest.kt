package io.kdmtracker.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Timeline
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.data.db.KdmDatabase
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class SettlementStateStoreTest {
    private fun ulid(seed: Long): Ulid = Ulid.generate(seed) { 0 }

    private fun tempDatabase(): KdmDatabase {
        val file = Files.createTempFile("kdm-settlement-state", ".db").toFile()
        file.deleteOnExit()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        KdmDatabase.Schema.create(driver)
        return KdmDatabase(driver)
    }

    private fun campaign(seed: Long): Campaign =
        Campaign(
            id = ulid(seed),
            name = "Grey Havens",
            rulesetId = "core-1.6",
            rulesetOverrides = RulesetOverrides(survivalLimit = 12),
            settlementIds = emptyList(),
            sync = SyncMeta(HybridLogicalClock(1_700_000_000_000, 3), rev = 7),
        )

    private fun settlement(
        seed: Long,
        campaignId: Ulid,
        name: String = "First Lantern",
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
            sync = SyncMeta(HybridLogicalClock(1_700_000_000_500, 4), rev = 11),
        )

    private fun populatedState(): SettlementState =
        SettlementState(
            timeline = Timeline.core16().withEvent(1, "Returning Survivors"),
            innovations =
                InnovationCollection(
                    innovations =
                        listOf(
                            Innovation(
                                name = "Lantern Oven",
                                description = "Cook a resource to gain its benefit.",
                                consequences = listOf("Lantern Oven", "Cooking"),
                                survivalLimitBonus = 1,
                            ),
                            Innovation(
                                name = "Paint",
                                description = "Survivors gain survival at the start of the hunt.",
                                consequences = listOf("Paint"),
                                survivalLimitBonus = 0,
                            ),
                        ),
                ),
            locations = LocationSet(built = listOf("Lantern Hoard", "Organ Grinder")),
            resourceEntries =
                listOf(
                    ResourceEntry("Broken Lantern", ResourceCategory.BASIC, 2),
                    ResourceEntry("Broken Lantern", ResourceCategory.BASIC, -1),
                    ResourceEntry("White Lion Skull", ResourceCategory.MONSTER, 1),
                ),
            deaths = 3,
            births = 5,
        )

    @Test
    fun aSettlementRecordWithStateRoundTripsLosslessly() {
        val database = tempDatabase()
        val store = SqlDelightCampaignStore(database)
        val parent = campaign(1)
        val record = SettlementRecord(settlement(2, parent.id), populatedState())
        store.upsertCampaign(parent)
        store.upsertSettlement(record)

        val reloaded = SqlDelightCampaignStore(database).loadRecords(parent.id.value)

        assertEquals(listOf(record), reloaded)
    }

    @Test
    fun aScalarWriteAfterARecordKeepsTheStoredState() {
        val database = tempDatabase()
        val store = SqlDelightCampaignStore(database)
        val parent = campaign(1)
        val original = settlement(2, parent.id)
        val state = populatedState()
        store.upsertCampaign(parent)
        store.upsertSettlement(SettlementRecord(original, state))

        store.upsertSettlement(original.copy(name = "Renamed"))

        val reloaded = store.loadRecords(parent.id.value).single()
        assertEquals("Renamed", reloaded.settlement.name)
        assertEquals(state, reloaded.state)
    }

    @Test
    fun migrateAddsStateJsonToAV3ShapedDatabase() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, V3_CAMPAIGN, 0)
        driver.execute(null, V3_SETTLEMENT, 0)
        driver.execute(null, V3_ROW, 0)
        driver.execute(null, "PRAGMA user_version = 3;", 0)

        KdmDatabase.Schema.migrate(driver, 3L, KdmDatabase.Schema.version)

        val database = KdmDatabase(driver)
        assertEquals(4L, KdmDatabase.Schema.version)
        val records = SqlDelightCampaignStore(database).loadRecords(CAMPAIGN_ID)
        assertEquals(1, records.size)
        assertEquals(SettlementState(), records.single().state)
    }

    private companion object {
        private const val CAMPAIGN_ID = "01H00000000000000000000001"
        private const val SETTLEMENT_ID = "01H00000000000000000000002"

        private const val V3_CAMPAIGN =
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

        private const val V3_SETTLEMENT =
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

        private const val V3_ROW =
            """
            INSERT INTO settlement
              (id, campaignId, name, lanternYear, phase, currentStep, endeavors, population,
               settlementEvent, updatedAtMillis, updatedAtCounter, rev, deletedAtMillis, deletedAtCounter)
            VALUES
              ('$SETTLEMENT_ID', '$CAMPAIGN_ID', 'First Lantern', 3, 'HUNT', 5, 2, 9,
               'Murder', 1700000000500, 4, 11, NULL, NULL);
            """
    }
}

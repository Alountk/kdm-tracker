package io.kdmtracker.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kdmtracker.core.content.MonsterNode
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.transfer.CampaignDto
import io.kdmtracker.core.transfer.HybridLogicalClockDto
import io.kdmtracker.core.transfer.SyncMetaDto
import io.kdmtracker.data.db.KdmDatabase
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pins that the monster chosen for each node survives the local store, the outbox and an incoming merge. */
class CampaignNodeChoicesStoreTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun inMemoryDatabase(): KdmDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KdmDatabase.Schema.create(driver)
        return KdmDatabase(driver)
    }

    private val campaign =
        Campaign(
            id = Ulid.generate(1) { 0 },
            name = "Grey Havens",
            rulesetId = "core-1.6",
            rulesetOverrides = RulesetOverrides(),
            settlementIds = emptyList(),
            sync = SyncMeta(HybridLogicalClock(1_700_000_000_000, 3), rev = 7),
            nodeChoices = mapOf(MonsterNode.NQ1 to "Gorm"),
        )

    @Test
    fun theChoicesSurviveAWriteAndRead() {
        val store = SqlDelightCampaignStore(inMemoryDatabase())

        store.upsertCampaign(campaign)

        assertEquals(
            campaign.nodeChoices,
            store
                .loadSnapshots()
                .single()
                .campaign.nodeChoices,
        )
    }

    @Test
    fun theOutboxCarriesTheChoicesToTheOtherDevices() {
        val database = inMemoryDatabase()
        SqlDelightCampaignStore(database).upsertCampaign(campaign)

        val payload = SqlDelightSyncStore(database).pending(campaign.id.value).single().payload

        assertEquals(mapOf("NQ1" to "Gorm"), json.decodeFromString(CampaignDto.serializer(), payload).nodeChoices)
    }

    @Test
    fun aMergedCampaignFromAnotherDeviceKeepsItsChoices() {
        val database = inMemoryDatabase()
        val store = SqlDelightCampaignStore(database)
        store.upsertCampaign(campaign.copy(nodeChoices = emptyMap()))
        val dto =
            CampaignDto(
                id = campaign.id.value,
                name = "Remote",
                rulesetId = "core-1.6",
                sync = SyncMetaDto(updatedAt = HybridLogicalClockDto(1_700_000_999_500, 2), rev = 43),
                nodeChoices = mapOf("NN1" to "Slenderman"),
            )
        val record =
            ChangeRecord(
                campaignId = campaign.id.value,
                entityType = "campaign",
                entityId = campaign.id.value,
                op = ChangeOp.UPSERT,
                baseRev = null,
                payload = json.encodeToString(CampaignDto.serializer(), dto),
                updatedAtMillis = 1_700_000_999_500,
                updatedAtCounter = 2,
                rev = 43,
            )

        SqlDelightSyncStore(database).applyMerged(campaign.id.value, record)

        assertEquals(
            mapOf(MonsterNode.NN1 to "Slenderman"),
            store
                .loadSnapshots()
                .single()
                .campaign.nodeChoices,
        )
    }

    @Test
    fun migrateAddsNoChoicesToAV5ShapedDatabase() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, V5_CAMPAIGN, 0)
        driver.execute(
            null,
            "INSERT INTO campaign VALUES ('00000000000000000000000000','N','core-1.6','{}',0,0,1,NULL,NULL,'[]');",
            0,
        )
        driver.execute(null, "PRAGMA user_version = 5;", 0)

        KdmDatabase.Schema.migrate(driver, 5L, KdmDatabase.Schema.version)

        val database = KdmDatabase(driver)
        assertEquals(
            "{}",
            database.campaignQueries
                .selectAllCampaigns()
                .executeAsOne()
                .nodeChoicesJson,
        )
        assertTrue(KdmDatabase.Schema.version >= 6L)
    }

    private companion object {
        private const val V5_CAMPAIGN =
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
              deletedAtCounter INTEGER,
              enabledPacksJson TEXT NOT NULL DEFAULT '[]'
            );
            """
    }
}

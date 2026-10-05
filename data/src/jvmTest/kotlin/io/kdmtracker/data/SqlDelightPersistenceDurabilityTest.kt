package io.kdmtracker.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.CampaignSnapshot
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.data.db.KdmDatabase
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Proves state outlives the connection that wrote it: a temp-file database is written, closed, and
 * reopened through a brand-new driver and store, and the aggregate reads back intact. This exercises
 * real file persistence, unlike the in-memory tests that reuse one open connection.
 */
class SqlDelightPersistenceDurabilityTest {
    @Test
    fun aggregateSurvivesClosingAndReopeningTheDatabase() {
        val file = Files.createTempFile("kdm-durability", ".db").toFile()
        file.deleteOnExit()
        val url = "jdbc:sqlite:${file.absolutePath}"
        val campaignId = Ulid.generate(1L) { 0 }
        val settlementId = Ulid.generate(2L) { 0 }
        val campaign =
            Campaign(
                id = campaignId,
                name = "Grey Havens",
                rulesetId = "core-1.6",
                rulesetOverrides = RulesetOverrides(survivalLimit = 12),
                settlementIds = emptyList(),
                sync = SyncMeta(HybridLogicalClock(1_700_000_000_000, 3), rev = 7),
            )
        val settlement =
            Settlement(
                id = settlementId,
                campaignId = campaignId,
                name = "First Lantern",
                lanternYear = 3,
                phase = SettlementPhase.HUNT,
                currentStep = 5,
                endeavors = 2,
                population = 9,
                settlementEvent = "Murder",
                sync = SyncMeta(HybridLogicalClock(1_700_000_000_500, 4), rev = 11),
            )

        val firstDriver = JdbcSqliteDriver(url)
        KdmDatabase.Schema.create(firstDriver)
        SqlDelightCampaignStore(KdmDatabase(firstDriver)).apply {
            upsertCampaign(campaign)
            upsertSettlement(settlement)
        }
        firstDriver.close()

        val secondDriver = JdbcSqliteDriver(url)
        val reloaded = SqlDelightCampaignStore(KdmDatabase(secondDriver)).loadSnapshots()
        secondDriver.close()

        assertEquals(
            listOf(CampaignSnapshot(campaign.copy(settlementIds = listOf(settlementId)), listOf(settlement))),
            reloaded,
        )
    }
}

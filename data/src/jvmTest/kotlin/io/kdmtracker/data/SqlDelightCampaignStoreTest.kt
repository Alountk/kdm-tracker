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
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightCampaignStoreTest {
    private fun ulid(seed: Long): Ulid = Ulid.generate(seed) { 0 }

    private fun inMemoryDatabase(): KdmDatabase =
        KdmDatabaseFactory.create(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))

    private fun campaign(
        seed: Long,
        name: String = "Grey Havens",
    ): Campaign =
        Campaign(
            id = ulid(seed),
            name = name,
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

    @Test
    fun secondStoreInstanceOverTheSameDatabaseRoundTripsTheAggregate() {
        val database = inMemoryDatabase()
        val writtenCampaign = campaign(1)
        val writtenSettlement = settlement(2, writtenCampaign.id)

        SqlDelightCampaignStore(database).apply {
            upsertCampaign(writtenCampaign)
            upsertSettlement(writtenSettlement)
        }

        val reloaded = SqlDelightCampaignStore(database).loadSnapshots()

        assertEquals(
            listOf(CampaignSnapshot(writtenCampaign, listOf(writtenSettlement))),
            reloaded,
        )
    }

    @Test
    fun upsertingTheSameSettlementTwiceLeavesOneRow() {
        val database = inMemoryDatabase()
        val store = SqlDelightCampaignStore(database)
        val parent = campaign(1)
        store.upsertCampaign(parent)
        store.upsertSettlement(settlement(2, parent.id))
        store.upsertSettlement(settlement(2, parent.id, name = "Renamed"))

        val stored =
            database.settlementQueries
                .selectSettlementsByCampaign(parent.id.value)
                .executeAsList()
        val settlements = store.loadSnapshots().single().settlements

        assertEquals(1, stored.size)
        assertEquals(1, settlements.size)
        assertEquals("Renamed", settlements.single().name)
    }

    @Test
    fun softDeletedCampaignsAreExcludedFromSnapshots() {
        val database = inMemoryDatabase()
        val store = SqlDelightCampaignStore(database)
        val live = campaign(1)
        val removed = campaign(2)
        store.upsertCampaign(live)
        store.upsertCampaign(
            removed.copy(sync = removed.sync.copy(deletedAt = HybridLogicalClock(1_700_000_001_000, 0))),
        )

        assertEquals(listOf(live), store.loadSnapshots().map { it.campaign })
    }
}

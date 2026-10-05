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
import kotlin.test.assertNull

class SqlDelightCampaignStoreTest {
    private fun ulid(seed: Long): Ulid = Ulid.generate(seed) { 0 }

    private fun inMemoryDatabase(): KdmDatabase =
        KdmDatabaseFactory.create(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))

    private fun campaign(
        seed: Long,
        name: String = "Grey Havens",
        settlementIds: List<Ulid> = emptyList(),
    ): Campaign =
        Campaign(
            id = ulid(seed),
            name = name,
            rulesetId = "core-1.6",
            rulesetOverrides = RulesetOverrides(survivalLimit = 12),
            settlementIds = settlementIds,
            sync = SyncMeta(HybridLogicalClock(1_700_000_000_000, 3), rev = 7),
        )

    private fun settlement(
        seed: Long,
        campaignId: Ulid,
        name: String = "First Lantern",
        settlementEvent: String? = "Murder",
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
            settlementEvent = settlementEvent,
            sync = SyncMeta(HybridLogicalClock(1_700_000_000_500, 4), rev = 11),
        )

    @Test
    fun secondStoreInstanceOverTheSameDatabaseRoundTripsTheAggregate() {
        val database = inMemoryDatabase()
        val baseCampaign = campaign(1)
        val writtenSettlement = settlement(2, baseCampaign.id)
        val writtenCampaign = baseCampaign.copy(settlementIds = listOf(writtenSettlement.id))

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
    fun roundTripsASettlementWithNoSettlementEvent() {
        val database = inMemoryDatabase()
        val store = SqlDelightCampaignStore(database)
        val parent = campaign(1)
        val child = settlement(2, parent.id, settlementEvent = null)
        store.upsertCampaign(parent)
        store.upsertSettlement(child)

        val reloaded = SqlDelightCampaignStore(database).loadSnapshots().single()

        assertEquals(listOf(child), reloaded.settlements)
        assertNull(reloaded.settlements.single().settlementEvent)
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

package io.kdmtracker.app

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Timeline
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.data.SqlDelightCampaignStore
import io.kdmtracker.data.db.KdmDatabase
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins that the app repository holds a [io.kdmtracker.core.domain.SettlementRecord] and round-trips
 * its [SettlementState] losslessly through the store.
 *
 * The primary coverage runs over the **real** [SqlDelightCampaignStore] (a temp-file JDBC database),
 * so the JSON state column and its read path are exercised for real; one test repeats the round-trip
 * against the in-memory fake to pin the port contract.
 */
class SettlementRecordRepositoryTest {
    private val databaseFile: File = File.createTempFile("kdm-record-repo", ".db").apply { deleteOnExit() }
    private val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun sqlDelightStore(): SqlDelightCampaignStore {
        KdmDatabase.Schema.create(driver)
        return SqlDelightCampaignStore(KdmDatabase(driver))
    }

    @Test
    fun exposesTheSeedRecordBackedByTheSettlement() {
        val store = sqlDelightStore()
        val repository = SettlementRepository(store)

        val seeded = store.loadRecords(repository.campaign.id.value).single()

        assertEquals("New Settlement", seeded.settlement.name)
        assertEquals(1, seeded.settlement.lanternYear)
        assertEquals(seeded, repository.record)
    }

    @Test
    fun updateRecordPersistsThePopulatedStateLosslessly() {
        val store = sqlDelightStore()
        val repository = SettlementRepository(store)
        val state = populatedState()

        repository.update(repository.record.copy(state = state))

        assertEquals(state, store.loadRecords(repository.campaign.id.value).single().state)
    }

    @Test
    fun reloadReadsThePersistedStateBack() {
        val store = sqlDelightStore()
        val repository = SettlementRepository(store)
        val state = populatedState()
        repository.update(repository.record.copy(state = state))

        repository.reload()

        assertEquals(state, repository.record.state)
    }

    @Test
    fun aFreshRepositoryReadsThePersistedStateBack() {
        val store = sqlDelightStore()
        val first = SettlementRepository(store)
        val state = populatedState()
        first.update(first.record.copy(state = state))

        val reloaded = SettlementRepository(store)

        assertEquals(state, reloaded.record.state)
    }

    @Test
    fun scalarUpdatePreservesTheStoredState() {
        val store = sqlDelightStore()
        val repository = SettlementRepository(store)
        val state = populatedState()
        repository.update(repository.record.copy(state = state))

        repository.update(repository.settlement.copy(lanternYear = 5))

        assertEquals(state, repository.record.state)
        assertEquals(state, SettlementRepository(store).record.state)
    }

    @Test
    fun inMemoryStoreAlsoRoundTripsTheState() {
        val store = InMemoryCampaignStore()
        val repository = SettlementRepository(store)
        val state = populatedState()

        repository.update(repository.record.copy(state = state))

        assertEquals(state, SettlementRepository(store).record.state)
    }

    private fun populatedState(): SettlementState =
        SettlementState(
            timeline = Timeline.core16().withEvent(3, "Baptism of Blood"),
            innovations =
                InnovationCollection(
                    innovations =
                        listOf(
                            Innovation(
                                id = InnovationId("language"),
                                names = LocalizedText(es = "Language"),
                                description = "The survivors give names to the world around them.",
                                survivalLimitBonus = 1,
                            ),
                        ),
                ),
            resourceEntries = listOf(ResourceEntry(id = ResourceId("Bone"), delta = 3)),
            deaths = 5,
            births = 2,
            // Matches the seeded settlement's population, so loading does not seed a roster.
            survivors = List(4) { Survivor(id = Ulid.generate(200L + it) { 0 }) },
        )
}

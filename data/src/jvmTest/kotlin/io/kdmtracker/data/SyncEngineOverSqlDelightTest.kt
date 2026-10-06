package io.kdmtracker.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.sync.CampaignSummary
import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.ChangeSet
import io.kdmtracker.core.sync.PullResponse
import io.kdmtracker.core.sync.SyncApi
import io.kdmtracker.core.sync.SyncEngine
import io.kdmtracker.core.transfer.HybridLogicalClockDto
import io.kdmtracker.core.transfer.SettlementDto
import io.kdmtracker.core.transfer.SyncMetaDto
import io.kdmtracker.data.db.KdmDatabase
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Drives the real [SyncEngine] over the real [SqlDelightSyncStore] and [SqlDelightCampaignStore]
 * (Ruling AI). The pure-engine fakes keep `local` and the outbox identical, which hid the C1
 * canonical-vs-outbox divergence; this suite exercises the actual store so that bug cannot return.
 */
class SyncEngineOverSqlDelightTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val campaignId = Ulid.generate(1L) { 0 }
    private val settlementId = Ulid.generate(2L) { 0 }

    private fun inMemoryDatabase(): KdmDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KdmDatabase.Schema.create(driver)
        return KdmDatabase(driver)
    }

    private fun seed(
        database: KdmDatabase,
        settlementUpdatedAt: HybridLogicalClock = HybridLogicalClock(1_700_000_000_500, 0),
        settlementName: String = "Local",
    ): SqlDelightCampaignStore {
        val store = SqlDelightCampaignStore(database)
        store.upsertCampaign(
            Campaign(
                id = campaignId,
                name = "Grey Havens",
                rulesetId = "core-1.6",
                rulesetOverrides = RulesetOverrides(),
                settlementIds = listOf(settlementId),
                sync = SyncMeta(HybridLogicalClock(1_700_000_000_000, 0), rev = 1),
            ),
        )
        store.upsertSettlement(
            Settlement(
                id = settlementId,
                campaignId = campaignId,
                name = settlementName,
                lanternYear = 1,
                phase = SettlementPhase.SETTLEMENT,
                currentStep = 1,
                endeavors = 0,
                population = 4,
                settlementEvent = null,
                sync = SyncMeta(settlementUpdatedAt, rev = 2),
            ),
        )
        return store
    }

    private fun settlementRecord(
        name: String,
        updatedAt: HybridLogicalClock,
        rev: Long,
    ): ChangeRecord {
        val dto =
            SettlementDto(
                id = settlementId.value,
                campaignId = campaignId.value,
                name = name,
                lanternYear = 1,
                phase = SettlementPhase.SETTLEMENT,
                currentStep = 1,
                endeavors = 0,
                population = 4,
                settlementEvent = null,
                sync =
                    SyncMetaDto(
                        updatedAt = HybridLogicalClockDto(updatedAt.millis, updatedAt.counter),
                        rev = rev,
                        deletedAt = null,
                    ),
            )
        return ChangeRecord(
            campaignId = campaignId.value,
            entityType = "settlement",
            entityId = settlementId.value,
            op = ChangeOp.UPSERT,
            baseRev = null,
            payload = json.encodeToString(SettlementDto.serializer(), dto),
            updatedAtMillis = updatedAt.millis,
            updatedAtCounter = updatedAt.counter,
            rev = rev,
        )
    }

    private class FakeSyncApi(
        var pullResponse: PullResponse = PullResponse(0, emptyList()),
    ) : SyncApi {
        val pushedSets = mutableListOf<ChangeSet>()

        override fun listCampaigns(): List<CampaignSummary> = emptyList()

        override fun createCampaign(name: String): CampaignSummary =
            throw UnsupportedOperationException("createCampaign is not exercised by SyncEngineOverSqlDelightTest")

        override fun pull(
            campaignId: String,
            since: Long,
        ): PullResponse = pullResponse.copy(records = pullResponse.records.filter { it.rev > since })

        override fun push(
            campaignId: String,
            changeSet: ChangeSet,
        ): Long {
            pushedSets += changeSet
            return changeSet.records.maxOfOrNull { it.rev } ?: 0L
        }
    }

    @Test
    fun canonicalLocalNewerThanStaleRemoteSurvivesAndIsReEnqueued() {
        val database = inMemoryDatabase()
        val campaignStore = seed(database)
        val syncStore = SqlDelightSyncStore(database)
        // Acknowledge the local edit so only the canonical row remains (the C1 scenario).
        syncStore.clearAcknowledged(campaignId.value, syncStore.pending(campaignId.value))
        assertTrue(syncStore.pending(campaignId.value).isEmpty())

        val api =
            FakeSyncApi(
                pullResponse =
                    PullResponse(
                        latestRev = 9,
                        records =
                            listOf(
                                settlementRecord(
                                    name = "Stale remote",
                                    updatedAt = HybridLogicalClock(1_700_000_000_100, 0),
                                    rev = 9,
                                ),
                            ),
                    ),
            )

        val outcome = SyncEngine(api, syncStore).sync(campaignId.value)

        assertEquals(
            "Local",
            database.settlementQueries
                .selectSettlementById(settlementId.value)
                .executeAsOne()
                .name,
            "a stale remote must not overwrite the canonical local winner",
        )
        assertEquals(
            settlementId.value,
            api.pushedSets
                .single()
                .records
                .single()
                .entityId,
            "the canonical local winner must be re-pushed to the hub",
        )
        assertEquals(1, outcome.pushed)
        assertEquals(9, syncStore.cursor(campaignId.value))
        assertTrue(
            campaignStore
                .loadSnapshots()
                .single()
                .settlements
                .single()
                .name == "Local",
        )
    }

    @Test
    fun unknownEntityTypeInTheBatchIsSkippedAndTheCursorStillAdvances() {
        val database = inMemoryDatabase()
        seed(database)
        val skipped = mutableListOf<String>()
        val syncStore = SqlDelightSyncStore(database, log = skipped::add)
        syncStore.setCursor(campaignId.value, 4)

        val unknown =
            ChangeRecord(
                campaignId = campaignId.value,
                entityType = "survivor",
                entityId = "survivor-1",
                op = ChangeOp.UPSERT,
                baseRev = null,
                payload = """{"name":"Wendigo Hunter"}""",
                updatedAtMillis = 1_700_000_900_000,
                updatedAtCounter = 0,
                rev = 10,
            )
        val known =
            settlementRecord(
                name = "Remote after migration",
                updatedAt = HybridLogicalClock(1_700_000_999_000, 0),
                rev = 11,
            )
        val api = FakeSyncApi(pullResponse = PullResponse(latestRev = 11, records = listOf(unknown, known)))

        SyncEngine(api, syncStore).sync(campaignId.value)

        assertEquals(11, syncStore.cursor(campaignId.value), "the cursor must advance past the skipped record")
        assertTrue(skipped.single().contains("survivor"), "the unknown type must be logged")
        assertEquals(
            "Remote after migration",
            database.settlementQueries
                .selectSettlementById(settlementId.value)
                .executeAsOne()
                .name,
            "a known record in the same batch must still apply",
        )
    }
}

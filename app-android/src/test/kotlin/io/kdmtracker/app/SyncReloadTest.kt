package io.kdmtracker.app

import io.kdmtracker.app.sync.SyncCoordinator
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.sync.CampaignSummary
import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.ChangeSet
import io.kdmtracker.core.sync.InMemoryMembershipStore
import io.kdmtracker.core.sync.PullResponse
import io.kdmtracker.core.sync.SyncApi
import io.kdmtracker.core.sync.SyncEngine
import io.kdmtracker.core.sync.SyncStore
import io.kdmtracker.core.transfer.HybridLogicalClockDto
import io.kdmtracker.core.transfer.SettlementDto
import io.kdmtracker.core.transfer.SyncMetaDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * End-to-end C2 regression (Ruling AE): a remote change applied by a sync pass must reach the
 * in-memory repository/controller, and a subsequent local edit must not write the stale pre-sync
 * settlement back over it.
 */
class SyncReloadTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun remoteChangeIsVisibleAfterSyncAndSurvivesASubsequentLocalEdit() {
        val campaignStore = InMemoryCampaignStore()
        val repository = SettlementRepository(campaignStore)
        val controller = LanternYearController(repository)
        val seeded = repository.settlement

        val remote =
            seeded.copy(
                name = "Remote settlement",
                population = 42,
                sync =
                    SyncMeta(
                        updatedAt = HybridLogicalClock(seeded.sync.updatedAt.millis + 10_000, 0),
                        rev = 99,
                    ),
            )
        val syncStore = InMemorySyncStore(campaignStore)
        val api = FakeSyncApi(PullResponse(latestRev = 1, records = listOf(remote.toRecord())))
        val coordinator =
            SyncCoordinator(
                engine = SyncEngine(api, syncStore),
                campaignId = repository.campaign.id.value,
                memberships = InMemoryMembershipStore(),
                scope = CoroutineScope(Dispatchers.Unconfined),
                tokenProvider = { "token" },
                dispatcher = Dispatchers.Unconfined,
                onSynced = controller::reload,
            )

        coordinator.syncNow()

        assertEquals("Remote settlement", repository.settlement.name, "sync must refresh the repository")
        assertEquals(42, controller.state.value.population, "the controller must render the remote change")

        controller.advance()

        assertEquals(
            "Remote settlement",
            repository.settlement.name,
            "a local edit must not revert the remote change the cursor already advanced past",
        )
    }

    private fun Settlement.toRecord(): ChangeRecord {
        val dto =
            SettlementDto(
                id = id.value,
                campaignId = campaignId.value,
                name = name,
                lanternYear = lanternYear,
                phase = phase,
                currentStep = currentStep,
                endeavors = endeavors,
                population = population,
                settlementEvent = settlementEvent,
                sync =
                    SyncMetaDto(
                        updatedAt = HybridLogicalClockDto(sync.updatedAt.millis, sync.updatedAt.counter),
                        rev = sync.rev,
                        deletedAt = sync.deletedAt?.let { HybridLogicalClockDto(it.millis, it.counter) },
                    ),
            )
        return ChangeRecord(
            campaignId = campaignId.value,
            entityType = "settlement",
            entityId = id.value,
            op = ChangeOp.UPSERT,
            baseRev = null,
            payload = json.encodeToString(SettlementDto.serializer(), dto),
            updatedAtMillis = sync.updatedAt.millis,
            updatedAtCounter = sync.updatedAt.counter,
            rev = sync.rev,
        )
    }

    private fun SettlementDto.toDomain(): Settlement =
        Settlement(
            id = Ulid.parse(id),
            campaignId = Ulid.parse(campaignId),
            name = name,
            lanternYear = lanternYear,
            phase = phase,
            currentStep = currentStep,
            endeavors = endeavors,
            population = population,
            settlementEvent = settlementEvent,
            sync =
                SyncMeta(
                    updatedAt = HybridLogicalClock(sync.updatedAt.millis, sync.updatedAt.counter),
                    rev = sync.rev,
                    deletedAt = sync.deletedAt?.let { HybridLogicalClock(it.millis, it.counter) },
                ),
        )

    private class FakeSyncApi(
        private val pullResponse: PullResponse,
    ) : SyncApi {
        override fun listCampaigns(): List<CampaignSummary> = emptyList()

        override fun createCampaign(name: String): CampaignSummary =
            throw UnsupportedOperationException("createCampaign is not exercised by SyncReloadTest")

        override fun pull(
            campaignId: String,
            since: Long,
        ): PullResponse = pullResponse

        override fun push(
            campaignId: String,
            changeSet: ChangeSet,
        ): Long = changeSet.records.maxOfOrNull { it.rev } ?: 0L
    }

    /** Bridges the app's [InMemoryCampaignStore] to the [SyncStore] port for the reload test. */
    private inner class InMemorySyncStore(
        private val campaignStore: InMemoryCampaignStore,
    ) : SyncStore {
        private var cursor = 0L
        private val outbox = LinkedHashMap<Pair<String, String>, ChangeRecord>()

        override fun cursor(campaignId: String): Long = cursor

        override fun setCursor(
            campaignId: String,
            rev: Long,
        ) {
            cursor = rev
        }

        override fun pending(campaignId: String): List<ChangeRecord> = outbox.values.toList()

        override fun local(
            campaignId: String,
            entityType: String,
            entityId: String,
        ): ChangeRecord? =
            campaignStore
                .loadSnapshots()
                .flatMap { it.settlements }
                .firstOrNull { it.id.value == entityId }
                ?.toRecord()

        override fun enqueue(
            campaignId: String,
            record: ChangeRecord,
        ) {
            outbox[record.entityType to record.entityId] = record
        }

        override fun applyMerged(
            campaignId: String,
            record: ChangeRecord,
        ) {
            campaignStore.upsertSettlement(json.decodeFromString(SettlementDto.serializer(), record.payload).toDomain())
            outbox.remove(record.entityType to record.entityId)
        }

        override fun clearAcknowledged(
            campaignId: String,
            records: List<ChangeRecord>,
        ) {
            records.forEach { outbox.remove(it.entityType to it.entityId) }
        }
    }
}

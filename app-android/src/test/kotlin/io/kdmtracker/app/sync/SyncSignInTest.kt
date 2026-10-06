package io.kdmtracker.app.sync

import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.ChangeSet
import io.kdmtracker.core.sync.PullResponse
import io.kdmtracker.core.sync.SyncApi
import io.kdmtracker.core.sync.SyncEngine
import io.kdmtracker.core.sync.SyncStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Covers the token acquisition path (Ruling AG): signing in stores a bearer token, which flips the
 * coordinator from [SyncState.NotConfigured] to an actual (now authenticated) sync pass.
 */
class SyncSignInTest {
    @Test
    fun loginStoresATokenAndThenSyncProceeds() {
        val tokens = InMemoryTokenStore()
        assertNull(tokens.read())

        SyncSignIn(authApi = { _, _ -> "jwt-123" }, tokens = tokens).signIn("alice", "secret")

        assertEquals("jwt-123", tokens.read())

        var pullCalls = 0
        val api =
            object : SyncApi {
                override fun pull(
                    campaignId: String,
                    since: Long,
                ): PullResponse {
                    pullCalls++
                    return PullResponse(latestRev = 0, records = emptyList())
                }

                override fun push(
                    campaignId: String,
                    changeSet: ChangeSet,
                ): Long = 0
            }
        val coordinator =
            SyncCoordinator(
                engine = SyncEngine(api, NoopSyncStore()),
                campaignId = "c1",
                scope = CoroutineScope(Dispatchers.Unconfined),
                tokenProvider = tokens::read,
                dispatcher = Dispatchers.Unconfined,
            )

        coordinator.syncNow()

        assertEquals(1, pullCalls, "a stored token must let the sync pass reach the hub")
        assertIs<SyncState.Idle>(coordinator.state.value)
    }

    @Test
    fun aRejectedLoginStoresNoTokenAndLeavesSyncNotConfigured() {
        val tokens = InMemoryTokenStore()
        val signIn = SyncSignIn(authApi = { _, _ -> throw IOException("invalid credentials") }, tokens = tokens)

        runCatching { signIn.signIn("alice", "wrong") }

        assertNull(tokens.read())
        val coordinator =
            SyncCoordinator(
                engine =
                    SyncEngine(
                        api =
                            object : SyncApi {
                                override fun pull(
                                    campaignId: String,
                                    since: Long,
                                ): PullResponse = PullResponse(since, emptyList())

                                override fun push(
                                    campaignId: String,
                                    changeSet: ChangeSet,
                                ): Long = 0
                            },
                        store = NoopSyncStore(),
                    ),
                campaignId = "c1",
                scope = CoroutineScope(Dispatchers.Unconfined),
                tokenProvider = tokens::read,
                dispatcher = Dispatchers.Unconfined,
            )

        coordinator.syncNow()

        assertEquals(SyncState.NotConfigured, coordinator.state.value)
    }

    private class InMemoryTokenStore : TokenStore {
        private var token: String? = null

        override fun read(): String? = token

        override fun write(token: String?) {
            this.token = token
        }
    }

    private class NoopSyncStore : SyncStore {
        override fun cursor(campaignId: String): Long = 0L

        override fun setCursor(
            campaignId: String,
            rev: Long,
        ) = Unit

        override fun pending(campaignId: String): List<ChangeRecord> = emptyList()

        override fun local(
            campaignId: String,
            entityType: String,
            entityId: String,
        ): ChangeRecord? = null

        override fun enqueue(
            campaignId: String,
            record: ChangeRecord,
        ) = Unit

        override fun applyMerged(
            campaignId: String,
            record: ChangeRecord,
        ) = Unit

        override fun clearAcknowledged(
            campaignId: String,
            records: List<ChangeRecord>,
        ) = Unit
    }
}

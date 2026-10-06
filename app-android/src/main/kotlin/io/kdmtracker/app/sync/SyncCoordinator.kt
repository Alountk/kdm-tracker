package io.kdmtracker.app.sync

import io.kdmtracker.core.sync.MembershipStore
import io.kdmtracker.core.sync.SyncEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The observable status of the last sync attempt.
 *
 * [Idle] doubles as "never synced" ([lastSyncedAtMillis] is `null`) and "last sync succeeded at
 * [lastSyncedAtMillis]"; [Error] is non-fatal and terminal for that attempt — the app keeps working
 * and the outbox stays intact for a retry.
 */
sealed interface SyncState {
    data class Idle(
        val lastSyncedAtMillis: Long? = null,
    ) : SyncState

    data object Syncing : SyncState

    /** No bearer token is stored, so a sync would be rejected `401`; the user must sign in first. */
    data object NotConfigured : SyncState

    data class Error(
        val message: String,
    ) : SyncState
}

/**
 * Runs the `:core` [SyncEngine] off the UI thread and mirrors its outcome into [state].
 *
 * The engine call is blocking (HTTP + SQLDelight), so [syncNow] and [syncAll] dispatch it onto
 * [dispatcher] (Review Focus 5) and never let a failure escape: a thrown exception becomes
 * [SyncState.Error] and the exception is swallowed so a flaky or offline hub cannot crash the app
 * (Review Focus 4). [CancellationException] is rethrown so structured cancellation still works when
 * the owning scope is torn down. The scope is supplied by the caller — the app hands in its
 * ViewModel scope — which keeps the coordinator JVM-testable and free of Android types beyond the
 * dispatcher default.
 */
class SyncCoordinator(
    private val engine: SyncEngine,
    private val campaignId: String,
    private val memberships: MembershipStore,
    private val scope: CoroutineScope,
    private val tokenProvider: () -> String? = { null },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
    private val onSynced: () -> Unit = {},
) {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle())

    val state: StateFlow<SyncState> = _state.asStateFlow()

    /**
     * Whether a sync pass is currently running. An [AtomicBoolean] because [syncNow] is called from
     * both the UI thread (the manual button) and [Dispatchers.IO] (after a mutation), so the
     * claim/release must be atomic and visible across threads.
     */
    private val inFlight = AtomicBoolean(false)

    /**
     * Launches one pull → merge → push pass on [dispatcher].
     *
     * Without a stored token the pass is not attempted: the hub would answer `401`, so the state
     * short-circuits to [SyncState.NotConfigured] and the user is prompted to sign in (Ruling AG).
     *
     * A sync already in flight is a no-op: the guard is an atomic compare-and-set, so overlapping
     * callers cannot both start a pass (which would mean concurrent SQLDelight access and duplicate
     * pushes). The flag is released in `finally` once the pass ends. A failure leaves the outbox
     * untouched for the next attempt. On success [onSynced] runs before the state flips to [Idle]
     * so the repository reloads the applied remote changes (Ruling AE).
     */
    fun syncNow() {
        if (tokenProvider() == null) {
            _state.value = SyncState.NotConfigured
            return
        }
        if (!inFlight.compareAndSet(false, true)) return
        _state.value = SyncState.Syncing
        scope.launch(dispatcher) {
            try {
                engine.sync(campaignId)
                onSynced()
                _state.value = SyncState.Idle(lastSyncedAtMillis = clock())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.value = SyncState.Error(failure.message ?: failure::class.simpleName ?: "Sync failed")
            } finally {
                inFlight.set(false)
            }
        }
    }

    /**
     * Syncs every campaign the signed-in user can reach (Review Focus 1).
     *
     * Lists the caller's campaigns, caches each membership for offline badges, then runs a full
     * pull → merge → push pass for every campaign id — the server-listed ones plus any
     * [campaignIdsProvider] adds (a local campaign not on the hub yet). Each campaign has its own
     * cursor, so a campaign whose pull/push fails does not abort the others (Review Focus 3): the
     * healthy campaigns still advance and the failures are collected and surfaced as
     * [SyncState.Error] naming the campaigns that failed.
     *
     * Shares [syncNow]'s token and in-flight guards, so a manual sync and the sign-in sync cannot
     * overlap.
     */
    fun syncAll(campaignIdsProvider: () -> List<String> = { emptyList() }) {
        if (tokenProvider() == null) {
            _state.value = SyncState.NotConfigured
            return
        }
        if (!inFlight.compareAndSet(false, true)) return
        _state.value = SyncState.Syncing
        scope.launch(dispatcher) {
            try {
                val summaries = engine.listCampaigns()
                summaries.forEach { memberships.upsertMembership(it) }
                val campaignIds = (summaries.map { it.id } + campaignIdsProvider()).distinct()
                val failures = campaignIds.mapNotNull(::syncOne)
                onSynced()
                _state.value =
                    if (failures.isEmpty()) {
                        SyncState.Idle(lastSyncedAtMillis = clock())
                    } else {
                        SyncState.Error("Failed to sync ${failures.size} campaign(s): ${failures.joinToString("; ")}")
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.value = SyncState.Error(failure.message ?: failure::class.simpleName ?: "Sync failed")
            } finally {
                inFlight.set(false)
            }
        }
    }

    /** Runs one campaign's pass, mapping a failure to its `"id: message"` string; rethrows cancellation. */
    private fun syncOne(campaignId: String): String? =
        try {
            engine.sync(campaignId)
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            "$campaignId: ${failure.message ?: failure::class.simpleName ?: "sync failed"}"
        }
}

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
    campaignId: String,
    private val engine: SyncEngine,
    private val memberships: MembershipStore,
    private val scope: CoroutineScope,
    private val tokenProvider: () -> String? = { null },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
    private val onSynced: () -> Unit = {},
) {
    /**
     * The campaign a [syncNow] pass operates on.
     *
     * Re-bound by [rebind] when the Home selection changes (Ruling AJ): the id is captured at
     * construction, so a selection must move the sync target or [syncNow] would keep syncing the
     * campaign the app started on. Volatile because the UI thread rebinds while an IO pass reads.
     */
    @Volatile
    var campaignId: String = campaignId
        private set

    /** Points [syncNow] at [campaignId]'s campaign after a Home selection. */
    fun rebind(campaignId: String) {
        this.campaignId = campaignId
    }

    /**
     * The last attempt's status.
     *
     * Seeded to [SyncState.NotConfigured] when no token is stored, so the sign-in form is visible
     * before the first sync is ever attempted (sign-in minor). With a token it starts [SyncState.Idle].
     */
    private val _state = MutableStateFlow(initialState())

    val state: StateFlow<SyncState> = _state.asStateFlow()

    private fun initialState(): SyncState =
        if (tokenProvider() == null) {
            SyncState.NotConfigured
        } else {
            SyncState.Idle()
        }

    /** Marks the session signed out (its token was cleared), so the UI reflects it immediately. */
    fun markSignedOut() {
        _state.value = SyncState.NotConfigured
    }

    /**
     * Increments once per completed sync pass, whatever its outcome.
     *
     * The Home card list refreshes on every completion — a failing campaign must not hide the
     * campaigns that did sync (Ruling AP). [state] alone cannot signal this: it stays [SyncState.Error]
     * when any campaign fails, so a success-only refresh would drop the whole list.
     */
    private val _completedSyncs = MutableStateFlow(0L)

    val completedSyncs: StateFlow<Long> = _completedSyncs.asStateFlow()

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
                _completedSyncs.value += 1
            }
        }
    }

    /**
     * Syncs every campaign the signed-in user can reach (Review Focus 1).
     *
     * The sync set is derived from the **hub**, never from local rows (Ruling AQ): the campaigns the
     * hub's `listCampaigns` returns, plus any campaign the app created through the hub (cached as a
     * membership by `create`). A local, client-minted campaign the hub does not know — the first-run
     * seed — has no membership and is therefore never submitted, so it cannot 403 the pass. Each
     * campaign has its own cursor, so a campaign whose pull/push fails does not abort the others
     * (Review Focus 3): the healthy campaigns still advance and the failures are collected and
     * surfaced as [SyncState.Error] naming the campaigns that failed.
     *
     * Shares [syncNow]'s token and in-flight guards, so a manual sync and the sign-in sync cannot
     * overlap. On every completion [completedSyncs] ticks so the Home list refreshes regardless of
     * the outcome (Ruling AP).
     */
    fun syncAll() {
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
                val campaignIds = (summaries.map { it.id } + memberships.memberships().map { it.id }).distinct()
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
                _completedSyncs.value += 1
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

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
 * The engine call is blocking (HTTP + SQLDelight), so it is dispatched onto [dispatcher]. A thrown
 * exception becomes [SyncState.Error] instead of escaping, so a flaky or offline hub cannot crash
 * the app; [CancellationException] is rethrown so the owning scope can still cancel.
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
     * The campaign [syncNow] operates on. The constructor value is captured once, so [rebind] must
     * move it when the Home selection changes. Volatile: the UI thread rebinds while an IO pass reads.
     */
    @Volatile
    var campaignId: String = campaignId
        private set

    fun rebind(campaignId: String) {
        this.campaignId = campaignId
    }

    /** Starts [SyncState.NotConfigured] without a stored token, so the sign-in form shows before any sync. */
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
     * Increments once per completed pass, whatever its outcome. [state] cannot signal this: it stays
     * [SyncState.Error] while any campaign fails, so a success-only refresh would drop the Home list.
     */
    private val _completedSyncs = MutableStateFlow(0L)

    val completedSyncs: StateFlow<Long> = _completedSyncs.asStateFlow()

    /**
     * Whether a pass is running. Atomic because [syncNow] is called from the UI thread (manual button)
     * and from [Dispatchers.IO] (after a mutation).
     */
    private val inFlight = AtomicBoolean(false)

    /**
     * Launches one pull → merge → push pass on [dispatcher].
     *
     * Without a token the hub would answer `401`, so the state short-circuits to
     * [SyncState.NotConfigured]. An in-flight pass makes this a no-op (atomic compare-and-set), since
     * overlapping passes would mean concurrent SQLDelight access and duplicate pushes. [onSynced] runs
     * before the state flips to [SyncState.Idle] so the repository reloads the applied remote changes.
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
     * Syncs every campaign the signed-in user can reach.
     *
     * The set comes from the **hub** (`listCampaigns`) plus campaigns created through it and cached as
     * memberships, never from local rows: a client-minted campaign the hub does not know (the first-run
     * seed) is skipped so it cannot 403 the pass. Each campaign has its own cursor, so one failing does
     * not abort the others; failures are collected into a single [SyncState.Error] naming them.
     *
     * Shares [syncNow]'s token and in-flight guards.
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

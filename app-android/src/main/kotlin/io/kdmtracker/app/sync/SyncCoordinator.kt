package io.kdmtracker.app.sync

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

    data class Error(
        val message: String,
    ) : SyncState
}

/**
 * Runs the `:core` [SyncEngine] off the UI thread and mirrors its outcome into [state].
 *
 * The engine call is blocking (HTTP + SQLDelight), so [syncNow] dispatches it onto [dispatcher]
 * (Review Focus 5) and never lets a failure escape: a thrown exception becomes [SyncState.Error]
 * and the exception is swallowed so a flaky or offline hub cannot crash the app (Review Focus 4).
 * [CancellationException] is rethrown so structured cancellation still works when the owning scope
 * is torn down. The scope is supplied by the caller — the app hands in its ViewModel scope — which
 * keeps the coordinator JVM-testable and free of Android types beyond the dispatcher default.
 */
class SyncCoordinator(
    private val engine: SyncEngine,
    private val campaignId: String,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
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
     * A sync already in flight is a no-op: the guard is an atomic compare-and-set, so overlapping
     * callers cannot both start a pass (which would mean concurrent SQLDelight access and duplicate
     * pushes). The flag is released in `finally` once the pass ends. A failure leaves the outbox
     * untouched for the next attempt.
     */
    fun syncNow() {
        if (!inFlight.compareAndSet(false, true)) return
        _state.value = SyncState.Syncing
        scope.launch(dispatcher) {
            try {
                engine.sync(campaignId)
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
}

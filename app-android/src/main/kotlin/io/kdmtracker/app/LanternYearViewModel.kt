package io.kdmtracker.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.kdmtracker.app.sync.AndroidKeystoreTokenStore
import io.kdmtracker.app.sync.DEFAULT_SYNC_BASE_URL
import io.kdmtracker.app.sync.HttpSyncApi
import io.kdmtracker.app.sync.SyncCoordinator
import io.kdmtracker.app.sync.SyncState
import io.kdmtracker.core.sync.SyncEngine
import io.kdmtracker.data.KdmDatabaseFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * Everything the ViewModel builds off the main thread.
 *
 * Constructing [controller] loads the campaign from the database, so the whole bundle is produced
 * inside the ViewModel's IO coroutine: the UI thread never touches the store. [close] releases
 * whatever the bundle opened (the SQLDelight driver), and [syncCoordinator] may be `null` when
 * wiring sync is not desired (for example in a unit test).
 */
class AppComponents(
    val controller: LanternYearController,
    val syncCoordinator: SyncCoordinator? = null,
    val close: () -> Unit = {},
)

/**
 * Retains the lantern-year loop across recomposition and configuration changes.
 *
 * The [LanternYearController] is built lazily by [buildComponents] on [dispatcher] (Review Focus 5:
 * no DB I/O on the UI thread), so [state] starts `null` until the load completes; the UI renders a
 * loading placeholder until then. Every mutation runs on the same IO dispatcher and, once it lands,
 * triggers [SyncCoordinator.syncNow] (a no-op when no coordinator is wired). [syncState] is always
 * available — it defaults to [SyncState.Idle] — so the sync control is usable before the load ends.
 *
 * [scope] is injectable so a JVM unit test can drive the ViewModel with `Dispatchers.Unconfined`
 * and observe results synchronously; production leaves it `null` and uses [viewModelScope], which
 * the lifecycle cancels automatically.
 */
class LanternYearViewModel(
    private val buildComponents: (CoroutineScope) -> AppComponents,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    scope: CoroutineScope? = null,
) : ViewModel() {
    private val scopeOverride: CoroutineScope? = scope
    private val workScope: CoroutineScope
        get() = scopeOverride ?: viewModelScope

    private val _state = MutableStateFlow<UiState?>(null)
    val state: StateFlow<UiState?> = _state.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * The loaded bundle, published from the IO load coroutine and read by mutations, [syncNow] and
     * [onCleared] on other threads. Held behind an [AtomicReference] so a reader never observes a
     * half-published trio (a stale `null` controller would silently drop a mutation).
     */
    private val components = AtomicReference<AppComponents?>(null)

    init {
        workScope.launch(dispatcher) {
            val loaded = buildComponents(workScope)
            components.set(loaded)
            _state.value = loaded.controller.state.value
            launch { loaded.controller.state.collect { _state.value = it } }
            loaded.syncCoordinator?.let { coordinator ->
                launch { coordinator.state.collect { _syncState.value = it } }
            }
        }
    }

    fun advance() = mutate { it.advance() }

    fun failHunt() = mutate { it.failHunt() }

    fun previous() = mutate { it.previous() }

    fun selectStep(number: Int) = mutate { it.selectStep(number) }

    fun syncNow() {
        components.get()?.syncCoordinator?.syncNow()
    }

    private fun mutate(block: (LanternYearController) -> Unit) {
        workScope.launch(dispatcher) {
            val loaded = components.get() ?: return@launch
            block(loaded.controller)
            loaded.syncCoordinator?.syncNow()
        }
    }

    override fun onCleared() {
        components.get()?.close?.invoke()
        scopeOverride?.cancel()
    }

    companion object {
        /** Creates the production ViewModel over the on-device database opened with [context]. */
        fun factory(context: Context): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val appContext = context.applicationContext
                    LanternYearViewModel(
                        buildComponents = { scope ->
                            val handle = KdmDatabaseFactory.openAndroidStore(appContext)
                            val repository = SettlementRepository(handle.store)
                            val tokenStore = AndroidKeystoreTokenStore(appContext)
                            val engine =
                                SyncEngine(
                                    api = HttpSyncApi(DEFAULT_SYNC_BASE_URL, tokenStore::read),
                                    store = handle.syncStore,
                                )
                            AppComponents(
                                controller = LanternYearController(repository),
                                syncCoordinator = SyncCoordinator(engine, repository.campaign.id.value, scope),
                                close = handle::close,
                            )
                        },
                    )
                }
            }
    }
}

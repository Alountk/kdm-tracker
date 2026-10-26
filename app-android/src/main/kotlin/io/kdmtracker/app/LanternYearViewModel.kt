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
import io.kdmtracker.app.sync.HttpSyncAuthApi
import io.kdmtracker.app.sync.SyncCoordinator
import io.kdmtracker.app.sync.SyncSignIn
import io.kdmtracker.app.sync.SyncState
import io.kdmtracker.app.ui.shell.ShellTab
import io.kdmtracker.core.sync.SyncEngine
import io.kdmtracker.data.KdmDatabaseFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Built off the main thread (constructing the controller loads from the DB). [close] releases the
 * SQLDelight driver; [syncCoordinator] is `null` when sync is not wired, for example in unit tests.
 */
class AppComponents(
    val controller: LanternYearController,
    val syncCoordinator: SyncCoordinator? = null,
    val signIn: SyncSignIn? = null,
    val signOut: (() -> Unit)? = null,
    val close: () -> Unit = {},
)

/**
 * Retains the lantern-year loop across recomposition and configuration changes.
 *
 * Components are built on [dispatcher] so no DB I/O touches the UI thread; [state] is `null` until
 * the load completes, while [syncState] is available from the start. [scope] is injectable so JVM
 * tests can use `Dispatchers.Unconfined`; production uses [viewModelScope].
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

    /** One card per stored campaign; refreshed after loads, creates and syncs. */
    private val _homeCards = MutableStateFlow<List<CampaignCard>>(emptyList())
    val homeCards: StateFlow<List<CampaignCard>> = _homeCards.asStateFlow()

    /** Set when [create] fails, so the Home can show it inline. */
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    /**
     * Increments once per successful [create], so the Home can clear the New-settlement field only
     * when the create actually landed (a failure keeps the typed name).
     */
    private val _createSucceeded = MutableStateFlow(0L)
    val createSucceeded: StateFlow<Long> = _createSucceeded.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /** Set on a failed sign-in so the Account screen shows it inline; no token is stored, so the form stays. */
    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError.asStateFlow()

    /** Set when the initial load fails, so the UI can show an error instead of spinning forever. */
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    /**
     * Whether the Home is the visible destination. [select] and [create] leave it; every controller
     * frame is published with [Screen.Home] while it is set. Volatile: the UI thread flips it while
     * the IO collector reads it.
     */
    @Volatile
    private var onHome = true

    /** Whether the Account overlay is showing, above Home and the phases. Volatile like [onHome]. */
    @Volatile
    private var accountOpen = false

    /** The tab the user last picked, overriding the phase-derived screen; `null` follows it. Volatile like [onHome]. */
    @Volatile
    private var shellTab: ShellTab? = null

    /**
     * The loaded bundle, published from the IO load coroutine and read by mutations, [syncNow] and
     * [onCleared] on other threads. Held behind an [AtomicReference] so a reader never observes a
     * half-published trio (a stale `null` controller would silently drop a mutation).
     */
    private val components = AtomicReference<AppComponents?>(null)

    /**
     * Set when [onCleared] runs. If teardown races the IO load, the freshly built bundle is closed
     * as soon as it is published instead of being leaked; paired with an idempotent [close].
     */
    private val cleared = AtomicBoolean(false)

    init {
        workScope.launch(dispatcher) {
            val loaded =
                try {
                    buildComponents(workScope)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    _loadError.value = failure.message ?: failure::class.simpleName ?: "Failed to load"
                    return@launch
                }
            components.set(loaded)
            if (cleared.get()) loaded.close()
            _homeCards.value = loaded.controller.cards()
            _state.value =
                loaded.controller.state.value
                    .forDisplay()
            launch { loaded.controller.state.collect { _state.value = it.forDisplay() } }
            loaded.syncCoordinator?.let { coordinator ->
                launch {
                    coordinator.state.collect { syncState ->
                        _syncState.value = syncState
                    }
                }
                launch {
                    coordinator.completedSyncs.collect {
                        _homeCards.value = loaded.controller.cards()
                    }
                }
            }
        }
    }

    /** Overrides the phase-derived screen with [Screen.Home] or [Screen.Account] while set. */
    private fun UiState.forDisplay(): UiState {
        val tab = shellTab
        return when {
            accountOpen -> copy(screen = Screen.Account)
            onHome -> copy(screen = Screen.Home)
            tab != null -> copy(screen = tab.screen)
            else -> this
        }
    }

    /** Selects a shell tab — the design's free navigation. Clears any stale action error. */
    fun show(screen: Screen) {
        val tab = ShellTab.of(screen) ?: return
        shellTab = tab
        _actionError.value = null
        components.get()?.let {
            _state.value =
                it.controller.state.value
                    .forDisplay()
        }
    }

    /** Opens the campaign in the cockpit, re-points the coordinator at it and syncs; an unknown id is a no-op. */
    fun select(campaignId: String) {
        workScope.launch(dispatcher) {
            val loaded = components.get() ?: return@launch
            if (!loaded.controller.selectCampaign(campaignId)) return@launch
            onHome = false
            shellTab = ShellTab.initialFor(loaded.controller.state.value.phase)
            loaded.syncCoordinator?.rebind(campaignId)
            _state.value =
                loaded.controller.state.value
                    .forDisplay()
            loaded.syncCoordinator?.syncNow()
        }
    }

    /** [onHome] flips immediately; the blocking read that rebuilds the cards runs on [dispatcher]. */
    fun goHome() {
        onHome = true
        shellTab = null
        workScope.launch(dispatcher) {
            val loaded = components.get() ?: return@launch
            _state.value =
                loaded.controller.state.value
                    .forDisplay()
            _homeCards.value = loaded.controller.cards()
        }
    }

    /** Opens the Account overlay above the current screen; [closeAccount] returns to it. */
    fun goAccount() {
        accountOpen = true
        components.get()?.let {
            _state.value =
                it.controller.state.value
                    .forDisplay()
        }
    }

    /** Closes the Account overlay, returning to Home or the phase it was opened from. */
    fun closeAccount() {
        accountOpen = false
        components.get()?.let {
            _state.value =
                it.controller.state.value
                    .forDisplay()
        }
    }

    /**
     * Creates a settlement through the hub and opens it.
     *
     * Any failure (transport, non-2xx, malformed body) surfaces as [actionError] and leaves no
     * half-created state. [actionError] is cleared first so a stale one cannot linger during a retry.
     */
    fun create(name: String) {
        if (name.isBlank()) return
        workScope.launch(dispatcher) {
            val loaded = components.get() ?: return@launch
            _actionError.value = null
            try {
                val card = loaded.controller.createCampaign(name)
                _homeCards.value = loaded.controller.cards()
                _createSucceeded.value += 1
                onHome = false
                shellTab = ShellTab.initialFor(loaded.controller.state.value.phase)
                loaded.syncCoordinator?.rebind(card.campaignId)
                _state.value =
                    loaded.controller.state.value
                        .forDisplay()
                loaded.syncCoordinator?.syncNow()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _actionError.value = failure.message ?: "Could not create the settlement"
            }
        }
    }

    /** Syncs every campaign the user can reach. */
    fun syncAll() {
        components.get()?.syncCoordinator?.syncAll()
    }

    /** Step 4 convenience: records a random event from the deck. */
    fun drawSettlementEvent() {
        val deck = _state.value?.settlementEventDeck.orEmpty()
        if (deck.isNotEmpty()) {
            mutate { settlement.recordSettlementEvent(deck.random().name) }
        }
    }

    fun syncNow() {
        components.get()?.syncCoordinator?.syncNow()
    }

    /** Clears the stored token and marks the state signed out without waiting for a failed sync. */
    fun signOut() {
        components.get()?.signOut?.invoke()
        _syncState.value = SyncState.NotConfigured
    }

    /** A failure surfaces as [signInError] instead of crashing. */
    fun signIn(
        username: String,
        password: String,
    ) {
        _signInError.value = null
        workScope.launch(dispatcher) {
            val loaded = components.get() ?: return@launch
            val signIn = loaded.signIn ?: return@launch
            try {
                signIn.signIn(username, password)
                loaded.syncCoordinator?.syncAll()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // A failed sign-in stores no token: keep the session signed out (the form stays) and
                // surface the reason, rather than flipping to Error which reads as "signed in".
                _signInError.value = failure.message ?: failure::class.simpleName ?: "Sign in failed"
            }
        }
    }

    /** Runs [block] on the controller off the main thread, then syncs. */
    fun mutate(block: LanternYearController.() -> Unit) {
        workScope.launch(dispatcher) {
            val loaded = components.get() ?: return@launch
            loaded.controller.block()
            loaded.syncCoordinator?.syncNow()
        }
    }

    override fun onCleared() {
        cleared.set(true)
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
                            val tokenStore = AndroidKeystoreTokenStore(appContext)
                            val api = HttpSyncApi(DEFAULT_SYNC_BASE_URL, tokenStore::read)
                            val repository =
                                SettlementRepository(
                                    handle.store,
                                    memberships = handle.membershipStore,
                                    createCampaign = api::createCampaign,
                                )
                            val controller = LanternYearController(repository)
                            val engine =
                                SyncEngine(
                                    api = api,
                                    store = handle.syncStore,
                                )
                            val syncCoordinator =
                                SyncCoordinator(
                                    engine = engine,
                                    campaignId = repository.campaign.id.value,
                                    memberships = handle.membershipStore,
                                    scope = scope,
                                    tokenProvider = tokenStore::read,
                                    onSynced = controller::reload,
                                )
                            AppComponents(
                                controller = controller,
                                syncCoordinator = syncCoordinator,
                                signIn = SyncSignIn(HttpSyncAuthApi(DEFAULT_SYNC_BASE_URL), tokenStore),
                                signOut = {
                                    tokenStore.write(null)
                                    syncCoordinator.markSignedOut()
                                },
                                close = handle::close,
                            )
                        },
                    )
                }
            }
    }
}

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
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid
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
    val signIn: SyncSignIn? = null,
    val signOut: (() -> Unit)? = null,
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

    /** The Home campaign list, one card per stored campaign, refreshed after loads, creates and syncs. */
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

    /**
     * Set when a sign-in attempt fails, so the Account screen can show it inline while the session
     * stays signed out (a failed sign-in stores no token, so the form must remain).
     */
    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError.asStateFlow()

    /** Set when the initial load fails, so the UI can show an error instead of spinning forever. */
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    /**
     * Whether the Home is the visible destination (Ruling AK). The app starts here; [select] and
     * [create] leave it for the cockpit, and every controller frame is published with
     * [Screen.Home] while it is set. Volatile because the UI thread flips it while the IO collector
     * reads it on each emission.
     */
    @Volatile
    private var onHome = true

    /**
     * Whether the Account overlay is showing. It sits above Home and the phases; [forDisplay] renders
     * [Screen.Account] while it is set. Volatile for the same reason as [onHome].
     */
    @Volatile
    private var accountOpen = false

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
    private fun UiState.forDisplay(): UiState =
        when {
            accountOpen -> copy(screen = Screen.Account)
            onHome -> copy(screen = Screen.Home)
            else -> this
        }

    /**
     * Opens [campaignId]'s campaign in the cockpit and re-points the coordinator at it (Ruling AJ).
     *
     * Runs on [dispatcher] (the repository read is blocking) and triggers a sync of the newly
     * selected campaign. An unknown id is a no-op, leaving the Home in place.
     */
    fun select(campaignId: String) {
        workScope.launch(dispatcher) {
            val loaded = components.get() ?: return@launch
            if (!loaded.controller.selectCampaign(campaignId)) return@launch
            onHome = false
            loaded.syncCoordinator?.rebind(campaignId)
            _state.value = loaded.controller.state.value
            loaded.syncCoordinator?.syncNow()
        }
    }

    /**
     * Returns to the Home from the cockpit and republishes the Home frame (Ruling AR).
     *
     * [onHome] flips immediately (the controller collector reads it per emission); the blocking
     * repository read that rebuilds the card list runs on [dispatcher] (Review Focus 5).
     */
    fun goHome() {
        onHome = true
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
     * Creates a settlement named [name] through the hub and opens it.
     *
     * The hub round-trip runs on [dispatcher]; any failure — a transport error, a non-2xx hub reply,
     * or a malformed 2xx body that fails to parse — surfaces as [actionError] and the Home stays put
     * with no half-created state (Review Focus 4). [actionError] is cleared at the start of the
     * attempt so a stale error cannot linger while the retry is in flight.
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
                loaded.syncCoordinator?.rebind(card.campaignId)
                _state.value = loaded.controller.state.value
                loaded.syncCoordinator?.syncNow()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _actionError.value = failure.message ?: "Could not create the settlement"
            }
        }
    }

    /** Syncs every campaign the user can reach; the Home's Sync control (Review Focus 1). */
    fun syncAll() {
        components.get()?.syncCoordinator?.syncAll()
    }

    fun advance() = mutate { it.advance() }

    fun failHunt() = mutate { it.failHunt() }

    fun previous() = mutate { it.previous() }

    fun selectStep(number: Int) = mutate { it.selectStep(number) }

    /** Unlocks [innovationName] through the Innovate action; a `Blocked` reason is published. */
    fun innovate(innovationName: String) = mutate { it.innovate(innovationName) }

    /** Builds the settlement location named [locationName]. */
    fun build(locationName: String) = mutate { it.build(locationName) }

    /** Spends one endeavor for a generic Settlement Phase action. */
    fun spendEndeavor() = mutate { it.spendEndeavor() }

    /** Appends [entry] to the settlement's resource ledger. */
    fun addResource(entry: ResourceEntry) = mutate { it.addResource(entry) }

    fun craft(
        locationName: String,
        gearName: String,
    ) = mutate { it.craft(locationName, gearName) }

    /** Records [count] deaths at Step 5. */
    fun recordDeaths(count: Int) = mutate { it.recordDeaths(count) }

    /** Records [count] births at Step 5. */
    fun recordBirths(count: Int) = mutate { it.recordBirths(count) }

    /** Records the chosen principle [label] for [category] (Steps 5/6). */
    fun choosePrinciple(
        category: PrincipleCategory,
        label: String,
    ) = mutate { it.choosePrinciple(category, label) }

    /** Ticks the reached milestone [id] as resolved (Step 6). */
    fun resolveMilestone(id: MilestoneId) = mutate { it.resolveMilestone(id) }

    /** Records the Settlement Event [name] drawn this year (Step 4). */
    fun recordSettlementEvent(name: String) = mutate { it.recordSettlementEvent(name) }

    /** Draws a random settlement event from the deck and records it (Step 4 convenience). */
    fun drawSettlementEvent() {
        val deck = _state.value?.settlementEventDeck.orEmpty()
        if (deck.isNotEmpty()) {
            recordSettlementEvent(deck.random().name)
        }
    }

    fun syncNow() {
        components.get()?.syncCoordinator?.syncNow()
    }

    /** Heals the returning survivors (Step 2). */
    fun healSurvivors() = mutate { it.healSurvivors() }

    /** Chooses the departing survivors (Step 8). */
    fun markDeparting(ids: List<Ulid>) = mutate { it.markDeparting(ids) }

    /** Assigns settlement gear to a survivor (Step 8). */
    fun assignGear(
        survivorId: Ulid,
        gearName: String,
    ) = mutate { it.assignGear(survivorId, gearName) }

    /** Removes gear from a survivor (Step 8). */
    fun unassignGear(
        survivorId: Ulid,
        gearName: String,
    ) = mutate { it.unassignGear(survivorId, gearName) }

    /** Saves an edited survivor from the Survivor Editor. */
    fun updateSurvivor(survivor: Survivor) = mutate { it.updateSurvivor(survivor) }

    /** Sets up the hunt board (quarry + level + players). */
    fun setUpHunt(
        quarry: String,
        level: Int,
        players: Int,
    ) = mutate { it.setUpHunt(quarry, level, players) }

    /** Assigns a party survivor to a player (1-based). */
    fun assignPlayer(
        survivorId: Ulid,
        player: Int,
    ) = mutate { it.assignPlayer(survivorId, player) }

    /** Edits a hunt-board space's kind. */
    fun markSpaceKind(
        index: Int,
        kind: HuntSpaceKind,
    ) = mutate { it.markSpaceKind(index, kind) }

    /** Advances the hunt party one space. */
    fun advanceHunt() = mutate { it.advanceHunt() }

    /** Places a hunt event on a space. */
    fun assignHuntEvent(
        index: Int,
        eventName: String,
    ) = mutate { it.assignHuntEvent(index, eventName) }

    /** Marks a hunt event resolved. */
    fun resolveHuntEvent(
        index: Int,
        eventName: String,
    ) = mutate { it.resolveHuntEvent(index, eventName) }

    /** Records the hunt outcome and moves the lantern year. */
    fun finishHunt(outcome: HuntStatus) = mutate { it.finishHunt(outcome) }

    /**
     * Signs the session out: clears the stored token through the components and marks the state
     * signed out, so the chip and Account screen reflect it without waiting for a failed sync.
     */
    fun signOut() {
        components.get()?.signOut?.invoke()
        _syncState.value = SyncState.NotConfigured
    }

    /**
     * Signs in with the given credentials on the IO thread, stores the token, then attempts a sync.
     *
     * A failure surfaces as [SyncState.Error] instead of crashing, mirroring the sync path.
     */
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

    private fun mutate(block: (LanternYearController) -> Unit) {
        workScope.launch(dispatcher) {
            val loaded = components.get() ?: return@launch
            block(loaded.controller)
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

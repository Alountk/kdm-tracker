# Top Bar Chrome + Account Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the two stacked top chromes (the Android ActionBar and the Compose SyncBar) with a single global `AppTopBar` (title + tappable sync-status chip + Home + Account) and a dedicated Account screen for sign-in/out.

**Architecture:** All chrome state is derived by a pure `AppChrome.of(screen, settlementName, syncState)` seam in `:app-android`; `KdmTrackerApp` renders one `TopAppBar` from it and dispatches Home / the three phases / Account. Sign-in, sign-out and manual sync live on the Account screen, driven by the existing `LanternYearViewModel`.

**Tech Stack:** Kotlin, Jetpack Compose (Material3 `TopAppBar`/`Scaffold`), Android XML theme, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-10-08-top-bar-chrome-design.md`

## Global Constraints

- No Compose UI tests (repo convention); testable behavior goes through the pure `AppChrome` seam and the ViewModel.
- UI copy is **English**, consistent with the existing chrome (`Home`, `Sync`, `Sign in`, `Previous`/`Next`).
- Rules stay in `:core`; `SyncState` is reused as-is.
- Conventional Commits in English; **no** `Co-Authored-By` / AI attribution.
- Pre-commit hook runs `spotlessKotlinCheck`; run `./gradlew spotlessApply` before committing or the commit is rejected.
- Full suite must stay green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.

## Review Focus

Failure modes a person will hit that the tasks must pin with tests or verify:

1. **Account overlay returns to the wrong screen** — opening Account from a phase then closing must land back on that phase, not Home (Task 3).
2. **Signed-out chip syncs instead of opening Account** — with no session, tapping the chip must open Account, never fire a 401 sync (Task 1 + Task 4).
3. **The system ActionBar survives** — if the theme is not applied, two bars remain and the slice failed (Task 2, emulator).
4. **A leftover SyncBar/sign-in row remains** — the old `SyncBar`/`HomeSyncControl` must be gone so there is exactly one bar (Task 4, emulator).
5. **No way to sign in from Home** — after removing the inline Home control, the Account screen must still be reachable from Home via the chip and ◯ (Task 4, emulator).

---

## Task 1: AppChrome derivation

**Files:**
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/AppChrome.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/AppChromeTest.kt`

**Interfaces:**
- Consumes: `Screen` (`:app`), `SyncState` (`:app/sync`).
- Produces: `ChipTone`; `ChipAction`; `AppChrome(title, chipLabel, chipTone, chipAction, showHome, showAccount)`; `AppChrome.of(screen: Screen, settlementName: String, syncState: SyncState): AppChrome`.

- [ ] **Step 1: Write the failing test**

`AppChromeTest` (table-driven):
- `homeTitleIsSettlements` — `AppChrome.of(Screen.Home, "New Settlement", SyncState.Idle()).title == "Settlements"`.
- `phaseTitleIsTheSettlementName` — for each of `Settlement`/`Hunt`/`Showdown`, title is the settlement name.
- `accountTitleIsAccount`.
- `chipTonePerSyncState` — `Idle` → `CONNECTED` (label `"Connected"`); `Syncing` → `SYNCING` (`"Syncing…"`); `Error("x")` → `ERROR` (`"Sync error"`); `NotConfigured` → `SIGNED_OUT` (`"Not signed in"`).
- `signedOutChipOpensAccountOthersSync` — `NotConfigured` → `chipAction == ACCOUNT`; `Idle`/`Syncing`/`Error` → `SYNC` (**Review Focus 2**).
- `homeIconOnlyOnPhases` — `showHome` true for `Settlement`/`Hunt`/`Showdown`, false for `Home`/`Account`.
- `accountIconAlwaysVisible` — `showAccount` true for every screen.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.AppChromeTest"`
Expected: FAIL — unresolved `AppChrome`.

- [ ] **Step 3: Implement**

```kotlin
enum class ChipTone { CONNECTED, SYNCING, ERROR, SIGNED_OUT }
enum class ChipAction { SYNC, ACCOUNT }

data class AppChrome(
    val title: String,
    val chipLabel: String,
    val chipTone: ChipTone,
    val chipAction: ChipAction,
    val showHome: Boolean,
    val showAccount: Boolean,
) {
    companion object {
        fun of(screen: Screen, settlementName: String, syncState: SyncState): AppChrome =
            AppChrome(
                title = when (screen) { Screen.Home -> "Settlements"; Screen.Account -> "Account"; else -> settlementName },
                chipLabel = when (syncState) {
                    is SyncState.Idle -> "Connected"
                    SyncState.Syncing -> "Syncing…"
                    is SyncState.Error -> "Sync error"
                    SyncState.NotConfigured -> "Not signed in"
                },
                chipTone = when (syncState) {
                    is SyncState.Idle -> ChipTone.CONNECTED
                    SyncState.Syncing -> ChipTone.SYNCING
                    is SyncState.Error -> ChipTone.ERROR
                    SyncState.NotConfigured -> ChipTone.SIGNED_OUT
                },
                chipAction = if (syncState is SyncState.NotConfigured) ChipAction.ACCOUNT else ChipAction.SYNC,
                showHome = screen == Screen.Settlement || screen == Screen.Hunt || screen == Screen.Showdown,
                showAccount = true,
            )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.AppChromeTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/ui/AppChrome.kt app-android/src/test/kotlin/io/kdmtracker/app/AppChromeTest.kt
git commit -m "feat(app): derive the top-bar chrome from screen and sync state"
```

---

## Task 2: NoActionBar Android theme

**Files:**
- Create: `app-android/src/main/res/values/themes.xml`
- Modify: `app-android/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: nothing.
- Produces: a `Theme.KdmTracker` style with no action bar; the manifest applies it.

- [ ] **Step 1: Add the theme**

Create `app-android/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- NoActionBar: the app draws its own Compose AppTopBar; the platform ActionBar (android:label)
         must not render a second chrome. Framework parent because the module has no Material XML theme. -->
    <style name="Theme.KdmTracker" parent="@android:style/Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 2: Apply it in the manifest**

In `AndroidManifest.xml`, set `android:theme="@style/Theme.KdmTracker"` on `<application>`.

- [ ] **Step 3: Build to verify**

Run: `./gradlew :app-android:assembleDebug`
Expected: BUILD SUCCESSFUL (the theme resource resolves).

- [ ] **Step 4: Commit**

```bash
git add app-android/src/main/res/values/themes.xml app-android/src/main/AndroidManifest.xml
git commit -m "feat(app): drop the platform action bar with a NoActionBar theme"
```

---

## Task 3: Account navigation and sign-out

**Files:**
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt` (add `Screen.Account`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/sync/SyncCoordinator.kt` (add `markSignedOut()`)
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt`
- Test: `app-android/src/test/kotlin/io/kdmtracker/app/AccountNavigationTest.kt`

**Interfaces:**
- Consumes: `Screen`, `SyncState`, `AppComponents`, `SyncCoordinator`.
- Produces: `Screen.Account`; `SyncCoordinator.markSignedOut()`; `AppComponents.signOut: (() -> Unit)?`; `LanternYearViewModel.{goAccount, closeAccount, signOut}`.

- [ ] **Step 1: Write the failing test**

`AccountNavigationTest` (mirror `LanternYearViewModelTest`: `buildComponents = { AppComponents(LanternYearController(SettlementRepository(InMemoryCampaignStore()))) }`, `Dispatchers.Unconfined`):
- `openingAccountFromHomeAndClosingReturnsToHome` — `viewModel.goAccount()` → `state.screen == Screen.Account`; `closeAccount()` → `Screen.Home` (**Review Focus 1**).
- `closingAccountReturnsToThePhaseItWasOpenedFrom` — `select(campaignId)` (take `homeCards.value.first().campaignId`) → phase screen; `goAccount()` → Account; `closeAccount()` → the same phase screen.
- `signOutMarksTheSessionSignedOut` — `viewModel.signOut()` → `viewModel.syncState.value == SyncState.NotConfigured`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.AccountNavigationTest"`
Expected: FAIL — unresolved `Screen.Account` / `goAccount`.

- [ ] **Step 3: Implement**

- `UiState.kt`: add `data object Account : Screen`.
- `SyncCoordinator`: add `fun markSignedOut() { _state.value = SyncState.NotConfigured }`.
- `AppComponents`: add `val signOut: (() -> Unit)? = null`.
- `LanternYearViewModel`:
  - `@Volatile private var accountOpen = false`.
  - `forDisplay()` becomes `when { accountOpen -> copy(screen = Screen.Account); onHome -> copy(screen = Screen.Home); else -> this }`.
  - `fun goAccount() { accountOpen = true; _state.value = _state.value?.forDisplay() }`, `fun closeAccount() { accountOpen = false; _state.value = _state.value?.forDisplay() }` (mirror `goHome`).
  - `fun signOut() { components.get()?.signOut?.invoke(); _syncState.value = SyncState.NotConfigured }`.
  - In the factory, wire `signOut = { tokenStore.write(null); syncCoordinator.markSignedOut() }` into the `AppComponents(...)`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app-android:testDebugUnitTest --tests "io.kdmtracker.app.AccountNavigationTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app-android/src/main/kotlin/io/kdmtracker/app/UiState.kt app-android/src/main/kotlin/io/kdmtracker/app/sync/SyncCoordinator.kt app-android/src/main/kotlin/io/kdmtracker/app/LanternYearViewModel.kt app-android/src/test/kotlin/io/kdmtracker/app/AccountNavigationTest.kt
git commit -m "feat(app): add the account screen navigation and sign-out"
```

---

## Task 4: AppTopBar, Account screen and shell restructure

**Files:**
- Modify: `app-android/build.gradle.kts` (add the Material icons dependency)
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/AppTopBar.kt`
- Create: `app-android/src/main/kotlin/io/kdmtracker/app/ui/AccountScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/HomeScreen.kt`
- Modify: `app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt`

**Interfaces:**
- Consumes: `AppChrome`, `ChipAction`, `Screen.Account`, ViewModel navigation (Task 3).
- Produces: `AppTopBar(...)`; `AccountScreen(...)`; the single-bar shell.

> This task adds no unit test: the project deliberately has no Compose UI test harness, and the chrome derivation and navigation behind every control are already pinned by `AppChromeTest` (Task 1) and `AccountNavigationTest` (Task 3). Verification is compilation, `spotlessCheck`, the full suite, and the manual emulator pass.

- [ ] **Step 1: Add the Material icons dependency**

The project has no Material icons artifact today. In `app-android/build.gradle.kts` `dependencies`, add:

```kotlin
implementation(compose.materialIconsExtended)
```

(Same Compose plugin, so the version aligns with `compose.material3`.)

- [ ] **Step 2: Implement `AppTopBar`**

```kotlin
@Composable
fun AppTopBar(
    chrome: AppChrome,
    onChipAction: (ChipAction) -> Unit,
    onHome: () -> Unit,
    onAccount: () -> Unit,
)
```
A Material3 `TopAppBar` with `title = { Text(chrome.title) }` and `actions`:
- the status chip: a `Surface`/pill showing a colored dot + `chrome.chipLabel`, `onClick = { onChipAction(chrome.chipAction) }` (colour from `chrome.chipTone`).
- an `IconButton` with `Icons.Default.Home`, shown when `chrome.showHome`.
- an `IconButton` with `Icons.Default.AccountCircle`, shown when `chrome.showAccount`.

- [ ] **Step 3: Implement `AccountScreen`**

```kotlin
@Composable
fun AccountScreen(
    syncState: SyncState,
    onSignIn: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit,
    onClose: () -> Unit,
)
```
Shows `syncState.describe()`; when `syncState is SyncState.NotConfigured` renders the sign-in fields (Username, Password, `Sign in`); otherwise renders `Sync now` and `Sign out`, plus a `Close` control. Owning its own username/password `remember` state (moved out of `KdmTrackerApp`).

- [ ] **Step 4: Restructure `KdmTrackerApp`**

- `Scaffold(topBar = { AppTopBar(chrome = AppChrome.of(current.screen, current.settlementName, syncState), onChipAction = { if (it == ChipAction.ACCOUNT) viewModel.goAccount() else viewModel.syncNow() }, onHome = viewModel::goHome, onAccount = viewModel::goAccount) })`.
- `when (current.screen)` dispatches `Home` → `HomeScreen` (no sync params), `Settlement`/`Hunt`/`Showdown` → the phase screens **without** the `Cockpit` wrapper, `Account` → `AccountScreen(...)` with a `BackHandler { viewModel.closeAccount() }`.
- Remove `Cockpit`, `SyncBar`, `SignInForm`, and the top-level `username`/`password` `remember`s.

- [ ] **Step 5: Trim the removed controls and header**

- `HomeScreen`: drop `HomeSyncControl` and the `syncState`/`onSync`/`onSignIn`/`username`/`password`/`onUsername`/`onPassword` parameters (**Review Focus 4**); keep the cards + New settlement form.
- `SettlementScreen`/`SettlementHeader`: drop the duplicated settlement name / `"Settlement"` line; keep a single stats line.

- [ ] **Step 6: Run the full suite**

Run: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Manual emulator pass**

Run: `./gradlew :app-android:installDebug`, then check: exactly **one** bar row on Home and on each phase; the chip shows `Connected` / `Not signed in` and its tap routes correctly; `Account` opens and back returns to the originating screen; the phase body starts directly under the step bar (**Review Focus 3, 4, 5**).

- [ ] **Step 8: Commit**

```bash
git add app-android/build.gradle.kts app-android/src/main/kotlin/io/kdmtracker/app/ui/AppTopBar.kt app-android/src/main/kotlin/io/kdmtracker/app/ui/AccountScreen.kt app-android/src/main/kotlin/io/kdmtracker/app/KdmTrackerApp.kt app-android/src/main/kotlin/io/kdmtracker/app/ui/HomeScreen.kt app-android/src/main/kotlin/io/kdmtracker/app/ui/SettlementScreen.kt
git commit -m "feat(app): consolidate the chrome into one top bar with an account screen"
```

---

## Self-Review

- **Spec coverage:** AppTopBar + chip + Home/Account (Tasks 1, 4) · Account screen + sign-in/out (Tasks 3, 4) · NoActionBar theme (Task 2) · removals of Cockpit/SyncBar/HomeSyncControl + header trim (Task 4) · `AppChrome` seam (Task 1).
- **Review Focus tests:** (1) `closingAccountReturnsToThePhaseItWasOpenedFrom`; (2) `signedOutChipOpensAccountOthersSync`; (3) Task 2 + Task 4 emulator; (4) Task 4 Step 5 + emulator; (5) Task 4 emulator.
- **Type consistency:** `AppChrome.of(screen, settlementName, syncState)`, `ChipTone`, `ChipAction`, `Screen.Account`, `goAccount`/`closeAccount`/`signOut` are used with the same shapes across tasks.

# Top Bar Chrome + Account Screen — Design

**Status:** Draft (awaiting review)
**Slice:** Replace the two stacked top chromes — the platform **ActionBar** (`KDM Tracker`) and the in-content Compose **SyncBar** — with a single global **`AppTopBar`** that carries the title, a tappable **sync-status chip**, **Home**, and **Account**; move sign-in to a dedicated **Account screen**.
**Depends on:** the shipped Settlement cockpit (PR #14) and the settlement bookkeeping steps (PR #15).

## Goal

Every phase screen currently spends ~140 dp of vertical chrome before its content: the Android
ActionBar (from the default theme's `android:label`) plus a two-row Compose `SyncBar`
(Home · status · Sync, and Username/Password/Sign in). On a 2400 px phone that is real lost vision.
This slice collapses all of it into one top bar.

Success is:
1. One `AppTopBar` across Home and the three phase screens; no ActionBar, no `SyncBar`.
2. A **status chip** that both shows sync state and acts (tap = sync; when signed out, opens Account).
3. Sign-in and sign-out live on a dedicated **Account** screen, reachable from the bar.
4. The phase body starts directly under the step bar; the settlement name is not duplicated.
5. Rules stay in `:core`; the bar and screens render state and report intent only.

## Design (validated with the user, browser mockups)

### `AppTopBar` (global)
- **Title**, screen-dependent: `"Settlements"` on Home; the settlement name on the three phases.
- **Status chip**, derived from `SyncState`:

  | State | Chip | Tap |
  |---|---|---|
  | session ok, idle | `● Conectado` (green) | `syncNow()` |
  | syncing | `Sync…` (blue, spinner dot) | no-op |
  | last sync failed | `Sin conexión` (amber) | `syncNow()` |
  | no session | `Sin sesión ▾` (grey) | open **Account** |

- **Home ⌂** — visible only on the phase screens (Home is already the Home screen); returns to the
  list (same as today's `goHome`, keeping the system-back parity).
- **Account ◯** — opens the Account screen from anywhere.

### Account screen (new destination)
- **Signed out:** the status line plus the sign-in form (Username, Password, **Entrar**).
- **Signed in:** the status line plus **Sync now** and **Desconectar** (sign out).
- Reached from the chip when `Sin sesión`, or from ◯. Back (and system back) returns to the screen
  it was opened from (the phase or Home).

### Phase body
- With the settlement name in the bar, drop the duplicated `Settlement` label / name line from
  `SettlementHeader`; keep a single compact stats line
  (`Lantern Year N · Population P · Endeavors E · Survival Limit S`) and the step bar directly
  beneath it.

### Removals
- `Cockpit`'s `SyncBar` and `SignInForm` (`KdmTrackerApp.kt`).
- Home's `HomeSyncControl` and the sign-in fields threaded through `KdmTrackerApp → HomeScreen`
  (username/password/sign-in callbacks).
- The platform ActionBar, via a NoActionBar theme.

## Scope

### In
- `:app-android`: `AppTopBar`, the `Screen.Account` destination + Account screen, the theme change,
  the shell restructure, and the settlement-header trim.
- A pure, testable `AppChrome` seam (title + chip label/color + visible actions) for the bar.

### Out (explicitly deferred)
- Any redesign of the Home content, the step bar, or the individual step bodies.
- Biometric/remember-me, multi-account, or a full auth-token UI.
- Server changes; the sync protocol is unchanged.
- Splitting the `SyncState` model — it is reused as-is.

## Architecture

### Theme (Android)
- Add `app-android/src/main/res/values/themes.xml` with a `NoActionBar` theme (framework
  `android:Theme.Material.*.NoActionBar`, or `Theme.Material3.DayNight.NoActionBar` if the
  Material Components XML theme is already available — the plan pins the exact parent against the
  module's dependencies). Set `android:theme` on `<application>` (or the activity) in
  `AndroidManifest.xml`.

### `:app-android`
- `AppTopBar(chrome: AppChrome, onSync, onHome, onAccount)` — a stateless Compose `TopAppBar`.
- `AppChrome` — a pure data class + `AppChrome.of(screen, settlementName, syncState): AppChrome`
  producing `title`, `chipLabel`, `chipTone` (`CONNECTED` / `SYNCING` / `ERROR` / `SIGNED_OUT`),
  `showHome`, `showAccount`. This is the testable seam (no Compose UI tests in this repo).
- `Screen.Account` added to the sealed `Screen`; the ViewModel remembers the screen to return to.
- `KdmTrackerApp`: `Scaffold(topBar = { AppTopBar(...) })`, `when (screen)` dispatches Home / the
  three phases / Account; the `Cockpit` wrapper is removed.
- `LanternYearViewModel`: `goAccount()` / `closeAccount()` (return to the remembered screen) and a
  `syncNow()` reuse; `signIn(...)` reused by the Account screen.
- `HomeScreen`: drop `HomeSyncControl` and the username/password/onSignIn parameters.
- `SettlementScreen`/`SettlementHeader`: drop the duplicated name/label line.

### `:core`
- None. `SyncState` (already in `:app/sync`) is the input to `AppChrome.of`.

## Data flow

`SyncState` (ViewModel) + `UiState.screen`/`settlementName` → `AppChrome.of` → `AppTopBar`. Bar
intents: chip tap → `syncNow()` or `goAccount()`; `Home` → `goHome()`; `Account` → `goAccount()`.
The Account screen calls the existing `signIn(...)`, `syncNow()`, and a new sign-out on the token
store path.

## Testing

- **`:app-android`** (no Compose UI tests): `AppChrome.of` table-driven — title per screen,
  chip label/tone per `SyncState`, `showHome` only on phases; the ViewModel navigation —
  `goAccount`/`closeAccount` returns to the originating screen, and a signed-out chip routes to
  account while a signed-in chip routes to sync.
- Full suite green: `./gradlew :core:jvmTest :data:jvmTest :app-android:testDebugUnitTest :server:test spotlessCheck`.
- Manual emulator pass: bar shows one row on Home and each phase; chip states + tap; Account sign-in
  and back; the phase body starts under the step bar.

## Risks

- **Theme change** — the NoActionBar parent must exist in the module's dependency set; a wrong
  parent breaks the build. Pin it against `app-android/build.gradle.kts`.
- **Status chip semantics** — one control with two meanings (sync vs account) can confuse; the label
  (`Sync…`/`Sin sesión`) and tone must make the action obvious. Pin with the `AppChrome` tests.
- **Navigation return** — Account is an overlay destination; if the "return to" screen is not
  remembered, back could send the user to the wrong place. Pin with `goAccount`/`closeAccount` tests.
- **Space win is the point** — if the bar or the trimmed header still wraps to two lines, the slice
  failed. Verify on the emulator (one bar row; single stats line).

## References

- Mockups (persisted): `.superpowers/brainstorm/19846-1791475362/content/{top-bar-options,top-bar-c-full}.html`
- Current chrome: `app-android/.../KdmTrackerApp.kt` (`Cockpit`, `SyncBar`, `SignInForm`),
  `app-android/.../ui/HomeScreen.kt` (`HomeSyncControl`), `app-android/.../sync/*` (`SyncState`)
- Manifest: `app-android/src/main/AndroidManifest.xml` (no `res/`, default ActionBar)

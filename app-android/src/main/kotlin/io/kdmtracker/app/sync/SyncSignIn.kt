package io.kdmtracker.app.sync

/**
 * Wires the sign-in flow: it authenticates against [authApi] and persists the resulting bearer
 * token through [tokens] so subsequent [SyncCoordinator] passes are authenticated.
 *
 * Kept separate from the transport and the coordinator so the "login stores a token" behaviour is
 * unit-testable with fakes and no Android or network machinery.
 */
class SyncSignIn(
    private val authApi: SyncAuthApi,
    private val tokens: TokenStore,
) {
    /** Logs in with [username]/[password] and stores the returned token; rethrows on failure. */
    fun signIn(
        username: String,
        password: String,
    ) {
        tokens.write(authApi.login(username, password))
    }
}

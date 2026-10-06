package io.kdmtracker.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Fails fast at startup when `sync.jwt.secret` is still the committed development default and no
 * `dev`/`test` profile is active. Shipping the default in a real deployment would let anyone mint a
 * valid token; refusing to boot is louder and safer than a warning that scrolls past. Local work can
 * opt in with `SPRING_PROFILES_ACTIVE=dev` (or a real `SYNC_JWT_SECRET`); the test suite uses its own
 * non-default secret, so it is unaffected.
 */
@Component
class JwtSecretGuard(
    @Value("\${sync.jwt.secret}") secret: String,
    environment: Environment,
) {
    init {
        val relaxed =
            environment.activeProfiles.any {
                it.equals("dev", ignoreCase = true) || it.equals("test", ignoreCase = true)
            }
        check(secret != DEFAULT_SECRET || relaxed) {
            "Refusing to start: sync.jwt.secret is the committed insecure default. " +
                "Set SYNC_JWT_SECRET to a strong secret, or run with SPRING_PROFILES_ACTIVE=dev for local work."
        }
    }

    companion object {
        /** The dev default in `application.yaml`; never valid outside dev/test. */
        const val DEFAULT_SECRET = "dev-only-insecure-secret-change-me"
    }
}

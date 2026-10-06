package io.kdmtracker.auth

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class JwtSecretGuardTest {
    private fun environment(vararg profiles: String) = MockEnvironment().apply { setActiveProfiles(*profiles) }

    @Test
    fun blankSecretIsRejectedOutsideDevAndTest() {
        assertThrows(IllegalStateException::class.java) {
            JwtSecretGuard("", environment())
        }
    }

    @Test
    fun shortSecretIsRejectedOutsideDevAndTest() {
        assertThrows(IllegalStateException::class.java) {
            JwtSecretGuard("0123456789", environment())
        }
    }

    @Test
    fun committedDefaultIsRejectedOutsideDevAndTest() {
        assertThrows(IllegalStateException::class.java) {
            JwtSecretGuard(JwtSecretGuard.DEFAULT_SECRET, environment())
        }
    }

    @Test
    fun strongNonDefaultSecretIsAccepted() {
        assertDoesNotThrow {
            JwtSecretGuard("0123456789abcdef0123456789abcdef", environment())
        }
    }

    @Test
    fun committedDefaultIsAcceptedUnderTestProfile() {
        assertDoesNotThrow {
            JwtSecretGuard(JwtSecretGuard.DEFAULT_SECRET, environment("test"))
        }
    }

    @Test
    fun blankSecretIsAcceptedUnderDevProfile() {
        assertDoesNotThrow {
            JwtSecretGuard("", environment("dev"))
        }
    }
}

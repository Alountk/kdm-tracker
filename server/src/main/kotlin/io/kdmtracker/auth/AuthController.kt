package io.kdmtracker.auth

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

data class CredentialsRequest(
    val username: String = "",
    val password: String = "",
)

data class TokenResponse(
    val token: String,
)

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {
    @PostMapping("/register")
    fun register(
        @RequestBody request: CredentialsRequest,
    ): TokenResponse {
        if (request.username in ReservedUsernames.ALL) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "username is reserved")
        }
        if (users.findByUsername(request.username) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "username already taken")
        }
        val passwordHash =
            requireNotNull(passwordEncoder.encode(request.password)) { "password encoder returned no hash" }
        val user =
            try {
                users.saveAndFlush(UserEntity(username = request.username, passwordHash = passwordHash))
            } catch (e: DataIntegrityViolationException) {
                // Lost a race with a concurrent registration for the same username.
                throw ResponseStatusException(HttpStatus.CONFLICT, "username already taken", e)
            }
        return TokenResponse(jwtService.issueToken(user.id!!))
    }

    @PostMapping("/login")
    fun login(
        @RequestBody request: CredentialsRequest,
    ): TokenResponse {
        val user = users.findByUsername(request.username)
        if (user == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials")
        }
        return TokenResponse(jwtService.issueToken(user.id!!))
    }
}

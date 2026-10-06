package io.kdmtracker.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Issues and verifies HS256 bearer tokens. The token subject is the user id; the signing secret
 * comes from `sync.jwt.secret` (`SYNC_JWT_SECRET`) with a dev-only default, and tokens expire after
 * `sync.jwt.expiry-seconds`.
 */
@Service
class JwtService(
    @Value("\${sync.jwt.secret}") secret: String,
    @Value("\${sync.jwt.expiry-seconds}") private val expirySeconds: Long,
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(secret)
    private val verifier = JWT.require(algorithm).build()

    fun issueToken(userId: Long): String =
        JWT
            .create()
            .withSubject(userId.toString())
            .withIssuedAt(Instant.now())
            .withExpiresAt(Instant.now().plusSeconds(expirySeconds))
            .sign(algorithm)

    fun verifyToken(token: String): Long? =
        try {
            verifier.verify(token).subject?.toLongOrNull()
        } catch (e: JWTVerificationException) {
            null
        }
}

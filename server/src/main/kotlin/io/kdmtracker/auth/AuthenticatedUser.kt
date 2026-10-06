package io.kdmtracker.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/** The authenticated user id stashed by [BearerTokenFilter], or `null` when it did not run. */
fun HttpServletRequest.authenticatedUserIdOrNull(): Long? =
    getAttribute(BearerTokenFilter.AUTHENTICATED_USER_ID) as? Long

/**
 * The authenticated user id, or a `401` when [BearerTokenFilter] did not populate it. Controllers use
 * this instead of an unchecked `as Long` cast so a missing guard fails as `401` rather than an NPE
 * mapped to `500`.
 */
fun HttpServletRequest.requireAuthenticatedUserId(): Long =
    authenticatedUserIdOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "no authenticated user")

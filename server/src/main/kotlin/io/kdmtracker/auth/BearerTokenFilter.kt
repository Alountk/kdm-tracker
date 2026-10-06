package io.kdmtracker.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Guards the `/api/v1/` API (except `/api/v1/auth/`) behind a valid bearer token. On success the
 * authenticated user id is exposed to controllers as the [AUTHENTICATED_USER_ID] request attribute;
 * a missing or invalid token yields 401. The legacy Thymeleaf routes are left untouched (Ruling S).
 */
@Component
class BearerTokenFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !path.startsWith(API_PREFIX) || path.startsWith(AUTH_PREFIX)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val userId = bearerToken(request)?.let { jwtService.verifyToken(it) }
        if (userId == null) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            return
        }
        request.setAttribute(AUTHENTICATED_USER_ID, userId)
        filterChain.doFilter(request, response)
    }

    private fun bearerToken(request: HttpServletRequest): String? =
        request
            .getHeader("Authorization")
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.removePrefix(BEARER_PREFIX)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    companion object {
        const val AUTHENTICATED_USER_ID = "authenticatedUserId"
        private const val API_PREFIX = "/api/v1/"
        private const val AUTH_PREFIX = "/api/v1/auth/"
        private const val BEARER_PREFIX = "Bearer "
    }
}

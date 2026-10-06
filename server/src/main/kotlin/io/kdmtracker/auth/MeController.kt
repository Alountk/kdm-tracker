package io.kdmtracker.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Minimal protected endpoint: echoes the authenticated user id set by [BearerTokenFilter]. It exists
 * so the bearer filter has an endpoint to guard and tests have a stable assertion target.
 */
@RestController
class MeController {
    @GetMapping("/api/v1/me")
    fun me(request: HttpServletRequest): Map<String, Long> = mapOf("userId" to authenticatedUserId(request))

    private fun authenticatedUserId(request: HttpServletRequest): Long =
        request.getAttribute(BearerTokenFilter.AUTHENTICATED_USER_ID) as Long
}

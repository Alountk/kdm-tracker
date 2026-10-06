package io.kdmtracker.auth

import guardprobe.GuardProbeController
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Import(GuardProbeController::class)
class BearerFilterTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var users: UserRepository

    private val filter = BearerTokenFilter(JwtService("unit-test-secret", 60))

    @Test
    fun apiPathsStayGuardedWhenTheAppRunsUnderAContextPath() {
        // requestURI carries the context path, servletPath does not; matching on servletPath keeps
        // the guard effective under a context-path deployment.
        val request =
            MockHttpServletRequest().apply {
                requestURI = "/ctx/api/v1/me"
                servletPath = "/api/v1/me"
            }

        assertFalse(filter.shouldNotFilter(request))
    }

    @Test
    fun authAndLegacyPathsAreNotGuarded() {
        assertTrue(filter.shouldNotFilter(pathOf("/api/v1/auth/login")))
        assertTrue(filter.shouldNotFilter(pathOf("/health")))
        assertTrue(filter.shouldNotFilter(pathOf("/")))
    }

    @Test
    fun newlyAddedApiRouteWithoutTokenIsUnauthorized() {
        mockMvc
            .perform(get("/api/v1/guard-probe"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun newlyAddedApiRouteWithTokenIsReachable() {
        val user = users.saveAndFlush(UserEntity(username = "guard-probe-ada", passwordHash = "test-hash"))
        val token = jwtService.issueToken(user.id!!)

        mockMvc
            .perform(get("/api/v1/guard-probe").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ok").value("true"))
    }

    private fun pathOf(path: String): MockHttpServletRequest =
        MockHttpServletRequest().apply {
            requestURI = path
            servletPath = path
        }
}

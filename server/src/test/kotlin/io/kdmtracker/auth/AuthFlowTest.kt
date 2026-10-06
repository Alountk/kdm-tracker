package io.kdmtracker.auth

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var users: UserRepository

    private fun register(
        username: String,
        password: String = "correct-horse",
    ): String {
        val body =
            mockMvc
                .perform(
                    post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"username":"$username","password":"$password"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.token").isNotEmpty)
                .andReturn()
                .response
                .contentAsString
        return JsonPath.read(body, "$.token")
    }

    @Test
    fun registerReturnsToken() {
        val token = register("register-ada")

        assert(token.isNotBlank()) { "registration must return a non-blank token" }
    }

    @Test
    fun loginReturnsToken() {
        register("login-ada", password = "let-me-in")

        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"username":"login-ada","password":"let-me-in"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
    }

    @Test
    fun duplicateRegistrationIsConflict() {
        register("dup-ada")

        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"username":"dup-ada","password":"another"}"""),
            ).andExpect(status().isConflict)
    }

    @Test
    fun loginWithWrongPasswordIsUnauthorized() {
        register("wrongpass-ada", password = "right-one")

        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"username":"wrongpass-ada","password":"wrong-one"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun meWithoutTokenIsUnauthorized() {
        mockMvc
            .perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun meWithInvalidTokenIsUnauthorized() {
        mockMvc
            .perform(get("/api/v1/me").header("Authorization", "Bearer not-a-real-token"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun meWithValidTokenReturnsAuthenticatedUserId() {
        val token = register("me-ada")
        val userId = users.findByUsername("me-ada")!!.id!!

        mockMvc
            .perform(get("/api/v1/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId))
    }

    @Test
    fun legacyHealthRouteStaysOpenWithoutToken() {
        mockMvc
            .perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ok"))
    }
}

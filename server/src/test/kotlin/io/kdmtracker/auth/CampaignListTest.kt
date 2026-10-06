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
class CampaignListTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var users: UserRepository

    @Autowired
    private lateinit var jwtService: JwtService

    private fun seedUser(username: String): UserEntity =
        users.saveAndFlush(UserEntity(username = username, passwordHash = "test-hash"))

    private fun tokenFor(user: UserEntity): String = jwtService.issueToken(user.id!!)

    private fun createCampaign(
        token: String,
        name: String,
    ): CampaignResponse {
        val body =
            mockMvc
                .perform(
                    post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"$name"}""")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        return CampaignResponse(
            id = JsonPath.read(body, "$.id"),
            name = JsonPath.read(body, "$.name"),
            inviteCode = JsonPath.read(body, "$.inviteCode"),
            role = JsonPath.read(body, "$.role"),
        )
    }

    private fun join(
        token: String,
        inviteCode: String,
    ) {
        mockMvc
            .perform(
                post("/api/v1/campaigns/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"inviteCode":"$inviteCode"}""")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
    }

    @Test
    fun ownerAloneSeesTheirCampaignWithOwnerRoleAndNoSharedMembers() {
        val owner = seedUser("list-owner-ada")
        val token = tokenFor(owner)
        createCampaign(token, "Solo Cradle")

        mockMvc
            .perform(get("/api/v1/campaigns").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Solo Cradle"))
            .andExpect(jsonPath("$[0].role").value("OWNER"))
            .andExpect(jsonPath("$[0].ownerUsername").value("list-owner-ada"))
            .andExpect(jsonPath("$[0].sharedWith").isEmpty)
    }

    @Test
    fun joinerSeesEditorRoleAndOwnerExcludedFromSharedWith() {
        val owner = seedUser("list-shared-owner")
        val joiner = seedUser("list-shared-joiner")
        val campaign = createCampaign(tokenFor(owner), "Shared Cradle")
        join(tokenFor(joiner), campaign.inviteCode)

        mockMvc
            .perform(get("/api/v1/campaigns").header("Authorization", "Bearer ${tokenFor(joiner)}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(campaign.id))
            .andExpect(jsonPath("$[0].role").value("EDITOR"))
            .andExpect(jsonPath("$[0].ownerUsername").value("list-shared-owner"))
            .andExpect(jsonPath("$[0].sharedWith.length()").value(1))
            .andExpect(jsonPath("$[0].sharedWith[0]").value("list-shared-owner"))
    }

    @Test
    fun ownerSeesTheJoinerInSharedWith() {
        val owner = seedUser("list-sharedback-owner")
        val joiner = seedUser("list-sharedback-joiner")
        val campaign = createCampaign(tokenFor(owner), "Shared Back Cradle")
        join(tokenFor(joiner), campaign.inviteCode)

        mockMvc
            .perform(get("/api/v1/campaigns").header("Authorization", "Bearer ${tokenFor(owner)}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].ownerUsername").value("list-sharedback-owner"))
            .andExpect(jsonPath("$[0].sharedWith.length()").value(1))
            .andExpect(jsonPath("$[0].sharedWith[0]").value("list-sharedback-joiner"))
    }

    @Test
    fun sharedWithListsOtherMembersOrderedAndExcludesCaller() {
        val owner = seedUser("list-order-owner")
        val campaign = createCampaign(tokenFor(owner), "Order Cradle")
        join(tokenFor(seedUser("list-order-zoe")), campaign.inviteCode)
        join(tokenFor(seedUser("list-order-abe")), campaign.inviteCode)

        mockMvc
            .perform(get("/api/v1/campaigns").header("Authorization", "Bearer ${tokenFor(owner)}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].sharedWith.length()").value(2))
            .andExpect(jsonPath("$[0].sharedWith[0]").value("list-order-abe"))
            .andExpect(jsonPath("$[0].sharedWith[1]").value("list-order-zoe"))
    }

    @Test
    fun campaignsAreOrderedByNameSoTheResponseIsStable() {
        val owner = seedUser("list-sort-ada")
        val token = tokenFor(owner)
        createCampaign(token, "Beta Cradle")
        createCampaign(token, "Alpha Cradle")

        mockMvc
            .perform(get("/api/v1/campaigns").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Alpha Cradle"))
            .andExpect(jsonPath("$[1].name").value("Beta Cradle"))
    }

    @Test
    fun userWithNoCampaignsSeesAnEmptyList() {
        val loner = seedUser("list-lonely-ada")

        mockMvc
            .perform(get("/api/v1/campaigns").header("Authorization", "Bearer ${tokenFor(loner)}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun listWithoutTokenIsUnauthorized() {
        mockMvc.perform(get("/api/v1/campaigns")).andExpect(status().isUnauthorized)
    }
}

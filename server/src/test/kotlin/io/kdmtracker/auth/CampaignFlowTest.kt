package io.kdmtracker.auth

import com.jayway.jsonpath.JsonPath
import io.kdmtracker.core.domain.Ulid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class CampaignFlowTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var users: UserRepository

    @Autowired
    private lateinit var members: CampaignMemberRepository

    @Autowired
    private lateinit var jwtService: JwtService

    private fun seedUser(username: String): UserEntity =
        users.saveAndFlush(UserEntity(username = username, passwordHash = "test-hash"))

    private fun createCampaign(
        token: String,
        name: String = "First Cradle",
    ): CampaignResponse {
        val body =
            mockMvc
                .perform(
                    post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"$name"}""")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.role").value("OWNER"))
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

    @Test
    fun createWithoutTokenIsUnauthorized() {
        mockMvc
            .perform(
                post("/api/v1/campaigns")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Sneaky"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun creatorBecomesOwnerAndTheHubIsImmediatelyUsable() {
        val user = seedUser("campaign-owner-ada")
        val token = jwtService.issueToken(user.id!!)

        val campaign = createCampaign(token)

        // The generated id is a real ULID and the creator holds an OWNER membership.
        assertEquals(26, campaign.id.length)
        assertEquals(campaign.id, Ulid.parse(campaign.id).value)
        assertTrue(campaign.inviteCode.isNotBlank())
        assertEquals("OWNER", members.findByCampaignIdAndUserId(campaign.id, user.id!!)?.role)

        // With the membership granted by create, push no longer 403s — the hub works end to end.
        val pushBody =
            """{"records":[{"campaignId":"${campaign.id}","entityType":"settlement","entityId":"settlement-1",""" +
                """"op":"UPSERT","baseRev":null,"payload":"{}","updatedAtMillis":1,"updatedAtCounter":1,"rev":0}]}"""
        mockMvc
            .perform(
                post("/api/v1/campaigns/${campaign.id}/changes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(pushBody)
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.latestRev").value(1))
    }

    @Test
    fun joiningByInviteCodeGrantsEditorAccess() {
        val owner = seedUser("campaign-join-owner")
        val joiner = seedUser("campaign-join-ada")
        val campaign = createCampaign(jwtService.issueToken(owner.id!!), "Joinable Cradle")
        val joinerToken = jwtService.issueToken(joiner.id!!)

        mockMvc
            .perform(
                post("/api/v1/campaigns/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"inviteCode":"${campaign.inviteCode}"}""")
                    .header("Authorization", "Bearer $joinerToken"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(campaign.id))
            .andExpect(jsonPath("$.role").value("EDITOR"))

        assertEquals("EDITOR", members.findByCampaignIdAndUserId(campaign.id, joiner.id!!)?.role)
    }

    @Test
    fun joiningUnknownInviteCodeIsNotFound() {
        val user = seedUser("campaign-nojoin-ada")
        val token = jwtService.issueToken(user.id!!)

        mockMvc
            .perform(
                post("/api/v1/campaigns/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"inviteCode":"NO-SUCH-CODE"}""")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun blankNameIsBadRequest() {
        val user = seedUser("campaign-blank-ada")
        val token = jwtService.issueToken(user.id!!)

        mockMvc
            .perform(
                post("/api/v1/campaigns")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"   "}""")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isBadRequest)
    }
}

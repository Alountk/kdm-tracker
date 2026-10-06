package io.kdmtracker.sync

import com.jayway.jsonpath.JsonPath
import io.kdmtracker.auth.CampaignEntity
import io.kdmtracker.auth.CampaignMemberEntity
import io.kdmtracker.auth.CampaignMemberRepository
import io.kdmtracker.auth.CampaignRepository
import io.kdmtracker.auth.JwtService
import io.kdmtracker.auth.UserEntity
import io.kdmtracker.auth.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
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
class PushChangesTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var users: UserRepository

    @Autowired
    private lateinit var campaigns: CampaignRepository

    @Autowired
    private lateinit var members: CampaignMemberRepository

    @Autowired
    private lateinit var changeRecords: ChangeRecordRepository

    @Autowired
    private lateinit var jwtService: JwtService

    private fun seedUser(username: String): UserEntity =
        users.saveAndFlush(UserEntity(username = username, passwordHash = "test-hash"))

    private fun seedCampaign(
        id: String,
        inviteCode: String,
    ): CampaignEntity = campaigns.saveAndFlush(CampaignEntity(id = id, name = "First Cradle", inviteCode = inviteCode))

    private fun seedMembership(
        campaignId: String,
        userId: Long,
    ) = members.saveAndFlush(CampaignMemberEntity(campaignId = campaignId, userId = userId, role = "OWNER"))

    private fun changesUrl(campaignId: String): String = "/api/v1/campaigns/$campaignId/changes"

    private fun jsonEscape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun recordJson(
        campaignId: String,
        entityId: String,
        payload: String,
        rev: Long = 0,
        updatedAtCounter: Int = 1,
    ): String =
        """
        {
          "campaignId": "$campaignId",
          "entityType": "settlement",
          "entityId": "$entityId",
          "op": "UPSERT",
          "baseRev": null,
          "payload": "${jsonEscape(payload)}",
          "updatedAtMillis": 1700000000000,
          "updatedAtCounter": $updatedAtCounter,
          "rev": $rev
        }
        """.trimIndent()

    private fun changeSetJson(vararg records: String): String = """{"records":[${records.joinToString(",")}]}"""

    private fun recordsFor(campaignId: String): List<ChangeRecordEntity> =
        changeRecords.findByCampaignIdAndRevGreaterThanOrderByRevAsc(campaignId, 0)

    @Test
    fun pushAssignsSequentialRevsAndDeltaPullSeesThem() {
        val user = seedUser("push-seq-ada")
        val campaign = seedCampaign(CAMPAIGN_SEQ, "INVITE-PUSH-SEQ")
        seedMembership(campaign.id, user.id!!)
        val token = jwtService.issueToken(user.id!!)
        val body =
            changeSetJson(
                recordJson(campaign.id, "settlement-1", """{"name":"A"}""", rev = 999),
                recordJson(campaign.id, "settlement-2", """{"name":"B"}"""),
            )

        mockMvc
            .perform(
                post(changesUrl(campaign.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.latestRev").value(2))

        val delta =
            mockMvc
                .perform(
                    get(changesUrl(campaign.id))
                        .param("since", "0")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.latestRev").value(2))
                .andExpect(jsonPath("$.records.length()").value(2))
                .andReturn()
                .response
                .contentAsString

        assertEquals(listOf(1, 2), JsonPath.read(delta, "$.records[*].rev"))
        assertEquals(listOf("settlement-1", "settlement-2"), JsonPath.read(delta, "$.records[*].entityId"))

        mockMvc
            .perform(
                get(changesUrl(campaign.id))
                    .param("since", "1")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.latestRev").value(2))
            .andExpect(jsonPath("$.records.length()").value(1))
            .andExpect(jsonPath("$.records[0].rev").value(2))
            .andExpect(jsonPath("$.records[0].entityId").value("settlement-2"))
    }

    @Test
    fun replayingIdenticalBatchDoesNotDuplicateOrBumpRev() {
        val user = seedUser("push-replay-ada")
        val campaign = seedCampaign(CAMPAIGN_REPLAY, "INVITE-PUSH-REPLAY")
        seedMembership(campaign.id, user.id!!)
        val token = jwtService.issueToken(user.id!!)
        val body =
            changeSetJson(
                recordJson(campaign.id, "settlement-1", """{"name":"A"}"""),
                recordJson(campaign.id, "settlement-2", """{"name":"B"}"""),
            )

        fun push() {
            mockMvc
                .perform(
                    post(changesUrl(campaign.id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.latestRev").value(2))
        }

        push()
        push()

        mockMvc
            .perform(
                get(changesUrl(campaign.id))
                    .param("since", "0")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.records.length()").value(2))

        mockMvc
            .perform(
                get(changesUrl(campaign.id))
                    .param("since", "2")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.latestRev").value(2))
            .andExpect(jsonPath("$.records.length()").value(0))

        assertEquals(2, recordsFor(campaign.id).size)
        assertEquals(2L, changeRecords.maxRev(campaign.id))
    }

    @Test
    fun nonMemberIsForbidden() {
        val user = seedUser("push-outsider-ada")
        val campaign = seedCampaign(CAMPAIGN_OUTSIDER, "INVITE-PUSH-OUTSIDE")
        val token = jwtService.issueToken(user.id!!)

        mockMvc
            .perform(
                post(changesUrl(campaign.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(changeSetJson(recordJson(campaign.id, "settlement-1", """{"name":"A"}""")))
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun recordWithDifferentCampaignIdCannotWriteIntoAnotherCampaign() {
        val user = seedUser("push-scope-ada")
        val campaign = seedCampaign(CAMPAIGN_SCOPE, "INVITE-PUSH-SCOPE")
        seedMembership(campaign.id, user.id!!)
        val token = jwtService.issueToken(user.id!!)
        val body = changeSetJson(recordJson(OTHER_CAMPAIGN, "settlement-hijack", """{"name":"A"}"""))

        mockMvc
            .perform(
                post(changesUrl(campaign.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.latestRev").value(1))

        assertEquals(1, recordsFor(campaign.id).size)
        assertEquals(0, recordsFor(OTHER_CAMPAIGN).size)
    }

    private companion object {
        const val CAMPAIGN_SEQ = "01J0PUSH0000000000000000AA"
        const val CAMPAIGN_REPLAY = "01J0PUSH0000000000000000BB"
        const val CAMPAIGN_OUTSIDER = "01J0PUSH0000000000000000CC"
        const val CAMPAIGN_SCOPE = "01J0PUSH0000000000000000DD"
        const val OTHER_CAMPAIGN = "01J0PUSH0000000000000000EE"
    }
}

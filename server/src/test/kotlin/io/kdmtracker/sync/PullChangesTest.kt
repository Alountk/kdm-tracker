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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class PullChangesTest {
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

    private fun seedRecord(
        campaignId: String,
        entityId: String,
        rev: Long,
    ) = changeRecords.saveAndFlush(
        ChangeRecordEntity(
            campaignId = campaignId,
            entityType = "settlement",
            entityId = entityId,
            op = "UPSERT",
            payload = """{"name":"$entityId"}""",
            updatedAtMillis = 1_700_000_000_000,
            updatedAtCounter = rev.toInt(),
            baseRev = null,
            rev = rev,
        ),
    )

    private fun changesUrl(campaignId: String): String = "/api/v1/campaigns/$campaignId/changes"

    @Test
    fun sinceZeroReturnsAllRecordsOrderedByRev() {
        val user = seedUser("pull-all-ada")
        val campaign = seedCampaign(CAMPAIGN_ALL, "INVITE-PULL-ALL")
        seedMembership(campaign.id, user.id!!)
        seedRecord(campaign.id, "settlement-2", rev = 2)
        seedRecord(campaign.id, "settlement-1", rev = 1)
        val token = jwtService.issueToken(user.id!!)

        val body =
            mockMvc
                .perform(get(changesUrl(campaign.id)).param("since", "0").header("Authorization", "Bearer $token"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.latestRev").value(2))
                .andExpect(jsonPath("$.records.length()").value(2))
                .andReturn()
                .response
                .contentAsString

        assertEquals(listOf(1, 2), JsonPath.read(body, "$.records[*].rev"))
        assertEquals(listOf("settlement-1", "settlement-2"), JsonPath.read(body, "$.records[*].entityId"))
    }

    @Test
    fun sinceFirstRevReturnsOnlyNewerRecords() {
        val user = seedUser("pull-delta-ada")
        val campaign = seedCampaign(CAMPAIGN_DELTA, "INVITE-PULL-DELTA")
        seedMembership(campaign.id, user.id!!)
        seedRecord(campaign.id, "settlement-1", rev = 1)
        seedRecord(campaign.id, "settlement-2", rev = 2)
        val token = jwtService.issueToken(user.id!!)

        mockMvc
            .perform(get(changesUrl(campaign.id)).param("since", "1").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.latestRev").value(2))
            .andExpect(jsonPath("$.records.length()").value(1))
            .andExpect(jsonPath("$.records[0].rev").value(2))
            .andExpect(jsonPath("$.records[0].entityId").value("settlement-2"))
    }

    @Test
    fun doesNotLeakRecordsFromOtherCampaigns() {
        val user = seedUser("pull-scope-ada")
        val campaign = seedCampaign(CAMPAIGN_SCOPE, "INVITE-PULL-SCOPE")
        seedMembership(campaign.id, user.id!!)
        seedRecord(campaign.id, "settlement-mine", rev = 1)
        seedRecord(OTHER_CAMPAIGN, "settlement-theirs", rev = 2)
        val token = jwtService.issueToken(user.id!!)

        mockMvc
            .perform(get(changesUrl(campaign.id)).param("since", "0").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.latestRev").value(1))
            .andExpect(jsonPath("$.records.length()").value(1))
            .andExpect(jsonPath("$.records[0].entityId").value("settlement-mine"))
    }

    @Test
    fun nonMemberIsForbidden() {
        val user = seedUser("pull-outsider-ada")
        val campaign = seedCampaign(CAMPAIGN_OUTSIDER, "INVITE-PULL-OUTSIDE")
        seedRecord(campaign.id, "settlement-1", rev = 1)
        val token = jwtService.issueToken(user.id!!)

        mockMvc
            .perform(get(changesUrl(campaign.id)).header("Authorization", "Bearer $token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun missingTokenIsUnauthorized() {
        mockMvc
            .perform(get(changesUrl(CAMPAIGN_OUTSIDER)))
            .andExpect(status().isUnauthorized)
    }

    private companion object {
        const val CAMPAIGN_ALL = "01J0PULL0000000000000000AA"
        const val CAMPAIGN_DELTA = "01J0PULL0000000000000000BB"
        const val CAMPAIGN_SCOPE = "01J0PULL0000000000000000CC"
        const val CAMPAIGN_OUTSIDER = "01J0PULL0000000000000000DD"
        const val OTHER_CAMPAIGN = "01J0PULL0000000000000000EE"
    }
}

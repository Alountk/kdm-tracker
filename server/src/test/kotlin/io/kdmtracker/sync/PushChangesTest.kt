package io.kdmtracker.sync

import com.jayway.jsonpath.JsonPath
import io.kdmtracker.auth.CampaignEntity
import io.kdmtracker.auth.CampaignMemberEntity
import io.kdmtracker.auth.CampaignMemberRepository
import io.kdmtracker.auth.CampaignRepository
import io.kdmtracker.auth.JwtService
import io.kdmtracker.auth.UserEntity
import io.kdmtracker.auth.UserRepository
import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.ChangeSet
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
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    private lateinit var changeRecordService: ChangeRecordService

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

    @Test
    fun changedPayloadOnSameTripleAssignsANewRev() {
        val user = seedUser("push-update-ada")
        val campaign = seedCampaign(CAMPAIGN_UPDATE, "INVITE-PUSH-UPDATE")
        seedMembership(campaign.id, user.id!!)
        val token = jwtService.issueToken(user.id!!)

        fun pushPayload(payload: String): Int {
            val body =
                mockMvc
                    .perform(
                        post(changesUrl(campaign.id))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(changeSetJson(recordJson(campaign.id, "settlement-1", payload)))
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().isOk)
                    .andReturn()
                    .response
                    .contentAsString
            return JsonPath.read(body, "$.latestRev")
        }

        assertEquals(1, pushPayload("""{"name":"A"}"""))
        assertEquals(2, pushPayload("""{"name":"B"}"""))

        val stored = recordsFor(campaign.id)
        assertEquals(1, stored.size)
        assertEquals(2L, stored.single().rev)
        assertEquals("""{"name":"B"}""", stored.single().payload)

        mockMvc
            .perform(
                get(changesUrl(campaign.id))
                    .param("since", "1")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.latestRev").value(2))
            .andExpect(jsonPath("$.records.length()").value(1))
            .andExpect(jsonPath("$.records[0].rev").value(2))
            .andExpect(jsonPath("$.records[0].payload").value("""{"name":"B"}"""))
    }

    @Test
    fun concurrentPushesToSameCampaignGetDistinctAscendingRevs() {
        val campaign = seedCampaign(CAMPAIGN_CONCURRENT, "INVITE-PUSH-CONCURRENT")
        val ready = CountDownLatch(2)
        val go = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val tasks =
                listOf("settlement-1", "settlement-2").map { entityId ->
                    Callable {
                        ready.countDown()
                        go.await()
                        changeRecordService.push(
                            campaign.id,
                            ChangeSet(listOf(wireRecord(campaign.id, entityId, """{"name":"$entityId"}"""))),
                        )
                    }
                }
            val futures = tasks.map { pool.submit(it) }
            ready.await()
            go.countDown()
            val returnedRevs = futures.map { it.get(10, TimeUnit.SECONDS) }

            assertEquals(setOf(1L, 2L), returnedRevs.toSet())
            assertEquals(2L, changeRecords.maxRev(campaign.id))
            assertEquals(setOf(1L, 2L), recordsFor(campaign.id).map { it.rev }.toSet())
            assertEquals(2, recordsFor(campaign.id).size)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun malformedBodyIsBadRequest() {
        val user = seedUser("push-malformed-ada")
        val campaign = seedCampaign(CAMPAIGN_MALFORMED, "INVITE-PUSH-MALFORMED")
        seedMembership(campaign.id, user.id!!)
        val token = jwtService.issueToken(user.id!!)

        mockMvc
            .perform(
                post(changesUrl(campaign.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"records": [ {"campaignId": }""")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun memberOfAnotherCampaignIsForbiddenOnThisCampaignPath() {
        val user = seedUser("push-cross-ada")
        val campaignA = seedCampaign(CAMPAIGN_CROSS_A, "INVITE-CROSS-A")
        val campaignB = seedCampaign(CAMPAIGN_CROSS_B, "INVITE-CROSS-B")
        seedMembership(campaignA.id, user.id!!)
        val token = jwtService.issueToken(user.id!!)

        mockMvc
            .perform(
                post(changesUrl(campaignB.id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(changeSetJson(recordJson(campaignB.id, "settlement-1", """{"name":"A"}""")))
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isForbidden)
    }

    private fun wireRecord(
        campaignId: String,
        entityId: String,
        payload: String,
    ): ChangeRecord =
        ChangeRecord(
            campaignId = campaignId,
            entityType = "settlement",
            entityId = entityId,
            op = ChangeOp.UPSERT,
            baseRev = null,
            payload = payload,
            updatedAtMillis = 1_700_000_000_000,
            updatedAtCounter = 1,
            rev = 0,
        )

    private companion object {
        const val CAMPAIGN_SEQ = "01J0PUSH0000000000000000AA"
        const val CAMPAIGN_REPLAY = "01J0PUSH0000000000000000BB"
        const val CAMPAIGN_OUTSIDER = "01J0PUSH0000000000000000CC"
        const val CAMPAIGN_SCOPE = "01J0PUSH0000000000000000DD"
        const val OTHER_CAMPAIGN = "01J0PUSH0000000000000000EE"
        const val CAMPAIGN_UPDATE = "01J0PUSH0000000000000000FF"
        const val CAMPAIGN_CONCURRENT = "01J0PUSH0000000000000000GG"
        const val CAMPAIGN_MALFORMED = "01J0PUSH0000000000000000HH"
        const val CAMPAIGN_CROSS_A = "01J0PUSH0000000000000000JJ"
        const val CAMPAIGN_CROSS_B = "01J0PUSH0000000000000000KK"
    }
}

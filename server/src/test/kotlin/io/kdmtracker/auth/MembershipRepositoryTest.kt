package io.kdmtracker.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException

@DataJpaTest
class MembershipRepositoryTest {
    @Autowired
    private lateinit var users: UserRepository

    @Autowired
    private lateinit var campaigns: CampaignRepository

    @Autowired
    private lateinit var members: CampaignMemberRepository

    private fun user(username: String = "ada") =
        users.saveAndFlush(UserEntity(username = username, passwordHash = "test-hash"))

    private fun campaign(
        id: String = CAMPAIGN_ID,
        name: String = "First Cradle",
        inviteCode: String = "INVITE-ADA",
    ) = campaigns.saveAndFlush(CampaignEntity(id = id, name = name, inviteCode = inviteCode))

    @Test
    fun addsMembershipAndFindsItByCampaignAndUser() {
        val user = user()
        val campaign = campaign()

        members.saveAndFlush(
            CampaignMemberEntity(campaignId = campaign.id, userId = user.id!!, role = "OWNER"),
        )

        assertTrue(members.existsByCampaignIdAndUserId(campaign.id, user.id!!))
        val found = members.findByCampaignIdAndUserId(campaign.id, user.id!!)
        assertNotNull(found)
        assertEquals("OWNER", found?.role)
        assertEquals(listOf(campaign.id), members.findByUserId(user.id!!).map { it.campaignId })
    }

    @Test
    fun findsCampaignByInviteCode() {
        val campaign = campaign(inviteCode = "JOIN-ME-42")

        val found = campaigns.findByInviteCode("JOIN-ME-42")

        assertEquals(campaign.id, found?.id)
        assertEquals("First Cradle", found?.name)
        assertNull(campaigns.findByInviteCode("NOT-A-CODE"))
    }

    @Test
    fun findsUserByUsername() {
        val user = user(username = "ada")

        assertEquals(user.id, users.findByUsername("ada")?.id)
        assertNull(users.findByUsername("nobody"))
    }

    @Test
    fun membershipOnlyMatchesItsOwnCampaignAndUser() {
        val user = user()
        val campaign = campaign()
        members.saveAndFlush(
            CampaignMemberEntity(campaignId = campaign.id, userId = user.id!!, role = "VIEWER"),
        )

        assertFalse(members.existsByCampaignIdAndUserId(campaign.id, user.id!! + 999))
        assertFalse(members.existsByCampaignIdAndUserId(OTHER_CAMPAIGN_ID, user.id!!))
    }

    @Test
    fun duplicateUsernameIsRejected() {
        user(username = "ada")

        assertThrows(DataIntegrityViolationException::class.java) {
            users.saveAndFlush(UserEntity(username = "ada", passwordHash = "other-hash"))
        }
    }

    @Test
    fun duplicateInviteCodeIsRejected() {
        campaign(inviteCode = "INVITE-ADA")

        assertThrows(DataIntegrityViolationException::class.java) {
            campaigns.saveAndFlush(
                CampaignEntity(id = OTHER_CAMPAIGN_ID, name = "Second Cradle", inviteCode = "INVITE-ADA"),
            )
        }
    }

    @Test
    fun duplicateMembershipForSameCampaignAndUserIsRejected() {
        val user = user()
        val campaign = campaign()
        members.saveAndFlush(
            CampaignMemberEntity(campaignId = campaign.id, userId = user.id!!, role = "OWNER"),
        )

        assertThrows(DataIntegrityViolationException::class.java) {
            members.saveAndFlush(
                CampaignMemberEntity(campaignId = campaign.id, userId = user.id!!, role = "EDITOR"),
            )
        }
    }

    private companion object {
        const val CAMPAIGN_ID = "01J0KDM0000000000000000000"
        const val OTHER_CAMPAIGN_ID = "01J0KDM0000000000000000001"
    }
}

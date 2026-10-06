package io.kdmtracker.auth

import org.springframework.data.jpa.repository.JpaRepository

interface CampaignRepository : JpaRepository<CampaignEntity, String> {
    fun findByInviteCode(inviteCode: String): CampaignEntity?
}

package io.kdmtracker.auth

import org.springframework.data.jpa.repository.JpaRepository

interface CampaignMemberRepository : JpaRepository<CampaignMemberEntity, Long> {
    fun findByCampaignIdAndUserId(
        campaignId: String,
        userId: Long,
    ): CampaignMemberEntity?

    fun existsByCampaignIdAndUserId(
        campaignId: String,
        userId: Long,
    ): Boolean

    fun findByUserId(userId: Long): List<CampaignMemberEntity>
}

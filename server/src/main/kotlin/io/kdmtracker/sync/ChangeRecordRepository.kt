package io.kdmtracker.sync

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ChangeRecordRepository : JpaRepository<ChangeRecordEntity, Long> {
    fun findByCampaignIdAndRevGreaterThanOrderByRevAsc(
        campaignId: String,
        rev: Long,
    ): List<ChangeRecordEntity>

    fun findByCampaignIdAndEntityTypeAndEntityId(
        campaignId: String,
        entityType: String,
        entityId: String,
    ): ChangeRecordEntity?

    @Query("SELECT MAX(c.rev) FROM ChangeRecordEntity c WHERE c.campaignId = :campaignId")
    fun maxRev(campaignId: String): Long?
}

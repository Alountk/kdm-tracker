package io.kdmtracker.repository

import io.kdmtracker.domain.Milestone
import org.springframework.data.jpa.repository.JpaRepository

interface MilestoneRepository : JpaRepository<Milestone, Long> {
    fun findBySettlementIdAndKey(
        settlementId: Long,
        key: String,
    ): Milestone?

    fun findAllBySettlementId(settlementId: Long): List<Milestone>
}

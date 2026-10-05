package io.kdmtracker.repository

import io.kdmtracker.domain.Innovation
import org.springframework.data.jpa.repository.JpaRepository

interface InnovationRepository : JpaRepository<Innovation, Long> {
    fun findAllBySettlementIdOrderByUnlockedInYearDescNameAsc(settlementId: Long): List<Innovation>

    fun findAllBySettlementId(settlementId: Long): List<Innovation>

    fun countBySettlementId(settlementId: Long): Long
}

package io.kdmtracker.repository

import io.kdmtracker.domain.Survivor
import org.springframework.data.jpa.repository.JpaRepository

interface SurvivorRepository : JpaRepository<Survivor, Long> {
    fun findAllBySettlementIdOrderByAliveDescNameAsc(settlementId: Long): List<Survivor>

    fun findAllBySettlementId(settlementId: Long): List<Survivor>

    fun countBySettlementId(settlementId: Long): Long

    fun countBySettlementIdAndAliveTrue(settlementId: Long): Long
}

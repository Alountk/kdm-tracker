package io.kdmtracker.repository

import io.kdmtracker.domain.HuntRecord
import io.kdmtracker.domain.HuntStatus
import org.springframework.data.jpa.repository.JpaRepository

interface HuntRecordRepository : JpaRepository<HuntRecord, Long> {
    /** The single ACTIVE hunt for a settlement — null when no hunt is running. */
    fun findBySettlementIdAndStatus(
        settlementId: Long,
        status: HuntStatus,
    ): HuntRecord?

    /** Newest first (id is monotonic) — drives the hunt history list. */
    fun findAllBySettlementIdOrderByIdDesc(settlementId: Long): List<HuntRecord>

    fun findAllBySettlementId(settlementId: Long): List<HuntRecord>
}

package io.kdmtracker.repository

import io.kdmtracker.domain.LogEntry
import org.springframework.data.jpa.repository.JpaRepository

interface LogEntryRepository : JpaRepository<LogEntry, Long> {
    fun findAllBySettlementIdOrderByLanternYearDescCreatedAtDesc(settlementId: Long): List<LogEntry>

    fun findAllBySettlementId(settlementId: Long): List<LogEntry>

    fun findTop5BySettlementIdOrderByLanternYearDescCreatedAtDesc(settlementId: Long): List<LogEntry>

    fun findFirstBySettlementIdOrderByLanternYearDescCreatedAtDesc(settlementId: Long): LogEntry?
}

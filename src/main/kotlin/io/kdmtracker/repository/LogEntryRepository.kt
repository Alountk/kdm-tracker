package io.kdmtracker.repository

import io.kdmtracker.domain.LogEntry
import org.springframework.data.jpa.repository.JpaRepository

interface LogEntryRepository : JpaRepository<LogEntry, Long> {
    fun findAllByOrderByLanternYearDescCreatedAtDesc(): List<LogEntry>

    fun findTop5ByOrderByLanternYearDescCreatedAtDesc(): List<LogEntry>
}

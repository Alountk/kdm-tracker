package io.kdmtracker.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "log_entries")
class LogEntry(
    var lanternYear: Int = 1,
    var title: String = "",
    var body: String = "",
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var settlementId: Long = 1L,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

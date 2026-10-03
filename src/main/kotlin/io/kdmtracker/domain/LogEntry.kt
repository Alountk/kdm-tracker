package io.kdmtracker.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "log_entries")
class LogEntry(
    var lanternYear: Int = 1,
    var title: String = "",
    var body: String = "",
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @field:Column(name = "settlement_id")
    var settlementId: Long = 1L,
    @field:ManyToOne(optional = false, fetch = FetchType.LAZY)
    @field:JoinColumn(
        name = "settlement_id",
        insertable = false,
        updatable = false,
        foreignKey = ForeignKey(name = "fk_log_entries_settlement"),
    )
    var settlement: Settlement? = null,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

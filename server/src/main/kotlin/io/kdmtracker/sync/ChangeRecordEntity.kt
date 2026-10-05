package io.kdmtracker.sync

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * Server-side change-log row keyed by `(campaignId, entityType, entityId)`.
 *
 * [op] stores the [io.kdmtracker.core.sync.ChangeOp] name as text so this persistence type stays
 * decoupled from the `:core` wire enum. [rev] is server-assigned and monotonic per campaign.
 */
@Entity
@Table(
    name = "change_records",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["campaignId", "entityType", "entityId"]),
    ],
)
class ChangeRecordEntity(
    var campaignId: String = "",
    var entityType: String = "",
    var entityId: String = "",
    var op: String = "UPSERT",
    @field:Column(columnDefinition = "TEXT")
    var payload: String = "",
    var updatedAtMillis: Long = 0,
    var updatedAtCounter: Int = 0,
    var baseRev: Long? = null,
    var rev: Long = 0,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "change_record_seq")
    @field:SequenceGenerator(name = "change_record_seq", sequenceName = "change_record_seq", allocationSize = 1)
    var id: Long? = null,
)

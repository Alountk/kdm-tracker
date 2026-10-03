package io.kdmtracker.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * One official step-6 milestone ticked for a settlement.
 *
 * Deliberately has no foreign key to `settlements` (same import-wipe pattern as [StepNote]):
 * `/api/import` (schemaVersion 2) wipes and restores milestones alongside settlements.
 * Rows are purged explicitly when a settlement is deleted.
 *
 * Rows are created lazily on the first save; the panel displays all [MilestoneKey] entries
 * regardless (default false/false). No gating: a milestone can be reached at any point in
 * the phase (research §5), so the user ticks it when it happens.
 */
@Entity
@Table(
    name = "milestones",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_milestones_settlement_key", columnNames = ["settlement_id", "milestone_key"]),
    ],
)
class Milestone(
    var settlementId: Long = 1L,
    // "key" is a risky identifier in H2, so the column gets an explicit name.
    @field:Column(name = "milestone_key")
    var key: String = "",
    var reached: Boolean = false,
    var resolved: Boolean = false,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

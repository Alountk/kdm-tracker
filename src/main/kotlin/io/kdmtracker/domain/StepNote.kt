package io.kdmtracker.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * Free-form note attached to one settlement step.
 *
 * Deliberately has no foreign key to `settlements`: `/api/import` (schemaVersion 2)
 * wipes and restores step_notes alongside settlements in one transaction, so an FK
 * would add ceremony without value. Notes are cleaned up explicitly when a settlement
 * is deleted.
 */
@Entity
@Table(
    name = "step_notes",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_step_notes_settlement_step", columnNames = ["settlement_id", "step"]),
    ],
)
class StepNote(
    var settlementId: Long = 1L,
    var step: Int = 1,
    var text: String = "",
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

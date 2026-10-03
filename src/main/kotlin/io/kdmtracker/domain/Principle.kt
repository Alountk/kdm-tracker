package io.kdmtracker.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * One principle choice per category for a settlement (step 6).
 *
 * Deliberately has no foreign key to `settlements` — same import-wipe pattern as
 * [StepNote]; rows are purged explicitly when a settlement is deleted. The unique
 * (settlement, category) constraint makes re-saving an update, never a duplicate.
 */
@Entity
@Table(
    name = "principles",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_principles_settlement_category", columnNames = ["settlement_id", "category"]),
    ],
)
class Principle(
    var settlementId: Long = 1L,
    @field:Enumerated(EnumType.STRING)
    var category: PrincipleCategory = PrincipleCategory.DEATH,
    var choice: String = "",
    var chosenInYear: Int = 1,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

package io.kdmtracker.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * One hunt phase run for a settlement (research §3): quarry + monster level, the
 * 13-space board position, the event-log notes, the lantern year it started in and
 * the outcome status. The party lives in [HuntMember] rows.
 *
 * Deliberately has no foreign key to `settlements` (same import-wipe pattern as
 * [StepNote]): `/api/import` (schemaVersion 2) wipes hunt rows in the replace-all,
 * and settlement delete purges them explicitly. Export stays schema v2 — hunts are
 * not exported yet (TODO: export v3).
 *
 * The single ACTIVE hunt per settlement is enforced in code (the database cannot
 * express a portable partial unique index), see the hunt start endpoint.
 */
@Entity
@Table(name = "hunt_records")
class HuntRecord(
    var settlementId: Long = 1L,
    var quarry: String = "",
    var level: Int = 1,
    @field:Enumerated(EnumType.STRING)
    var status: HuntStatus = HuntStatus.ACTIVE,
    @field:Column(columnDefinition = "TEXT")
    var notes: String = "",
    // The lantern year the hunt started in — copied from the settlement at start.
    var lanternYear: Int = 1,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    // The board is exactly 13 spaces: the space clamps into 1..13 on every write,
    // so advancing at Space 13 (Starvation) is a no-op by construction.
    var currentSpace: Int = 1
        set(value) {
            field = value.coerceIn(1, SPACES)
        }

    companion object {
        const val SPACES = 13
    }
}

package io.kdmtracker.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * One survivor seat in a hunt party (≤4 per hunt, seat = departure order 0..3).
 *
 * Stores ids only — no foreign keys (established import-wipe pattern): rows are wiped
 * by `/api/import` replace-all and purged when the settlement is deleted. Party names
 * are resolved at render time via `HuntViews.party`.
 */
@Entity
@Table(name = "hunt_members")
class HuntMember(
    var huntId: Long = 1L,
    var survivorId: Long = 1L,
    var seat: Int = 0,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

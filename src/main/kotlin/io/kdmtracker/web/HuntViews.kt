package io.kdmtracker.web

import io.kdmtracker.domain.HuntMember
import io.kdmtracker.domain.HuntRecord
import io.kdmtracker.domain.HuntStatus
import io.kdmtracker.domain.Survivor

/** One hunt party chip: the survivor identity resolved from a [HuntMember] row. */
data class HuntPartyMember(
    val survivorId: Long,
    val name: String,
    val seat: Int,
)

/** One finished-hunt row for the history list (newest first, label precomposed). */
data class HuntHistoryRow(
    val lanternYear: Int,
    val quarry: String,
    val level: Int,
    val outcome: String,
    val fallenLink: Boolean,
)

/**
 * Read models for the hunt panel. Hunt members store survivor ids only (no FKs),
 * so party names are resolved here against the settlement's survivor list, in seat
 * order; history labels come from [HuntStatus.label] so the template stays dumb.
 */
object HuntViews {
    fun party(
        members: List<HuntMember>,
        survivors: List<Survivor>,
    ): List<HuntPartyMember> =
        members
            .sortedBy { it.seat }
            .map { member ->
                HuntPartyMember(
                    survivorId = member.survivorId,
                    name = survivors.firstOrNull { it.id == member.survivorId }?.name ?: "Unknown survivor",
                    seat = member.seat,
                )
            }

    fun history(hunts: List<HuntRecord>): List<HuntHistoryRow> =
        hunts.map { hunt ->
            HuntHistoryRow(
                lanternYear = hunt.lanternYear,
                quarry = hunt.quarry,
                level = hunt.level,
                outcome = hunt.status.label,
                // A failed hunt leaves survivors to be marked not-alive by hand.
                fallenLink = hunt.status == HuntStatus.FAILED,
            )
        }
}

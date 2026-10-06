package io.kdmtracker.core.sync

import kotlinx.serialization.Serializable

/**
 * A campaign the signed-in user can reach, as reported by the sync hub.
 *
 * [role] is the caller's role in the campaign (`OWNER`, `EDITOR`, `VIEWER`, …). [ownerUsername] is
 * the campaign owner's username. [sharedWith] lists the usernames the campaign is shared with,
 * excluding the owner.
 */
@Serializable
data class CampaignSummary(
    val id: String,
    val name: String,
    val role: String,
    val ownerUsername: String,
    val sharedWith: List<String>,
)

/** How a campaign relates to the signed-in user, driving the badge shown in the campaign list. */
enum class CampaignBadge {
    OWNED,
    SHARED_BY_ME,
    SHARED_WITH_ME,
}

/** The badge kind plus its user-facing [label] for a campaign. */
data class CampaignBanner(
    val badge: CampaignBadge,
    val label: String,
)

/** Pure rule mapping a [CampaignSummary] to its [CampaignBanner]. */
object CampaignBadges {
    fun of(summary: CampaignSummary): CampaignBanner =
        when {
            summary.role == "OWNER" && summary.sharedWith.isEmpty() ->
                CampaignBanner(CampaignBadge.OWNED, "Tuyo")

            summary.role == "OWNER" ->
                CampaignBanner(
                    CampaignBadge.SHARED_BY_ME,
                    "Compartido con: ${summary.sharedWith.joinToString(", ")}",
                )

            else ->
                CampaignBanner(
                    CampaignBadge.SHARED_WITH_ME,
                    "Compartido con vos por ${summary.ownerUsername}",
                )
        }
}

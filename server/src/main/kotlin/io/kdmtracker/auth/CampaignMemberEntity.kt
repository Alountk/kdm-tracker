package io.kdmtracker.auth

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * Links a [UserEntity] to a [CampaignEntity] with a [role] (`OWNER`/`EDITOR`/`VIEWER`).
 *
 * [campaignId] is a `String` because campaign ids are ULIDs (see [CampaignEntity.id]); [userId] is a
 * `Long` because user ids are generated. A user joins a given campaign at most once, enforced by the
 * `(campaignId, userId)` unique constraint.
 */
@Entity
@Table(
    name = "campaign_members",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["campaign_id", "user_id"]),
    ],
)
class CampaignMemberEntity(
    var campaignId: String = "",
    var userId: Long = 0,
    var role: String = "VIEWER",
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

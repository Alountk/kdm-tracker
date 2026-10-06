package io.kdmtracker.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * A sync campaign. [id] is a client-assigned ULID (26 characters of Crockford base32) so it matches
 * both the `campaignId` carried by `io.kdmtracker.core.sync.ChangeRecord` and the app's
 * `io.kdmtracker.core.domain.Ulid` identity. [inviteCode] is what a user shares to join.
 */
@Entity
@Table(name = "campaigns")
class CampaignEntity(
    @field:Id
    var id: String = "",
    var name: String = "",
    @field:Column(unique = true)
    var inviteCode: String = "",
)

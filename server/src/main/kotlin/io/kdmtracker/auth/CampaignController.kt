package io.kdmtracker.auth

import io.kdmtracker.core.domain.Ulid
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom

data class CreateCampaignRequest(
    val name: String = "",
)

data class JoinCampaignRequest(
    val inviteCode: String = "",
)

data class CampaignResponse(
    val id: String,
    val name: String,
    val inviteCode: String,
    val role: String,
)

/**
 * Campaign lifecycle for the sync hub. Both routes are behind the bearer filter (they live under
 * `/api/v1/campaigns`) and require a valid token. Creating a campaign makes the caller its `OWNER`;
 * joining by invite code makes the caller an `EDITOR`. Without these, a freshly registered user has
 * no membership and every change-log call would be `403`, so the hub would be unusable end to end.
 */
@RestController
@RequestMapping("/api/v1/campaigns")
class CampaignController(
    private val campaigns: CampaignRepository,
    private val members: CampaignMemberRepository,
) {
    private val random = SecureRandom()

    @PostMapping
    fun create(
        @RequestBody request: CreateCampaignRequest,
        httpRequest: HttpServletRequest,
    ): CampaignResponse {
        val userId = httpRequest.requireAuthenticatedUserId()
        val name = request.name.trim()
        if (name.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "campaign name is required")
        }
        val campaign =
            campaigns.save(
                CampaignEntity(id = newCampaignId(), name = name, inviteCode = newInviteCode()),
            )
        members.save(CampaignMemberEntity(campaignId = campaign.id, userId = userId, role = OWNER_ROLE))
        return campaign.toResponse(OWNER_ROLE)
    }

    @PostMapping("/join")
    fun join(
        @RequestBody request: JoinCampaignRequest,
        httpRequest: HttpServletRequest,
    ): CampaignResponse {
        val userId = httpRequest.requireAuthenticatedUserId()
        val campaign =
            campaigns.findByInviteCode(request.inviteCode.trim())
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "no campaign for that invite code")
        val existing = members.findByCampaignIdAndUserId(campaign.id, userId)
        if (existing != null) {
            // Joining is idempotent: keep the current role rather than downgrading an owner.
            return campaign.toResponse(existing.role)
        }
        members.save(CampaignMemberEntity(campaignId = campaign.id, userId = userId, role = EDITOR_ROLE))
        return campaign.toResponse(EDITOR_ROLE)
    }

    private fun newCampaignId(): String {
        var id: String
        do {
            id = Ulid.generate(System.currentTimeMillis()) { random.nextInt() }.value
        } while (campaigns.existsById(id))
        return id
    }

    private fun newInviteCode(): String {
        var code: String
        do {
            code = randomCode()
        } while (campaigns.findByInviteCode(code) != null)
        return code
    }

    private fun randomCode(): String =
        (1..INVITE_CODE_LENGTH).map { INVITE_ALPHABET[random.nextInt(INVITE_ALPHABET.length)] }.joinToString("")

    private fun CampaignEntity.toResponse(role: String): CampaignResponse =
        CampaignResponse(id = id, name = name, inviteCode = inviteCode, role = role)

    private companion object {
        const val OWNER_ROLE = "OWNER"
        const val EDITOR_ROLE = "EDITOR"
        const val INVITE_CODE_LENGTH = 10
        const val INVITE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    }
}

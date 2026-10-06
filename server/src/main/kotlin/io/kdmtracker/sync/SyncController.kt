package io.kdmtracker.sync

import io.kdmtracker.auth.BearerTokenFilter
import io.kdmtracker.auth.CampaignMemberRepository
import io.kdmtracker.core.sync.ChangeSet
import io.kdmtracker.core.sync.PullResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Result of a push: the campaign's new high-water mark after the batch is applied. */
data class PushResponse(
    val latestRev: Long,
)

/**
 * Sync hub pull/push endpoints. The bearer filter has already authenticated the caller and exposed
 * the user id as [BearerTokenFilter.AUTHENTICATED_USER_ID]; this controller enforces campaign
 * membership before delegating to [ChangeRecordService]. A non-member gets 403; a missing or invalid
 * token is rejected upstream with 401.
 */
@RestController
class SyncController(
    private val changeRecords: ChangeRecordService,
    private val members: CampaignMemberRepository,
) {
    @GetMapping("/api/v1/campaigns/{id}/changes")
    fun pull(
        @PathVariable id: String,
        @RequestParam since: Long = 0,
        request: HttpServletRequest,
    ): PullResponse {
        request.requireMemberOf(id)
        return changeRecords.pull(id, since)
    }

    @PostMapping("/api/v1/campaigns/{id}/changes")
    fun push(
        @PathVariable id: String,
        @RequestBody changeSet: ChangeSet,
        request: HttpServletRequest,
    ): PushResponse {
        request.requireMemberOf(id)
        return PushResponse(latestRev = changeRecords.push(id, changeSet))
    }

    private fun HttpServletRequest.requireMemberOf(campaignId: String) {
        val userId = getAttribute(BearerTokenFilter.AUTHENTICATED_USER_ID) as Long
        if (!members.existsByCampaignIdAndUserId(campaignId, userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "not a member of campaign $campaignId")
        }
    }
}

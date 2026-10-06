package io.kdmtracker.sync

import io.kdmtracker.auth.BearerTokenFilter
import io.kdmtracker.auth.CampaignMemberRepository
import io.kdmtracker.core.sync.PullResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Sync hub pull endpoint. The bearer filter has already authenticated the caller and exposed the user
 * id as [BearerTokenFilter.AUTHENTICATED_USER_ID]; this controller enforces campaign membership before
 * delegating to [ChangeRecordService]. A non-member gets 403; a missing or invalid token is rejected
 * upstream with 401.
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
        val userId = request.getAttribute(BearerTokenFilter.AUTHENTICATED_USER_ID) as Long
        if (!members.existsByCampaignIdAndUserId(id, userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "not a member of campaign $id")
        }
        return changeRecords.pull(id, since)
    }
}

package io.kdmtracker.sync

import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.PullResponse
import org.springframework.stereotype.Service

/**
 * Read side of the server change log. Maps persistence rows to the `:core` sync wire contract so the
 * controller never depends on JPA types.
 */
@Service
class ChangeRecordService(
    private val changeRecords: ChangeRecordRepository,
) {
    /**
     * Returns every change for [campaignId] with `rev > since`, ordered by ascending `rev`, plus the
     * campaign's current high-water mark. When the campaign has no changes [PullResponse.latestRev] is
     * `0`.
     */
    fun pull(
        campaignId: String,
        since: Long,
    ): PullResponse {
        val records =
            changeRecords
                .findByCampaignIdAndRevGreaterThanOrderByRevAsc(campaignId, since)
                .map { it.toChangeRecord() }
        val latestRev = changeRecords.maxRev(campaignId) ?: 0
        return PullResponse(latestRev = latestRev, records = records)
    }

    private fun ChangeRecordEntity.toChangeRecord(): ChangeRecord =
        ChangeRecord(
            campaignId = campaignId,
            entityType = entityType,
            entityId = entityId,
            op = ChangeOp.valueOf(op),
            baseRev = baseRev,
            payload = payload,
            updatedAtMillis = updatedAtMillis,
            updatedAtCounter = updatedAtCounter,
            rev = rev,
        )
}

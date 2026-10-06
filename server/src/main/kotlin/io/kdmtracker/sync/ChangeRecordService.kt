package io.kdmtracker.sync

import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.ChangeSet
import io.kdmtracker.core.sync.PullResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

    /**
     * Applies [changeSet] to the campaign's change log and returns the campaign's new high-water
     * mark. Each record's client-supplied [ChangeRecord.campaignId] and [ChangeRecord.rev] are
     * ignored: the write is scoped to the path [campaignId] and the server assigns the revision.
     *
     * Each record is upserted by the `(campaignId, entityType, entityId)` triple. A new record, or
     * one whose op/payload/updatedAt differ from the stored row, is stamped with the next
     * per-campaign `rev` (`maxRev + 1`); an identical record is a no-op. Replaying a batch is
     * therefore idempotent — no duplicate rows and no revision bump.
     */
    @Transactional
    fun push(
        campaignId: String,
        changeSet: ChangeSet,
    ): Long {
        var latestRev = changeRecords.maxRev(campaignId) ?: 0
        for (record in changeSet.records) {
            val stored =
                changeRecords.findByCampaignIdAndEntityTypeAndEntityId(
                    campaignId,
                    record.entityType,
                    record.entityId,
                )
            when {
                stored == null -> {
                    latestRev += 1
                    changeRecords.save(record.toEntity(campaignId, latestRev))
                }

                stored.differsFrom(record) -> {
                    latestRev += 1
                    stored.applyFrom(record, latestRev)
                    changeRecords.save(stored)
                }
            }
        }
        return latestRev
    }

    private fun ChangeRecord.toEntity(
        campaignId: String,
        rev: Long,
    ): ChangeRecordEntity =
        ChangeRecordEntity(
            campaignId = campaignId,
            entityType = entityType,
            entityId = entityId,
            op = op.name,
            payload = payload,
            updatedAtMillis = updatedAtMillis,
            updatedAtCounter = updatedAtCounter,
            baseRev = baseRev,
            rev = rev,
        )

    private fun ChangeRecordEntity.differsFrom(record: ChangeRecord): Boolean =
        op != record.op.name ||
            payload != record.payload ||
            updatedAtMillis != record.updatedAtMillis ||
            updatedAtCounter != record.updatedAtCounter

    private fun ChangeRecordEntity.applyFrom(
        record: ChangeRecord,
        rev: Long,
    ) {
        op = record.op.name
        payload = record.payload
        updatedAtMillis = record.updatedAtMillis
        updatedAtCounter = record.updatedAtCounter
        baseRev = record.baseRev
        this.rev = rev
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

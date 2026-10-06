package io.kdmtracker.sync

import io.kdmtracker.auth.CampaignRepository
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
    private val campaigns: CampaignRepository,
) {
    /**
     * Returns every change for [campaignId] with `rev > since`, ordered by ascending `rev`, plus the
     * campaign's current high-water mark. When the campaign has no changes [PullResponse.latestRev] is
     * `0`.
     *
     * The high-water mark is read **before** the records and the records are clamped to
     * `rev <= latestRev`. A push that commits between the two reads can therefore only increase the
     * returned `latestRev` beyond a record the caller did not receive in *this* response; clamping
     * keeps the client's cursor from advancing past a change it never saw, so the change is delivered
     * on the next pull instead of being skipped silently.
     */
    fun pull(
        campaignId: String,
        since: Long,
    ): PullResponse {
        val latestRev = changeRecords.maxRev(campaignId) ?: 0
        val records =
            changeRecords
                .findByCampaignIdAndRevGreaterThanOrderByRevAsc(campaignId, since)
                .filter { it.rev <= latestRev }
                .map { it.toChangeRecord() }
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
     *
     * The campaign row is locked `PESSIMISTIC_WRITE` for the whole transaction, so concurrent pushes
     * to the same campaign serialize and cannot both assign the same `rev` from a stale `maxRev`.
     *
     * Conflict model (Ruling W): the server performs **no** server-side last-write-wins or merge. The
     * stored row is simply the most recent push for the `(campaignId, entityType, entityId)` triple,
     * and [ChangeRecord.baseRev] is persisted and echoed back verbatim, but never interpreted here.
     * LWW/conflict resolution is deferred to plan 3c (client-side merge), which uses `baseRev` to
     * detect when its change was computed against a revision the server has since replaced.
     */
    @Transactional
    fun push(
        campaignId: String,
        changeSet: ChangeSet,
    ): Long {
        campaigns.findWithLockById(campaignId)
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

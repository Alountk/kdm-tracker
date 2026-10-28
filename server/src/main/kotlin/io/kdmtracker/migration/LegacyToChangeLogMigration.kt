package io.kdmtracker.migration

import io.kdmtracker.auth.CampaignEntity
import io.kdmtracker.auth.CampaignMemberEntity
import io.kdmtracker.auth.CampaignMemberRepository
import io.kdmtracker.auth.CampaignRepository
import io.kdmtracker.auth.ReservedUsernames
import io.kdmtracker.auth.UserEntity
import io.kdmtracker.auth.UserRepository
import io.kdmtracker.core.sync.ChangeOp
import io.kdmtracker.core.sync.ChangeRecord
import io.kdmtracker.core.sync.ChangeSet
import io.kdmtracker.domain.Innovation
import io.kdmtracker.domain.LogEntry
import io.kdmtracker.domain.Milestone
import io.kdmtracker.domain.Principle
import io.kdmtracker.domain.Resource
import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.StepNote
import io.kdmtracker.domain.Survivor
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.repository.LogEntryRepository
import io.kdmtracker.repository.MilestoneRepository
import io.kdmtracker.repository.PrincipleRepository
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.repository.StepNoteRepository
import io.kdmtracker.repository.SurvivorRepository
import io.kdmtracker.sync.ChangeRecordRepository
import io.kdmtracker.sync.ChangeRecordService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.ZoneOffset

/**
 * Summary of a [LegacyToChangeLogMigration.migrate] run.
 *
 * [changesApplied] is the number of revisions the push actually wrote (new rows plus changed
 * payloads); a replayed run against unchanged legacy data returns `0`. [totalRecords] is the number
 * of legacy rows read, one per emitted `ChangeRecord`.
 */
data class MigrationReport(
    val campaignId: String,
    val settlements: Int,
    val survivors: Int,
    val resources: Int,
    val innovations: Int,
    val logEntries: Int,
    val stepNotes: Int,
    val milestones: Int,
    val principles: Int,
    val changesApplied: Int,
    val latestRev: Long,
) {
    val totalRecords: Int
        get() =
            settlements + survivors + resources + innovations + logEntries +
                stepNotes + milestones + principles
}

/**
 * One-shot, idempotent migration of the legacy relational rows into the server change log.
 *
 * The legacy model (settlements, survivors, resources, innovations, log entries, step notes,
 * milestones, principles — the full schema-v2 set) predates campaigns and accounts, so there is no
 * campaign to attribute the rows to. This migration folds every legacy row into a single
 * deterministic campaign ([CAMPAIGN_ID]) owned by a synthetic [MIGRATION_USERNAME] account, then
 * emits one `UPSERT` [ChangeRecord] per row and lets [ChangeRecordService] assign the per-campaign
 * `rev`. Records use a stable `entityType` per table: `settlement`, `survivor`, `resource`,
 * `innovation`, `log`, `step_note`, `milestone`, `principle`.
 *
 * Idempotency is delegated to the push path: the same `(campaignId, entityType, entityId)` triple
 * with an identical payload is a no-op, so a second [migrate] run neither adds rows nor bumps `rev`.
 * Legacy rows carry no `updatedAt`, so all records are stamped `(0, 0)`; log entries use their
 * `createdAt` (their closest available timestamp) converted to epoch millis in the fixed
 * [UTC_ZONE] so a second run under a different host time zone derives the same value.
 *
 * `migrate()` itself is deliberately **not** an `ApplicationRunner` — it is an explicit one-shot
 * call. `LegacyMigrationRunner` wires it to startup behind the off-by-default
 * `sync.migration.run-on-startup` flag (see the runbook in docs/server.md); from code it is just
 * `migration.migrate()`.
 */
@Service
class LegacyToChangeLogMigration(
    private val settlements: SettlementRepository,
    private val survivors: SurvivorRepository,
    private val resources: ResourceRepository,
    private val innovations: InnovationRepository,
    private val logEntries: LogEntryRepository,
    private val stepNotes: StepNoteRepository,
    private val milestones: MilestoneRepository,
    private val principles: PrincipleRepository,
    private val campaigns: CampaignRepository,
    private val members: CampaignMemberRepository,
    private val users: UserRepository,
    private val changeRecords: ChangeRecordRepository,
    private val changeRecordService: ChangeRecordService,
) {
    private val objectMapper: ObjectMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    /**
     * Reads every legacy row, folds it into the migration campaign, and pushes the resulting change
     * set. Returns a [MigrationReport] describing what was read and what the push applied.
     */
    @Transactional
    fun migrate(): MigrationReport {
        val campaignId = ensureMigrationCampaign()
        val beforeRev = changeRecords.maxRev(campaignId) ?: 0

        val settlementRecords =
            settlements
                .findAll()
                .sortedBy {
                    it.id
                }.map { it.toRecord(campaignId, "settlement", it.id, settlementPayload(it), 0, 0) }
        val survivorRecords =
            survivors
                .findAll()
                .sortedBy {
                    it.id
                }.map { it.toRecord(campaignId, "survivor", it.id, survivorPayload(it), 0, 0) }
        val resourceRecords =
            resources
                .findAll()
                .sortedBy {
                    it.id
                }.map { it.toRecord(campaignId, "resource", it.id, resourcePayload(it), 0, 0) }
        val innovationRecords =
            innovations
                .findAll()
                .sortedBy {
                    it.id
                }.map { it.toRecord(campaignId, "innovation", it.id, innovationPayload(it), 0, 0) }
        val logRecords =
            logEntries
                .findAll()
                .sortedBy {
                    it.id
                }.map { it.toRecord(campaignId, "log", it.id, logPayload(it), logUpdatedAtMillis(it), 0) }
        val stepNoteRecords =
            stepNotes
                .findAll()
                .sortedBy {
                    it.id
                }.map { it.toRecord(campaignId, "step_note", it.id, stepNotePayload(it), 0, 0) }
        val milestoneRecords =
            milestones
                .findAll()
                .sortedBy {
                    it.id
                }.map { it.toRecord(campaignId, "milestone", it.id, milestonePayload(it), 0, 0) }
        val principleRecords =
            principles
                .findAll()
                .sortedBy {
                    it.id
                }.map { it.toRecord(campaignId, "principle", it.id, principlePayload(it), 0, 0) }

        val records =
            settlementRecords + survivorRecords + resourceRecords + innovationRecords + logRecords +
                stepNoteRecords + milestoneRecords + principleRecords
        val latestRev = changeRecordService.push(campaignId, ChangeSet(records))

        return MigrationReport(
            campaignId = campaignId,
            settlements = settlementRecords.size,
            survivors = survivorRecords.size,
            resources = resourceRecords.size,
            innovations = innovationRecords.size,
            logEntries = logRecords.size,
            stepNotes = stepNoteRecords.size,
            milestones = milestoneRecords.size,
            principles = principleRecords.size,
            changesApplied = (latestRev - beforeRev).toInt(),
            latestRev = latestRev,
        )
    }

    /** Ensures the single deterministic migration campaign and its synthetic owner membership exist. */
    private fun ensureMigrationCampaign(): String {
        val campaign =
            campaigns.findById(CAMPAIGN_ID).orElse(null)
                ?: campaigns.save(CampaignEntity(id = CAMPAIGN_ID, name = CAMPAIGN_NAME, inviteCode = INVITE_CODE))
        val owner =
            users.findByUsername(MIGRATION_USERNAME)
                ?: users.save(UserEntity(username = MIGRATION_USERNAME, passwordHash = UNUSABLE_PASSWORD_HASH))
        if (!members.existsByCampaignIdAndUserId(campaign.id, owner.id!!)) {
            members.save(CampaignMemberEntity(campaignId = campaign.id, userId = owner.id!!, role = "OWNER"))
        }
        return campaign.id
    }

    private fun <T> T.toRecord(
        campaignId: String,
        entityType: String,
        entityId: Long?,
        payload: String,
        updatedAtMillis: Long,
        updatedAtCounter: Int,
    ): ChangeRecord =
        ChangeRecord(
            campaignId = campaignId,
            entityType = entityType,
            entityId = requireNotNull(entityId) { "$entityType row has no id" }.toString(),
            op = ChangeOp.UPSERT,
            baseRev = null,
            payload = payload,
            updatedAtMillis = updatedAtMillis,
            updatedAtCounter = updatedAtCounter,
            rev = 0,
        )

    private fun json(value: Any): String = objectMapper.writeValueAsString(value)

    private fun settlementPayload(row: Settlement): String =
        json(
            linkedMapOf(
                "id" to row.id,
                "name" to row.name,
                "lanternYear" to row.lanternYear,
                "notes" to row.notes,
                "phase" to row.phase.name,
                "currentStep" to row.currentStep,
                "settlementEvent" to row.settlementEvent,
                "endeavors" to row.endeavors,
                "population" to row.population,
            ),
        )

    private fun survivorPayload(row: Survivor): String =
        json(
            linkedMapOf(
                "id" to row.id,
                "name" to row.name,
                "sex" to row.sex?.name,
                "age" to row.age,
                "movement" to row.movement,
                "speed" to row.speed,
                "accuracy" to row.accuracy,
                "strength" to row.strength,
                "dexterity" to row.dexterity,
                "evasion" to row.evasion,
                "armor" to row.armor,
                "insanity" to row.insanity,
                "courage" to row.courage,
                "understanding" to row.understanding,
                "xp" to row.xp,
                "alive" to row.alive,
                "notes" to row.notes,
                "settlementId" to row.settlementId,
            ),
        )

    private fun resourcePayload(row: Resource): String =
        json(
            linkedMapOf(
                "id" to row.id,
                "name" to row.name,
                "category" to row.category.name,
                "quantity" to row.quantity,
                "settlementId" to row.settlementId,
            ),
        )

    private fun innovationPayload(row: Innovation): String =
        json(
            linkedMapOf(
                "id" to row.id,
                "name" to row.name,
                "unlockedInYear" to row.unlockedInYear,
                "notes" to row.notes,
                "settlementId" to row.settlementId,
            ),
        )

    private fun logPayload(row: LogEntry): String =
        json(
            linkedMapOf(
                "id" to row.id,
                "lanternYear" to row.lanternYear,
                "title" to row.title,
                "body" to row.body,
                "createdAt" to row.createdAt.toString(),
                "settlementId" to row.settlementId,
            ),
        )

    private fun stepNotePayload(row: StepNote): String =
        json(
            linkedMapOf(
                "id" to row.id,
                "settlementId" to row.settlementId,
                "step" to row.step,
                "text" to row.text,
            ),
        )

    private fun milestonePayload(row: Milestone): String =
        json(
            linkedMapOf(
                "id" to row.id,
                "settlementId" to row.settlementId,
                "key" to row.key,
                "reached" to row.reached,
                "resolved" to row.resolved,
            ),
        )

    private fun principlePayload(row: Principle): String =
        json(
            linkedMapOf(
                "id" to row.id,
                "settlementId" to row.settlementId,
                "category" to row.category.name,
                "choice" to row.choice,
                "chosenInYear" to row.chosenInYear,
            ),
        )

    /**
     * Log entries expose no `updatedAt`; `createdAt` is the closest available stamp. The conversion
     * uses the fixed [UTC_ZONE] so the derived millisecond value does not depend on the host time
     * zone and a replayed run stays a no-op everywhere.
     */
    private fun logUpdatedAtMillis(row: LogEntry): Long =
        row.createdAt
            .atZone(UTC_ZONE)
            .toInstant()
            .toEpochMilli()

    companion object {
        /**
         * Deterministic campaign id for the migration. It is a fixed, well-known Crockford-valid
         * 26-character ULID (not a generated id) so repeated runs fold into the same campaign, and any
         * client can address the migrated data by a stable name.
         */
        const val CAMPAIGN_ID = "01J0M1GRAT0000000000000000"

        /** Synthetic owner account for the migration campaign; no login is possible with this hash. */
        const val MIGRATION_USERNAME = ReservedUsernames.LEGACY_MIGRATION

        /** Fixed time zone for deriving event timestamps so migration output is host-independent. */
        private val UTC_ZONE = ZoneOffset.UTC

        private const val CAMPAIGN_NAME = "Legacy Migration"
        private const val INVITE_CODE = "LEGACY-MIGRATION"
        private const val UNUSABLE_PASSWORD_HASH = "!"
    }
}

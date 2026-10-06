package io.kdmtracker.migration

import io.kdmtracker.auth.CampaignMemberRepository
import io.kdmtracker.auth.CampaignRepository
import io.kdmtracker.auth.UserRepository
import io.kdmtracker.domain.LogEntry
import io.kdmtracker.domain.Milestone
import io.kdmtracker.domain.MilestoneKey
import io.kdmtracker.domain.Principle
import io.kdmtracker.domain.PrincipleCategory
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
import io.kdmtracker.sync.ChangeRecordEntity
import io.kdmtracker.sync.ChangeRecordRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class LegacyMigrationTest {
    @Autowired
    private lateinit var migration: LegacyToChangeLogMigration

    @Autowired
    private lateinit var settlements: SettlementRepository

    @Autowired
    private lateinit var survivors: SurvivorRepository

    @Autowired
    private lateinit var resources: ResourceRepository

    @Autowired
    private lateinit var innovations: InnovationRepository

    @Autowired
    private lateinit var logEntries: LogEntryRepository

    @Autowired
    private lateinit var stepNotes: StepNoteRepository

    @Autowired
    private lateinit var milestones: MilestoneRepository

    @Autowired
    private lateinit var principles: PrincipleRepository

    @Autowired
    private lateinit var changeRecords: ChangeRecordRepository

    @Autowired
    private lateinit var campaigns: CampaignRepository

    @Autowired
    private lateinit var members: CampaignMemberRepository

    @Autowired
    private lateinit var users: UserRepository

    private fun recordsFor(campaignId: String): List<ChangeRecordEntity> =
        changeRecords.findByCampaignIdAndRevGreaterThanOrderByRevAsc(campaignId, 0)

    private fun assertRecord(
        records: List<ChangeRecordEntity>,
        entityType: String,
        entityId: String,
    ) {
        val match = records.singleOrNull { it.entityType == entityType && it.entityId == entityId }
        assertNotNull(match, "expected a $entityType/$entityId change record among $records")
        assertEquals("UPSERT", match!!.op)
        assertTrue(match.payload.isNotBlank(), "payload for $entityType/$entityId should not be blank")
    }

    @Test
    fun migratesEveryLegacyRowOnceAndIsIdempotent() {
        val settlement = settlements.saveAndFlush(Settlement(name = "Migration Cradle", lanternYear = 7))
        val survivor = survivors.saveAndFlush(Survivor(name = "Migration Ada", settlementId = settlement.id!!))
        val resource =
            resources.saveAndFlush(
                Resource(name = "Migration Bone", quantity = 4, settlementId = settlement.id!!),
            )
        val innovation =
            innovations.saveAndFlush(
                io.kdmtracker.domain.Innovation(
                    name = "Migration Lantern",
                    unlockedInYear = 2,
                    settlementId = settlement.id!!,
                ),
            )
        val log =
            logEntries.saveAndFlush(
                LogEntry(
                    lanternYear = 3,
                    title = "Migration log",
                    body = "body",
                    settlementId = settlement.id!!,
                ),
            )
        val stepNote =
            stepNotes.saveAndFlush(
                StepNote(settlementId = settlement.id!!, step = 2, text = "Migration note"),
            )
        val milestone =
            milestones.saveAndFlush(
                Milestone(settlementId = settlement.id!!, key = MilestoneKey.PRINCIPLE_DEATH.name, reached = true),
            )
        val principle =
            principles.saveAndFlush(
                Principle(
                    settlementId = settlement.id!!,
                    category = PrincipleCategory.DEATH,
                    choice = "Grave Digging",
                    chosenInYear = 4,
                ),
            )

        val first = migration.migrate()

        // A campaign row + membership exist so the records are attributable.
        val campaignId = first.campaignId
        assertEquals(LegacyToChangeLogMigration.CAMPAIGN_ID, campaignId)
        assertTrue(campaigns.findById(campaignId).isPresent, "migration campaign should exist")
        val migrationUser = users.findByUsername(LegacyToChangeLogMigration.MIGRATION_USERNAME)
        assertNotNull(migrationUser, "migration owner user should exist")
        assertTrue(
            members.findByUserId(migrationUser!!.id!!).any { it.campaignId == campaignId },
            "migration user should be a member of the migration campaign",
        )

        // One change record per legacy row, keyed by (campaignId, entityType, entityId).
        val expectedRecords =
            (
                settlements.count() + survivors.count() + resources.count() +
                    innovations.count() + logEntries.count() + stepNotes.count() +
                    milestones.count() + principles.count()
            ).toInt()
        val firstRecords = recordsFor(campaignId)
        assertEquals(expectedRecords, firstRecords.size)
        assertEquals(expectedRecords, first.changesApplied)
        assertEquals(expectedRecords, first.totalRecords)
        assertEquals(stepNotes.count().toInt(), first.stepNotes)
        assertEquals(milestones.count().toInt(), first.milestones)
        assertEquals(principles.count().toInt(), first.principles)

        assertRecord(firstRecords, "settlement", settlement.id.toString())
        assertRecord(firstRecords, "survivor", survivor.id.toString())
        assertRecord(firstRecords, "resource", resource.id.toString())
        assertRecord(firstRecords, "innovation", innovation.id.toString())
        assertRecord(firstRecords, "log", log.id.toString())
        assertRecord(firstRecords, "step_note", stepNote.id.toString())
        assertRecord(firstRecords, "milestone", milestone.id.toString())
        assertRecord(firstRecords, "principle", principle.id.toString())

        // The service assigns a contiguous 1..N revision run; the migration never stamps rev itself.
        assertEquals((1..expectedRecords).map { it.toLong() }, firstRecords.map { it.rev })
        assertEquals(expectedRecords.toLong(), first.latestRev)

        // Second run: identical payloads are a no-op — no new records and no rev bump.
        val second = migration.migrate()

        assertEquals(0, second.changesApplied)
        assertEquals(first.latestRev, second.latestRev)
        assertEquals(expectedRecords, recordsFor(campaignId).size)
        assertEquals(expectedRecords.toLong(), changeRecords.maxRev(campaignId))
    }
}

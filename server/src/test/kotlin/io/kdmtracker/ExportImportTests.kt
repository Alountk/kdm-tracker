package io.kdmtracker

import io.kdmtracker.domain.Innovation
import io.kdmtracker.domain.LogEntry
import io.kdmtracker.domain.Milestone
import io.kdmtracker.domain.MilestoneKey
import io.kdmtracker.domain.Principle
import io.kdmtracker.domain.PrincipleCategory
import io.kdmtracker.domain.Resource
import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.SettlementPhase
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
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.matchesPattern
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ExportImportTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var settlementRepository: SettlementRepository

    @Autowired
    private lateinit var survivorRepository: SurvivorRepository

    @Autowired
    private lateinit var resourceRepository: ResourceRepository

    @Autowired
    private lateinit var innovationRepository: InnovationRepository

    @Autowired
    private lateinit var logEntryRepository: LogEntryRepository

    @Autowired
    private lateinit var stepNoteRepository: StepNoteRepository

    @Autowired
    private lateinit var milestoneRepository: MilestoneRepository

    @Autowired
    private lateinit var principleRepository: PrincipleRepository

    @Test
    fun exportReturnsJsonAttachmentWithSettlementBody() {
        val hold =
            settlementRepository.save(
                Settlement(
                    name = "Exported Hold",
                    lanternYear = 5,
                    settlementEvent = "The Storm",
                    endeavors = 7,
                    population = 9,
                ),
            )
        stepNoteRepository.save(StepNote(settlementId = hold.id!!, step = 2, text = "Exported note"))
        milestoneRepository.save(
            Milestone(settlementId = hold.id!!, key = MilestoneKey.GAME_OVER.name, reached = false, resolved = false),
        )
        principleRepository.save(
            Principle(
                settlementId = hold.id!!,
                category = PrincipleCategory.DEATH,
                choice = "The First Harvest",
                chosenInYear = 5,
            ),
        )

        mockMvc
            .perform(get("/api/export"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(
                header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    matchesPattern("attachment; filename=\"kdm-tracker-export-\\d{4}-\\d{2}-\\d{2}\\.json\""),
                ),
            ).andExpect(content().string(containsString("\"schemaVersion\":2")))
            .andExpect(content().string(containsString("\"exportedAt\"")))
            .andExpect(content().string(containsString("Exported Hold")))
            .andExpect(content().string(containsString("\"settlementEvent\":\"The Storm\"")))
            .andExpect(content().string(containsString("\"endeavors\":7")))
            .andExpect(content().string(containsString("\"population\":9")))
            .andExpect(content().string(containsString("\"stepNotes\"")))
            .andExpect(content().string(containsString("\"milestones\"")))
            .andExpect(content().string(containsString("\"principles\"")))
    }

    @Test
    fun importRoundTripRestoresSettlementsChildrenAndStats() {
        val alpha = settlementRepository.save(Settlement(name = "Round Trip Alpha", lanternYear = 3, currentStep = 4))
        val beta =
            settlementRepository.save(
                Settlement(name = "Round Trip Beta", lanternYear = 6, phase = SettlementPhase.HUNT),
            )
        survivorRepository.save(Survivor(name = "Alpha Scout", settlementId = alpha.id!!))
        survivorRepository.save(Survivor(name = "Alpha Smith", alive = false, settlementId = alpha.id!!))
        resourceRepository.save(Resource(name = "Round Trip Bone", quantity = 5, settlementId = alpha.id!!))
        innovationRepository.save(
            Innovation(name = "Round Trip Lantern", unlockedInYear = 3, settlementId = alpha.id!!),
        )
        logEntryRepository.save(
            LogEntry(lanternYear = 3, title = "Alpha chronicle", body = "It began.", settlementId = beta.id!!),
        )

        val settlementsBefore = settlementRepository.count()
        val survivorsBefore = survivorRepository.count()
        val resourcesBefore = resourceRepository.count()
        val innovationsBefore = innovationRepository.count()
        val logsBefore = logEntryRepository.count()
        val stepNotesBefore = stepNoteRepository.count()
        val milestonesBefore = milestoneRepository.count()
        val principlesBefore = principleRepository.count()

        val json =
            mockMvc
                .perform(get("/api/export"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        stepNoteRepository.deleteAllInBatch()
        milestoneRepository.deleteAllInBatch()
        principleRepository.deleteAllInBatch()
        survivorRepository.deleteAllInBatch()
        resourceRepository.deleteAllInBatch()
        innovationRepository.deleteAllInBatch()
        logEntryRepository.deleteAllInBatch()
        settlementRepository.deleteAllInBatch()
        assertEquals(0L, settlementRepository.count())

        mockMvc
            .perform(
                multipart("/api/import")
                    .file(MockMultipartFile("file", "backup.json", "application/json", json.toByteArray())),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))

        assertEquals(settlementsBefore, settlementRepository.count())
        assertEquals(survivorsBefore, survivorRepository.count())
        assertEquals(resourcesBefore, resourceRepository.count())
        assertEquals(innovationsBefore, innovationRepository.count())
        assertEquals(logsBefore, logEntryRepository.count())
        assertEquals(stepNotesBefore, stepNoteRepository.count())
        assertEquals(milestonesBefore, milestoneRepository.count())
        assertEquals(principlesBefore, principleRepository.count())

        val restoredAlpha = settlementRepository.findAll().first { it.name == "Round Trip Alpha" }
        val alphaId = restoredAlpha.id!!
        assertEquals(3, restoredAlpha.lanternYear)
        assertEquals(SettlementPhase.SETTLEMENT, restoredAlpha.phase)
        assertEquals(4, restoredAlpha.currentStep)

        val restoredBeta = settlementRepository.findAll().first { it.name == "Round Trip Beta" }
        val betaId = restoredBeta.id!!
        assertEquals(6, restoredBeta.lanternYear)
        assertEquals(SettlementPhase.HUNT, restoredBeta.phase)

        val alphaSurvivors = survivorRepository.findAllBySettlementId(alphaId)
        assertEquals(2, alphaSurvivors.size)
        assertTrue(alphaSurvivors.first { it.name == "Alpha Scout" }.alive)
        assertFalse(alphaSurvivors.first { it.name == "Alpha Smith" }.alive)

        val alphaResources = resourceRepository.findAllBySettlementId(alphaId)
        assertEquals(1, alphaResources.size)
        assertEquals(5, alphaResources.first().quantity)

        assertEquals(1, innovationRepository.countBySettlementId(alphaId))
        assertTrue(logEntryRepository.findAllBySettlementId(betaId).any { it.title == "Alpha chronicle" })

        val settlementIds = settlementRepository.findAll().mapNotNull { it.id }.toSet()
        assertTrue(survivorRepository.findAll().all { it.settlementId in settlementIds })
        assertTrue(resourceRepository.findAll().all { it.settlementId in settlementIds })
        assertTrue(stepNoteRepository.findAll().all { it.settlementId in settlementIds })
        assertTrue(milestoneRepository.findAll().all { it.settlementId in settlementIds })
        assertTrue(principleRepository.findAll().all { it.settlementId in settlementIds })
    }

    @Test
    fun v2RoundTripRestoresStepBookkeepingAndChildren() {
        val hold =
            settlementRepository.save(
                Settlement(
                    name = "M5 Round Trip",
                    lanternYear = 4,
                    currentStep = 6,
                    settlementEvent = "The Storm",
                    endeavors = 7,
                    population = 9,
                ),
            )
        val holdId = hold.id!!
        survivorRepository.save(Survivor(name = "M5 Scout", settlementId = holdId))
        stepNoteRepository.save(StepNote(settlementId = holdId, step = 1, text = "Setup note"))
        stepNoteRepository.save(StepNote(settlementId = holdId, step = 6, text = "Check milestones"))
        milestoneRepository.save(
            Milestone(
                settlementId = holdId,
                key = MilestoneKey.HOODED_KNIGHT.name,
                reached = true,
                resolved = true,
            ),
        )
        principleRepository.save(
            Principle(
                settlementId = holdId,
                category = PrincipleCategory.SOCIETY,
                choice = "Collective Toil",
                chosenInYear = 4,
            ),
        )

        val json =
            mockMvc
                .perform(get("/api/export"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        stepNoteRepository.deleteAllInBatch()
        milestoneRepository.deleteAllInBatch()
        principleRepository.deleteAllInBatch()
        survivorRepository.deleteAllInBatch()
        resourceRepository.deleteAllInBatch()
        innovationRepository.deleteAllInBatch()
        logEntryRepository.deleteAllInBatch()
        settlementRepository.deleteAllInBatch()
        assertEquals(0L, settlementRepository.count())

        mockMvc
            .perform(
                multipart("/api/import")
                    .file(MockMultipartFile("file", "v2.json", "application/json", json.toByteArray())),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))

        val restored = settlementRepository.findAll().first { it.name == "M5 Round Trip" }
        val restoredId = restored.id!!
        assertEquals(4, restored.lanternYear)
        assertEquals(6, restored.currentStep)
        assertEquals(SettlementPhase.SETTLEMENT, restored.phase)
        assertEquals("The Storm", restored.settlementEvent)
        assertEquals(7, restored.endeavors)
        // population is restored from the file — it must not fall back to the survivor count (1).
        assertEquals(9, restored.population)

        val notes = stepNoteRepository.findAllBySettlementId(restoredId)
        assertEquals(2, notes.size)
        assertEquals("Setup note", notes.first { it.step == 1 }.text)
        assertEquals("Check milestones", notes.first { it.step == 6 }.text)

        val milestone = milestoneRepository.findAllBySettlementId(restoredId).single()
        assertEquals(MilestoneKey.HOODED_KNIGHT.name, milestone.key)
        assertTrue(milestone.reached)
        assertTrue(milestone.resolved)

        val principle = principleRepository.findAllBySettlementId(restoredId).single()
        assertEquals(PrincipleCategory.SOCIETY, principle.category)
        assertEquals("Collective Toil", principle.choice)
        assertEquals(4, principle.chosenInYear)

        assertEquals(1, survivorRepository.findAllBySettlementId(restoredId).size)
    }

    @Test
    fun v1ImportAppliesLegacyDefaults() {
        // A real v1 backup carries the whole dataset, including the seeded basics that
        // ResourceFlowTests relies on surviving any successful import.
        val v1 =
            """
            {"schemaVersion":1,"exportedAt":"2024-01-01T00:00:00Z","settlements":[
              {"name":"Legacy Hold","lanternYear":2,"notes":"old times","phase":"SETTLEMENT","currentStep":3,
               "survivors":[{"name":"Old Scout"},{"name":"Old Smith"}],
               "resources":[],"innovations":[],"log":[]},
              {"name":"New Settlement","lanternYear":1,"notes":"","phase":"SETTLEMENT","currentStep":1,
               "survivors":[],"innovations":[],"log":[],
               "resources":[{"name":"Bone","category":"BASIC","quantity":0},
                            {"name":"Hide","category":"BASIC","quantity":0},
                            {"name":"Sinew","category":"BASIC","quantity":0},
                            {"name":"Amber","category":"SPECIAL","quantity":0},
                            {"name":"Cloth","category":"SPECIAL","quantity":0},
                            {"name":"Iron","category":"SPECIAL","quantity":0},
                            {"name":"Pigment","category":"SPECIAL","quantity":0},
                            {"name":"Scrap Iron","category":"SPECIAL","quantity":0}]}]}
            """.trimIndent()

        mockMvc
            .perform(
                multipart("/api/import")
                    .file(MockMultipartFile("file", "v1.json", "application/json", v1.toByteArray())),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))

        val restored = settlementRepository.findAll().first { it.name == "Legacy Hold" }
        val restoredId = restored.id!!
        assertEquals(2, restored.lanternYear)
        assertEquals(3, restored.currentStep)
        assertEquals(2, survivorRepository.findAllBySettlementId(restoredId).size)

        assertNull(restored.settlementEvent)
        assertEquals(0, restored.endeavors)
        assertEquals(2, restored.population)
        assertTrue(stepNoteRepository.findAllBySettlementId(restoredId).isEmpty())
        assertTrue(milestoneRepository.findAllBySettlementId(restoredId).isEmpty())
        assertTrue(principleRepository.findAllBySettlementId(restoredId).isEmpty())
    }

    @Test
    fun unsupportedSchemaVersionRejectedWithoutTouchingData() {
        settlementRepository.save(Settlement(name = "Version Marker", lanternYear = 9))
        val settlementsBefore = settlementRepository.count()
        val survivorsBefore = survivorRepository.count()

        val payloads =
            listOf(
                """{"schemaVersion":3,"settlements":[]}""",
                """{"schemaVersion":0,"settlements":[]}""",
                """{"settlements":[]}""",
            )
        payloads.forEach { payload ->
            mockMvc
                .perform(
                    multipart("/api/import")
                        .file(MockMultipartFile("file", "version.json", "application/json", payload.toByteArray())),
                ).andExpect(status().isOk)
                .andExpect(
                    content()
                        .string(containsString("Import failed: unsupported schemaVersion (expected 1 or 2).")),
                )
            assertEquals(settlementsBefore, settlementRepository.count())
            assertEquals(survivorsBefore, survivorRepository.count())
        }
        assertTrue(settlementRepository.findAll().any { it.name == "Version Marker" })
    }

    @Test
    fun invalidV2PayloadRejectedWithoutDataLoss() {
        val marker =
            settlementRepository.save(
                Settlement(name = "Untouched Marker", lanternYear = 11, settlementEvent = "Keeper"),
            )
        survivorRepository.save(Survivor(name = "Marker Scout", settlementId = marker.id!!))
        val settlementsBefore = settlementRepository.count()
        val survivorsBefore = survivorRepository.count()

        val payloads =
            listOf(
                """{"schemaVersion":2,"settlements":[{"name":"Doomed Hold","phase":"BOGUS"}]}""" to
                    "settlements[0].phase",
                """{"schemaVersion":2,"settlements":[{"lanternYear":5}]}""" to
                    "settlements[0].name is required.",
                """{"schemaVersion":2,"settlements":[{"name":"Bad Types","lanternYear":"three"}]}""" to
                    "has a value of the wrong type",
                """{"schemaVersion":2,"settlements":[{"name":"Dup Notes","phase":"SETTLEMENT","currentStep":1,
                    "stepNotes":[{"step":1,"text":"first"},{"step":1,"text":"second"}]}]}""" to
                    "could not be stored",
            )
        payloads.forEach { (payload, expectedError) ->
            mockMvc
                .perform(
                    multipart("/api/import")
                        .file(MockMultipartFile("file", "v2-invalid.json", "application/json", payload.toByteArray())),
                ).andExpect(status().isOk)
                .andExpect(content().string(containsString(expectedError)))
                .andExpect(content().string(not(containsString("at io.kdmtracker"))))
            assertEquals(settlementsBefore, settlementRepository.count())
            assertEquals(survivorsBefore, survivorRepository.count())
            assertTrue(settlementRepository.findAll().any { it.name == "Untouched Marker" })
        }
        assertFalse(settlementRepository.findAll().any { it.name == "Dup Notes" })
    }

    @Test
    fun invalidImportFileShowsClearErrorWithoutWipingData() {
        val before = settlementRepository.count()

        mockMvc
            .perform(
                multipart("/api/import")
                    .file(MockMultipartFile("file", "bad.json", "application/json", "{ not json".toByteArray())),
            ).andExpect(status().isOk)
            .andExpect(content().string(containsString("Import failed: the uploaded file is not valid JSON.")))
            .andExpect(content().string(not(containsString("Exception"))))
            .andExpect(content().string(not(containsString("at io.kdmtracker"))))

        assertEquals(before, settlementRepository.count())
    }
}

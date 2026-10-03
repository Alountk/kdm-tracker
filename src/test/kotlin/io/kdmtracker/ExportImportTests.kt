package io.kdmtracker

import io.kdmtracker.domain.Innovation
import io.kdmtracker.domain.LogEntry
import io.kdmtracker.domain.Resource
import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.SettlementPhase
import io.kdmtracker.domain.Survivor
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.repository.LogEntryRepository
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.repository.SurvivorRepository
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.matchesPattern
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun exportReturnsJsonAttachmentWithSettlementBody() {
        settlementRepository.save(Settlement(name = "Exported Hold", lanternYear = 5))

        mockMvc
            .perform(get("/api/export"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(
                header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    matchesPattern("attachment; filename=\"kdm-tracker-export-\\d{4}-\\d{2}-\\d{2}\\.json\""),
                ),
            ).andExpect(content().string(containsString("\"schemaVersion\":1")))
            .andExpect(content().string(containsString("\"exportedAt\"")))
            .andExpect(content().string(containsString("Exported Hold")))
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

        val json =
            mockMvc
                .perform(get("/api/export"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

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

        val wrongVersion = """{"schemaVersion":2,"settlements":[]}"""
        mockMvc
            .perform(
                multipart("/api/import")
                    .file(MockMultipartFile("file", "v2.json", "application/json", wrongVersion.toByteArray())),
            ).andExpect(status().isOk)
            .andExpect(content().string(containsString("Import failed: unsupported schemaVersion (expected 1).")))

        assertEquals(before, settlementRepository.count())
    }
}

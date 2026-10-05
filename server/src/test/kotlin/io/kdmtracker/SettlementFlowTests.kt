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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SettlementFlowTests {
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
    fun createSettlementRedirectsToItsDashboard() {
        val result =
            mockMvc
                .perform(
                    post("/settlements")
                        .param("name", "New Hold")
                        .param("lanternYear", "4")
                        .param("notes", "Founded on the fly"),
                ).andExpect(status().is3xxRedirection)
                .andReturn()

        val id = settlementRepository.findAllByOrderByIdAsc().first { it.name == "New Hold" }.id
        assertEquals("/settlements/$id", result.response.redirectedUrl)
        assertEquals(4, settlementRepository.findById(id!!).orElseThrow().lanternYear)
    }

    @Test
    fun blankSettlementNameIsRejectedAndFormRerenders() {
        mockMvc
            .perform(post("/settlements").param("name", "   "))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("field-error")))
    }

    @Test
    fun homeListsSettlementsWithSituation() {
        settlementRepository.save(Settlement(name = "Situation Hold", lanternYear = 2))

        mockMvc
            .perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Situation Hold")))
            .andExpect(content().string(containsString("Lantern Year 2")))
            .andExpect(content().string(containsString("Step 1/11")))
            .andExpect(content().string(containsString("In progress")))
            .andExpect(content().string(containsString("SETTLEMENT")))
    }

    @Test
    fun phaseAndStepCanBeUpdatedAndClamped() {
        val id = settlementRepository.save(Settlement(name = "Phase Hold")).id!!

        mockMvc
            .perform(post("/settlements/$id/phase-step").param("phase", "HUNT").param("currentStep", "15"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        assertEquals(SettlementPhase.HUNT, settlementRepository.findById(id).orElseThrow().phase)

        mockMvc
            .perform(post("/settlements/$id/phase-step").param("phase", "SETTLEMENT").param("currentStep", "4"))
            .andExpect(status().is3xxRedirection)
        val backToSettlement = settlementRepository.findById(id).orElseThrow()
        assertEquals(SettlementPhase.SETTLEMENT, backToSettlement.phase)
        assertEquals(4, backToSettlement.currentStep)

        // Unknown phase keeps the current one; the step is clamped to the official 1-11 range.
        mockMvc
            .perform(post("/settlements/$id/phase-step").param("phase", "BOGUS").param("currentStep", "99"))
            .andExpect(status().is3xxRedirection)
        val clamped = settlementRepository.findById(id).orElseThrow()
        assertEquals(SettlementPhase.SETTLEMENT, clamped.phase)
        assertEquals(11, clamped.currentStep)
    }

    @Test
    fun deleteSettlementCascadesOnlyItsChildren() {
        val doomed = settlementRepository.save(Settlement(name = "Doomed Hold"))
        val kept = settlementRepository.save(Settlement(name = "Kept Hold"))
        survivorRepository.save(Survivor(name = "Doomed One", settlementId = doomed.id!!))
        survivorRepository.save(Survivor(name = "Kept One", settlementId = kept.id!!))
        resourceRepository.save(Resource(name = "Doomed Bone", quantity = 3, settlementId = doomed.id!!))
        innovationRepository.save(Innovation(name = "Doomed Insight", settlementId = doomed.id!!))
        logEntryRepository.save(LogEntry(title = "Doomed chronicle", settlementId = doomed.id!!))

        mockMvc
            .perform(post("/settlements/${doomed.id}/delete"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))

        assertFalse(settlementRepository.existsById(doomed.id!!))
        assertTrue(survivorRepository.findAllBySettlementId(doomed.id!!).isEmpty())
        assertTrue(resourceRepository.findAllBySettlementId(doomed.id!!).isEmpty())
        assertTrue(innovationRepository.findAllBySettlementId(doomed.id!!).isEmpty())
        assertTrue(logEntryRepository.findAllBySettlementId(doomed.id!!).isEmpty())

        assertTrue(settlementRepository.existsById(kept.id!!))
        assertEquals(1, survivorRepository.findAllBySettlementId(kept.id!!).size)
    }

    @Test
    fun unknownSettlementReturns404() {
        mockMvc
            .perform(get("/settlements/99999991"))
            .andExpect(status().isNotFound())

        mockMvc
            .perform(get("/settlements/99999991/survivors"))
            .andExpect(status().isNotFound())
    }
}

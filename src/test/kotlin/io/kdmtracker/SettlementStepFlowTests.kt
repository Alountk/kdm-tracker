package io.kdmtracker

import io.kdmtracker.domain.LogEntry
import io.kdmtracker.domain.Resource
import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.SettlementPhase
import io.kdmtracker.domain.SettlementSteps
import io.kdmtracker.domain.Survivor
import io.kdmtracker.repository.LogEntryRepository
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.repository.StepNoteRepository
import io.kdmtracker.repository.SurvivorRepository
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
class SettlementStepFlowTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var settlementRepository: SettlementRepository

    @Autowired
    private lateinit var survivorRepository: SurvivorRepository

    @Autowired
    private lateinit var resourceRepository: ResourceRepository

    @Autowired
    private lateinit var logEntryRepository: LogEntryRepository

    @Autowired
    private lateinit var stepNoteRepository: StepNoteRepository

    @Test
    fun railRendersAllElevenStepsWithTitles() {
        val id = settlementRepository.save(Settlement(name = "Rail Hold", currentStep = 4)).id!!

        val page =
            mockMvc
                .perform(get("/settlements/$id"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        // Compare against decoded HTML: step 10's title contains '&', which th:text escapes.
        val decodedPage = page.replace("&amp;", "&")
        SettlementSteps.ALL.forEach { step ->
            assertTrue(decodedPage.contains(step.title), "rail must show step ${step.number} title: ${step.title}")
        }
        assertEquals(11, page.split("class=\"step-item").size - 1, "rail must render exactly 11 step buttons")
        assertTrue(page.contains("Step 4/11"), "detail header shows the current position")
        assertTrue(page.contains("BRANCHING"), "current step type badge rendered")
        assertTrue(page.contains("Notes for this step"), "notes block rendered on every step")
    }

    @Test
    fun dashboardRendersTheSidebarWithAVerticalStepList() {
        val id = settlementRepository.save(Settlement(name = "Sidebar Hold", currentStep = 6)).id!!

        val page =
            mockMvc
                .perform(get("/settlements/$id"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        assertTrue(page.contains("<aside class=\"side-rail"), "left sidebar container rendered")
        assertTrue(page.contains("dash-grid"), "two-column dashboard grid")
        assertTrue(page.contains("class=\"rail rail-vertical\""), "steps stack vertically in the sidebar")
        assertEquals(11, page.split("class=\"step-item").size - 1, "sidebar lists all 11 steps")
        assertTrue(page.contains("Step 6/11"), "sidebar nav shows the current position")
        assertTrue(page.contains("/steps/prev"), "compact prev button at the bottom of the list")
        assertTrue(page.contains("/steps/next"), "compact next button at the bottom of the list")
        assertTrue(page.contains("/settlements/$id/steps/6/go"), "each row posts the step-go endpoint")
    }

    @Test
    fun goNextPrevMoveCurrentStepWithClampsAndNoOps() {
        val id = settlementRepository.save(Settlement(name = "Nav Hold", currentStep = 1)).id!!

        goTo(id, "7")
        assertEquals(7, currentStepOf(id))

        goTo(id, "99")
        assertEquals(11, currentStepOf(id), "go clamps above 11")

        // Next at step 11 is a no-op: only end-phase may leave the terminal step.
        mockMvc
            .perform(post("/settlements/$id/steps/next"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        assertEquals(11, currentStepOf(id))

        mockMvc.perform(post("/settlements/$id/steps/prev")).andExpect(status().is3xxRedirection)
        assertEquals(10, currentStepOf(id))

        goTo(id, "0")
        assertEquals(1, currentStepOf(id), "go clamps below 1")

        // Prev at step 1 is a no-op.
        mockMvc.perform(post("/settlements/$id/steps/prev")).andExpect(status().is3xxRedirection)
        assertEquals(1, currentStepOf(id))

        mockMvc.perform(post("/settlements/$id/steps/next")).andExpect(status().is3xxRedirection)
        assertEquals(2, currentStepOf(id))
    }

    @Test
    fun stepNotesPersistUpdateAndDeleteOnEmptyText() {
        val id = settlementRepository.save(Settlement(name = "Note Hold")).id!!

        mockMvc
            .perform(post("/settlements/$id/steps/2/note").param("text", "  Heal everyone, tokens cleared  "))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        assertEquals("Heal everyone, tokens cleared", noteTextAt(id, 2))
        assertEquals(1, stepNoteRepository.findAllBySettlementId(id).size)

        mockMvc
            .perform(post("/settlements/$id/steps/2/note").param("text", "Updated note"))
            .andExpect(status().is3xxRedirection)
        assertEquals("Updated note", noteTextAt(id, 2), "same (settlement, step) row is updated, not duplicated")
        assertEquals(1, stepNoteRepository.findAllBySettlementId(id).size)

        mockMvc
            .perform(post("/settlements/$id/steps/3/note").param("text", "Endeavors: 4"))
            .andExpect(status().is3xxRedirection)

        // Notes render for the current step only: jump to step 2 to see its note.
        goTo(id, "2")
        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Updated note")))

        mockMvc
            .perform(post("/settlements/$id/steps/2/note").param("text", ""))
            .andExpect(status().is3xxRedirection)
        assertNull(stepNoteRepository.findBySettlementIdAndStep(id, 2), "empty text deletes the note row")
        assertEquals("Endeavors: 4", noteTextAt(id, 3), "other steps' notes stay untouched")
    }

    @Test
    fun stepFourAdvancesLanternYearAndSavesSettlementEvent() {
        val id =
            settlementRepository.save(Settlement(name = "Timeline Hold", lanternYear = 3, currentStep = 4)).id!!

        mockMvc
            .perform(
                post("/settlements/$id/lantern-year")
                    .param("delta", "1")
                    .param("settlementEvent", "A Posture of Defense"),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        assertEquals(4, settlementRepository.findById(id).orElseThrow().lanternYear)
        assertEquals("A Posture of Defense", settlementRepository.findById(id).orElseThrow().settlementEvent)

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("value=\"A Posture of Defense\"")))

        // Delta without a settlementEvent param leaves the event untouched.
        mockMvc
            .perform(post("/settlements/$id/lantern-year").param("delta", "-1"))
            .andExpect(status().is3xxRedirection)
        assertEquals(3, settlementRepository.findById(id).orElseThrow().lanternYear)
        assertEquals("A Posture of Defense", settlementRepository.findById(id).orElseThrow().settlementEvent)

        // The year never drops below 1, and an empty event field clears the nullable column.
        val settlement = settlementRepository.findById(id).orElseThrow()
        settlement.lanternYear = 1
        settlementRepository.save(settlement)
        mockMvc
            .perform(post("/settlements/$id/lantern-year").param("delta", "-1"))
            .andExpect(status().is3xxRedirection)
        assertEquals(1, settlementRepository.findById(id).orElseThrow().lanternYear)

        mockMvc
            .perform(post("/settlements/$id/lantern-year").param("delta", "0").param("settlementEvent", ""))
            .andExpect(status().is3xxRedirection)
        assertNull(settlementRepository.findById(id).orElseThrow().settlementEvent)
    }

    @Test
    fun stepSevenEndeavorsControlPersistsAndClampsAtZero() {
        val id = settlementRepository.save(Settlement(name = "Develop Hold", currentStep = 7)).id!!

        mockMvc
            .perform(post("/settlements/$id/steps/7/endeavors").param("endeavors", "5"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        assertEquals(5, settlementRepository.findById(id).orElseThrow().endeavors)

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("value=\"5\"")))
            .andExpect(content().string(containsString("/settlements/$id/resources")))
            .andExpect(content().string(containsString("/settlements/$id/innovations")))

        mockMvc
            .perform(post("/settlements/$id/steps/7/endeavors").param("endeavors", "-4"))
            .andExpect(status().is3xxRedirection)
        assertEquals(0, settlementRepository.findById(id).orElseThrow().endeavors, "endeavors clamp at 0")
    }

    @Test
    fun stepElevenEndPhaseNeedsConfirmThenCommitsToHunt() {
        val id = settlementRepository.save(Settlement(name = "Closing Hold", currentStep = 11)).id!!
        val closing = settlementRepository.findById(id).orElseThrow()
        closing.endeavors = 7
        settlementRepository.save(closing)

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("7 unspent endeavors will be discarded")))

        // Missing confirm checkbox: dashboard re-renders, every value stays untouched.
        mockMvc
            .perform(post("/settlements/$id/steps/end-phase"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Confirm that unspent endeavors are discarded")))
        val untouched = settlementRepository.findById(id).orElseThrow()
        assertEquals(7, untouched.endeavors)
        assertEquals(SettlementPhase.SETTLEMENT, untouched.phase)
        assertEquals(11, untouched.currentStep)

        // With the confirm checkbox the phase commits: endeavors zeroed, Hunt starts at step 1.
        mockMvc
            .perform(post("/settlements/$id/steps/end-phase").param("confirm", "true"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        val committed = settlementRepository.findById(id).orElseThrow()
        assertEquals(0, committed.endeavors)
        assertEquals(SettlementPhase.HUNT, committed.phase)
        assertEquals(1, committed.currentStep)
    }

    @Test
    fun resumeReturnsToSettlementPhaseKeepingTheStep() {
        val id =
            settlementRepository
                .save(Settlement(name = "Hunt Hold", currentStep = 7, phase = SettlementPhase.HUNT))
                .id!!

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Hunt in progress")))
            .andExpect(content().string(containsString("7/11")))
            .andExpect(content().string(containsString("Resume settlement phase")))
            // The sidebar keeps all 11 rows visible for orientation — dimmed and disabled.
            .andExpect(content().string(containsString("class=\"side-rail paused\"")))
            .andExpect(content().string(containsString("class=\"step-item")))
            .andExpect(content().string(containsString("disabled=\"disabled\"")))
            .andExpect(content().string(not(containsString("stroke-dashoffset"))))

        mockMvc
            .perform(post("/settlements/$id/steps/resume"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        val resumed = settlementRepository.findById(id).orElseThrow()
        assertEquals(SettlementPhase.SETTLEMENT, resumed.phase)
        assertEquals(7, resumed.currentStep, "resume keeps the step where it was")
    }

    @Test
    fun stepFiveListsDeadSurvivorsWithDeathNotes() {
        val id = settlementRepository.save(Settlement(name = "Death Hold", currentStep = 5)).id!!
        survivorRepository.save(
            Survivor(
                name = "Doomed Bob",
                alive = false,
                notes = "died at age 16 to the White Lion",
                settlementId = id,
            ),
        )
        survivorRepository.save(Survivor(name = "Living Ann", settlementId = id))

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Dead survivors (")))
            .andExpect(content().string(containsString("Doomed Bob")))
            .andExpect(content().string(containsString("died at age 16 to the White Lion")))
            .andExpect(content().string(not(containsString("No deaths recorded"))))
    }

    @Test
    fun stepsProgressRingRendersForTheCurrentStep() {
        val id = settlementRepository.save(Settlement(name = "Ring Hold", currentStep = 4)).id!!

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("<svg")))
            .andExpect(content().string(containsString("stroke-dashoffset=\"164\"")))
            .andExpect(content().string(containsString("3/11")))
            .andExpect(content().string(containsString("Steps progress")))
            .andExpect(content().string(containsString("Update Timeline")))
    }

    @Test
    fun storageSurvivorAndLogWidgetsRender() {
        val id = settlementRepository.save(Settlement(name = "Widget Hold", currentStep = 4)).id!!
        resourceRepository.save(Resource(name = "Widget Bone", quantity = 9, settlementId = id))
        resourceRepository.save(Resource(name = "Widget Hide", quantity = 5, settlementId = id))
        resourceRepository.save(Resource(name = "Widget Sinew", quantity = 2, settlementId = id))
        survivorRepository.save(Survivor(name = "Widget Ada", settlementId = id))
        survivorRepository.save(Survivor(name = "Widget Ben", settlementId = id))
        survivorRepository.save(Survivor(name = "Widget Cleo", settlementId = id))
        survivorRepository.save(Survivor(name = "Widget Dead Dave", alive = false, settlementId = id))
        logEntryRepository.save(LogEntry(lanternYear = 4, title = "Graduate Four", settlementId = id))
        logEntryRepository.save(LogEntry(lanternYear = 3, title = "Graduate Three", settlementId = id))
        logEntryRepository.save(LogEntry(lanternYear = 2, title = "Graduate Two", settlementId = id))
        logEntryRepository.save(LogEntry(lanternYear = 1, title = "Graduate One", settlementId = id))

        val page =
            mockMvc
                .perform(get("/settlements/$id"))
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("16 units")))
                .andExpect(content().string(containsString("Widget Bone")))
                .andExpect(content().string(containsString("3 / 4")))
                .andExpect(content().string(containsString("Widget Ada")))
                .andExpect(content().string(not(containsString("Widget Dead Dave"))))
                .andExpect(content().string(containsString("Graduate Four")))
                .andExpect(content().string(containsString("Graduate Three")))
                .andExpect(content().string(containsString("Graduate Two")))
                .andExpect(content().string(not(containsString("Graduate One"))))
                .andExpect(content().string(containsString("/settlements/$id/log")))
                .andExpect(content().string(containsString("/settlements/$id/survivors")))
                .andReturn()
                .response
                .contentAsString

        assertTrue(
            page.indexOf("Widget Bone") < page.indexOf("Widget Hide"),
            "storage lists resources by quantity descending",
        )
        assertTrue(page.indexOf("Graduate Four") < page.indexOf("Graduate Three"), "log keeps newest first")
    }

    private fun goTo(
        id: Long,
        n: String,
    ) {
        mockMvc
            .perform(post("/settlements/$id/steps/$n/go"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
    }

    private fun currentStepOf(id: Long): Int = settlementRepository.findById(id).orElseThrow().currentStep

    private fun noteTextAt(
        id: Long,
        step: Int,
    ): String? = stepNoteRepository.findBySettlementIdAndStep(id, step)?.text
}

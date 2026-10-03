package io.kdmtracker

import io.kdmtracker.domain.Innovation
import io.kdmtracker.domain.Milestone
import io.kdmtracker.domain.MilestoneKey
import io.kdmtracker.domain.Principle
import io.kdmtracker.domain.PrincipleCategory
import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.Survivor
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.repository.MilestoneRepository
import io.kdmtracker.repository.PrincipleRepository
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
class MilestonePrincipleFlowTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var settlementRepository: SettlementRepository

    @Autowired
    private lateinit var survivorRepository: SurvivorRepository

    @Autowired
    private lateinit var innovationRepository: InnovationRepository

    @Autowired
    private lateinit var milestoneRepository: MilestoneRepository

    @Autowired
    private lateinit var principleRepository: PrincipleRepository

    @Test
    fun milestonePanelShowsFiveOfficialRowsAndLazyCreatesOnFirstSave() {
        val id = settlementRepository.save(Settlement(name = "Milestone Hold", currentStep = 6)).id!!

        val page =
            mockMvc
                .perform(get("/settlements/$id"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        MilestoneKey.entries.forEach { key ->
            assertTrue(page.contains(key.title), "milestone row must show the title: ${key.title}")
            assertTrue(page.contains(key.condition), "milestone row must show the condition: ${key.condition}")
        }
        assertEquals(0, milestoneRepository.findAllBySettlementId(id).size, "rows are lazy-created on first save")

        mockMvc
            .perform(post("/settlements/$id/milestones/PRINCIPLE_DEATH").param("reached", "true"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        val saved = milestoneRepository.findAllBySettlementId(id)
        assertEquals(1, saved.size)
        assertEquals(MilestoneKey.PRINCIPLE_DEATH.name, saved[0].key)
        assertTrue(saved[0].reached)
        assertFalse(saved[0].resolved)

        // Unchecked boxes post nothing: a save without `reached` flips it back off.
        mockMvc
            .perform(post("/settlements/$id/milestones/PRINCIPLE_DEATH").param("resolved", "true"))
            .andExpect(status().is3xxRedirection)
        assertEquals(1, milestoneRepository.findAllBySettlementId(id).size, "same row updated, not duplicated")
        val updated = milestoneRepository.findAllBySettlementId(id).first()
        assertFalse(updated.reached, "missing checkbox param means false")
        assertTrue(updated.resolved)

        // Unknown keys are rejected instead of silently stored.
        mockMvc.perform(post("/settlements/$id/milestones/BOGUS")).andExpect(status().isNotFound)
        assertEquals(1, milestoneRepository.findAllBySettlementId(id).size)
    }

    @Test
    fun milestoneHintsDeriveFromDeathsPopulationAndInnovations() {
        // Scenario A: one death, population 15, five innovations.
        val a = settlementRepository.save(Settlement(name = "Hint Hold A", currentStep = 6)).id!!
        setPopulation(a, 15)
        survivorRepository.save(Survivor(name = "Hint Dead", alive = false, settlementId = a))
        survivorRepository.save(Survivor(name = "Hint Alive", settlementId = a))
        repeat(5) { i -> innovationRepository.save(Innovation(name = "Hint Innovation $i", settlementId = a)) }

        val pageA =
            mockMvc
                .perform(get("/settlements/$a"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        assertTrue(pageA.contains("eligible now — 1 death(s)"), "death hint fires with a dead survivor")
        assertTrue(pageA.contains("eligible now — population 15"), "society hint fires at population 15")
        assertTrue(pageA.contains("eligible now — 5 innovations"), "hooded knight fires at 5 innovations")
        assertTrue(pageA.contains("no data yet"), "new life has no data and says so")
        assertFalse(pageA.contains("eligible now — population 0"), "game over must not fire while populated")

        // Scenario B: population 0 flips Game Over on.
        val b = settlementRepository.save(Settlement(name = "Hint Hold B", currentStep = 6)).id!!
        val pageB =
            mockMvc
                .perform(get("/settlements/$b"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        assertTrue(pageB.contains("eligible now — population 0"), "game over fires at population 0")
    }

    @Test
    fun principleSavePersistsChoiceYearDefaultsAndStaysUniquePerCategory() {
        val id =
            settlementRepository.save(Settlement(name = "Principle Hold", currentStep = 6, lanternYear = 4)).id!!

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("not chosen yet")))
            .andExpect(content().string(containsString("principle-year-DEATH")))

        // Save without a year → chosenInYear falls back to the current lantern year.
        mockMvc
            .perform(
                post("/settlements/$id/principles/DEATH")
                    .param("choice", "The First Grave — Graves"),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        var rows = principleRepository.findAllBySettlementId(id)
        assertEquals(1, rows.size)
        assertEquals(PrincipleCategory.DEATH, rows[0].category)
        assertEquals("The First Grave — Graves", rows[0].choice)
        assertEquals(4, rows[0].chosenInYear, "year defaults to the settlement's lantern year")

        // Re-saving updates the same row instead of duplicating the category.
        mockMvc
            .perform(
                post("/settlements/$id/principles/DEATH")
                    .param("choice", "The First Harvest — Cannibalize")
                    .param("chosenInYear", "2"),
            ).andExpect(status().is3xxRedirection)
        rows = principleRepository.findAllBySettlementId(id)
        assertEquals(1, rows.size)
        assertEquals("The First Harvest — Cannibalize", rows[0].choice)
        assertEquals(2, rows[0].chosenInYear)

        // The other categories coexist — exactly one row per category.
        mockMvc
            .perform(
                post("/settlements/$id/principles/NEW_LIFE")
                    .param("choice", "With Affection — Protect the Young"),
            ).andExpect(status().is3xxRedirection)
        mockMvc
            .perform(
                post("/settlements/$id/principles/SOCIETY")
                    .param("choice", "Accept Darkness")
                    .param("chosenInYear", "5"),
            ).andExpect(status().is3xxRedirection)
        assertEquals(3, principleRepository.findAllBySettlementId(id).size)

        // Only the official choices are accepted.
        mockMvc
            .perform(post("/settlements/$id/principles/DEATH").param("choice", "Totally Made Up"))
            .andExpect(status().isNotFound)
        assertEquals(3, principleRepository.findAllBySettlementId(id).size)

        // Saved choices render as a chip together with the chosen year.
        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("The First Harvest — Cannibalize · LY 2")))
            .andExpect(content().string(containsString("Accept Darkness · LY 5")))
    }

    @Test
    fun populationControlSavesAndClampsAtZero() {
        val id = settlementRepository.save(Settlement(name = "Population Hold", currentStep = 6)).id!!

        mockMvc
            .perform(post("/settlements/$id/population").param("population", "12"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        assertEquals(12, settlementRepository.findById(id).orElseThrow().population)

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("value=\"12\"")))

        mockMvc
            .perform(post("/settlements/$id/population").param("population", "-5"))
            .andExpect(status().is3xxRedirection)
        assertEquals(0, settlementRepository.findById(id).orElseThrow().population, "population clamps at 0")
    }

    @Test
    fun deletingTheSettlementPurgesMilestonesAndPrinciples() {
        val id = settlementRepository.save(Settlement(name = "Purge Hold")).id!!
        milestoneRepository.save(Milestone(settlementId = id, key = MilestoneKey.GAME_OVER.name))
        principleRepository.save(
            Principle(settlementId = id, category = PrincipleCategory.SOCIETY, choice = "Collective Toil"),
        )

        mockMvc
            .perform(post("/settlements/$id/delete"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))

        assertTrue(milestoneRepository.findAllBySettlementId(id).isEmpty(), "milestones purged without an FK")
        assertTrue(principleRepository.findAllBySettlementId(id).isEmpty(), "principles purged without an FK")
    }

    private fun setPopulation(
        id: Long,
        value: Int,
    ) {
        val settlement = settlementRepository.findById(id).orElseThrow()
        settlement.population = value
        settlementRepository.save(settlement)
    }
}

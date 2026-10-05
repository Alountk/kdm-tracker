package io.kdmtracker

import io.kdmtracker.domain.Innovation
import io.kdmtracker.domain.Principle
import io.kdmtracker.domain.PrincipleCategory
import io.kdmtracker.domain.Resource
import io.kdmtracker.domain.ResourceCategory
import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.Survivor
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.repository.PrincipleRepository
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.repository.SurvivorRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
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
class StepBookkeepingFlowTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var settlementRepository: SettlementRepository

    @Autowired
    private lateinit var survivorRepository: SurvivorRepository

    @Autowired
    private lateinit var innovationRepository: InnovationRepository

    @Autowired
    private lateinit var resourceRepository: ResourceRepository

    @Autowired
    private lateinit var principleRepository: PrincipleRepository

    @Test
    fun stepOneSummaryRendersAsCompactChips() {
        val id =
            settlementRepository
                .save(Settlement(name = "Setup Hold", currentStep = 1, lanternYear = 6, population = 13))
                .id!!
        survivorRepository.save(Survivor(name = "Setup Ann", settlementId = id))
        survivorRepository.save(Survivor(name = "Setup Ben", settlementId = id))
        survivorRepository.save(Survivor(name = "Setup Dead Dave", alive = false, settlementId = id))
        innovationRepository.save(Innovation(name = "Setup Drums", settlementId = id))
        innovationRepository.save(Innovation(name = "Setup Paint", settlementId = id))
        principleRepository.save(
            Principle(
                settlementId = id,
                category = PrincipleCategory.DEATH,
                choice = "The First Grave — Graves",
                chosenInYear = 3,
            ),
        )

        // The setup summary shrinks to one row of chips — the update table is the star.
        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Settlement summary")))
            .andExpect(content().string(containsString("chip-row")))
            .andExpect(content().string(containsString("Population <b>13</b>")))
            .andExpect(content().string(containsString("Survivors <b>2 / 3</b>")))
            .andExpect(content().string(containsString("Innovations <b>2</b>")))
            .andExpect(content().string(containsString("Deaths <b>1</b>")))
            .andExpect(content().string(containsString("Principles chosen: 1")))

        // A settlement without principles still shows the chip with a zero count.
        val bare = settlementRepository.save(Settlement(name = "Bare Hold")).id!!
        mockMvc
            .perform(get("/settlements/$bare"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Principles chosen: 0")))
    }

    @Test
    fun stepThreeRendersTheEndeavorsControlAndPosts() {
        val id = settlementRepository.save(Settlement(name = "Gain Hold", currentStep = 3)).id!!

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("+1 endeavor per returning survivor")))
            .andExpect(content().string(containsString("/settlements/$id/steps/3/endeavors")))

        // Same settlement-scoped endpoint the step-7 control uses.
        mockMvc
            .perform(post("/settlements/$id/steps/3/endeavors").param("endeavors", "4"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        assertEquals(4, settlementRepository.findById(id).orElseThrow().endeavors)

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("value=\"4\"")))

        mockMvc
            .perform(post("/settlements/$id/steps/3/endeavors").param("endeavors", "-2"))
            .andExpect(status().is3xxRedirection)
        assertEquals(0, settlementRepository.findById(id).orElseThrow().endeavors, "endeavors clamp at 0")
    }

    @Test
    fun stepNineLinksToTheLogNewEntryForm() {
        val id = settlementRepository.save(Settlement(name = "Showdown Hold", currentStep = 9)).id!!

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Record the aftermath in the campaign log")))
            .andExpect(content().string(containsString("/settlements/$id/log/new")))

        // The quick action must land somewhere real: the new-entry form page.
        mockMvc
            .perform(get("/settlements/$id/log/new"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Add log entry")))
    }

    @Test
    fun stepTenRendersTheFullStorageTable() {
        val id = settlementRepository.save(Settlement(name = "Archive Hold", currentStep = 10)).id!!
        resourceRepository.save(
            Resource(name = "Archive Bone", category = ResourceCategory.BASIC, quantity = 7, settlementId = id),
        )
        resourceRepository.save(
            Resource(name = "Amber Chunk", category = ResourceCategory.SPECIAL, quantity = 3, settlementId = id),
        )
        resourceRepository.save(
            Resource(name = "White Lion Fur", category = ResourceCategory.MONSTER, quantity = 2, settlementId = id),
        )

        val page =
            mockMvc
                .perform(get("/settlements/$id"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        assertTrue(page.contains("Settlement storage"), "step-10 storage head")
        listOf("Archive Bone", "Amber Chunk", "White Lion Fur").forEach {
            assertTrue(page.contains(it), "storage row: $it")
        }
        listOf("BASIC", "SPECIAL", "MONSTER").forEach {
            assertTrue(page.contains(it), "storage category column: $it")
        }
        assertTrue(page.contains("/settlements/$id/resources"), "link to the resources page")
        assertTrue(
            page.indexOf("Amber Chunk") < page.indexOf("Archive Bone"),
            "storage table is sorted by resource name",
        )
    }
}

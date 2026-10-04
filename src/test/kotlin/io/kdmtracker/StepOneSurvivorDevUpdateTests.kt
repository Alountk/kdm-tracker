package io.kdmtracker

import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.Survivor
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
class StepOneSurvivorDevUpdateTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var settlementRepository: SettlementRepository

    @Autowired
    private lateinit var survivorRepository: SurvivorRepository

    @Test
    fun stepOneTableListsOnlyAliveSurvivorsWithPrefilledStats() {
        val id = settlementRepository.save(Settlement(name = "Dev Table Hold", currentStep = 1)).id!!
        val alive =
            survivorRepository.save(
                Survivor(name = "Dev Ann", age = 15, courage = 7, understanding = 5, insanity = 3, settlementId = id),
            )
        survivorRepository.save(Survivor(name = "Grave Dave", alive = false, settlementId = id))

        val page =
            mockMvc
                .perform(get("/settlements/$id"))
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Update survivors")))
                .andExpect(content().string(containsString("Courage")))
                .andExpect(content().string(containsString("Understanding")))
                .andExpect(content().string(containsString("Insanity")))
                .andReturn()
                .response
                .contentAsString

        assertTrue(
            page.contains("/settlements/$id/survivors/${alive.id}/dev-update"),
            "row form posts the dev-update endpoint",
        )
        assertTrue(
            page.contains("/settlements/$id/survivors/${alive.id}/edit"),
            "name links to the survivor sheet for notes",
        )
        assertTrue(page.contains("value=\"7\""), "courage prefilled")
        assertTrue(page.contains("value=\"5\""), "understanding prefilled")
        assertTrue(page.contains("value=\"3\""), "insanity prefilled")
        assertTrue(page.contains("min=\"0\""), "dev inputs clamp at 0 in the UI")
        assertFalse(page.contains("Grave Dave"), "dead survivors did not endure — excluded from the table")
    }

    @Test
    fun devUpdatePersistsClampsAndRedirectsToTheDashboard() {
        val id = settlementRepository.save(Settlement(name = "Dev Post Hold")).id!!
        val survivor =
            survivorRepository.save(
                Survivor(name = "Dev Ben", courage = 1, understanding = 1, insanity = 1, settlementId = id),
            )

        mockMvc
            .perform(
                post("/settlements/$id/survivors/${survivor.id}/dev-update")
                    .param("courage", "6")
                    .param("understanding", "4")
                    .param("insanity", "2"),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))

        val updated = survivorRepository.findById(survivor.id!!).orElseThrow()
        assertEquals(6, updated.courage)
        assertEquals(4, updated.understanding)
        assertEquals(2, updated.insanity)

        // Negative values clamp at 0 instead of going below the stat floor.
        mockMvc
            .perform(
                post("/settlements/$id/survivors/${survivor.id}/dev-update")
                    .param("courage", "-3")
                    .param("understanding", "-1")
                    .param("insanity", "0"),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))

        val clamped = survivorRepository.findById(survivor.id!!).orElseThrow()
        assertEquals(0, clamped.courage, "negative courage clamps at 0")
        assertEquals(0, clamped.understanding, "negative understanding clamps at 0")
        assertEquals(0, clamped.insanity)
    }

    @Test
    fun devUpdateRejectsUnknownAndForeignSurvivors() {
        val mine = settlementRepository.save(Settlement(name = "Dev Mine")).id!!
        val other = settlementRepository.save(Settlement(name = "Dev Other")).id!!
        val foreign = survivorRepository.save(Survivor(name = "Foreign Fay", settlementId = other))

        // Survivor of another settlement → 404, values untouched.
        mockMvc
            .perform(
                post("/settlements/$mine/survivors/${foreign.id}/dev-update")
                    .param("courage", "9")
                    .param("understanding", "9")
                    .param("insanity", "9"),
            ).andExpect(status().isNotFound())
        assertEquals(0, survivorRepository.findById(foreign.id!!).orElseThrow().courage)

        // Unknown survivor id → 404.
        mockMvc
            .perform(
                post("/settlements/$mine/survivors/99999991/dev-update")
                    .param("courage", "1")
                    .param("understanding", "1")
                    .param("insanity", "1"),
            ).andExpect(status().isNotFound())

        // Unknown settlement → 404.
        mockMvc
            .perform(
                post("/settlements/99999991/survivors/1/dev-update")
                    .param("courage", "1")
                    .param("understanding", "1")
                    .param("insanity", "1"),
            ).andExpect(status().isNotFound())
    }
}

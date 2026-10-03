package io.kdmtracker

import io.kdmtracker.domain.Sex
import io.kdmtracker.domain.Survivor
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
class SurvivorFlowTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var survivorRepository: SurvivorRepository

    @Test
    fun createSurvivorAppliesKdmDefaults() {
        mockMvc
            .perform(post("/survivors").param("name", "Ashen"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/survivors"))

        mockMvc
            .perform(get("/survivors"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Ashen")))

        val survivor = survivorRepository.findAll().first { it.name == "Ashen" }
        assertEquals(12, survivor.age)
        assertEquals(5, survivor.movement)
        assertEquals(4, survivor.speed)
        assertEquals(5, survivor.accuracy)
        assertEquals(1, survivor.strength)
        assertEquals(8, survivor.dexterity)
        assertEquals(0, survivor.evasion)
        assertEquals(0, survivor.armor)
        assertEquals(0, survivor.insanity)
        assertEquals(0, survivor.courage)
        assertEquals(0, survivor.understanding)
        assertEquals(0, survivor.xp)
        assertTrue(survivor.alive)
        assertEquals(1L, survivor.settlementId)
    }

    @Test
    fun blankNameIsRejectedAndFormRerenders() {
        mockMvc
            .perform(post("/survivors").param("name", "   "))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("field-error")))
    }

    @Test
    fun aliveBindingKeepsFirstRepeatedParamValue() {
        // Checked checkbox renders before the hidden fallback: alive=true&alive=false.
        mockMvc
            .perform(
                post("/survivors")
                    .param("name", "Checked Alive")
                    .param("alive", "true")
                    .param("alive", "false"),
            ).andExpect(status().is3xxRedirection)

        val checked = survivorRepository.findAll().first { it.name == "Checked Alive" }
        assertTrue(checked.alive)

        // Unchecked checkbox submits only the hidden fallback: alive=false.
        mockMvc
            .perform(
                post("/survivors")
                    .param("name", "Unchecked Alive")
                    .param("alive", "false"),
            ).andExpect(status().is3xxRedirection)

        val unchecked = survivorRepository.findAll().first { it.name == "Unchecked Alive" }
        assertFalse(unchecked.alive)
    }

    @Test
    fun editSurvivorPersistsChanges() {
        val survivor = survivorRepository.save(Survivor(name = "To Rename"))

        mockMvc
            .perform(get("/survivors/${survivor.id}/edit"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("To Rename")))

        mockMvc
            .perform(
                post("/survivors/${survivor.id}")
                    .param("name", "Renamed")
                    .param("sex", "FEMALE")
                    .param("age", "15")
                    .param("movement", "5")
                    .param("speed", "4")
                    .param("accuracy", "5")
                    .param("strength", "1")
                    .param("dexterity", "8")
                    .param("evasion", "0")
                    .param("armor", "0")
                    .param("insanity", "0")
                    .param("courage", "0")
                    .param("understanding", "0")
                    .param("xp", "3")
                    .param("alive", "true")
                    .param("notes", "Survived the first hunt"),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/survivors"))

        val reloaded = survivorRepository.findById(survivor.id!!).orElseThrow()
        assertEquals("Renamed", reloaded.name)
        assertEquals(Sex.FEMALE, reloaded.sex)
        assertEquals(15, reloaded.age)
        assertEquals(3, reloaded.xp)
    }
}

package io.kdmtracker

import io.kdmtracker.repository.InnovationRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
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
class InnovationFlowTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var innovationRepository: InnovationRepository

    @Test
    fun createInnovationAppearsInList() {
        mockMvc
            .perform(
                post("/innovations")
                    .param("name", "Bloodpaint")
                    .param("unlockedInYear", "2")
                    .param("notes", "Start each settlement with +1 understanding"),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/innovations"))

        mockMvc
            .perform(get("/innovations"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Bloodpaint")))

        val innovation = innovationRepository.findAll().first { it.name == "Bloodpaint" }
        assertEquals(2, innovation.unlockedInYear)
    }
}

package io.kdmtracker

import io.kdmtracker.domain.Settlement
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.repository.SettlementRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
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
    private lateinit var settlementRepository: SettlementRepository

    @Autowired
    private lateinit var innovationRepository: InnovationRepository

    private var settlementId: Long = 0L

    @BeforeEach
    fun createSettlement() {
        settlementId = settlementRepository.save(Settlement(name = "Innovation Flow Settlement")).id!!
    }

    @Test
    fun createInnovationAppearsInList() {
        mockMvc
            .perform(
                post("/settlements/$settlementId/innovations")
                    .param("name", "Bloodpaint")
                    .param("unlockedInYear", "2")
                    .param("notes", "Start each settlement with +1 understanding"),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$settlementId/innovations"))

        mockMvc
            .perform(get("/settlements/$settlementId/innovations"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Bloodpaint")))

        val innovation = innovationRepository.findAll().first { it.name == "Bloodpaint" }
        assertEquals(2, innovation.unlockedInYear)
        assertEquals(settlementId, innovation.settlementId)
    }
}

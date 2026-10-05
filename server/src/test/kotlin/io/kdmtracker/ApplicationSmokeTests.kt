package io.kdmtracker

import io.kdmtracker.domain.Settlement
import io.kdmtracker.repository.SettlementRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationSmokeTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var settlementRepository: SettlementRepository

    @Test
    fun homeRendersSettlementsList() {
        mockMvc
            .perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("kdm-tracker")))
            .andExpect(content().string(containsString("Settlements")))
            .andExpect(content().string(containsString("New settlement")))
            .andExpect(content().string(containsString("Export JSON")))
            .andExpect(content().string(containsString("Import JSON")))
    }

    @Test
    fun dashboardRendersSettlementProfile() {
        val settlement = settlementRepository.save(Settlement(name = "Smoke Settlement"))

        mockMvc
            .perform(get("/settlements/${settlement.id}"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("kdm-tracker")))
            .andExpect(content().string(containsString("Smoke Settlement")))
            .andExpect(content().string(containsString("Settlement profile")))
    }

    @Test
    fun healthEndpointReturnsOk() {
        mockMvc
            .perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(content().string("{\"status\":\"ok\"}"))
    }
}

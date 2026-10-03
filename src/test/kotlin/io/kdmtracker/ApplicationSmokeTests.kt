package io.kdmtracker

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

    @Test
    fun dashboardRenders() {
        mockMvc
            .perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("kdm-tracker")))
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

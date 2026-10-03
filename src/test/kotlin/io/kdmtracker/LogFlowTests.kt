package io.kdmtracker

import org.hamcrest.Matchers.containsString
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
class LogFlowTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun logEntriesAppearSortedByYearThenCreatedAt() {
        createEntry("3", "First winter record", "The settlement endured.")
        createEntry("1", "Dawn record", "The lantern is lit.")
        createEntry("3", "Second winter record", "Bone was spent.")

        val page =
            mockMvc
                .perform(get("/log"))
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("First winter record")))
                .andReturn()
                .response
                .contentAsString

        val second = page.indexOf("Second winter record")
        val first = page.indexOf("First winter record")
        val dawn = page.indexOf("Dawn record")
        assertTrue(second in 0 until first, "same-year newer entry must come first")
        assertTrue(first < dawn, "higher lantern year must come first")
    }

    @Test
    fun blankTitleIsRejectedAndListRerenders() {
        mockMvc
            .perform(post("/log").param("lanternYear", "2").param("title", ""))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("field-error")))
    }

    private fun createEntry(
        year: String,
        title: String,
        body: String,
    ) {
        mockMvc
            .perform(
                post("/log")
                    .param("lanternYear", year)
                    .param("title", title)
                    .param("body", body),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/log"))
    }
}

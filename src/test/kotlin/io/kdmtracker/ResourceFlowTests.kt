package io.kdmtracker

import io.kdmtracker.domain.ResourceCategory
import io.kdmtracker.domain.Settlement
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
class ResourceFlowTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var settlementRepository: SettlementRepository

    @Autowired
    private lateinit var resourceRepository: ResourceRepository

    private var settlementId: Long = 0L

    @BeforeEach
    fun createSettlement() {
        settlementId = settlementRepository.save(Settlement(name = "Resource Flow Settlement")).id!!
    }

    @Test
    fun seededBasicsArePresent() {
        val expected = listOf("Bone", "Hide", "Sinew", "Amber", "Cloth", "Iron", "Pigment", "Scrap Iron")
        val names = resourceRepository.findAll().map { it.name }
        assertTrue(names.containsAll(expected))
    }

    @Test
    fun incrementAndDecrementNeverDropBelowZero() {
        val bone = resourceRepository.findAll().first { it.name == "Bone" }
        val initial = bone.quantity

        adjust(bone.settlementId, bone.id!!, 1)
        assertEquals(initial + 1, quantityOf(bone.id!!))

        adjust(bone.settlementId, bone.id!!, -1)
        assertEquals(initial, quantityOf(bone.id!!))

        adjust(bone.settlementId, bone.id!!, -1)
        adjust(bone.settlementId, bone.id!!, -1)
        val floored = quantityOf(bone.id!!)
        assertEquals(if (initial >= 2) initial - 2 else 0, floored)
        assertTrue(floored >= 0)
    }

    @Test
    fun createCustomMonsterResource() {
        mockMvc
            .perform(
                post("/settlements/$settlementId/resources")
                    .param("name", "Phoenix Ash")
                    .param("category", "MONSTER")
                    .param("quantity", "2"),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$settlementId/resources"))

        mockMvc
            .perform(get("/settlements/$settlementId/resources"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Phoenix Ash")))

        val created = resourceRepository.findAll().first { it.name == "Phoenix Ash" }
        assertEquals(ResourceCategory.MONSTER, created.category)
        assertEquals(2, created.quantity)
        assertEquals(settlementId, created.settlementId)
    }

    private fun adjust(
        resourceSettlementId: Long,
        id: Long,
        delta: Int,
    ) {
        mockMvc
            .perform(post("/settlements/$resourceSettlementId/resources/$id/adjust").param("delta", delta.toString()))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$resourceSettlementId/resources"))
    }

    private fun quantityOf(id: Long): Int = resourceRepository.findById(id).orElseThrow().quantity
}

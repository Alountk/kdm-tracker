package io.kdmtracker

import io.kdmtracker.domain.SettlementPhase
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.seed.DataInitializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest
class DataInitializerTests {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var dataInitializer: DataInitializer

    @Autowired
    private lateinit var settlementRepository: SettlementRepository

    @Test
    fun legacyRowsWithNullPhaseAndStepGetDefaultsBackfilled() {
        // Simulates a row created before multi-settlement support: JPA adds both
        // columns as NULL for existing rows (this INSERT also proves they are nullable).
        jdbcTemplate.execute(
            "INSERT INTO settlements (id, name, lantern_year, notes, phase, current_step) " +
                "VALUES (999001, 'Legacy Volume', 3, 'created before multi-settlement support', NULL, NULL)",
        )

        dataInitializer.fixLegacyRows()

        val settlement = settlementRepository.findById(999001L).orElseThrow()
        assertEquals(SettlementPhase.SETTLEMENT, settlement.phase)
        assertEquals(1, settlement.currentStep)
        // Step-cockpit column: nullable endeavors is backfilled to 0 (settlement_event stays NULL).
        assertEquals(0, settlement.endeavors)
        assertNull(settlement.settlementEvent)

        dataInitializer.fixLegacyRows()
        assertEquals(SettlementPhase.SETTLEMENT, settlementRepository.findById(999001L).orElseThrow().phase)
        assertEquals(1, settlementRepository.findById(999001L).orElseThrow().currentStep)
        assertEquals(0, settlementRepository.findById(999001L).orElseThrow().endeavors)

        jdbcTemplate.execute("DELETE FROM settlements WHERE id = 999001")
    }
}

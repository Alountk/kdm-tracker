package io.kdmtracker

import io.kdmtracker.domain.SettlementPhase
import io.kdmtracker.domain.Survivor
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.repository.SurvivorRepository
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

    @Autowired
    private lateinit var survivorRepository: SurvivorRepository

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
        // Population column: no survivors for this row → backfilled to 0 from the count.
        assertEquals(0, settlement.population)

        dataInitializer.fixLegacyRows()
        assertEquals(SettlementPhase.SETTLEMENT, settlementRepository.findById(999001L).orElseThrow().phase)
        assertEquals(1, settlementRepository.findById(999001L).orElseThrow().currentStep)
        assertEquals(0, settlementRepository.findById(999001L).orElseThrow().endeavors)

        jdbcTemplate.execute("DELETE FROM settlements WHERE id = 999001")
    }

    @Test
    fun legacyRowsWithNullPopulationBackfillFromTheSurvivorCount() {
        // A row from a pre-population volume: the INSERT omits the column → NULL.
        jdbcTemplate.execute(
            "INSERT INTO settlements (id, name, lantern_year, notes, phase, current_step) " +
                "VALUES (999101, 'Legacy Population', 2, 'created before population existed', 'SETTLEMENT', 1)",
        )
        survivorRepository.save(Survivor(name = "Legacy Ann", settlementId = 999101L))
        survivorRepository.save(Survivor(name = "Legacy Bob", settlementId = 999101L))
        assertNull(
            jdbcTemplate.queryForObject("SELECT population FROM settlements WHERE id = 999101", Int::class.java),
            "the new column must be nullable on legacy rows",
        )

        dataInitializer.fixLegacyRows()
        assertEquals(2, settlementRepository.findById(999101L).orElseThrow().population)

        // Idempotent, and a population the user already set is never overwritten.
        dataInitializer.fixLegacyRows()
        assertEquals(2, settlementRepository.findById(999101L).orElseThrow().population)
        jdbcTemplate.update("UPDATE settlements SET population = 9 WHERE id = 999101")
        dataInitializer.fixLegacyRows()
        assertEquals(9, settlementRepository.findById(999101L).orElseThrow().population)

        survivorRepository.deleteAll(survivorRepository.findAllBySettlementId(999101L))
        jdbcTemplate.execute("DELETE FROM settlements WHERE id = 999101")
    }
}

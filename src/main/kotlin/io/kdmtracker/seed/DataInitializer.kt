package io.kdmtracker.seed

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * Startup repair for databases created before multi-settlement support.
 *
 * JPA adds the new `phase` and `current_step` columns as NULL on existing rows;
 * [fixLegacyRows] backfills the defaults (idempotent) before anything reads them,
 * does the same for the step-cockpit `endeavors` column, backfills `population` from
 * the survivor count, and makes sure the settlement
 * id sequence exists and starts above the highest existing settlement id (legacy
 * databases assigned ids manually starting at 1).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
class DataInitializer(
    private val jdbcTemplate: JdbcTemplate,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        fixLegacyRows()
    }

    fun fixLegacyRows() {
        jdbcTemplate.update(
            "UPDATE settlements SET phase = 'SETTLEMENT', current_step = 1 WHERE phase IS NULL OR current_step IS NULL",
        )
        // Step-cockpit columns (added nullable so Hibernate can alter the live volume).
        // endeavors is backfilled to 0 here — same care as current_step, one source of
        // truth instead of a read-time coalesce; settlement_event stays NULL (nullable read).
        jdbcTemplate.update("UPDATE settlements SET endeavors = 0 WHERE endeavors IS NULL")
        // Population column (added nullable for the same legacy-volume reason): backfill NULL
        // rows from the survivor count so old volumes read population = survivors. New
        // settlements are never NULL (Kotlin default 0 — the rules start a campaign at 4,
        // the user sets it in the step-6 panel).
        jdbcTemplate.update(
            "UPDATE settlements SET population = " +
                "(SELECT COUNT(*) FROM survivors WHERE settlement_id = settlements.id) " +
                "WHERE population IS NULL",
        )
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS settlement_seq")
        val maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM settlements", Long::class.java) ?: 0L
        jdbcTemplate.execute("ALTER SEQUENCE settlement_seq RESTART WITH ${maxId + 1}")
    }
}

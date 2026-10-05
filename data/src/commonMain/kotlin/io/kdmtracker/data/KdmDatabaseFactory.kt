package io.kdmtracker.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import io.kdmtracker.data.db.KdmDatabase

/**
 * Builds a [KdmDatabase] over [driver], creating the schema only when it is absent.
 *
 * The JVM tests pass a fresh in-memory driver; the Android app passes the driver it
 * opened for the on-device file. Probing the tables first keeps the factory idempotent,
 * so reopening an existing database does not try to recreate its schema.
 */
object KdmDatabaseFactory {
    fun create(driver: SqlDriver): KdmDatabase {
        if (!schemaExists(driver)) {
            KdmDatabase.Schema.create(driver)
        }
        return KdmDatabase(driver)
    }

    private fun schemaExists(driver: SqlDriver): Boolean {
        val tables = KdmDatabase.allTableNames()
        val placeholders = tables.joinToString(", ") { "?" }
        val found =
            driver
                .executeQuery(
                    identifier = null,
                    sql = "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name IN ($placeholders)",
                    mapper = { cursor ->
                        QueryResult.Value(
                            if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L,
                        )
                    },
                    parameters = tables.size,
                    binders = { tables.forEachIndexed { index, table -> bindString(index, table) } },
                ).value
        return found == tables.size.toLong()
    }
}

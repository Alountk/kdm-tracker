package io.kdmtracker.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kdmtracker.data.db.KdmDatabase
import kotlin.test.Test
import kotlin.test.assertTrue

class KdmDatabaseSmokeTest {
    @Test
    fun selectAllCampaignsReturnsEmptyOnFreshDatabase() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KdmDatabase.Schema.create(driver)
        val database = KdmDatabase(driver)

        val campaigns = database.campaignQueries.selectAllCampaigns().executeAsList()

        assertTrue(campaigns.isEmpty())
    }
}

package io.kdmtracker.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.persistence.CampaignStoreContract
import io.kdmtracker.data.db.KdmDatabase
import java.nio.file.Files

/**
 * Runs the shared [CampaignStoreContract] against the production [SqlDelightCampaignStore] over a
 * temp-file database, so the executable specification is verified against the real implementation
 * rather than only the in-memory fake.
 */
class SqlDelightCampaignStoreContractTest : CampaignStoreContract() {
    private val openDrivers = mutableListOf<JdbcSqliteDriver>()

    override fun createStore(): CampaignStore {
        val file = Files.createTempFile("kdm-contract", ".db").toFile()
        file.deleteOnExit()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        KdmDatabase.Schema.create(driver)
        openDrivers += driver
        return SqlDelightCampaignStore(KdmDatabase(driver))
    }

    override fun closeStore(store: CampaignStore) {
        openDrivers.forEach { it.close() }
        openDrivers.clear()
    }
}

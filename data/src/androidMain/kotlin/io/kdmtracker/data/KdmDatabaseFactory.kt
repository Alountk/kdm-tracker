package io.kdmtracker.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.data.db.KdmDatabase

/**
 * A [CampaignStore] together with the driver it owns.
 *
 * [close] releases the database connection, so callers must invoke it once the store is no longer
 * in use (the app's ViewModel does so from `onCleared`).
 */
class CampaignStoreHandle internal constructor(
    val store: CampaignStore,
    private val driver: AndroidSqliteDriver,
) {
    fun close() = driver.close()
}

/**
 * Opens the on-device database and returns a store over it.
 *
 * Schema creation and migration are delegated to [AndroidSqliteDriver]'s schema-aware constructor,
 * so an existing install is upgraded by SQLDelight's migration plan and new `.sqm` files need no
 * change here. The caller's [Context] is only used to derive the application context, so the driver
 * never retains an Activity.
 */
object KdmDatabaseFactory {
    private const val DATABASE_NAME = "kdm-tracker.db"

    fun openAndroidStore(context: Context): CampaignStoreHandle {
        val driver =
            AndroidSqliteDriver(KdmDatabase.Schema, context.applicationContext, DATABASE_NAME)
        return CampaignStoreHandle(SqlDelightCampaignStore(KdmDatabase(driver)), driver)
    }
}

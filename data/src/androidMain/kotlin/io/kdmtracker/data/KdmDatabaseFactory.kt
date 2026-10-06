package io.kdmtracker.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.sync.SyncStore
import io.kdmtracker.data.db.KdmDatabase
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The stores the app builds over one database, together with the driver they share.
 *
 * [store] serves the domain repository and [syncStore] the sync engine; both read and write the same
 * connection, so they must not outlive it. [close] releases the database connection and is
 * idempotent, so a teardown race (or a defensive double close) cannot fail or double-free the
 * driver.
 */
class CampaignStoreHandle internal constructor(
    val store: CampaignStore,
    val syncStore: SyncStore,
    private val driver: AndroidSqliteDriver,
) : Closeable {
    private val closed = AtomicBoolean(false)

    override fun close() {
        if (closed.compareAndSet(false, true)) driver.close()
    }
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
        val database = KdmDatabase(driver)
        return CampaignStoreHandle(
            store = SqlDelightCampaignStore(database),
            syncStore = SqlDelightSyncStore(database),
            driver = driver,
        )
    }
}

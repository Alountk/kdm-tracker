package io.kdmtracker.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.kdmtracker.data.KdmDatabaseFactory
import io.kdmtracker.data.SqlDelightCampaignStore
import io.kdmtracker.data.db.KdmDatabase
import kotlinx.coroutines.flow.StateFlow

/**
 * Retains the lantern-year loop across recomposition and configuration changes.
 *
 * The [LanternYearController] is created once and held here, so a rotation or process re-creation
 * no longer resets the loop to year 1 / step 1. The ViewModel owns no rules: it exposes the
 * controller's [state] and forwards navigation. The [Companion.factory] builds the production
 * controller over the on-device SQLDelight database; tests inject a controller over a fake store.
 */
class LanternYearViewModel(
    private val controller: LanternYearController,
) : ViewModel() {
    val state: StateFlow<UiState> = controller.state

    fun advance() = controller.advance()

    fun failHunt() = controller.failHunt()

    fun previous() = controller.previous()

    fun selectStep(number: Int) = controller.selectStep(number)

    companion object {
        private const val DATABASE_NAME = "kdm-tracker.db"

        /** Creates the production ViewModel over the on-device database opened with [context]. */
        fun factory(context: Context): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val driver = AndroidSqliteDriver(KdmDatabase.Schema, context, DATABASE_NAME)
                    val database = KdmDatabaseFactory.create(driver)
                    val repository = SettlementRepository(SqlDelightCampaignStore(database))
                    LanternYearViewModel(LanternYearController(repository))
                }
            }
    }
}

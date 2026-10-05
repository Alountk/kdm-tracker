package io.kdmtracker.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.kdmtracker.data.CampaignStoreHandle
import io.kdmtracker.data.KdmDatabaseFactory
import kotlinx.coroutines.flow.StateFlow

/**
 * Retains the lantern-year loop across recomposition and configuration changes.
 *
 * The [LanternYearController] is created once and held here, so a rotation or process re-creation
 * no longer resets the loop to year 1 / step 1. The ViewModel owns no rules: it exposes the
 * controller's [state] and forwards navigation. The [Companion.factory] builds the production
 * controller over the on-device SQLDelight database and retains its [CampaignStoreHandle], which is
 * closed in [onCleared] so the connection and the application context are released. Tests inject a
 * controller over an in-memory store and no handle.
 */
class LanternYearViewModel(
    private val controller: LanternYearController,
    private val storeHandle: CampaignStoreHandle? = null,
) : ViewModel() {
    val state: StateFlow<UiState> = controller.state

    fun advance() = controller.advance()

    fun failHunt() = controller.failHunt()

    fun previous() = controller.previous()

    fun selectStep(number: Int) = controller.selectStep(number)

    override fun onCleared() {
        storeHandle?.close()
    }

    companion object {
        /** Creates the production ViewModel over the on-device database opened with [context]. */
        fun factory(context: Context): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val handle = KdmDatabaseFactory.openAndroidStore(context)
                    val repository = SettlementRepository(handle.store)
                    LanternYearViewModel(LanternYearController(repository), handle)
                }
            }
    }
}

package io.kdmtracker.app

import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the ViewModel's derivation of [UiState] from the controller it owns.
 *
 * The components are built on [Dispatchers.Unconfined] so the deferred load completes synchronously
 * during construction, which lets the test assert the first frame immediately. A true
 * configuration-change test (rotate the activity and assert the ViewModel survives) needs
 * instrumentation, which is unavailable in this environment; that scenario is deferred. These tests
 * pin the state contract the retained ViewModel exposes.
 */
class LanternYearViewModelTest {
    @Test
    fun initialStateIsTheOpeningSettlementStep() {
        val viewModel = viewModel()

        val state = viewModel.state.value

        assertEquals(Screen.Settlement, state?.screen)
        assertEquals(SettlementPhase.SETTLEMENT, state?.phase)
        assertEquals(1, state?.lanternYear)
        assertEquals(1, state?.currentStep)
    }

    @Test
    fun advancingUpdatesTheExposedState() {
        val viewModel = viewModel()

        viewModel.advance()

        assertEquals(2, viewModel.state.value?.currentStep)
    }

    private fun viewModel(): LanternYearViewModel =
        LanternYearViewModel(
            buildComponents = {
                AppComponents(LanternYearController(SettlementRepository(InMemoryCampaignStore())))
            },
            dispatcher = Dispatchers.Unconfined,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
}

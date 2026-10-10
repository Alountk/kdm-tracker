package io.kdmtracker.app

import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import io.kdmtracker.core.sync.CampaignSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the shell's navigation lifecycle: the picked tab must not outlive the campaign it was picked
 * in, the Account overlay sits above it, and a screen that has no tab is refused.
 */
class ShellNavigationTest {
    @Test
    fun aNewlyCreatedSettlementOpensOnItsPhasesTabNotTheLastOnePicked() {
        val viewModel = viewModel()
        viewModel.select(
            viewModel.homeCards.value
                .first()
                .campaignId,
        )
        viewModel.show(Screen.Hunt)
        assertEquals(Screen.Hunt, viewModel.state.value?.screen, "the picked tab must win over the phase")

        viewModel.goHome()
        viewModel.create("Second")

        assertEquals(
            Screen.Settlement,
            viewModel.state.value?.screen,
            "a new settlement is in the Settlement phase, so it must open on Asentamiento",
        )
    }

    @Test
    fun reEnteringACampaignOpensOnItsPhasesTab() {
        val viewModel = viewModel()
        val campaignId =
            viewModel.homeCards.value
                .first()
                .campaignId
        viewModel.select(campaignId)
        viewModel.show(Screen.Showdown)
        viewModel.goHome()

        viewModel.select(campaignId)

        assertEquals(Screen.Settlement, viewModel.state.value?.screen)
    }

    @Test
    fun theAccountOverlaySitsAboveThePickedTab() {
        val viewModel = viewModel()
        viewModel.select(
            viewModel.homeCards.value
                .first()
                .campaignId,
        )
        viewModel.show(Screen.Hunt)

        viewModel.goAccount()
        assertEquals(Screen.Account, viewModel.state.value?.screen)

        viewModel.closeAccount()
        assertEquals(Screen.Hunt, viewModel.state.value?.screen)
    }

    @Test
    fun aScreenWithoutATabIsRefused() {
        val viewModel = viewModel()
        viewModel.select(
            viewModel.homeCards.value
                .first()
                .campaignId,
        )
        val before = viewModel.state.value?.screen

        viewModel.show(Screen.Home)

        assertEquals(before, viewModel.state.value?.screen)
    }

    private fun viewModel(): LanternYearViewModel =
        LanternYearViewModel(
            buildComponents = {
                AppComponents(
                    LanternYearController(
                        SettlementRepository(
                            store = InMemoryCampaignStore(),
                            createCampaign = { name ->
                                CampaignSummary(Ulid.generate(1_000L) { 7 }.value, name, "OWNER", "", emptyList())
                            },
                        ),
                    ),
                )
            },
            dispatcher = Dispatchers.Unconfined,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
}

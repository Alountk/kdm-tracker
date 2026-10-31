package io.kdmtracker.app

import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Rules from the rulebook (KDM 1.5 ES) that the app does not follow yet. Each test states the rule;
 * they run once the behavior exists. The reasons are recorded in `docs/backlog.md`.
 */
class RulebookDivergencesTest {
    private val controller = LanternYearController(SettlementRepository(InMemoryCampaignStore()))

    private val state get() = controller.state.value

    private fun failTheHunt() {
        while (state.currentStep < 11) controller.flow.advance()
        controller.chooseTheDepartingParty()
        controller.flow.advance()
        controller.hunt.setUpHunt("White Lion", 1, 4)
        controller.flow.finishHunt(HuntStatus.FAILED)
        assertEquals(SettlementPhase.SETTLEMENT, state.phase)
    }

    @Ignore("backlog: Gain Endeavors counts the whole population, not the survivors that returned")
    @Test
    fun noEndeavorsAreEarnedWhenNoSurvivorReturnsFromTheHunt() {
        // Settlement phase, step 3: each survivor that returns earns one endeavor; a failed hunt
        // is a party wipe, so nobody returns.
        failTheHunt()

        while (state.currentStep < 3) controller.flow.advance()

        assertEquals(0, state.endeavors)
    }

    @Ignore("backlog: starvation is only a board space; it takes no resources from the storage")
    @Test
    fun aFailedHuntLosesBetweenOneAndFiveStoredResourcesToStarvation() {
        // Hunt phase, starvation: when the party wipes the hunt fails and starvation costs d5
        // resources from the settlement storage.
        controller.settlement.addResource(ResourceEntry(ResourceId("bone.monster"), 10))

        failTheHunt()

        val stored = state.storageLines.sumOf { it.quantity }
        assertTrue(stored in 5..9, "storage kept $stored of 10 resources")
    }
}

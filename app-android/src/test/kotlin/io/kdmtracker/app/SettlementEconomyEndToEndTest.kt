package io.kdmtracker.app

import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Drives the **default seeded** campaign end to end through the control that would have caught both
 * develop-economy criticals: entering Step 3 funds the endeavor pool, the ledger is funded through
 * the manual "record a resource" bridge, Innovate succeeds at the seeded (already-built) Lantern
 * Hoard, a location is built, an endeavor is spent, and every mutation persists to the store.
 *
 * Everything runs over the shipped `core-1.6` seed and the real [LanternYearController]; no test
 * catalog is injected, so the path a player takes is the path under test.
 */
class SettlementEconomyEndToEndTest {
    @Test
    fun theSeededCampaignFundsStepThreeThenInnovatesBuildsSpendsAndPersists() {
        val store = InMemoryCampaignStore()
        val controller = LanternYearController(SettlementRepository(store))

        controller.advance()
        assertEquals(2, controller.state.value.currentStep)
        assertEquals(0, controller.state.value.endeavors)

        controller.advance()
        assertEquals(3, controller.state.value.currentStep)
        assertEquals(4, controller.state.value.endeavors, "Step 3 funds the pool from the population")

        controller.previous()
        controller.selectStep(3)
        assertEquals(4, controller.state.value.endeavors, "a Step 3 revisit must not double-fund")

        controller.addResource(ResourceEntry("Bone", ResourceCategory.BASIC, 1))
        controller.addResource(ResourceEntry("Organ", ResourceCategory.BASIC, 1))
        controller.addResource(ResourceEntry("Hide", ResourceCategory.BASIC, 1))
        assertNull(controller.state.value.actionError, "the manual resource bridge records gains")

        controller.innovate("Language")
        assertNull(controller.state.value.actionError)
        val afterInnovate = controller.state.value.record
        assertTrue(
            afterInnovate!!
                .state.innovations.innovations
                .any { it.name == "Language" },
            "Innovate unlocks the innovation at the seeded, built Lantern Hoard",
        )
        assertEquals(3, afterInnovate.settlement.endeavors)

        controller.build("Skinnery")
        assertNull(controller.state.value.actionError)
        assertTrue(
            "Skinnery" in
                controller.state.value.record!!
                    .state.locations.built,
        )

        controller.spendEndeavor()
        assertNull(controller.state.value.actionError)
        assertEquals(
            2,
            controller.state.value.record!!
                .settlement.endeavors,
        )

        val persisted = SettlementRepository(store).record
        assertEquals(controller.state.value.record, persisted)
        assertTrue("Lantern Hoard" in persisted.state.locations.built)
        assertTrue("Skinnery" in persisted.state.locations.built)
        assertTrue(
            persisted.state.innovations.innovations
                .any { it.name == "Language" },
        )
    }
}

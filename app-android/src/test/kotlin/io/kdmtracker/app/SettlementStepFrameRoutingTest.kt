package io.kdmtracker.app

import io.kdmtracker.app.ui.settlement.StepFrameModel
import io.kdmtracker.core.rules.SettlementStepCatalog
import kotlin.test.Test
import kotlin.test.assertTrue

/** The frame must render for every step, including the ten whose bodies no design covers. */
class SettlementStepFrameRoutingTest {
    @Test
    fun everyStepRendersTheFrame() {
        SettlementStepCatalog.ALL.forEach { step ->
            assertTrue(
                StepFrameModel.of(step, SettlementStepCatalog.ALL).title.startsWith("Paso ${step.number}."),
                "step ${step.number} must carry its own heading",
            )
        }
    }
}

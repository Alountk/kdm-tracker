package io.kdmtracker.app

import io.kdmtracker.app.ui.settlement.StepFrameModel
import io.kdmtracker.core.rules.SettlementStepCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Pins the step frame's heading and its advance action: the design names the step and the next one,
 * and the last step must not promise a step that does not exist.
 */
class StepFrameTest {
    @Test
    fun theTitleCarriesTheStepNumberAndItsSpanishName() {
        val model = StepFrameModel.of(SettlementStepCatalog.ALL[6], SettlementStepCatalog.ALL)

        assertEquals("Paso 7. Desarrollo", model.title)
    }

    @Test
    fun theLastStepAdvancesWithoutNamingAStepTwelve() {
        val last = StepFrameModel.of(SettlementStepCatalog.ALL[10], SettlementStepCatalog.ALL)

        assertFalse(last.advanceLabel.contains("12"), "there is no step 12")
    }

    @Test
    fun aMiddleStepNamesTheNext() {
        val model = StepFrameModel.of(SettlementStepCatalog.ALL[6], SettlementStepCatalog.ALL)

        assertEquals("Avanzar al Paso 8: Preparar Supervivientes", model.advanceLabel)
    }

    @Test
    fun theFrameCarriesTheStepsOwnSummary() {
        val model = StepFrameModel.of(SettlementStepCatalog.ALL[6], SettlementStepCatalog.ALL)

        assertEquals(SettlementStepCatalog.ALL[6].summary, model.summary)
    }
}

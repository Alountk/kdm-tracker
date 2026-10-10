package io.kdmtracker.app.ui.settlement

import io.kdmtracker.app.ui.shell.PhaseCarouselModel
import io.kdmtracker.core.rules.SettlementStep

/**
 * The step frame's heading and its advance action.
 *
 * Pure: it derives everything from the step and its siblings, so the heading and the next-step label
 * can never disagree with the catalog.
 */
data class StepFrameModel(
    val number: Int,
    val title: String,
    val summary: String,
    val icon: String,
    val advanceLabel: String,
) {
    companion object {
        /** The label the last step shows instead of naming a step that does not exist. */
        const val CLOSE_PHASE_LABEL = "Avanzar: cerrar la fase"

        fun of(
            step: SettlementStep,
            steps: List<SettlementStep>,
        ): StepFrameModel {
            val next = steps.firstOrNull { it.number == step.number + 1 }
            return StepFrameModel(
                number = step.number,
                title = "Paso ${step.number}. ${step.title}",
                summary = step.summary,
                icon = PhaseCarouselModel.steps.firstOrNull { it.number == step.number }?.icon ?: "flare",
                advanceLabel =
                    if (next != null) {
                        "Avanzar al Paso ${next.number}: ${next.title}"
                    } else {
                        CLOSE_PHASE_LABEL
                    },
            )
        }
    }
}

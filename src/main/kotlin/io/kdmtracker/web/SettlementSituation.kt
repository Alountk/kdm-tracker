package io.kdmtracker.web

import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.SettlementPhase

/**
 * Situation presentation shared by the home cards and the settlement dashboard.
 *
 * Status chip mapping for the home situation cards:
 *   HUNT -> "Hunting"; SHOWDOWN -> "Showdown";
 *   SETTLEMENT: step 8 -> "Ready to depart", step 11 -> "Closing year",
 *   every other step (1-7 and 9-10) -> "In progress".
 */
object SettlementSituation {
    const val OFFICIAL_STEPS = 11

    fun phaseTone(phase: SettlementPhase): String =
        when (phase) {
            SettlementPhase.SETTLEMENT -> "accent"
            SettlementPhase.HUNT -> "success"
            SettlementPhase.SHOWDOWN -> "danger"
        }

    fun statusLabel(settlement: Settlement): String =
        when (settlement.phase) {
            SettlementPhase.HUNT -> "Hunting"
            SettlementPhase.SHOWDOWN -> "Showdown"
            SettlementPhase.SETTLEMENT ->
                when (settlement.currentStep) {
                    8 -> "Ready to depart"
                    11 -> "Closing year"
                    else -> "In progress"
                }
        }

    fun statusTone(settlement: Settlement): String =
        when (settlement.phase) {
            SettlementPhase.HUNT -> "success"
            SettlementPhase.SHOWDOWN -> "danger"
            SettlementPhase.SETTLEMENT ->
                when (settlement.currentStep) {
                    8 -> "info"
                    11 -> "accent"
                    else -> "warning"
                }
        }

    fun situationText(settlement: Settlement): String =
        when (settlement.phase) {
            SettlementPhase.SETTLEMENT -> "Step ${settlement.currentStep}/$OFFICIAL_STEPS"
            SettlementPhase.HUNT -> "Hunt in progress"
            SettlementPhase.SHOWDOWN -> "Showdown in progress"
        }
}

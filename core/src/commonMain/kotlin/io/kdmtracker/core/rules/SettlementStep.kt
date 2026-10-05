package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.StepType

/** One official Settlement Phase step. */
data class SettlementStep(
    val number: Int,
    val title: String,
    val summary: String,
    val type: StepType,
)

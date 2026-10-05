package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.SettlementPhase

/** A position in the lantern-year flow: a [phase] and its 1-based [step] number. */
data class PhaseStep(
    val phase: SettlementPhase,
    val step: Int,
)

package io.kdmtracker.domain

import io.kdmtracker.core.rules.SettlementStepCatalog

/** Classification of a settlement step (research §5: bookkeeping / decision / branching). */
enum class StepType { BOOKKEEPING, DECISION, BRANCHING }

/** One official Settlement Phase step. */
data class SettlementStep(
    val number: Int,
    val title: String,
    val summary: String,
    val type: StepType,
)

/**
 * The 11 official Settlement Phase steps in rulebook order.
 *
 * The content lives in `:core`'s [SettlementStepCatalog]; this object only maps it into the
 * server's own [SettlementStep] view types so the templates and tests keep their shape.
 */
object SettlementSteps {
    const val TOTAL = SettlementStepCatalog.TOTAL

    val ALL: List<SettlementStep> =
        SettlementStepCatalog.ALL.map {
            SettlementStep(it.number, it.title, it.summary, StepType.valueOf(it.type.name))
        }

    fun byNumber(number: Int): SettlementStep = ALL.first { it.number == number }
}

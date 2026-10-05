package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.StepType

/**
 * The 11 official Settlement Phase steps in rulebook order.
 *
 * Content condensed from `static/prototypes/research/lantern-year-steps.md` (§2 step list,
 * §5 UX classification) and copied here as plain English constants — the markdown is a
 * research artifact and is never parsed at runtime.
 */
object SettlementStepCatalog {
    const val TOTAL = 11

    val ALL: List<SettlementStep> =
        listOf(
            SettlementStep(
                1,
                "Set Up the Settlement",
                "Lay out the board, record sheets, decks, locations and principles; " +
                    "the Innovation Deck must contain exactly the unlocked consequences.",
                StepType.BOOKKEEPING,
            ),
            SettlementStep(
                2,
                "Survivors Return",
                "Heal injuries and clear tokens from returnees (survival and insanity do not refill), " +
                    "then apply principle and event return effects.",
                StepType.DECISION,
            ),
            SettlementStep(
                3,
                "Gain Endeavors",
                "+1 endeavor per returning survivor, plus bonuses granted by innovations and principles.",
                StepType.BOOKKEEPING,
            ),
            SettlementStep(
                4,
                "Update Timeline",
                "Advance the lantern year, draw and resolve the settlement event, " +
                    "then fire this year's story events (nemesis choice included).",
                StepType.BRANCHING,
            ),
            SettlementStep(
                5,
                "Update the Death Count",
                "Record every death; the first death-count update ever triggers the " +
                    "Principle: Death story event.",
                StepType.BRANCHING,
            ),
            SettlementStep(
                6,
                "Check Milestones",
                "Tick reached milestones in any order and resolve their story events " +
                    "(principles, Hooded Knight, Game Over).",
                StepType.BRANCHING,
            ),
            SettlementStep(
                7,
                "Develop",
                "Craft, endeavor, innovate and build — free-form and repeatable; " +
                    "close the step before departure.",
                StepType.DECISION,
            ),
            SettlementStep(
                8,
                "Prepare Departing Survivors",
                "End the session or pick the 4 departing survivors (fewer if population < 4) " +
                    "and load their gear grids.",
                StepType.DECISION,
            ),
            SettlementStep(
                9,
                "Special Showdown",
                "Only when the timeline lists one: fight it, then loop back to step 5 " +
                    "to update the death count.",
                StepType.BRANCHING,
            ),
            SettlementStep(
                10,
                "Record & Archive Resources",
                "Write unspent resources into settlement storage and archive the physical cards.",
                StepType.BOOKKEEPING,
            ),
            SettlementStep(
                11,
                "End Settlement Phase",
                "Discard unspent endeavors and commit: the settlement proceeds to the Hunt Phase.",
                StepType.BOOKKEEPING,
            ),
        )

    fun byNumber(number: Int): SettlementStep = ALL.first { it.number == number }
}

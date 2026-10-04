package io.kdmtracker.web

import io.kdmtracker.domain.Milestone
import io.kdmtracker.domain.MilestoneKey
import io.kdmtracker.domain.Principle
import io.kdmtracker.domain.PrincipleCategory

/** One milestone row in the step-6 panel: official metadata + saved flags + derived hint. */
data class MilestoneRow(
    val key: String,
    val title: String,
    val condition: String,
    val hint: String,
    val eligible: Boolean,
    val reached: Boolean,
    val resolved: Boolean,
)

/** One principle category group in the step-6 panel (two radio choices + chosen year). */
data class PrincipleGroup(
    val category: String,
    val label: String,
    val choices: List<String>,
    val savedChoice: String?,
    val year: Int,
)

/**
 * Read models for the step-6 panel (milestones + principles).
 *
 * The milestone hint is derived at render time from survivor/population/innovation data —
 * it is a hint only and never gates anything: milestones are ticked manually when reached.
 */
object StepSixViews {
    fun milestoneRows(
        saved: List<Milestone>,
        deadCount: Int,
        population: Int,
        innovationCount: Int,
    ): List<MilestoneRow> =
        MilestoneKey.entries.map { key ->
            val row = saved.firstOrNull { it.key == key.name }
            val (eligible, hint) = hintFor(key, deadCount, population, innovationCount)
            MilestoneRow(
                key = key.name,
                title = key.title,
                condition = key.condition,
                hint = hint,
                eligible = eligible,
                reached = row?.reached == true,
                resolved = row?.resolved == true,
            )
        }

    fun principleGroups(
        saved: List<Principle>,
        lanternYear: Int,
    ): List<PrincipleGroup> =
        PrincipleCategory.entries.map { category ->
            val row = saved.firstOrNull { it.category == category }
            PrincipleGroup(
                category = category.name,
                label = category.label,
                choices = category.choices,
                savedChoice = row?.choice,
                year = row?.chosenInYear ?: lanternYear,
            )
        }

    private fun hintFor(
        key: MilestoneKey,
        deadCount: Int,
        population: Int,
        innovationCount: Int,
    ): Pair<Boolean, String> =
        when (key) {
            MilestoneKey.PRINCIPLE_DEATH ->
                if (deadCount >= 1) {
                    true to "eligible now — $deadCount death(s)"
                } else {
                    false to "$deadCount death(s)"
                }
            // No birth tracking exists yet — say so instead of guessing.
            MilestoneKey.PRINCIPLE_NEW_LIFE -> false to "no data yet"
            MilestoneKey.PRINCIPLE_SOCIETY ->
                if (population >= 15) {
                    true to "eligible now — population $population"
                } else {
                    false to "population $population/15"
                }
            MilestoneKey.HOODED_KNIGHT ->
                if (innovationCount >= 5) {
                    true to "eligible now — $innovationCount innovations"
                } else {
                    false to "$innovationCount/5 innovations"
                }
            MilestoneKey.GAME_OVER ->
                if (population <= 0) {
                    true to "eligible now — population $population"
                } else {
                    false to "population $population"
                }
        }
}

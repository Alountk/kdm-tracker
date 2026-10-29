package io.kdmtracker.app.ui

import io.kdmtracker.app.MilestoneView
import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus

/**
 * One token per living survivor — the same `status != DEAD` basis
 * [io.kdmtracker.core.usecase.GainEndeavors] funds — labeled with the survivor's first name (the
 * first word of `name`). Dead survivors contribute nothing; departing ones still count (they have
 * not died).
 */
fun endeavorTokenNames(survivors: List<Survivor>): List<String> =
    survivors
        .filter { it.status != SurvivorStatus.DEAD }
        .map { firstName(it.name) }

private fun firstName(name: String): String = name.trim().substringBefore(' ').ifBlank { "Sin nombre" }

/** The `{n} Activos` meta chip — a milestone is active while reached and not yet resolved. */
fun activeMilestoneCount(views: List<MilestoneView>): Int = views.count { it.reached && !it.resolved }

/**
 * The derived per-card status chip. The design's own condition chips (`Pendiente (0 Bajas)`,
 * `Población ≥ 15`) have no model behind them, so the chip derives from state instead — a recorded
 * derivation.
 */
enum class MilestoneStatus(
    val chip: String,
) {
    PENDIENTE("Pendiente"),
    ALCANZADA("Alcanzada"),
    RESUELTA("Resuelta"),
}

/** The [MilestoneStatus] chip for one milestone: `Resuelta` > `Alcanzada` > `Pendiente`. */
fun milestoneStatus(
    reached: Boolean,
    resolved: Boolean,
): MilestoneStatus =
    when {
        resolved -> MilestoneStatus.RESUELTA
        reached -> MilestoneStatus.ALCANZADA
        else -> MilestoneStatus.PENDIENTE
    }

/**
 * The milestone card's title. The two milestones the design names use the design's own Spanish
 * titles (`PRINCIPLE_DEATH` → `Principio: Muerte`, `PRINCIPLE_SOCIETY` →
 * `Principio: Sociedad` — the same ids [milestoneGlyph] keys on); every other milestone keeps its
 * core description (undesigned content keeps its text — language selection is Track 2/B4).
 */
fun milestoneTitle(
    id: MilestoneId,
    description: String,
): String =
    when (id) {
        Core16Milestones.PRINCIPLE_DEATH -> "Principio: Muerte"
        Core16Milestones.PRINCIPLE_SOCIETY -> "Principio: Sociedad"
        else -> description
    }

/**
 * The milestone card's glyph. The design draws `bookmark_border` on its Death card and
 * `family_restroom` on its Society card; every milestone the design does not name keeps the
 * unnamed default.
 */
fun milestoneGlyph(id: MilestoneId): String =
    when (id) {
        Core16Milestones.PRINCIPLE_DEATH -> "bookmark_border"
        Core16Milestones.PRINCIPLE_SOCIETY -> "family_restroom"
        else -> "bookmark_border"
    }

/**
 * The design's flavor paragraph, verbatim, only for the two milestones the design names;
 * `null` renders no paragraph. The design names exactly those two cards — `Principio: Muerte` and
 * `Principio: Sociedad` (the latter's paragraph speaks of the first child being born, the design's
 * own Society/New-Life conflation), so the mapping follows the named cards and
 * `principle-new-life`, `hooded-knight` and `game-over` stay unnamed.
 */
fun milestoneFlavor(id: MilestoneId): String? =
    when (id) {
        Core16Milestones.PRINCIPLE_DEATH ->
            "Se activa automáticamente cuando se registre la primera muerte en el asentamiento. " +
                "El grupo deberá elegir una filosofía funeraria."
        Core16Milestones.PRINCIPLE_SOCIETY ->
            "Se activará cuando el primer niño nazca en el asentamiento."
        else -> null
    }

/**
 * The step-5 deaths row subtitle, rendered only while `deaths == 0` — the design has no copy
 * for a nonzero count, so it omits rather than invents.
 */
fun deathsRowSubtitle(deaths: Int): String? =
    if (deaths == 0) {
        "Ningún superviviente pereció ante el León Blanco."
    } else {
        null
    }

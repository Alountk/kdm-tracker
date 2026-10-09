package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus

/** The roster filter chips. [ALL] is the default. */
enum class SurvivorFilter { ALL, INJURED, DEPARTING, UNNAMED, DEAD }

/**
 * The survivors matching [filter].
 *
 * Every category filter is expressed through [survivorTone], so a chip can never show a survivor
 * whose dot contradicts it (a dead survivor is never under "Injured", a departing one never under
 * "Injured"/"Unnamed", etc.). A pure function so the filter is testable without Compose.
 */
fun filterSurvivors(
    survivors: List<Survivor>,
    filter: SurvivorFilter,
): List<Survivor> =
    when (filter) {
        SurvivorFilter.ALL -> survivors
        SurvivorFilter.DEPARTING -> survivors.filter { survivorTone(it) == SurvivorTone.DEPARTING }
        SurvivorFilter.DEAD -> survivors.filter { survivorTone(it) == SurvivorTone.DEAD }
        SurvivorFilter.UNNAMED -> survivors.filter { survivorTone(it) == SurvivorTone.UNNAMED }
        SurvivorFilter.INJURED -> survivors.filter { survivorTone(it) == SurvivorTone.INJURED }
    }

/** The status tone a survivor's dot uses. */
enum class SurvivorTone { ALIVE, DEPARTING, DEAD, INJURED, UNNAMED }

/** Whether the survivor carries any light or heavy wound. */
internal fun Survivor.isInjured(): Boolean = wounds.values.any { it.light || it.heavy }

/**
 * The dot tone for [survivor], with a fixed precedence: dead beats departing beats injured beats
 * unnamed beats plain alive. The filters key off this same function.
 */
fun survivorTone(survivor: Survivor): SurvivorTone =
    when {
        survivor.status == SurvivorStatus.DEAD -> SurvivorTone.DEAD
        survivor.status == SurvivorStatus.DEPARTING -> SurvivorTone.DEPARTING
        survivor.isInjured() -> SurvivorTone.INJURED
        survivor.name.isBlank() -> SurvivorTone.UNNAMED
        else -> SurvivorTone.ALIVE
    }

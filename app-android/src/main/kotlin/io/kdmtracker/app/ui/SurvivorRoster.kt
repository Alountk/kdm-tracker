package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus

/** The roster filter chips. [ALL] is the default. */
enum class SurvivorFilter { ALL, INJURED, DEPARTING, UNNAMED, DEAD }

/**
 * The survivors matching [filter].
 *
 * `INJURED` is any location with a light or heavy wound; `UNNAMED` is a blank name; the rest key off
 * the survivor status. A pure function so the filter is testable without Compose.
 */
fun filterSurvivors(
    survivors: List<Survivor>,
    filter: SurvivorFilter,
): List<Survivor> =
    when (filter) {
        SurvivorFilter.ALL -> survivors
        SurvivorFilter.DEPARTING -> survivors.filter { it.status == SurvivorStatus.DEPARTING }
        SurvivorFilter.DEAD -> survivors.filter { it.status == SurvivorStatus.DEAD }
        SurvivorFilter.UNNAMED -> survivors.filter { it.name.isBlank() }
        SurvivorFilter.INJURED -> survivors.filter { survivor -> survivor.wounds.values.any { it.light || it.heavy } }
    }

/** The status tone a survivor's dot uses. */
enum class SurvivorTone { ALIVE, DEPARTING, DEAD, INJURED, UNNAMED }

/**
 * The dot tone for [survivor], with a fixed precedence: dead beats departing beats injured beats
 * unnamed beats plain alive.
 */
fun survivorTone(survivor: Survivor): SurvivorTone =
    when {
        survivor.status == SurvivorStatus.DEAD -> SurvivorTone.DEAD
        survivor.status == SurvivorStatus.DEPARTING -> SurvivorTone.DEPARTING
        survivor.wounds.values.any { it.light || it.heavy } -> SurvivorTone.INJURED
        survivor.name.isBlank() -> SurvivorTone.UNNAMED
        else -> SurvivorTone.ALIVE
    }

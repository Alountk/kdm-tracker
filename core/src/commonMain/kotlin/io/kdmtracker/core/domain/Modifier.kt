package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/** Where a [Modifier] applies: a survivor's stats, or the settlement's. */
enum class ModifierScope { SURVIVOR, SETTLEMENT }

/** What a [Modifier] changes. */
enum class ModifierTarget {
    STRENGTH,
    SPEED,
    ACCURACY,
    EVASION,
    LUCK,
    MOVEMENT,
    SURVIVAL,
    ARMOR,
    INSANITY,
    SURVIVAL_LIMIT,
    STORAGE,
    ENDEAVORS,
}

/**
 * A structured bonus a piece of content grants (gear now; innovations and other cards later).
 *
 * Data only: a [ModifierScope.SETTLEMENT] modifier feeds a derived value; a
 * [ModifierScope.SURVIVOR] modifier is stored and displayed, and applies once a Survivor
 * model exists.
 *
 * @property condition optional display text (e.g. "while insane"); never evaluated.
 */
@Serializable
data class Modifier(
    val target: ModifierTarget,
    val value: Int,
    val scope: ModifierScope = ModifierScope.SURVIVOR,
    val condition: String? = null,
)

package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.WoundState

/**
 * The sheet's controls as pure survivor transforms. Every one clamps to the range the ruleset
 * allows; the screen only has to call `onSave` with the result.
 */
object SurvivorSheetEdits {
    /** Adds [delta] to survival, staying inside `0..limit`. */
    fun survival(
        survivor: Survivor,
        limit: Int,
        delta: Int,
    ): Survivor = survivor.copy(survival = (survivor.survival + delta).coerceIn(0, limit.coerceAtLeast(0)))

    /** Adds [delta] to insanity, never below zero. */
    fun insanity(
        survivor: Survivor,
        delta: Int,
    ): Survivor = survivor.copy(insanity = (survivor.insanity + delta).coerceAtLeast(0))

    /** Adds [delta] to hunt XP, staying inside `0..max`. */
    fun huntXp(
        survivor: Survivor,
        max: Int,
        delta: Int,
    ): Survivor = survivor.copy(huntXp = (survivor.huntXp + delta).coerceIn(0, max))

    /** Adds [delta] to a hit location's armour, never below zero. */
    fun armor(
        survivor: Survivor,
        location: HitLocation,
        delta: Int,
    ): Survivor = survivor.withWound(location) { it.copy(armor = (it.armor + delta).coerceAtLeast(0)) }

    /** Flips a hit location's light (herida firme) box. */
    fun toggleLight(
        survivor: Survivor,
        location: HitLocation,
    ): Survivor = survivor.withWound(location) { it.copy(light = !it.light) }

    /** Flips a hit location's heavy (herida grave) box. */
    fun toggleHeavy(
        survivor: Survivor,
        location: HitLocation,
    ): Survivor = survivor.withWound(location) { it.copy(heavy = !it.heavy) }

    /** Adds or removes a survival token. */
    fun toggleToken(
        survivor: Survivor,
        token: String,
    ): Survivor =
        survivor.copy(
            tokens = if (token in survivor.tokens) survivor.tokens - token else survivor.tokens + token,
        )

    private fun Survivor.withWound(
        location: HitLocation,
        transform: (WoundState) -> WoundState,
    ): Survivor {
        val wound = wounds[location] ?: WoundState()
        return copy(wounds = wounds + (location to transform(wound)))
    }
}

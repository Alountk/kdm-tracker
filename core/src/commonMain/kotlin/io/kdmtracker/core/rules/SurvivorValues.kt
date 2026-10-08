package io.kdmtracker.core.rules

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.Attributes
import io.kdmtracker.core.domain.ModifierScope
import io.kdmtracker.core.domain.ModifierTarget
import io.kdmtracker.core.domain.Survivor

/**
 * Derives a survivor's effective attribute values from the printed sheet and their assigned gear.
 *
 * Nothing here is stored: the printed [Attributes] are the base and the catalog's gear modifiers the
 * survivor-scope gear bonuses. A settlement-scope modifier with the same target is ignored (it feeds
 * a settlement derived value instead), so the two scopes never cross.
 */
object SurvivorValues {
    /**
     * The effective value of [target] for [survivor]: the printed attribute plus the
     * survivor-scope gear modifiers of [survivor]'s assigned gear in [catalog].
     *
     * A target that is not one of the six attributes has no survivor base (returns 0 plus any
     * survivor-scope modifier).
     */
    fun effectiveAttribute(
        survivor: Survivor,
        target: ModifierTarget,
        catalog: ContentCatalog,
    ): Int =
        base(survivor.attributes, target) +
            DerivedValues.modifierTotal(catalog.gearModifiersFor(survivor.gear), target, ModifierScope.SURVIVOR)

    private fun base(
        attributes: Attributes,
        target: ModifierTarget,
    ): Int =
        when (target) {
            ModifierTarget.STRENGTH -> attributes.strength
            ModifierTarget.SPEED -> attributes.speed
            ModifierTarget.ACCURACY -> attributes.accuracy
            ModifierTarget.EVASION -> attributes.evasion
            ModifierTarget.LUCK -> attributes.luck
            ModifierTarget.MOVEMENT -> attributes.movement
            else -> 0
        }
}

package io.kdmtracker.core.rules

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.GearRecipe
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.usecase.Craft

/**
 * Whether a recipe can be crafted right now, and if not, why.
 *
 * @property missing a human-readable reason when [craftable] is false; `null` when it is.
 */
data class CraftCheck(
    val craftable: Boolean,
    val missing: String? = null,
)

/**
 * Derives a recipe's craftability by asking [Craft] directly, so the badge can never drift from the
 * action: `craftable` is exactly "`Craft.run` returns `Ok`", and `missing` is its `Blocked` reason.
 */
object Craftability {
    fun check(
        record: SettlementRecord,
        locationName: String,
        recipe: GearRecipe,
        catalog: ContentCatalog,
    ): CraftCheck =
        when (val result = Craft.run(record, locationName, recipe.gearName, catalog)) {
            is StepResult.Ok -> CraftCheck(craftable = true)
            is StepResult.Blocked -> CraftCheck(craftable = false, missing = result.reason)
        }
}

package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Crafts one gear at a built settlement location during the Develop step.
 *
 * Enforces, in order: the location exists and offers a recipe for [gearName]; the location
 * is built; the recipe's [io.kdmtracker.core.domain.GearRecipe.requires] innovations are
 * unlocked; storage holds the recipe's resources. A failure returns [StepResult.Blocked]
 * and leaves the record unchanged; success spends the resources (via the ledger guard) and
 * adds the gear to the settlement.
 */
object Craft {
    fun run(
        record: SettlementRecord,
        locationName: String,
        gearName: String,
        catalog: ContentCatalog,
    ): StepResult<SettlementRecord> {
        val location =
            catalog.locationDefinitions.firstOrNull { it.name == locationName }
                ?: return StepResult.Blocked("unknown location: $locationName")
        val recipe =
            location.recipes.firstOrNull { it.gearName == gearName }
                ?: return StepResult.Blocked("$locationName cannot craft $gearName")
        if (locationName !in record.state.locations.built) {
            return StepResult.Blocked("$locationName is not built")
        }
        val unlocked =
            record.state.innovations.innovations
                .mapTo(mutableSetOf()) { it.name }
        recipe.requires.firstOrNull { it !in unlocked }?.let { missing ->
            return StepResult.Blocked("$gearName requires $missing")
        }

        val categoryByName = catalog.resourceDefinitions.associate { it.name to it.category }
        var spent = record
        for ((resource, count) in recipe.resourceCost) {
            val category = categoryByName[resource] ?: return StepResult.Blocked("unknown resource: $resource")
            when (val result = AddResourceEntry.run(spent, ResourceEntry(resource, category, -count))) {
                is StepResult.Ok -> spent = result.value
                is StepResult.Blocked -> return result
            }
        }
        return StepResult.Ok(spent.copy(state = spent.state.withGear(gearName)))
    }
}

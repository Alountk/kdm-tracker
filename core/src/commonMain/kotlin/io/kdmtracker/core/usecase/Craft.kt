package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.LocationId
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
        locationId: LocationId,
        gearName: String,
        catalog: ContentCatalog,
    ): StepResult<SettlementRecord> {
        val location =
            catalog.locationDefinitions.firstOrNull { it.id == locationId }
                ?: return StepResult.Blocked("unknown location: ${locationId.value}")
        val displayName = location.names.es
        val recipe =
            location.recipes.firstOrNull { it.gearName == gearName }
                ?: return StepResult.Blocked("$displayName cannot craft $gearName")
        if (locationId !in record.state.locations.built) {
            return StepResult.Blocked("$displayName is not built")
        }
        // Recipes still name their prerequisite innovations (the gear commit re-keys them to
        // ids); match against either language so a Spanish recipe unlocks on a seed innovation.
        val unlocked =
            record.state.innovations.innovations
                .flatMapTo(mutableSetOf()) { innovation -> listOfNotNull(innovation.names.es, innovation.names.en) }
        recipe.requires.firstOrNull { it !in unlocked }?.let { missing ->
            return StepResult.Blocked("$gearName requires $missing")
        }

        var spent = record
        for ((resource, count) in recipe.resourceCost) {
            val definition =
                catalog.resourceDefinitionFor(resource)
                    ?: return StepResult.Blocked("unknown resource: $resource")
            when (val result = AddResourceEntry.run(spent, ResourceEntry(definition.id, -count))) {
                is StepResult.Ok -> spent = result.value
                is StepResult.Blocked -> return result
            }
        }
        return StepResult.Ok(spent.copy(state = spent.state.withGear(gearName)))
    }
}

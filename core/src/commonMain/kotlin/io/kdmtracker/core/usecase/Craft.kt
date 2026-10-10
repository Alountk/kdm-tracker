package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Crafts one gear at a built settlement location during the Develop step.
 *
 * Enforces, in order: the location exists and offers a recipe for [gearId]; the location
 * is built; the recipe's [io.kdmtracker.core.domain.GearRecipe.requires] innovations are
 * unlocked; storage holds the recipe's resources. A failure returns [StepResult.Blocked]
 * and leaves the record unchanged; success spends the resources (via the ledger guard) and
 * adds the gear to the settlement.
 *
 * A successful craft records the gear's stable id in the settlement's crafted-gear list.
 */
object Craft {
    fun run(
        record: SettlementRecord,
        locationId: LocationId,
        gearId: GearId,
        catalog: ContentCatalog,
    ): StepResult<SettlementRecord> {
        val location =
            catalog.locationDefinitions.firstOrNull { it.id == locationId }
                ?: return StepResult.Blocked("unknown location: ${locationId.value}")
        val gearName =
            catalog.gearDefinitions
                .firstOrNull { it.id == gearId }
                ?.names
                ?.es ?: gearId.value
        val displayName = location.names.es
        val recipe =
            location.recipes.firstOrNull { it.gearId == gearId }
                ?: return StepResult.Blocked("$displayName cannot craft $gearName")
        if (locationId !in record.state.locations.built) {
            return StepResult.Blocked("$displayName is not built")
        }
        val unlocked =
            record.state.innovations.innovations
                .mapTo(mutableSetOf()) { it.id }
        recipe.requires.firstOrNull { it !in unlocked }?.let { missing ->
            val missingName =
                catalog.innovationDefinitions
                    .firstOrNull { it.id == missing }
                    ?.names
                    ?.es ?: missing.value
            return StepResult.Blocked("$gearName requires $missingName")
        }

        var spent = record
        for ((resourceId, count) in recipe.resourceCost) {
            val definition =
                catalog.resourceDefinitions.firstOrNull { it.id == resourceId }
                    ?: return StepResult.Blocked("unknown resource: ${resourceId.value}")
            when (val result = AddResourceEntry.run(spent, ResourceEntry(definition.id, -count))) {
                is StepResult.Ok -> spent = result.value
                is StepResult.Blocked -> return result
            }
        }
        return StepResult.Ok(spent.copy(state = spent.state.withGear(gearId)))
    }
}

package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Builds a settlement location by paying its cost during the Settlement Phase.
 *
 * Building is one transaction: every precondition is checked before any state changes, so
 * a refusal leaves [record] untouched. The location's prerequisites must already be built,
 * its endeavor cost must fit the settlement pool, and its resource cost must fit the
 * ledger — each resource is spent through [AddResourceEntry], whose guard refuses to
 * overdraw. The location's prerequisites, cost, and endeavor price are content read from
 * the [ContentCatalog], never literals here.
 */
object BuildLocation {
    /**
     * Returns [StepResult.Ok] with the advanced record, or [StepResult.Blocked] with the
     * reason the location could not be built.
     *
     * Blocks, without changing anything, when: [locationName] is unknown, the location is
     * already built, one of its `requires` is unmet, the endeavor pool cannot fund its
     * `buildEndeavors`, a cost resource has no catalog definition, or the ledger cannot
     * cover a spend. On success it spends the cost, subtracts the endeavors, and adds the
     * location to [io.kdmtracker.core.domain.SettlementState.locations].
     *
     * @param locationName the name of the location to build.
     */
    fun run(
        record: SettlementRecord,
        locationName: String,
        catalog: ContentCatalog,
    ): StepResult<SettlementRecord> {
        val location =
            catalog.locationDefinitions.firstOrNull { it.name == locationName }
                ?: return StepResult.Blocked("unknown location $locationName")
        if (locationName in record.state.locations.built) {
            return StepResult.Blocked("$locationName already built")
        }
        if (!record.state.locations.canBuild(location)) {
            return StepResult.Blocked("$locationName requires an unbuilt location")
        }
        if (record.settlement.endeavors < location.buildEndeavors) {
            return StepResult.Blocked("insufficient endeavors")
        }

        var spent = record
        for ((name, count) in location.buildCost) {
            val definition =
                catalog.resourceDefinitionFor(name)
                    ?: return StepResult.Blocked("unknown resource $name")
            spent =
                when (val result = AddResourceEntry.run(spent, ResourceEntry(name, definition.category, -count))) {
                    is StepResult.Ok -> result.value
                    is StepResult.Blocked -> return result
                }
        }

        return StepResult.Ok(
            spent.copy(
                settlement =
                    spent.settlement.copy(
                        endeavors = spent.settlement.endeavors - location.buildEndeavors,
                    ),
                state =
                    spent.state.copy(
                        locations = spent.state.locations.with(location),
                    ),
            ),
        )
    }
}

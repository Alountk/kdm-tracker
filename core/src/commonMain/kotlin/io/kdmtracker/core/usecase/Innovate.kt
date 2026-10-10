package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.StepResult

/**
 * Runs the Innovate action a settlement location offers during the Settlement Phase.
 *
 * Innovate unlocks one named innovation at a location that grants it: the Lantern Hoard
 * (and its campaign variants) offers the action once per Settlement Phase for an endeavor
 * plus a resource cost. This use case enforces the whole rule as one transaction — every
 * precondition is checked before any state changes, so a refusal leaves [record] untouched.
 *
 * The action's cost is content, never a literal here: its [SettlementRecord.settlement]
 * endeavor pool funds [io.kdmtracker.core.domain.SettlementAction.endeavorCost], and each
 * resource in [io.kdmtracker.core.domain.SettlementAction.resourceCost] is spent through
 * [AddResourceEntry], which refuses to overdraw the ledger. The resource's sheet category is
 * resolved from [ContentCatalog.resourceDefinitions], so the ledger entry carries content
 * truth rather than a guessed category.
 */
object Innovate {
    /**
     * Returns [StepResult.Ok] with the advanced record, or [StepResult.Blocked] with the
     * reason the action could not run.
     *
     * Blocks, without changing anything, when: [locationName] is unknown, the location offers
     * no [ActionKind.INNOVATE] action, the location is not yet built, the location's action was
     * already used this phase, [innovationName] is unknown, the endeavor pool cannot fund the
     * action, a cost resource has no catalog definition, or the ledger cannot cover a spend. On
     * success it spends the cost, unlocks the innovation, and marks the location's action used
     * for the phase.
     *
     * @param locationName the name of the settlement location whose Innovate action runs.
     * @param innovationName the name of the innovation to unlock.
     */
    fun run(
        record: SettlementRecord,
        locationName: String,
        innovationName: String,
        catalog: ContentCatalog,
    ): StepResult<SettlementRecord> {
        val location =
            catalog.locationDefinitions.firstOrNull { it.name == locationName }
                ?: return StepResult.Blocked("unknown location $locationName")
        val action =
            location.actions.firstOrNull { it.kind == ActionKind.INNOVATE }
                ?: return StepResult.Blocked("$locationName offers no Innovate action")
        if (locationName !in record.state.locations.built) {
            return StepResult.Blocked("$locationName is not built")
        }
        if (record.state.usedInPhase(locationName)) {
            return StepResult.Blocked("$locationName already used this phase")
        }
        val innovation =
            catalog.innovationDefinitions.firstOrNull { it.name == innovationName }
                ?: return StepResult.Blocked("unknown innovation $innovationName")
        if (record.settlement.endeavors < action.endeavorCost) {
            return StepResult.Blocked("insufficient endeavors")
        }

        var spent = record
        for ((name, count) in action.resourceCost) {
            val definition =
                catalog.resourceDefinitionFor(name)
                    ?: return StepResult.Blocked("unknown resource $name")
            spent =
                when (val result = AddResourceEntry.run(spent, ResourceEntry(definition.id, -count))) {
                    is StepResult.Ok -> result.value
                    is StepResult.Blocked -> return result
                }
        }

        return StepResult.Ok(
            spent.copy(
                settlement =
                    spent.settlement.copy(
                        endeavors = spent.settlement.endeavors - action.endeavorCost,
                    ),
                state =
                    spent.state
                        .withPhaseAction(locationName)
                        .copy(innovations = spent.state.innovations.with(innovation)),
            ),
        )
    }
}

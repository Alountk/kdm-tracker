package io.kdmtracker.core.usecase

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.InnovationAccess
import io.kdmtracker.core.rules.InnovationStatus
import io.kdmtracker.core.rules.StepResult

/**
 * Runs the Innovate action a settlement location offers during the Settlement Phase.
 *
 * Innovate unlocks one named innovation at a location that grants it: the Lantern Hoard
 * (and its campaign variants) offers the action once per Settlement Phase for an endeavor
 * plus a resource cost. This use case enforces the whole rule as one transaction — every
 * precondition is checked before any state changes, so a refusal leaves [record] untouched.
 *
 * Access is one of those preconditions: [innovationId] must be [InnovationStatus.AVAILABLE] in
 * the settlement's unlock tree — in the deck, not yet unlocked and not walled behind an
 * unreached parent — because that is exactly the set a draw can offer.
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
     * Blocks, without changing anything, when: [locationId] is unknown, the location offers
     * no [ActionKind.INNOVATE] action, the location is not yet built, the location's action was
     * already used this phase, [innovationId] is unknown, [innovationId] is not
     * [InnovationStatus.AVAILABLE] (already unlocked, or absent from the deck), the endeavor pool
     * cannot fund the action, a cost resource has no catalog definition, or the ledger cannot
     * cover a spend. On
     * success it spends the cost, unlocks the innovation, and marks the location's action used
     * for the phase.
     *
     * @param locationId the id of the settlement location whose Innovate action runs.
     * @param innovationId the id of the innovation to unlock.
     */
    fun run(
        record: SettlementRecord,
        locationId: LocationId,
        innovationId: InnovationId,
        catalog: ContentCatalog,
    ): StepResult<SettlementRecord> {
        val location =
            catalog.locationDefinitions.firstOrNull { it.id == locationId }
                ?: return StepResult.Blocked("unknown location ${locationId.value}")
        val displayName = location.names.es
        val action =
            location.actions.firstOrNull { it.kind == ActionKind.INNOVATE }
                ?: return StepResult.Blocked("$displayName offers no Innovate action")
        if (locationId !in record.state.locations.built) {
            return StepResult.Blocked("$displayName is not built")
        }
        if (record.state.usedInPhase(locationId.value)) {
            return StepResult.Blocked("$displayName already used this phase")
        }
        val innovation =
            catalog.innovationDefinitions.firstOrNull { it.id == innovationId }
                ?: return StepResult.Blocked("unknown innovation ${innovationId.value}")
        val status =
            InnovationAccess
                .view(record.state, catalog)
                .firstOrNull { it.innovation.id == innovationId }
                ?.status
        when (status) {
            InnovationStatus.AVAILABLE -> Unit
            InnovationStatus.UNLOCKED ->
                return StepResult.Blocked("innovation ${innovation.names.es} is already unlocked")
            InnovationStatus.NO_ACCESS, null ->
                return StepResult.Blocked("innovation ${innovation.names.es} is not available to innovate")
        }
        if (record.settlement.endeavors < action.endeavorCost) {
            return StepResult.Blocked("insufficient endeavors")
        }

        var spent = record
        for ((resourceId, count) in action.resourceCost) {
            val definition =
                catalog.resourceDefinitions.firstOrNull { it.id == resourceId }
                    ?: return StepResult.Blocked("unknown resource ${resourceId.value}")
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
                        .withPhaseAction(locationId.value)
                        .copy(
                            innovations =
                                spent.state.innovations.with(
                                    innovation.copy(unlockedInYear = record.settlement.lanternYear),
                                ),
                        ),
            ),
        )
    }
}

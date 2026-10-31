package io.kdmtracker.app

import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.usecase.AddResourceEntry
import io.kdmtracker.core.usecase.BuildLocation
import io.kdmtracker.core.usecase.ChoosePrinciple
import io.kdmtracker.core.usecase.Craft
import io.kdmtracker.core.usecase.DiscardForStarvation
import io.kdmtracker.core.usecase.Innovate
import io.kdmtracker.core.usecase.RecordBirths
import io.kdmtracker.core.usecase.RecordDeaths
import io.kdmtracker.core.usecase.RecordSettlementEvent
import io.kdmtracker.core.usecase.ResolveMilestone
import io.kdmtracker.core.usecase.SpendEndeavor

/** Settlement Phase actions: develop (innovate, build, craft), bookkeeping and principles. */
internal class SettlementActions(
    session: ControllerSession,
) : FeatureActions(session) {
    /**
     * Innovates at the first built location offering [ActionKind.INNOVATE]; with none, it surfaces a
     * reason instead of silently doing nothing.
     */
    fun innovate(innovationName: String) {
        val innovationId = innovationIdFor(innovationName)
        if (innovationId == null) {
            publishBlocked("unknown innovation $innovationName")
            return
        }
        val location =
            content.locationDefinitions.firstOrNull { location ->
                location.actions.any { it.kind == ActionKind.INNOVATE } &&
                    location.id in repository.record.state.locations.built
            }
        if (location == null) {
            publishBlocked("no built settlement location offers Innovate")
            return
        }
        applyResult(Innovate.run(repository.record, location.id, innovationId, content))
    }

    fun build(locationName: String) {
        val locationId = locationIdFor(locationName)
        if (locationId == null) {
            publishBlocked("unknown location $locationName")
            return
        }
        applyResult(BuildLocation.run(repository.record, locationId, content))
    }

    /** Resolves a displayed name (either language) to its stable id through the held [content]. */
    private fun locationIdFor(locationName: String): LocationId? =
        content.locationDefinitions.firstOrNull { it.names.es == locationName || it.names.en == locationName }?.id

    private fun innovationIdFor(innovationName: String): InnovationId? =
        content.innovationDefinitions.firstOrNull { it.names.es == innovationName || it.names.en == innovationName }?.id

    /** The action's effect is resolved by the players; only the pool is decremented. */
    fun spendEndeavor() {
        applyResult(SpendEndeavor.run(repository.record))
    }

    /** Pays one unit of the starvation debt with the resource the player chose. */
    fun discardForStarvation(id: ResourceId) {
        applyResult(DiscardForStarvation.run(repository.record, id))
    }

    fun addResource(entry: ResourceEntry) {
        applyResult(AddResourceEntry.run(repository.record, entry))
    }

    fun craft(
        locationName: String,
        gearName: String,
    ) {
        val locationId = locationIdFor(locationName)
        if (locationId == null) {
            publishBlocked("unknown location $locationName")
            return
        }
        val gearId = gearIdFor(gearName)
        if (gearId == null) {
            publishBlocked("unknown gear $gearName")
            return
        }
        applyResult(Craft.run(repository.record, locationId, gearId, content))
    }

    private fun gearIdFor(gearName: String): GearId? =
        content.gearDefinitions.firstOrNull { it.names.es == gearName || it.names.en == gearName }?.id

    fun recordDeaths(count: Int) {
        applyResult(RecordDeaths.run(repository.record, count))
    }

    fun recordBirths(count: Int) {
        applyResult(RecordBirths.run(repository.record, count, newSurvivorId))
    }

    fun choosePrinciple(
        category: PrincipleCategory,
        label: String,
    ) {
        applyResult(ChoosePrinciple.run(repository.record, category, label, ruleset))
    }

    fun resolveMilestone(id: MilestoneId) {
        applyResult(ResolveMilestone.run(repository.record, id, ruleset))
    }

    /** The name must be one the catalog ships. */
    fun recordSettlementEvent(name: String) {
        applyResult(RecordSettlementEvent.run(repository.record, name, content))
    }
}

package io.kdmtracker.app

import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import io.kdmtracker.core.usecase.AssignGear
import io.kdmtracker.core.usecase.MarkDeparting
import io.kdmtracker.core.usecase.ReturnSurvivors
import io.kdmtracker.core.usecase.UnassignGear
import io.kdmtracker.core.usecase.UpdateSurvivor

/** Survivor roster actions: healing, departing party, gear and edits. */
internal class SurvivorActions(
    session: ControllerSession,
) : FeatureActions(session) {
    /** Step 2: erases light/heavy injuries and clears tokens. */
    fun healSurvivors() {
        applyResult(StepResult.Ok(ReturnSurvivors.run(repository.record)))
    }

    /** Step 8; capped at the ruleset's departing limit. */
    fun markDeparting(ids: List<Ulid>) {
        applyResult(MarkDeparting.run(repository.record, ids, ruleset.departingLimit))
    }

    /** Step 8; takes from the settlement's crafted gear. */
    fun assignGear(
        survivorId: Ulid,
        gearName: String,
    ) {
        applyResult(AssignGear.run(repository.record, survivorId, gearName))
    }

    fun unassignGear(
        survivorId: Ulid,
        gearName: String,
    ) {
        applyResult(UnassignGear.run(repository.record, survivorId, gearName))
    }

    fun updateSurvivor(survivor: Survivor) {
        applyResult(UpdateSurvivor.run(repository.record, survivor))
    }
}

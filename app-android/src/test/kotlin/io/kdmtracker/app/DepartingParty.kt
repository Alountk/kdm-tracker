package io.kdmtracker.app

import io.kdmtracker.core.domain.SurvivorStatus

/** Step 8 for tests that only need a legal commit out of step 11: up to four living survivors depart. */
internal fun LanternYearController.chooseTheDepartingParty() {
    val living = state.value.survivors.filter { it.status != SurvivorStatus.DEAD }
    survivors.markDeparting(living.take(4).map { it.id })
}

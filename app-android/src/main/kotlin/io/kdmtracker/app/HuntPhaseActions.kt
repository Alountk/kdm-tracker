package io.kdmtracker.app

import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.usecase.AdvanceHunt
import io.kdmtracker.core.usecase.AssignHuntEvent
import io.kdmtracker.core.usecase.AssignPlayer
import io.kdmtracker.core.usecase.EditHuntRoll
import io.kdmtracker.core.usecase.MarkSpaceKind
import io.kdmtracker.core.usecase.MoveMonster
import io.kdmtracker.core.usecase.ResolveHuntEvent
import io.kdmtracker.core.usecase.RetreatMonster
import io.kdmtracker.core.usecase.SetUpHunt

/** Hunt board actions: setup, party and beast movement, events and rolls. */
internal class HuntPhaseActions(
    session: ControllerSession,
) : FeatureActions(session) {
    fun setUpHunt(
        quarry: String,
        level: Int,
        players: Int,
    ) {
        applyResult(SetUpHunt.run(repository.record, quarry, level, players))
    }

    fun markSpaceKind(
        index: Int,
        kind: HuntSpaceKind,
    ) {
        applyResult(MarkSpaceKind.run(repository.record, index, kind))
    }

    fun advanceHunt() {
        val quarry = repository.record.state.hunt.quarry
        val deck = quarry?.let { content.huntEventsFor(it) }.orEmpty()
        applyResult(AdvanceHunt.run(repository.record, deck = deck))
    }

    /** Steps the monster toward the party, ambushing it on contact. */
    fun moveMonster() {
        applyResult(MoveMonster.run(repository.record))
    }

    /** A hunt-event retreat: steps the monster away from the party. */
    fun retreatMonster() {
        applyResult(RetreatMonster.run(repository.record))
    }

    /** [player] is 1-based. */
    fun assignPlayer(
        survivorId: Ulid,
        player: Int,
    ) {
        applyResult(AssignPlayer.run(repository.record, survivorId, player))
    }

    fun assignHuntEvent(
        index: Int,
        eventName: String,
    ) {
        applyResult(AssignHuntEvent.run(repository.record, index, eventName))
    }

    fun resolveHuntEvent(
        index: Int,
        eventName: String,
    ) {
        applyResult(ResolveHuntEvent.run(repository.record, index, eventName))
    }

    /** Manual override of a step's stored 2d10 roll. */
    fun editHuntRoll(
        index: Int,
        roll: Int,
    ) {
        applyResult(EditHuntRoll.run(repository.record, index, roll))
    }
}

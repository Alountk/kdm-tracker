package io.kdmtracker.app

import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.usecase.DrawShowdownCard
import io.kdmtracker.core.usecase.HealMonster
import io.kdmtracker.core.usecase.RecordHuntXp
import io.kdmtracker.core.usecase.SetSpecialShowdown
import io.kdmtracker.core.usecase.SetUpShowdown
import io.kdmtracker.core.usecase.WoundMonster

/** Showdown actions: encounter setup, AI deck, wounds and hunt XP. */
internal class ShowdownActions(
    session: ControllerSession,
) : FeatureActions(session) {
    fun declareSpecialShowdown(enabled: Boolean) {
        applyResult(SetSpecialShowdown.run(repository.record, enabled))
    }

    fun setUpShowdown(
        monster: String,
        level: Int,
        deck: List<String>,
    ) {
        applyResult(SetUpShowdown.run(repository.record, monster, level, deck.shuffled()))
    }

    fun drawShowdownCard() {
        applyResult(DrawShowdownCard.run(repository.record))
    }

    /** Moves the top AI card to the wound stack. */
    fun woundMonster() {
        applyResult(WoundMonster.run(repository.record))
    }

    /** Returns the last wound to the top of the AI deck. */
    fun healMonster() {
        applyResult(HealMonster.run(repository.record))
    }

    /** +1 hunt XP per listed survivor. */
    fun grantHuntXp(survivorIds: List<Ulid>) {
        applyResult(RecordHuntXp.run(repository.record, survivorIds))
    }
}

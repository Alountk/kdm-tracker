package io.kdmtracker.app.ui.settlement

import io.kdmtracker.app.UiState
import io.kdmtracker.core.content.ContentIdentity
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.rules.InnovationDraw

/**
 * Which of the reference's states the Innovaciones section is in.
 *
 * The order matters: [of] resolves exactly one, highest first, so a section that has already
 * spent its action this phase never also claims it is out of cards or short of resources.
 */
enum class PhaseState { OFFER, USED_THIS_PHASE, NO_CARDS, UNAFFORDABLE }

/** One thing the Innovate action needs, and how much of it the settlement holds. */
data class ResourceNeed(
    val name: String,
    val have: Int,
    val need: Int,
) {
    val ok: Boolean get() = have >= need
}

/** What the action costs, whether the settlement can pay it, and the reference's `Motivo:` sentence. */
data class CostView(
    val endeavors: ResourceNeed,
    val resources: List<ResourceNeed>,
    val ok: Boolean,
    val reason: String,
)

/** One card of the drawn hand, built from the catalog definition rather than the persisted state. */
data class InnovationCard(
    val id: InnovationId,
    val name: String,
    val category: String,
    val subtitle: String,
    val effect: String,
    val survivalBonus: Int,
    val consequenceNote: String?,
)

/**
 * The Innovaciones panel's values, derived from one [UiState] frame.
 *
 * [drawLabel] and [principleSlots] keep their Phase 1 behaviour (ruleset text, chosen labels
 * passed through unchanged); everything else is the reference's five states.
 */
data class InnovationsModel(
    val deckSize: Int,
    val principleSlots: List<String>,
    val emptySlots: Int,
    val drawLabel: String,
    val hand: List<InnovationCard>,
    val phaseState: PhaseState,
    val cost: CostView,
    val categoryChips: List<Pair<String, Int>>,
    val acquisitionChips: List<Pair<String, Int>>,
) {
    companion object {
        /** The design's three slots: the chosen principle plus two to discover. */
        const val SLOT_COUNT = 3

        fun of(state: UiState): InnovationsModel {
            val definitions = state.innovationDefinitions
            val unlocked =
                state.record
                    ?.state
                    ?.innovations
                    ?.innovations
                    ?.mapTo(mutableSetOf()) { it.id } ?: mutableSetOf()

            val hand =
                InnovationDraw
                    .of(
                        state.record
                            ?.settlement
                            ?.campaignId
                            ?.value,
                        state.innovationViews,
                        state.innovationDraw,
                    ).map { card -> cardOf(state, card) }

            val action = state.innovationViews.firstOrNull()
            val endeavors = ResourceNeed(name = "Esfuerzo", have = state.endeavors, need = action?.endeavorCost ?: 0)
            val resources =
                (action?.resourceCost ?: emptyMap()).map { (id, need) ->
                    ResourceNeed(
                        name =
                            state.resourceDefinitions
                                .firstOrNull { it.id == id }
                                ?.names
                                ?.es ?: id.value,
                        have = state.storageLines.firstOrNull { it.id == id }?.quantity ?: 0,
                        need = need,
                    )
                }
            val affordable = endeavors.ok && resources.all { it.ok }
            val cost =
                CostView(
                    endeavors = endeavors,
                    resources = resources,
                    ok = affordable,
                    reason = if (affordable) "" else reasonFor(endeavors, resources),
                )

            val phaseState =
                when {
                    state.innovationViews.any { it.used } -> PhaseState.USED_THIS_PHASE
                    hand.isEmpty() -> PhaseState.NO_CARDS
                    !cost.ok -> PhaseState.UNAFFORDABLE
                    else -> PhaseState.OFFER
                }

            val acquired = definitions.count { it.id in unlocked }
            return InnovationsModel(
                deckSize = state.innovationDeck.size,
                principleSlots = state.chosenPrinciples.values.toList(),
                emptySlots = (SLOT_COUNT - state.chosenPrinciples.size).coerceAtLeast(0),
                drawLabel = "Robar ${state.innovationDraw}, elegir ${state.innovationKeep}",
                hand = hand,
                phaseState = phaseState,
                cost = cost,
                categoryChips =
                    definitions
                        .groupingBy { it.category }
                        .eachCount()
                        .toList()
                        .sortedBy { (label, _) -> label },
                acquisitionChips =
                    listOf(
                        "Todas" to definitions.size,
                        "Adquiridas" to acquired,
                        "No adquiridas" to definitions.size - acquired,
                    ),
            )
        }

        /** The reference's `Motivo:` sentence: the whole price, named, not only the shortfall. */
        private fun reasonFor(
            endeavors: ResourceNeed,
            resources: List<ResourceNeed>,
        ): String {
            val price = listOf("${endeavors.need} ${endeavors.name}") + resources.map { "${it.need} ${it.name}" }
            return "No es posible seleccionar una innovación hasta tener disponibles en el asentamiento: " +
                price.joinToString(" + ")
        }

        private fun cardOf(
            state: UiState,
            card: Innovation,
        ): InnovationCard {
            val parent =
                state.innovationDefinitions.firstOrNull { candidate ->
                    candidate.consequences.any { ContentIdentity.innovationIdFor(it) == card.id }
                }
            return InnovationCard(
                id = card.id,
                name = card.names.es,
                category = card.category,
                subtitle = if (parent == null) "innovación inicial" else "consecuencia de ${parent.names.es}",
                effect = card.description,
                survivalBonus = card.survivalLimitBonus,
                consequenceNote =
                    if (card.consequences.isEmpty()) {
                        null
                    } else {
                        "Añade consecuencias de ${card.names.es} al mazo"
                    },
            )
        }
    }
}

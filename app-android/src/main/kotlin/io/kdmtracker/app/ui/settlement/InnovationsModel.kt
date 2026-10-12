package io.kdmtracker.app.ui.settlement

import io.kdmtracker.app.UiState
import io.kdmtracker.core.content.ContentIdentity
import io.kdmtracker.core.domain.ActionKind
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

/**
 * Whether the Innovar action is reachable at all, affordable, or neither — the design's outer gate,
 * decided before anything else in the section is drawn.
 */
enum class InnovarGate { NO_ACCESS, SHORT_ON_RESOURCES, READY }

/**
 * How a milestone-grid cell got there. The board paints the three differently: gold and
 * `ACTIVO PERMANENTE` for [OWNED], green with `NUEVA` for [NEW_THIS_YEAR], dashed grey for [EMPTY].
 */
enum class SlotState {
    OWNED,
    NEW_THIS_YEAR,
    EMPTY,
    ;

    companion object {
        /**
         * Gold when it was already there, green when it arrived this lantern year. `0` means the
         * document predates the field and reads as "already owned", never as "new".
         */
        fun of(
            unlockedInYear: Int,
            lanternYear: Int,
        ): SlotState = if (unlockedInYear == 0 || unlockedInYear < lanternYear) OWNED else NEW_THIS_YEAR
    }
}

/**
 * The design's minified price for the draw row — the effort count and the resource initials,
 * as `1H 1Ó 1P`. [resources] holds the initials; [abbreviated] is what the row prints.
 */
data class CostChip(
    val endeavors: Int,
    val resources: List<String>,
    val abbreviated: String,
)

/** One filled cell of the design's 2x2 milestone grid. */
data class SlotView(
    val name: String,
    val state: SlotState,
)

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

/** The location whose [ActionKind.INNOVATE] action is feeding this draw — the reference's `MAZO ACTIVO` slot. */
data class ActiveDeck(
    val name: String,
    val note: String,
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
    val gate: InnovarGate,
    val missing: List<ResourceNeed>,
    val motivo: String,
    val slots: List<SlotView>,
    val costChip: CostChip,
    val categoryChips: List<Pair<String, Int>>,
    val acquisitionChips: List<Pair<String, Int>>,
    val activeDeck: ActiveDeck?,
    val offerTitle: String,
    val ruleLine: String,
    val lanternYear: Int,
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

            val innovatingLocation =
                state.catalogLocations
                    .firstOrNull { location ->
                        location.names.es in state.builtLocations &&
                            location.actions.any { it.kind == ActionKind.INNOVATE }
                    }
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
            val gate =
                when {
                    innovatingLocation == null -> InnovarGate.NO_ACCESS
                    !cost.ok -> InnovarGate.SHORT_ON_RESOURCES
                    else -> InnovarGate.READY
                }
            val hand =
                if (gate == InnovarGate.READY) {
                    InnovationDraw
                        .of(
                            state.record
                                ?.settlement
                                ?.campaignId
                                ?.value,
                            state.innovationViews,
                            state.innovationDraw,
                        ).map { card -> cardOf(state, card) }
                } else {
                    emptyList()
                }

            val phaseState =
                when {
                    state.innovationViews.any { it.used } -> PhaseState.USED_THIS_PHASE
                    !cost.ok -> PhaseState.UNAFFORDABLE
                    hand.isEmpty() -> PhaseState.NO_CARDS
                    else -> PhaseState.OFFER
                }

            val acquired = definitions.count { it.id in unlocked }
            val draw = state.innovationDraw
            val keep = state.innovationKeep
            val slots =
                state.record
                    ?.state
                    ?.innovations
                    ?.innovations
                    ?.map {
                        SlotView(
                            name = it.names.es,
                            state = SlotState.of(it.unlockedInYear, state.lanternYear),
                        )
                    } ?: emptyList()
            return InnovationsModel(
                deckSize = state.innovationDeck.size,
                principleSlots = state.chosenPrinciples.values.toList(),
                emptySlots = (SLOT_COUNT - state.chosenPrinciples.size).coerceAtLeast(0),
                drawLabel = "Robar ${state.innovationDraw}, elegir ${state.innovationKeep}",
                hand = hand,
                phaseState = phaseState,
                cost = cost,
                gate = gate,
                missing = (listOf(cost.endeavors) + cost.resources).filter { !it.ok },
                motivo = cost.reason,
                slots = slots,
                costChip = costChip(endeavors, resources),
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
                activeDeck =
                    innovatingLocation?.let { location ->
                        ActiveDeck(name = location.names.es, note = "Robando $draw cartas...")
                    },
                offerTitle = "Innovar: Elige $keep de $draw Cartas",
                ruleLine = ruleLine(draw, keep),
                lanternYear = state.lanternYear,
            )
        }

        /** The minified price: `1` plus the first letter of each costed resource. */
        private fun costChip(
            endeavors: ResourceNeed,
            resources: List<ResourceNeed>,
        ): CostChip {
            fun initial(name: String) = name.take(1).uppercase()
            val head = listOf("${endeavors.need}${resources.firstOrNull()?.let { initial(it.name) } ?: ""}")
            val tail = resources.drop(1).map { "${it.need}${initial(it.name)}" }
            return CostChip(
                endeavors = endeavors.need,
                resources = resources.map { initial(it.name) },
                abbreviated = (head + tail).joinToString(" "),
            )
        }

        /**
         * The reference's rule line, with the numbers the ruleset actually holds so a house rule
         * never leaves a stale "2" on screen. `la otra` only reads correctly while one card is kept.
         */
        private fun ruleLine(
            draw: Int,
            keep: Int,
        ): String {
            val tail = if (keep == 1) "la otra" else "el resto"
            return "Regla oficial KDM: Roba $draw innovaciones, quédate $keep y devuelve $tail al mazo."
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

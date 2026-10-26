package io.kdmtracker.app

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.domain.InnovationDeck
import io.kdmtracker.core.domain.ModifierScope
import io.kdmtracker.core.domain.ModifierTarget
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.rules.Craftability
import io.kdmtracker.core.rules.DerivedValues
import io.kdmtracker.core.rules.FlowContext
import io.kdmtracker.core.rules.InnovationAccess
import io.kdmtracker.core.rules.LanternYearFlow
import io.kdmtracker.core.rules.PhaseStep
import io.kdmtracker.core.ruleset.Ruleset

/** Builds the [UiState] the screens render from the held record, the ruleset and the content catalog. */
internal class UiStateMapper(
    private val ruleset: Ruleset,
    private val contentProvider: () -> ContentCatalog,
    private val catalogError: () -> String?,
) {
    private val content: ContentCatalog get() = contentProvider()

    fun map(
        record: SettlementRecord,
        lastTransition: String?,
    ): UiState {
        val settlement = record.settlement
        val from = PhaseStep(settlement.phase, settlement.currentStep)
        val context = FlowContext(ruleset = ruleset)
        return UiState(
            screen = settlement.phase.toScreen(),
            lanternYear = settlement.lanternYear,
            phase = settlement.phase,
            settlementName = settlement.name,
            currentStep = settlement.currentStep,
            stepTitle =
                ruleset.settlementSteps
                    .firstOrNull { it.number == settlement.currentStep }
                    ?.title
                    .orEmpty(),
            population = settlement.population,
            endeavors = settlement.endeavors,
            steps = ruleset.settlementSteps,
            reachableSteps =
                ruleset.settlementSteps
                    .filter { LanternYearFlow.canEnterStep(it.number, from, context) }
                    .map { it.number }
                    .toSet(),
            lastTransition = lastTransition,
            record = record,
            catalogLocations = content.locationDefinitions,
            resourceDefinitions = content.resourceDefinitions,
            innovationDefinitions = content.innovationDefinitions,
            storageLines = record.storage(),
            endeavorBreakdown = DerivedValues.endeavorsFor(settlement.population, ruleset),
            catalogError = catalogError(),
            innovationDeck = InnovationDeck.of(record.state.innovations).cards,
            builtLocations =
                record.state.locations.built.map { locationId ->
                    content.locationDefinitions
                        .firstOrNull { it.id == locationId }
                        ?.names
                        ?.es ?: locationId.value
                },
            gear =
                record.state.gear.map { gearId ->
                    content.gearDefinitions
                        .firstOrNull { it.id == gearId }
                        ?.names
                        ?.es ?: gearId.value
                },
            survivalLimit =
                record.survivalLimit(ruleset) +
                    DerivedValues.modifierTotal(
                        content.gearModifiersFor(record.state.gear.map { it.value }),
                        ModifierTarget.SURVIVAL_LIMIT,
                        ModifierScope.SETTLEMENT,
                    ),
            huntXpTrackMax = ruleset.huntXpTrackMax,
            insanityThreshold = ruleset.insanityThreshold,
            innovationDraw = ruleset.innovationDraw,
            innovationKeep = ruleset.innovationKeep,
            resourceGroups = content.resourceGroups(),
            innovationViews = InnovationAccess.view(record.state, content),
            craftRecipes =
                record.state.locations.built.flatMap { locationId ->
                    val location = content.locationDefinitions.firstOrNull { it.id == locationId }
                    location
                        ?.recipes
                        .orEmpty()
                        .map { recipe ->
                            CraftRecipeView(
                                location?.names?.es ?: locationId.value,
                                recipe,
                                Craftability.check(record, locationId, recipe, content),
                                gearType(
                                    content.gearDefinitions
                                        .firstOrNull { it.id == recipe.gearId }
                                        ?.keywords
                                        .orEmpty(),
                                ),
                                gearName =
                                    content.gearDefinitions
                                        .firstOrNull { it.id == recipe.gearId }
                                        ?.names
                                        ?.es
                                        ?: recipe.gearId.value,
                            )
                        }
                },
            settlementEventDeck = content.settlementEventDefinitions,
            settlementEvent = settlement.settlementEvent,
            deaths = record.state.deaths,
            births = record.state.births,
            survivors = record.state.survivors,
            hunt = record.state.hunt,
            showdown = record.state.showdown,
            specialShowdown = record.state.specialShowdown,
            nemesis = record.currentNemesis(),
            monsterCards = content.monsterCardDefinitions,
            quarries = content.quarries,
            nemeses = content.nemeses,
            huntEvents =
                record.state.hunt.quarry
                    ?.let { content.huntEventsFor(it) }
                    .orEmpty(),
            chosenPrinciples = record.state.principles,
            milestoneViews =
                ruleset.milestones.map { definition ->
                    MilestoneView(
                        id = definition.id,
                        description = definition.description,
                        reached = definition.id in record.reachedMilestones(ruleset),
                        resolved = definition.id in record.state.resolvedMilestones,
                        choices =
                            Core16Milestones
                                .principleCategoryOf(definition.id)
                                ?.let { ruleset.principles[it] }
                                .orEmpty(),
                    )
                },
            principles =
                ruleset.principles.entries
                    .sortedBy { it.key.name }
                    .flatMap { (_, choices) -> choices.map { it.label } },
            yearEvents =
                content.timeline
                    .filter { it.lanternYear == settlement.lanternYear }
                    .map { it.label } +
                    listOfNotNull(settlement.settlementEvent),
        )
    }
}

/** A gear's coarse type from its keywords, for grouping the Fabricar tab by type. */
private fun gearType(keywords: List<String>): String =
    when {
        "arma" in keywords -> "Armas"
        "armadura" in keywords -> "Armaduras"
        "objeto" in keywords -> "Objetos"
        "consumible" in keywords -> "Consumibles"
        else -> "Otros"
    }

private fun SettlementPhase.toScreen(): Screen =
    when (this) {
        SettlementPhase.SETTLEMENT -> Screen.Settlement
        SettlementPhase.HUNT -> Screen.Hunt
        SettlementPhase.SHOWDOWN -> Screen.Showdown
    }

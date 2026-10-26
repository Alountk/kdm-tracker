package io.kdmtracker.app

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.InnovationDeck
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.ModifierScope
import io.kdmtracker.core.domain.ModifierTarget
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.Craftability
import io.kdmtracker.core.rules.DerivedValues
import io.kdmtracker.core.rules.FlowContext
import io.kdmtracker.core.rules.FlowTransition
import io.kdmtracker.core.rules.InnovationAccess
import io.kdmtracker.core.rules.LanternYearFlow
import io.kdmtracker.core.rules.PhaseStep
import io.kdmtracker.core.rules.StepResult
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.Ruleset
import io.kdmtracker.core.usecase.AddResourceEntry
import io.kdmtracker.core.usecase.AdvanceHunt
import io.kdmtracker.core.usecase.AdvanceStep
import io.kdmtracker.core.usecase.AdvanceTimeline
import io.kdmtracker.core.usecase.AssignGear
import io.kdmtracker.core.usecase.AssignHuntEvent
import io.kdmtracker.core.usecase.AssignPlayer
import io.kdmtracker.core.usecase.BuildLocation
import io.kdmtracker.core.usecase.ChoosePrinciple
import io.kdmtracker.core.usecase.ClearShowdown
import io.kdmtracker.core.usecase.Craft
import io.kdmtracker.core.usecase.DrawShowdownCard
import io.kdmtracker.core.usecase.EditHuntRoll
import io.kdmtracker.core.usecase.GainEndeavors
import io.kdmtracker.core.usecase.Innovate
import io.kdmtracker.core.usecase.MarkDeparting
import io.kdmtracker.core.usecase.MarkSpaceKind
import io.kdmtracker.core.usecase.MoveMonster
import io.kdmtracker.core.usecase.RecordBirths
import io.kdmtracker.core.usecase.RecordDeaths
import io.kdmtracker.core.usecase.RecordHuntXp
import io.kdmtracker.core.usecase.RecordSettlementEvent
import io.kdmtracker.core.usecase.ResolveHuntEvent
import io.kdmtracker.core.usecase.ResolveMilestone
import io.kdmtracker.core.usecase.RetreatMonster
import io.kdmtracker.core.usecase.ReturnSurvivors
import io.kdmtracker.core.usecase.SetHuntOutcome
import io.kdmtracker.core.usecase.SetShowdownOutcome
import io.kdmtracker.core.usecase.SetSpecialShowdown
import io.kdmtracker.core.usecase.SetUpHunt
import io.kdmtracker.core.usecase.SetUpShowdown
import io.kdmtracker.core.usecase.SpendEndeavor
import io.kdmtracker.core.usecase.UnassignGear
import io.kdmtracker.core.usecase.UpdateSurvivor
import io.kdmtracker.core.usecase.WoundMonster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Mirrors `:core` results into a [UiState] for the UI. Plain Kotlin (no Android APIs) so it runs in
 * JVM tests.
 */
class LanternYearController(
    private val repository: SettlementRepository,
    private val ruleset: Ruleset = Core16Ruleset,
    private val catalog: ContentCatalog? = null,
    private val newSurvivorId: () -> Ulid = { Ulid.generate(System.currentTimeMillis()) { Random.nextInt() } },
) {
    /** The injected [catalog] (tests) or the repository's composed one. */
    private val content: ContentCatalog get() = catalog ?: repository.catalog

    private val _state =
        MutableStateFlow(uiState(repository.record, lastTransition = null))

    val state: StateFlow<UiState> = _state.asStateFlow()

    fun cards(): List<CampaignCard> = repository.listCards()

    /** Returns `false` and changes nothing if the id is unknown. */
    fun selectCampaign(campaignId: String): Boolean {
        if (!repository.select(campaignId)) return false
        reload()
        return true
    }

    /** A hub failure escapes unchanged and the prior selection is kept. */
    fun createCampaign(name: String): CampaignCard {
        val card = repository.create(name)
        reload()
        return card
    }

    fun advance() {
        val context = flowContext()
        applyStep(
            returnees = repository.settlement.population,
            context = context,
        )
        if (context.hasSpecialShowdown) {
            // The special showdown is resolved: clear it and the encounter so the phase continues
            // normally instead of looping at step 9 forever and reusing the encounter.
            applyResult(SetSpecialShowdown.run(repository.record, false))
            applyResult(ClearShowdown.run(repository.record))
        }
    }

    /** A nemesis year lists a nemesis in its timeline; a declared Special Showdown loops step 9 back. */
    private fun flowContext(): FlowContext =
        FlowContext(
            ruleset = ruleset,
            isNemesisYear = nemesisFor(repository.record) != null,
            hasSpecialShowdown = repository.record.state.specialShowdown,
        )

    fun nemesisFor(record: SettlementRecord): String? = record.nemesisOf(record.settlement.lanternYear)

    fun declareSpecialShowdown(enabled: Boolean) {
        applyResult(SetSpecialShowdown.run(repository.record, enabled))
    }

    /** No survivors return from a failed hunt, so the restart gets zero returnees. */
    fun failHunt() {
        applyStep(
            returnees = 0,
            context = FlowContext(ruleset = ruleset, huntFailed = true),
        )
    }

    /**
     * The one path [advance], [failHunt] and [finishHunt] take. Returnees are explicit so a total party
     * kill can express "0 returning survivors" instead of reusing the population.
     */
    private fun applyStep(
        returnees: Int,
        context: FlowContext,
    ) {
        val settlement = repository.settlement
        val fromStep = settlement.currentStep
        val result =
            AdvanceStep.run(
                settlement = settlement,
                returnedSurvivors = returnees,
                context = context,
            )
        val current = repository.record
        val moved =
            if (result.transition.landsOnNewYear(settlement.phase)) {
                current.copy(
                    settlement = result.settlement,
                    state = current.state.copy(phaseActionsUsed = emptySet()),
                )
            } else {
                current.copy(settlement = result.settlement)
            }
        repository.update(fundGainEndeavors(advanceTimelineIfNeeded(fromStep, moved)))
        _state.value = uiState(repository.record, result.transition.describe())
    }

    /** No-op outside the settlement screen; the step is floored at 1. */
    fun previous() {
        val settlement = repository.settlement
        if (settlement.phase != SettlementPhase.SETTLEMENT || settlement.currentStep <= 1) return
        moveToStep(settlement.currentStep - 1)
    }

    /** Gated by [LanternYearFlow.canEnterStep]: no skipping forward, revisiting earlier steps is fine. */
    fun selectStep(number: Int) {
        val settlement = repository.settlement
        val from = PhaseStep(settlement.phase, settlement.currentStep)
        if (!LanternYearFlow.canEnterStep(number, from, FlowContext(ruleset = ruleset))) return
        moveToStep(number)
    }

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

    /** +1 hunt XP per listed survivor. */
    fun grantHuntXp(survivorIds: List<Ulid>) {
        applyResult(RecordHuntXp.run(repository.record, survivorIds))
    }

    /** Records the outcome, then advances out of the Showdown. */
    fun finishShowdown(status: ShowdownStatus) {
        when (val result = SetShowdownOutcome.run(repository.record, status)) {
            is StepResult.Ok -> {
                repository.update(result.value)
                advance()
            }
            is StepResult.Blocked -> publishBlocked(result.reason)
        }
    }

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

    /** Found or ambushed goes to the Showdown; failed or run away re-enters the Settlement Phase. */
    fun finishHunt(outcome: HuntStatus) {
        when (val result = SetHuntOutcome.run(repository.record, outcome)) {
            is StepResult.Ok -> {
                repository.update(result.value)
                when (outcome) {
                    // Found or ambushed: the showdown begins.
                    HuntStatus.QUARRY_FOUND, HuntStatus.AMBUSH ->
                        applyStep(
                            returnees = repository.settlement.population,
                            context = FlowContext(ruleset = ruleset),
                        )
                    // The hunt ends without a showdown: a party wipe (no returnees) or a run away
                    // (the survivors return); both re-enter the Settlement Phase.
                    HuntStatus.FAILED ->
                        applyStep(returnees = 0, context = FlowContext(ruleset = ruleset, huntFailed = true))
                    HuntStatus.RUN_AWAY ->
                        applyStep(
                            returnees = repository.settlement.population,
                            context = FlowContext(ruleset = ruleset, huntFailed = true),
                        )
                    HuntStatus.ACTIVE -> publishBlocked("invalid outcome")
                }
            }
            is StepResult.Blocked -> publishBlocked(result.reason)
        }
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

    /** Ok is persisted and republished; Blocked leaves the record untouched and surfaces its reason. */
    private fun applyResult(result: StepResult<SettlementRecord>) {
        when (result) {
            is StepResult.Ok -> {
                repository.update(result.value)
                _state.value = uiState(repository.record, _state.value.lastTransition)
            }
            is StepResult.Blocked -> publishBlocked(result.reason)
        }
    }

    private fun publishBlocked(reason: String) {
        _state.value = _state.value.copy(actionError = reason)
    }

    /**
     * A sync pass mutates the store behind the controller's back; without this the UI keeps rendering
     * the pre-sync frame.
     */
    fun reload() {
        repository.reload()
        _state.value = uiState(repository.record, _state.value.lastTransition)
    }

    private fun moveToStep(step: Int) {
        val fromStep = repository.settlement.currentStep
        val moved = repository.record.copy(settlement = repository.settlement.copy(currentStep = step))
        repository.update(fundGainEndeavors(advanceTimelineIfNeeded(fromStep, moved)))
        _state.value = uiState(repository.record, _state.value.lastTransition)
    }

    /**
     * Fires only when leaving Step 4 forward. [AdvanceTimeline] is idempotent per phase, so revisiting
     * Step 4 cannot double-increment.
     */
    private fun advanceTimelineIfNeeded(
        fromStep: Int,
        record: SettlementRecord,
    ): SettlementRecord =
        if (fromStep == UPDATE_TIMELINE_STEP && record.settlement.currentStep > UPDATE_TIMELINE_STEP) {
            AdvanceTimeline.run(record)
        } else {
            record
        }

    /**
     * Funds the pool once per Settlement Phase on entering Step 3; [GAIN_ENDEAVORS_MARKER] stops a
     * revisit from funding twice.
     */
    private fun fundGainEndeavors(record: SettlementRecord): SettlementRecord {
        val settlement = record.settlement
        if (settlement.phase != SettlementPhase.SETTLEMENT || settlement.currentStep != GAIN_ENDEAVORS_STEP) {
            return record
        }
        if (record.state.usedInPhase(GAIN_ENDEAVORS_MARKER)) return record
        val gained = GainEndeavors.run(record, returnedSurvivors = settlement.population, ruleset = ruleset)
        return gained.copy(state = gained.state.withPhaseAction(GAIN_ENDEAVORS_MARKER))
    }

    private fun uiState(
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
            catalogError = repository.catalogError,
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
            nemesis = nemesisFor(record),
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

private fun FlowTransition.describe(): String =
    when (this) {
        is FlowTransition.Advance -> "Advance → ${to.phase} ${to.step}"
        is FlowTransition.LoopBack -> "LoopBack → ${to.phase} ${to.step}"
        is FlowTransition.Commit -> "Commit → ${to.phase} ${to.step}"
        is FlowTransition.Blocked -> "Blocked: $reason"
    }

/**
 * Whether this transition out of [from] opens a fresh year, resetting once-per-phase actions.
 *
 * Scoped to the **source** phase: a mid-year move (even a hypothetical `Commit` from Hunt or
 * Showdown) keeps the spent actions.
 */
internal fun FlowTransition.landsOnNewYear(from: SettlementPhase): Boolean =
    when (this) {
        is FlowTransition.Commit -> from == SettlementPhase.SETTLEMENT
        is FlowTransition.Advance ->
            from != SettlementPhase.SETTLEMENT && to == PhaseStep(SettlementPhase.SETTLEMENT, 1)
        is FlowTransition.LoopBack ->
            from != SettlementPhase.SETTLEMENT && to == PhaseStep(SettlementPhase.SETTLEMENT, 1)
        is FlowTransition.Blocked -> false
    }

private const val GAIN_ENDEAVORS_STEP = 3

private const val UPDATE_TIMELINE_STEP = 4

/** The phase marker recording that Step 3 funded the pool, so a revisit cannot double-fund it. */
private const val GAIN_ENDEAVORS_MARKER = "Gain Endeavors"

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
import io.kdmtracker.core.usecase.GainEndeavors
import io.kdmtracker.core.usecase.Innovate
import io.kdmtracker.core.usecase.MarkDeparting
import io.kdmtracker.core.usecase.MarkSpaceKind
import io.kdmtracker.core.usecase.RecordBirths
import io.kdmtracker.core.usecase.RecordDeaths
import io.kdmtracker.core.usecase.RecordHuntXp
import io.kdmtracker.core.usecase.RecordSettlementEvent
import io.kdmtracker.core.usecase.ResolveHuntEvent
import io.kdmtracker.core.usecase.ResolveMilestone
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
 * Drives the lantern-year loop for the UI.
 *
 * This class is deliberately plain Kotlin: it holds no Android APIs so it can be unit-tested on
 * the JVM. Every phase/step transition is delegated to `:core` ([AdvanceStep] over
 * [io.kdmtracker.core.rules.LanternYearFlow]); the controller only mirrors the resulting
 * [Settlement] into a [UiState] and maps the phase to a [Screen]. No business rules live here.
 */
class LanternYearController(
    private val repository: SettlementRepository,
    private val ruleset: Ruleset = Core16Ruleset,
    private val catalog: ContentCatalog? = null,
    private val newSurvivorId: () -> Ulid = { Ulid.generate(System.currentTimeMillis()) { Random.nextInt() } },
) {
    /**
     * The content the step actions read.
     *
     * Production leaves [catalog] null so the repository's composed catalog is used; a JVM test can
     * inject a composed catalog so the Step 7 use-cases run against a controlled pack. This is
     * content plumbing only — every rule stays in `:core`.
     */
    private val content: ContentCatalog get() = catalog ?: repository.catalog

    private val _state =
        MutableStateFlow(uiState(repository.record, lastTransition = null))

    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Every stored campaign as a Home card, joined with its sharing badge (Ruling AJ/AK). */
    fun cards(): List<CampaignCard> = repository.listCards()

    /**
     * Swaps the held campaign and settlement to [campaignId]'s and republishes the cockpit.
     *
     * Returns `false` and changes nothing when no stored campaign has that id (Review Focus 3).
     */
    fun selectCampaign(campaignId: String): Boolean {
        if (!repository.select(campaignId)) return false
        reload()
        return true
    }

    /**
     * Creates [name] on the hub, persists and opens it, and returns its Home card.
     *
     * The hub round-trip and any local write live in [SettlementRepository.create]; a thrown
     * failure escapes unchanged so the caller can surface it, and the prior selection is kept
     * (Review Focus 4).
     */
    fun createCampaign(name: String): CampaignCard {
        val card = repository.create(name)
        reload()
        return card
    }

    /** Advances one position in the lantern-year flow, delegating the move to `:core`. */
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

    /**
     * The flow context for an advance: the current year is a nemesis year when its timeline lists a
     * nemesis, and step 9 loops back when a Special Showdown is declared.
     */
    private fun flowContext(): FlowContext =
        FlowContext(
            ruleset = ruleset,
            isNemesisYear = nemesisFor(repository.record) != null,
            hasSpecialShowdown = repository.record.state.specialShowdown,
        )

    /** The nemesis the current year's timeline lists, or `null`. */
    fun nemesisFor(record: SettlementRecord): String? = record.nemesisOf(record.settlement.lanternYear)

    /** Declares (or clears) a Special Showdown for the current year. */
    fun declareSpecialShowdown(enabled: Boolean) {
        applyResult(SetSpecialShowdown.run(repository.record, enabled))
    }

    /**
     * Records a failed hunt (for example a total party kill) and restarts the settlement year.
     *
     * The restart is produced entirely by `:core`'s hunt-failure branch; the controller only
     * supplies the [FlowContext] that selects it, so the settlement re-enters at step 1. No
     * survivors return from a failed hunt, so [returnees] is zero.
     */
    fun failHunt() {
        applyStep(
            returnees = 0,
            context = FlowContext(ruleset = ruleset, huntFailed = true),
        )
    }

    /**
     * Runs one `:core` transition for [context] with [returnees] returning survivors and mirrors
     * the resulting settlement.
     *
     * This is the single path both [advance] and [failHunt] take; neither reimplements any step or
     * phase rule — the target always comes from `:core`. The returnee count is passed through so a
     * TPK can express "0 returning survivors" instead of silently reusing the population.
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

    /**
     * Moves the settlement one step back without crossing a phase boundary.
     *
     * Only meaningful on the settlement screen; on Hunt/Showdown it is a no-op, and the step is
     * floored at 1.
     */
    fun previous() {
        val settlement = repository.settlement
        if (settlement.phase != SettlementPhase.SETTLEMENT || settlement.currentStep <= 1) return
        moveToStep(settlement.currentStep - 1)
    }

    /**
     * Jumps to [number] when `:core`'s flow allows entering it from the current position.
     *
     * The gate is [LanternYearFlow.canEnterStep]: a step may not be skipped forward (step 11 from
     * step 1 is ignored), while revisiting the current step or any earlier one is allowed. The
     * controller never decides this itself.
     */
    fun selectStep(number: Int) {
        val settlement = repository.settlement
        val from = PhaseStep(settlement.phase, settlement.currentStep)
        if (!LanternYearFlow.canEnterStep(number, from, FlowContext(ruleset = ruleset))) return
        moveToStep(number)
    }

    /**
     * Runs the Step 7 Innovate action to unlock [innovationName].
     *
     * The innovating location is found from the content: the first catalog location offering an
     * [ActionKind.INNOVATE] action (the Lantern Hoard and its variants). The whole rule — the
     * once-per-phase limit, the cost, the inventory guard and adding the innovation — is enforced
     * by `:core`'s [Innovate]; the controller only supplies the record, the location and [content].
     * An [StepResult.Ok] is persisted and republished; a [StepResult.Blocked] surfaces its reason.
     *
     * When the catalog models no Innovate location, nothing runs and an explanatory reason is
     * surfaced rather than silently doing nothing.
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

    /**
     * Builds the settlement location named [locationName].
     *
     * Prerequisites, cost and storage are enforced entirely by `:core`'s [BuildLocation]; the
     * controller resolves the displayed name to its stable id through the held [content], passes
     * the held record and [content], and mirrors the outcome.
     */
    fun build(locationName: String) {
        val locationId = locationIdFor(locationName)
        if (locationId == null) {
            publishBlocked("unknown location $locationName")
            return
        }
        applyResult(BuildLocation.run(repository.record, locationId, content))
    }

    /**
     * The id of the location the screens display as [locationName], resolved against the held
     * [content] in either language — the catalog is the authority that carries both the id and
     * the names, and it is what the campaign actually enables.
     */
    private fun locationIdFor(locationName: String): LocationId? =
        content.locationDefinitions.firstOrNull { it.names.es == locationName || it.names.en == locationName }?.id

    /** The id of the innovation the screens display as [innovationName]; see [locationIdFor]. */
    private fun innovationIdFor(innovationName: String): InnovationId? =
        content.innovationDefinitions.firstOrNull { it.names.es == innovationName || it.names.en == innovationName }?.id

    /**
     * Spends one endeavor for a generic Settlement Phase action.
     *
     * The single shared rule lives in `:core`'s [SpendEndeavor]; the action's effect is resolved by
     * the players, so the controller only decrements the pool.
     */
    fun spendEndeavor() {
        applyResult(SpendEndeavor.run(repository.record))
    }

    /**
     * Appends [entry] to the settlement's resource ledger.
     *
     * `:core`'s [AddResourceEntry] guards a spend that would overdraw the ledger; the controller
     * mirrors the outcome like the other develop actions.
     */
    fun addResource(entry: ResourceEntry) {
        applyResult(AddResourceEntry.run(repository.record, entry))
    }

    /**
     * Crafts [gearName] at the built location [locationName].
     *
     * The whole rule — the location must be built, its recipe, the innovation prerequisite and the
     * resource cost — is enforced by `:core`'s [Craft]; the controller mirrors the outcome like the
     * other develop actions.
     */
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

    /** The id of the gear the screens display as [gearName]; see [locationIdFor]. */
    private fun gearIdFor(gearName: String): GearId? =
        content.gearDefinitions.firstOrNull { it.names.es == gearName || it.names.en == gearName }?.id

    /**
     * Records [count] deaths (Step 5): `:core`'s [RecordDeaths] raises the counter and lowers the
     * population, floored at zero; the controller mirrors the outcome on a refusal.
     */
    fun recordDeaths(count: Int) {
        applyResult(RecordDeaths.run(repository.record, count))
    }

    /**
     * Records [count] births (Step 5's counter panel): `:core`'s [RecordBirths] raises the counter
     * and the population; the controller mirrors the outcome on a refusal.
     */
    fun recordBirths(count: Int) {
        applyResult(RecordBirths.run(repository.record, count, newSurvivorId))
    }

    /** Heals the returning survivors (Step 2): erase light/heavy injuries and clear tokens. */
    fun healSurvivors() {
        applyResult(StepResult.Ok(ReturnSurvivors.run(repository.record)))
    }

    /** Chooses the departing survivors (Step 8), capped at the ruleset's departing limit. */
    fun markDeparting(ids: List<Ulid>) {
        applyResult(MarkDeparting.run(repository.record, ids, ruleset.departingLimit))
    }

    /** Assigns [gearName] from the settlement's crafted gear to a survivor (Step 8). */
    fun assignGear(
        survivorId: Ulid,
        gearName: String,
    ) {
        applyResult(AssignGear.run(repository.record, survivorId, gearName))
    }

    /** Removes [gearName] from a survivor (Step 8). */
    fun unassignGear(
        survivorId: Ulid,
        gearName: String,
    ) {
        applyResult(UnassignGear.run(repository.record, survivorId, gearName))
    }

    /** Saves an edited survivor from the Survivor Editor. */
    fun updateSurvivor(survivor: Survivor) {
        applyResult(UpdateSurvivor.run(repository.record, survivor))
    }

    /** Sets up the showdown encounter (monster + level + the assembled AI deck). */
    fun setUpShowdown(
        monster: String,
        level: Int,
        deck: List<String>,
    ) {
        applyResult(SetUpShowdown.run(repository.record, monster, level, deck.shuffled()))
    }

    /** Draws the monster's AI card (the monster's turn). */
    fun drawShowdownCard() {
        applyResult(DrawShowdownCard.run(repository.record))
    }

    /** Wounds the monster (removes the top AI card to the wound stack). */
    fun woundMonster() {
        applyResult(WoundMonster.run(repository.record))
    }

    /** Grants +1 hunt XP to each listed survivor. */
    fun grantHuntXp(survivorIds: List<Ulid>) {
        applyResult(RecordHuntXp.run(repository.record, survivorIds))
    }

    /** Records the showdown outcome and returns the settlement to its next phase. */
    fun finishShowdown(status: ShowdownStatus) {
        when (val result = SetShowdownOutcome.run(repository.record, status)) {
            is StepResult.Ok -> {
                repository.update(result.value)
                advance()
            }
            is StepResult.Blocked -> publishBlocked(result.reason)
        }
    }

    /** Sets up the hunt board (quarry + level + players). */
    fun setUpHunt(
        quarry: String,
        level: Int,
        players: Int,
    ) {
        applyResult(SetUpHunt.run(repository.record, quarry, level, players))
    }

    /** Edits a hunt-board space's kind. */
    fun markSpaceKind(
        index: Int,
        kind: HuntSpaceKind,
    ) {
        applyResult(MarkSpaceKind.run(repository.record, index, kind))
    }

    /** Advances the hunt party one space. */
    fun advanceHunt() {
        applyResult(AdvanceHunt.run(repository.record))
    }

    /** Assigns a party survivor to a player (1-based). */
    fun assignPlayer(
        survivorId: Ulid,
        player: Int,
    ) {
        applyResult(AssignPlayer.run(repository.record, survivorId, player))
    }

    /** Places a hunt event on a space. */
    fun assignHuntEvent(
        index: Int,
        eventName: String,
    ) {
        applyResult(AssignHuntEvent.run(repository.record, index, eventName))
    }

    /** Marks a hunt event resolved. */
    fun resolveHuntEvent(
        index: Int,
        eventName: String,
    ) {
        applyResult(ResolveHuntEvent.run(repository.record, index, eventName))
    }

    /**
     * Records the hunt outcome and moves the lantern year: a failed hunt restarts the Settlement
     * Phase (no returnees); otherwise the settlement proceeds to the Showdown.
     */
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

    /**
     * Records the settlement's chosen principle [label] for [category] (Steps 5/6).
     *
     * The whole rule — the label must be a ruleset choice — lives in `:core`'s [ChoosePrinciple];
     * an unknown choice surfaces its reason.
     */
    fun choosePrinciple(
        category: PrincipleCategory,
        label: String,
    ) {
        applyResult(ChoosePrinciple.run(repository.record, category, label, ruleset))
    }

    /** Ticks the reached milestone [id] as resolved (Step 6), via `:core`'s [ResolveMilestone]. */
    fun resolveMilestone(id: MilestoneId) {
        applyResult(ResolveMilestone.run(repository.record, id, ruleset))
    }

    /**
     * Records the Settlement Event [name] drawn this year (Step 4), via `:core`'s
     * [RecordSettlementEvent]; the name must be one the catalog ships.
     */
    fun recordSettlementEvent(name: String) {
        applyResult(RecordSettlementEvent.run(repository.record, name, content))
    }

    /**
     * Mirrors one `:core` action outcome: an [StepResult.Ok] is persisted and republished with no
     * error, while a [StepResult.Blocked] leaves the record untouched and surfaces its reason.
     */
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
     * Re-reads the repository after an external (sync) change and republishes [state].
     *
     * The controller mirrors the settlement into [state] once per mutation; a sync pass mutates the
     * store behind the controller's back, so without this the UI would keep rendering the pre-sync
     * frame (Ruling AE).
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
     * Advances the lantern year when a move leaves the Update Timeline step forward.
     *
     * The Step 4 rule lives in `:core`'s [AdvanceTimeline]; this helper only decides *when* it
     * applies — a move whose source is Step 4 and whose target is later ([UPDATE_TIMELINE_STEP]).
     * Going back into Step 4, or moving anywhere else, leaves the record untouched. [AdvanceTimeline]
     * is idempotent per phase, so revisiting Step 4 cannot double-increment.
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
     * Funds the endeavor pool exactly once per Settlement Phase when the settlement enters Step 3.
     *
     * The Step 3 rule lives in `:core`'s [GainEndeavors]: the earned total is derived from the
     * settlement's population through the ruleset and *added* to the pool. The controller never
     * reimplements it — it only decides when the rule applies. A phase marker carried in
     * [SettlementState.phaseActionsUsed] (which resets at the year boundary) keeps a revisit of
     * Step 3 from funding a second time.
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
 * Whether a transition out of [from] starts a fresh settlement year, so the once-per-phase
 * actions reset.
 *
 * The boundary is scoped to the **source** phase: a [FlowTransition.Commit] counts only when it
 * leaves the Settlement Phase (which is the only place `:core`'s
 * [io.kdmtracker.core.rules.LanternYearFlow] emits one), and any transition landing on Settlement
 * step 1 opens the next year empty (a successful Showdown's aftermath or a failed hunt). A
 * mid-year move — including a hypothetical `Commit` from Hunt or Showdown — keeps the spent actions.
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

/** The Settlement Phase step where the survivors' endeavors are earned. */
private const val GAIN_ENDEAVORS_STEP = 3

/** The Settlement Phase step where the lantern year advances, Update Timeline. */
private const val UPDATE_TIMELINE_STEP = 4

/** The phase marker recording that Step 3 funded the pool, so a revisit cannot double-fund it. */
private const val GAIN_ENDEAVORS_MARKER = "Gain Endeavors"

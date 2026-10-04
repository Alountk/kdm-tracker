package io.kdmtracker.web

import io.kdmtracker.domain.HuntMember
import io.kdmtracker.domain.HuntRecord
import io.kdmtracker.domain.HuntStatus
import io.kdmtracker.domain.Milestone
import io.kdmtracker.domain.MilestoneKey
import io.kdmtracker.domain.Principle
import io.kdmtracker.domain.PrincipleCategory
import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.SettlementPhase
import io.kdmtracker.domain.SettlementSteps
import io.kdmtracker.domain.StepNote
import io.kdmtracker.repository.HuntMemberRepository
import io.kdmtracker.repository.HuntRecordRepository
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.repository.LogEntryRepository
import io.kdmtracker.repository.MilestoneRepository
import io.kdmtracker.repository.PrincipleRepository
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.repository.StepNoteRepository
import io.kdmtracker.repository.SurvivorRepository
import io.kdmtracker.web.form.SettlementForm
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import kotlin.math.roundToInt

@Controller
class DashboardController(
    private val settlementRepository: SettlementRepository,
    private val locator: SettlementLocator,
    private val survivorRepository: SurvivorRepository,
    private val resourceRepository: ResourceRepository,
    private val innovationRepository: InnovationRepository,
    private val logEntryRepository: LogEntryRepository,
    private val stepNoteRepository: StepNoteRepository,
    private val milestoneRepository: MilestoneRepository,
    private val principleRepository: PrincipleRepository,
    private val huntRecordRepository: HuntRecordRepository,
    private val huntMemberRepository: HuntMemberRepository,
) {
    @GetMapping("/settlements/{id}")
    fun dashboard(
        @PathVariable id: Long,
        model: Model,
    ): String {
        val settlement = locator.require(id)
        fillModel(settlement, model)
        if (!model.containsAttribute(FORM)) {
            model.addAttribute(FORM, settlement.toForm())
        }
        return "dashboard"
    }

    @PostMapping("/settlements/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @ModelAttribute(FORM) form: SettlementForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        val settlement = locator.require(id)
        fillModel(settlement, model)
        if (bindingResult.hasErrors()) {
            return "dashboard"
        }
        form.toEntityFields(settlement)
        settlementRepository.save(settlement)
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/phase-step")
    fun updatePhaseStep(
        @PathVariable id: Long,
        @RequestParam phase: String,
        @RequestParam currentStep: Int,
    ): String {
        val settlement = locator.require(id)
        settlement.phase =
            SettlementPhase.entries.firstOrNull { it.name.equals(phase, ignoreCase = true) } ?: settlement.phase
        settlement.currentStep = currentStep.coerceIn(1, SettlementSituation.OFFICIAL_STEPS)
        settlementRepository.save(settlement)
        return "redirect:/settlements/$id"
    }

    // ---- Settlement step cockpit navigation (all POST-redirect-GET to the dashboard) ----

    @PostMapping("/settlements/{id}/steps/{n}/go")
    fun goToStep(
        @PathVariable id: Long,
        @PathVariable n: Int,
    ): String {
        val settlement = locator.require(id)
        // Any step is clickable on purpose: steps are revisitable by design (research §5 —
        // every step's outputs are corrections-prone, so jumping back/forward is normal).
        settlement.currentStep = n.coerceIn(1, SettlementSteps.TOTAL)
        settlementRepository.save(settlement)
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/steps/next")
    fun nextStep(
        @PathVariable id: Long,
    ): String {
        val settlement = locator.require(id)
        // No-op at step 11: the terminal step only exits through end-phase below.
        settlement.currentStep = (settlement.currentStep + 1).coerceAtMost(SettlementSteps.TOTAL)
        settlementRepository.save(settlement)
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/steps/prev")
    fun prevStep(
        @PathVariable id: Long,
    ): String {
        val settlement = locator.require(id)
        // No-op at step 1.
        settlement.currentStep = (settlement.currentStep - 1).coerceAtLeast(1)
        settlementRepository.save(settlement)
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/steps/resume")
    fun resumeSettlementPhase(
        @PathVariable id: Long,
    ): String {
        val settlement = locator.require(id)
        // Return-from-hunt convenience: the phase flips back, the saved step stays put.
        settlement.phase = SettlementPhase.SETTLEMENT
        settlementRepository.save(settlement)
        return "redirect:/settlements/$id"
    }

    // ---- Step panel actions ----

    @PostMapping("/settlements/{id}/steps/{n}/note")
    fun saveStepNote(
        @PathVariable id: Long,
        @PathVariable n: Int,
        @RequestParam text: String,
    ): String {
        locator.require(id)
        val step = n.coerceIn(1, SettlementSteps.TOTAL)
        val content = text.trim()
        val existing = stepNoteRepository.findBySettlementIdAndStep(id, step)
        if (content.isEmpty()) {
            // Empty text clears the note instead of keeping a blank row.
            existing?.let { stepNoteRepository.delete(it) }
        } else {
            stepNoteRepository.save((existing ?: StepNote(settlementId = id, step = step)).also { it.text = content })
        }
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/lantern-year")
    fun updateLanternYear(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") delta: Int,
        @RequestParam(required = false) settlementEvent: String?,
    ): String {
        val settlement = locator.require(id)
        settlement.lanternYear = (settlement.lanternYear + delta.coerceIn(-1, 1)).coerceAtLeast(1)
        // Absent param leaves the event untouched; an empty field clears it (column is nullable).
        if (settlementEvent != null) {
            settlement.settlementEvent = settlementEvent.trim().ifEmpty { null }
        }
        settlementRepository.save(settlement)
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/steps/{n}/endeavors")
    fun saveEndeavors(
        @PathVariable id: Long,
        @PathVariable n: Int,
        @RequestParam endeavors: Int,
    ): String {
        val settlement = locator.require(id)
        // n is the step that renders the control (3 · Gain Endeavors or 7 · Develop);
        // the column is settlement-scoped.
        settlement.endeavors = endeavors.coerceAtLeast(0)
        settlementRepository.save(settlement)
        return "redirect:/settlements/$id"
    }

    // ---- Step 6 bookkeeping: population, milestones, principles ----

    @PostMapping("/settlements/{id}/population")
    fun savePopulation(
        @PathVariable id: Long,
        @RequestParam population: Int,
    ): String {
        val settlement = locator.require(id)
        // The official rules start a settlement at 4 population; the number is user-owned
        // bookkeeping (population changes come from events, not from a rules engine here).
        settlement.population = population.coerceAtLeast(0)
        settlementRepository.save(settlement)
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/milestones/{key}")
    fun saveMilestone(
        @PathVariable id: Long,
        @PathVariable key: String,
        @RequestParam(defaultValue = "false") reached: Boolean,
        @RequestParam(defaultValue = "false") resolved: Boolean,
    ): String {
        locator.require(id)
        val milestoneKey =
            MilestoneKey.entries.firstOrNull { it.name == key }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown milestone $key")
        // Lazy create: a row only exists after its first save (checkboxes post nothing
        // when unchecked, so a missing param means false). No gating anywhere — milestones
        // reached mid-phase are ticked by the user whenever they happen.
        val existing = milestoneRepository.findBySettlementIdAndKey(id, milestoneKey.name)
        milestoneRepository.save(
            (existing ?: Milestone(settlementId = id, key = milestoneKey.name)).also {
                it.reached = reached
                it.resolved = resolved
            },
        )
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/principles/{category}")
    fun savePrinciple(
        @PathVariable id: Long,
        @PathVariable category: String,
        @RequestParam choice: String,
        @RequestParam(required = false) chosenInYear: Int?,
    ): String {
        val settlement = locator.require(id)
        val principleCategory =
            PrincipleCategory.entries.firstOrNull { it.name == category }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown principle category $category")
        if (choice !in principleCategory.choices) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown $category choice: $choice")
        }
        // One row per (settlement, category): the unique constraint plus this lookup make
        // re-saving an update, never a duplicate. Year defaults to the current lantern year.
        val existing = principleRepository.findBySettlementIdAndCategory(id, principleCategory)
        principleRepository.save(
            (existing ?: Principle(settlementId = id, category = principleCategory)).also {
                it.choice = choice
                it.chosenInYear = (chosenInYear ?: settlement.lanternYear).coerceAtLeast(1)
            },
        )
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/steps/end-phase")
    fun endPhase(
        @PathVariable id: Long,
        @RequestParam(required = false) confirm: String?,
        model: Model,
    ): String {
        val settlement = locator.require(id)
        if (confirm == null) {
            // Missing confirm checkbox: re-render with an error, leave every value untouched.
            fillModel(settlement, model)
            if (!model.containsAttribute(FORM)) {
                model.addAttribute(FORM, settlement.toForm())
            }
            model.addAttribute(END_PHASE_ERROR, "Confirm that unspent endeavors are discarded before ending the phase.")
            return "dashboard"
        }
        settlement.endeavors = 0
        settlement.phase = SettlementPhase.HUNT
        settlement.currentStep = 1
        settlementRepository.save(settlement)
        return "redirect:/settlements/$id"
    }

    // ---- Hunt phase companion (research §3): setup → board → outcome → history ----

    @PostMapping("/settlements/{id}/hunt/start")
    fun startHunt(
        @PathVariable id: Long,
        @RequestParam(required = false) quarry: String?,
        @RequestParam(defaultValue = "1") level: Int,
        @RequestParam(required = false) members: List<Long>?,
        model: Model,
    ): String {
        val settlement = locator.require(id)
        val selected = members.orEmpty()
        val error = huntStartError(settlement, quarry, level, selected)
        if (error != null) {
            // Re-render the dashboard with the error and the submitted values (PRG only on success).
            model.addAttribute(HUNT_ERROR, error)
            model.addAttribute(HUNT_QUARRY, quarry ?: "")
            model.addAttribute(HUNT_LEVEL, level)
            model.addAttribute(HUNT_SELECTED, selected)
            fillModel(settlement, model)
            if (!model.containsAttribute(FORM)) {
                model.addAttribute(FORM, settlement.toForm())
            }
            return "dashboard"
        }
        val hunt =
            huntRecordRepository.save(
                HuntRecord(
                    settlementId = id,
                    quarry = quarry!!.trim(),
                    level = level,
                    status = HuntStatus.ACTIVE,
                    lanternYear = settlement.lanternYear,
                ),
            )
        selected.forEachIndexed { index, survivorId ->
            huntMemberRepository.save(HuntMember(huntId = hunt.id!!, survivorId = survivorId, seat = index))
        }
        // Defensive: normally step-11 end-phase already moved the settlement into HUNT.
        if (settlement.phase == SettlementPhase.SETTLEMENT) {
            settlement.phase = SettlementPhase.HUNT
            settlementRepository.save(settlement)
        }
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/hunt/advance")
    fun advanceHunt(
        @PathVariable id: Long,
    ): String {
        locator.require(id)
        // The currentSpace setter clamps at 13, so advancing at Starvation is a no-op.
        huntRecordRepository.findBySettlementIdAndStatus(id, HuntStatus.ACTIVE)?.let { hunt ->
            hunt.currentSpace = hunt.currentSpace + 1
            huntRecordRepository.save(hunt)
        }
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/hunt/notes")
    fun saveHuntNotes(
        @PathVariable id: Long,
        @RequestParam text: String,
    ): String {
        locator.require(id)
        huntRecordRepository.findBySettlementIdAndStatus(id, HuntStatus.ACTIVE)?.let { hunt ->
            hunt.notes = text
            huntRecordRepository.save(hunt)
        }
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/hunt/found")
    fun foundQuarry(
        @PathVariable id: Long,
    ): String = huntOutcome(id, HuntStatus.QUARRY_FOUND, SettlementPhase.SHOWDOWN, resetStep = false)

    @PostMapping("/settlements/{id}/hunt/ambush")
    fun ambushHunt(
        @PathVariable id: Long,
    ): String = huntOutcome(id, HuntStatus.AMBUSH, SettlementPhase.SHOWDOWN, resetStep = false)

    @PostMapping("/settlements/{id}/hunt/failed")
    fun failedHunt(
        @PathVariable id: Long,
    ): String = huntOutcome(id, HuntStatus.FAILED, SettlementPhase.SETTLEMENT, resetStep = true)

    @PostMapping("/settlements/{id}/hunt/run-away")
    fun runAwayHunt(
        @PathVariable id: Long,
    ): String = huntOutcome(id, HuntStatus.RUN_AWAY, SettlementPhase.SETTLEMENT, resetStep = true)

    @PostMapping("/settlements/{id}/hunt/resolve")
    fun resolveShowdown(
        @PathVariable id: Long,
    ): String {
        val settlement = locator.require(id)
        // Showdown aftermath closed: the hunt keeps its outcome status in the history
        // and a new Settlement Phase begins at step 1 (research §1 loop).
        settlement.phase = SettlementPhase.SETTLEMENT
        settlement.currentStep = 1
        settlementRepository.save(settlement)
        return "redirect:/settlements/$id"
    }

    private fun huntOutcome(
        id: Long,
        status: HuntStatus,
        phase: SettlementPhase,
        resetStep: Boolean,
    ): String {
        val settlement = locator.require(id)
        huntRecordRepository.findBySettlementIdAndStatus(id, HuntStatus.ACTIVE)?.let { hunt ->
            hunt.status = status
            huntRecordRepository.save(hunt)
            settlement.phase = phase
            if (resetStep) {
                // Hunt failed / run away: the lantern year is over, a new Settlement Phase begins.
                settlement.currentStep = 1
            }
            settlementRepository.save(settlement)
        }
        return "redirect:/settlements/$id"
    }

    private fun huntStartError(
        settlement: Settlement,
        quarry: String?,
        level: Int,
        members: List<Long>,
    ): String? {
        if (huntRecordRepository.findBySettlementIdAndStatus(settlement.id!!, HuntStatus.ACTIVE) != null) {
            return "This settlement already has an active hunt — resolve it before starting another."
        }
        if (quarry.isNullOrBlank()) {
            return "Choose a quarry — enter the monster the party is hunting."
        }
        if (level !in 1..3) {
            return "Monster level must be 1, 2 or 3."
        }
        if (members.isEmpty()) {
            return "Pick at least 1 survivor for the hunt party."
        }
        if (members.size > 4) {
            return "A hunt party can have at most 4 survivors."
        }
        val aliveIds =
            survivorRepository
                .findAllBySettlementId(settlement.id!!)
                .filter { it.alive }
                .mapNotNull { it.id }
                .toSet()
        if (members.distinct().size != members.size || members.any { it !in aliveIds }) {
            return "The hunt party must be unique, alive survivors of this settlement."
        }
        return null
    }

    @PostMapping("/settlements/{id}/delete")
    fun delete(
        @PathVariable id: Long,
    ): String {
        locator.require(id)
        survivorRepository.deleteAll(survivorRepository.findAllBySettlementId(id))
        resourceRepository.deleteAll(resourceRepository.findAllBySettlementId(id))
        innovationRepository.deleteAll(innovationRepository.findAllBySettlementId(id))
        logEntryRepository.deleteAll(logEntryRepository.findAllBySettlementId(id))
        stepNoteRepository.deleteAll(stepNoteRepository.findAllBySettlementId(id))
        // Milestones and principles have no FK (import-wipe pattern), so purge explicitly.
        milestoneRepository.deleteAll(milestoneRepository.findAllBySettlementId(id))
        principleRepository.deleteAll(principleRepository.findAllBySettlementId(id))
        // Hunt tables have no FK either: members first, then the hunt records.
        val hunts = huntRecordRepository.findAllBySettlementId(id)
        huntMemberRepository.deleteAll(huntMemberRepository.findAllByHuntIdIn(hunts.mapNotNull { it.id }))
        huntRecordRepository.deleteAll(hunts)
        settlementRepository.deleteById(id)
        return "redirect:/"
    }

    private fun fillModel(
        settlement: Settlement,
        model: Model,
    ) {
        val id = settlement.id!!
        val stepNo = settlement.currentStep.coerceIn(1, SettlementSteps.TOTAL)
        val resources = resourceRepository.findAllBySettlementId(id)
        val survivors = survivorRepository.findAllBySettlementIdOrderByAliveDescNameAsc(id)
        model.addAttribute("settlement", settlement)
        model.addAttribute("settlementId", id)
        model.addAttribute("survivorAliveCount", survivorRepository.countBySettlementIdAndAliveTrue(id))
        model.addAttribute("survivorTotalCount", survivorRepository.countBySettlementId(id))
        model.addAttribute("resourceTotalQuantity", resources.sumOf { it.quantity })
        val innovationCount = innovationRepository.countBySettlementId(id)
        model.addAttribute("innovationCount", innovationCount)
        model.addAttribute(
            "recentLogs",
            logEntryRepository.findTop5BySettlementIdOrderByLanternYearDescCreatedAtDesc(id),
        )
        model.addAttribute("phaseTone", SettlementSituation.phaseTone(settlement.phase))
        model.addAttribute("stepText", SettlementSituation.situationText(settlement))
        // Steps cockpit
        model.addAttribute("steps", SettlementSteps.ALL)
        model.addAttribute("totalSteps", SettlementSteps.TOTAL)
        model.addAttribute("inSettlementPhase", settlement.phase == SettlementPhase.SETTLEMENT)
        model.addAttribute("currentStepDef", SettlementSteps.byNumber(stepNo))
        model.addAttribute("stepNote", stepNoteRepository.findBySettlementIdAndStep(id, stepNo)?.text ?: "")
        model.addAttribute("deadSurvivors", survivors.filter { !it.alive })
        model.addAttribute("aliveSurvivors", survivors.filter { it.alive })
        model.addAttribute("widgetSurvivors", survivors.filter { it.alive }.take(4))
        model.addAttribute("topResources", resources.sortedByDescending { it.quantity }.take(5))
        // Step-1/6/10 bookkeeping: derived views. The milestone hint is render-only —
        // it never gates progression (milestones are ticked manually when reached).
        val population = settlement.population ?: 0
        val deadCount = survivors.count { !it.alive }
        val principles = principleRepository.findAllBySettlementId(id)
        model.addAttribute("population", population)
        model.addAttribute("deadCount", deadCount)
        model.addAttribute("storageResources", resources.sortedBy { it.name })
        model.addAttribute(
            "milestones",
            StepSixViews.milestoneRows(
                milestoneRepository.findAllBySettlementId(id),
                deadCount,
                population,
                innovationCount.toInt(),
            ),
        )
        model.addAttribute("principleGroups", StepSixViews.principleGroups(principles, settlement.lanternYear))
        model.addAttribute("principleCount", principles.size)
        // Hunt companion: the dash-main column renders by phase (cockpit / hunt board / showdown).
        val activeHunt = huntRecordRepository.findBySettlementIdAndStatus(id, HuntStatus.ACTIVE)
        val allHunts = huntRecordRepository.findAllBySettlementIdOrderByIdDesc(id)
        val showdownHunt =
            if (settlement.phase == SettlementPhase.SHOWDOWN) {
                allHunts.firstOrNull { it.status == HuntStatus.QUARRY_FOUND || it.status == HuntStatus.AMBUSH }
            } else {
                null
            }
        model.addAttribute("activeHunt", activeHunt)
        model.addAttribute(
            "huntParty",
            activeHunt
                ?.let { HuntViews.party(huntMemberRepository.findAllByHuntIdOrderBySeat(it.id!!), survivors) }
                ?: emptyList<HuntPartyMember>(),
        )
        model.addAttribute("showdownHunt", showdownHunt)
        model.addAttribute(
            "showdownParty",
            showdownHunt
                ?.let { HuntViews.party(huntMemberRepository.findAllByHuntIdOrderBySeat(it.id!!), survivors) }
                ?: emptyList<HuntPartyMember>(),
        )
        model.addAttribute("showdownAmbush", showdownHunt?.status == HuntStatus.AMBUSH)
        model.addAttribute(
            "huntHistory",
            HuntViews.history(allHunts.filter { it.status != HuntStatus.ACTIVE }.take(HUNT_HISTORY_LIMIT)),
        )
        model.addAttribute("inHuntPhase", settlement.phase == SettlementPhase.HUNT)
        model.addAttribute("inShowdownPhase", settlement.phase == SettlementPhase.SHOWDOWN)
        if (!model.containsAttribute(HUNT_ERROR)) {
            model.addAttribute(HUNT_ERROR, null)
            model.addAttribute(HUNT_QUARRY, "")
            model.addAttribute(HUNT_LEVEL, 1)
            model.addAttribute(HUNT_SELECTED, emptyList<Long>())
        }
        val ringDone = stepNo - 1
        model.addAttribute("ringDone", ringDone)
        model.addAttribute("ringOffset", ((1.0 - ringDone.toDouble() / SettlementSteps.TOTAL) * RING_C).roundToInt())
        model.addAttribute("endeavorsDanger", settlement.endeavors > 0)
        model.addAttribute(
            "endPhaseWarning",
            if (settlement.endeavors > 0) {
                "${settlement.endeavors} unspent endeavors will be discarded"
            } else {
                "No unspent endeavors — nothing will be lost."
            },
        )
        if (!model.containsAttribute(END_PHASE_ERROR)) {
            model.addAttribute(END_PHASE_ERROR, null)
        }
    }

    private fun SettlementForm.toEntityFields(target: Settlement) {
        target.name = name
        target.lanternYear = lanternYear
        target.notes = notes
    }

    private fun Settlement.toForm() = SettlementForm(name = name, lanternYear = lanternYear, notes = notes)

    companion object {
        private const val FORM = "form"
        private const val END_PHASE_ERROR = "endPhaseError"

        // Hunt panel model attributes (error + submitted values are pre-seeded on re-render).
        private const val HUNT_ERROR = "huntError"
        private const val HUNT_QUARRY = "huntQuarry"
        private const val HUNT_LEVEL = "huntLevel"
        private const val HUNT_SELECTED = "huntSelected"
        private const val HUNT_HISTORY_LIMIT = 10

        // SVG ring circumference used by the progress widget (2·π·36, as in the bento prototype).
        private const val RING_C = 226.0
    }
}

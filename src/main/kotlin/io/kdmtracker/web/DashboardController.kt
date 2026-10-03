package io.kdmtracker.web

import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.SettlementPhase
import io.kdmtracker.domain.SettlementSteps
import io.kdmtracker.domain.StepNote
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.repository.LogEntryRepository
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.repository.StepNoteRepository
import io.kdmtracker.repository.SurvivorRepository
import io.kdmtracker.web.form.SettlementForm
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
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
        // n is the step that renders the control (7 · Develop); the column is settlement-scoped.
        settlement.endeavors = endeavors.coerceAtLeast(0)
        settlementRepository.save(settlement)
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
        model.addAttribute("innovationCount", innovationRepository.countBySettlementId(id))
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

        // SVG ring circumference used by the progress widget (2·π·36, as in the bento prototype).
        private const val RING_C = 226.0
    }
}

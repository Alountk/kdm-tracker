package io.kdmtracker.web

import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.SettlementPhase
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.repository.LogEntryRepository
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
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

@Controller
class DashboardController(
    private val settlementRepository: SettlementRepository,
    private val locator: SettlementLocator,
    private val survivorRepository: SurvivorRepository,
    private val resourceRepository: ResourceRepository,
    private val innovationRepository: InnovationRepository,
    private val logEntryRepository: LogEntryRepository,
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

    @PostMapping("/settlements/{id}/delete")
    fun delete(
        @PathVariable id: Long,
    ): String {
        locator.require(id)
        survivorRepository.deleteAll(survivorRepository.findAllBySettlementId(id))
        resourceRepository.deleteAll(resourceRepository.findAllBySettlementId(id))
        innovationRepository.deleteAll(innovationRepository.findAllBySettlementId(id))
        logEntryRepository.deleteAll(logEntryRepository.findAllBySettlementId(id))
        settlementRepository.deleteById(id)
        return "redirect:/"
    }

    private fun fillModel(
        settlement: Settlement,
        model: Model,
    ) {
        val id = settlement.id!!
        model.addAttribute("settlement", settlement)
        model.addAttribute("settlementId", id)
        model.addAttribute("survivorAliveCount", survivorRepository.countBySettlementIdAndAliveTrue(id))
        model.addAttribute("survivorTotalCount", survivorRepository.countBySettlementId(id))
        model.addAttribute("resourceTotalQuantity", resourceRepository.findAllBySettlementId(id).sumOf { it.quantity })
        model.addAttribute("innovationCount", innovationRepository.countBySettlementId(id))
        model.addAttribute(
            "recentLogs",
            logEntryRepository.findTop5BySettlementIdOrderByLanternYearDescCreatedAtDesc(id),
        )
        model.addAttribute("lastLog", logEntryRepository.findFirstBySettlementIdOrderByLanternYearDescCreatedAtDesc(id))
        model.addAttribute("phaseTone", SettlementSituation.phaseTone(settlement.phase))
        model.addAttribute("stepText", SettlementSituation.situationText(settlement))
        model.addAttribute("phases", SettlementPhase.entries)
    }

    private fun SettlementForm.toEntityFields(target: Settlement) {
        target.name = name
        target.lanternYear = lanternYear
        target.notes = notes
    }

    private fun Settlement.toForm() = SettlementForm(name = name, lanternYear = lanternYear, notes = notes)

    companion object {
        private const val FORM = "form"
    }
}

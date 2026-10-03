package io.kdmtracker.web

import io.kdmtracker.domain.Settlement
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
import org.springframework.web.bind.annotation.PostMapping

@Controller
class DashboardController(
    private val settlementRepository: SettlementRepository,
    private val survivorRepository: SurvivorRepository,
    private val resourceRepository: ResourceRepository,
    private val innovationRepository: InnovationRepository,
    private val logEntryRepository: LogEntryRepository,
) {
    @GetMapping("/")
    fun dashboard(model: Model): String {
        fillModel(model)
        if (!model.containsAttribute(FORM)) {
            model.addAttribute(FORM, currentSettlement().toForm())
        }
        return "dashboard"
    }

    @PostMapping("/settlement")
    fun update(
        @Valid @ModelAttribute(FORM) form: SettlementForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        fillModel(model)
        if (bindingResult.hasErrors()) {
            return "dashboard"
        }
        val settlement = currentSettlement()
        form.toEntityFields(settlement)
        settlementRepository.save(settlement)
        return "redirect:/"
    }

    private fun fillModel(model: Model) {
        model.addAttribute("settlement", currentSettlement())
        model.addAttribute("survivorAliveCount", survivorRepository.countByAliveTrue())
        model.addAttribute("survivorTotalCount", survivorRepository.count())
        model.addAttribute("resourceTotalQuantity", resourceRepository.findAll().sumOf { it.quantity })
        model.addAttribute("innovationCount", innovationRepository.count())
        model.addAttribute("recentLogs", logEntryRepository.findTop5ByOrderByLanternYearDescCreatedAtDesc())
    }

    private fun currentSettlement(): Settlement =
        settlementRepository
            .findById(SINGLE_SETTLEMENT_ID)
            .orElseGet { settlementRepository.save(Settlement()) }

    private fun SettlementForm.toEntityFields(target: Settlement) {
        target.name = name
        target.lanternYear = lanternYear
        target.notes = notes
    }

    private fun Settlement.toForm() = SettlementForm(name = name, lanternYear = lanternYear, notes = notes)

    companion object {
        private const val FORM = "form"
        private const val SINGLE_SETTLEMENT_ID = 1L
    }
}

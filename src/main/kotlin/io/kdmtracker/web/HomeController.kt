package io.kdmtracker.web

import io.kdmtracker.domain.Settlement
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.web.form.SettlementForm
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping

@Controller
class HomeController(
    private val settlementRepository: SettlementRepository,
    private val homeViewBuilder: HomeViewBuilder,
) {
    @GetMapping("/")
    fun home(model: Model): String {
        homeViewBuilder.addToModel(model)
        return "home"
    }

    @GetMapping("/settlements/new")
    fun createForm(model: Model): String {
        if (!model.containsAttribute(FORM)) {
            model.addAttribute(FORM, SettlementForm())
        }
        return "settlement-form"
    }

    @PostMapping("/settlements")
    fun create(
        @Valid @ModelAttribute(FORM) form: SettlementForm,
        bindingResult: BindingResult,
    ): String {
        if (bindingResult.hasErrors()) {
            return "settlement-form"
        }
        val settlement =
            settlementRepository.save(
                Settlement(name = form.name, lanternYear = form.lanternYear, notes = form.notes),
            )
        return "redirect:/settlements/${settlement.id}"
    }

    companion object {
        private const val FORM = "form"
    }
}

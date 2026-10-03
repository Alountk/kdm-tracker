package io.kdmtracker.web

import io.kdmtracker.domain.Innovation
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.web.form.InnovationForm
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.server.ResponseStatusException

@Controller
class InnovationController(
    private val innovationRepository: InnovationRepository,
) {
    @GetMapping("/innovations")
    fun list(model: Model): String {
        fillList(model)
        model.addAttribute(FORM, InnovationForm())
        return "innovations"
    }

    @PostMapping("/innovations")
    fun create(
        @Valid @ModelAttribute(FORM) form: InnovationForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        if (bindingResult.hasErrors()) {
            fillList(model)
            return "innovations"
        }
        innovationRepository.save(form.toEntity())
        return "redirect:/innovations"
    }

    @GetMapping("/innovations/{id}/edit")
    fun editForm(
        @PathVariable id: Long,
        model: Model,
    ): String {
        model.addAttribute(FORM, requireInnovation(id).toForm())
        return "innovation-form"
    }

    @PostMapping("/innovations/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @ModelAttribute(FORM) form: InnovationForm,
        bindingResult: BindingResult,
    ): String {
        val innovation = requireInnovation(id)
        form.id = id
        if (bindingResult.hasErrors()) {
            return "innovation-form"
        }
        form.applyTo(innovation)
        innovationRepository.save(innovation)
        return "redirect:/innovations"
    }

    @PostMapping("/innovations/{id}/delete")
    fun delete(
        @PathVariable id: Long,
    ): String {
        if (innovationRepository.existsById(id)) {
            innovationRepository.deleteById(id)
        }
        return "redirect:/innovations"
    }

    private fun fillList(model: Model) {
        model.addAttribute("innovations", innovationRepository.findAllByOrderByUnlockedInYearDescNameAsc())
    }

    private fun requireInnovation(id: Long): Innovation =
        innovationRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Innovation $id not found")
        }

    private fun InnovationForm.toEntity() =
        Innovation().also {
            applyTo(it)
        }

    private fun InnovationForm.applyTo(target: Innovation) {
        target.name = name
        target.unlockedInYear = unlockedInYear
        target.notes = notes
    }

    private fun Innovation.toForm() =
        InnovationForm(
            id = id,
            name = name,
            unlockedInYear = unlockedInYear,
            notes = notes,
        )

    companion object {
        private const val FORM = "form"
    }
}

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
    private val locator: SettlementLocator,
    private val innovationRepository: InnovationRepository,
) {
    @GetMapping("/settlements/{id}/innovations")
    fun list(
        @PathVariable id: Long,
        model: Model,
    ): String {
        locator.require(id)
        fillList(id, model)
        model.addAttribute(FORM, InnovationForm())
        return "innovations"
    }

    @PostMapping("/settlements/{id}/innovations")
    fun create(
        @PathVariable id: Long,
        @Valid @ModelAttribute(FORM) form: InnovationForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        locator.require(id)
        fillList(id, model)
        if (bindingResult.hasErrors()) {
            return "innovations"
        }
        innovationRepository.save(form.toEntity(id))
        return "redirect:/settlements/$id/innovations"
    }

    @GetMapping("/settlements/{id}/innovations/{iid}/edit")
    fun editForm(
        @PathVariable id: Long,
        @PathVariable iid: Long,
        model: Model,
    ): String {
        locator.require(id)
        model.addAttribute("settlementId", id)
        model.addAttribute(FORM, requireInnovation(id, iid).toForm())
        return "innovation-form"
    }

    @PostMapping("/settlements/{id}/innovations/{iid}")
    fun update(
        @PathVariable id: Long,
        @PathVariable iid: Long,
        @Valid @ModelAttribute(FORM) form: InnovationForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        locator.require(id)
        model.addAttribute("settlementId", id)
        val innovation = requireInnovation(id, iid)
        form.id = iid
        if (bindingResult.hasErrors()) {
            return "innovation-form"
        }
        form.applyTo(innovation)
        innovationRepository.save(innovation)
        return "redirect:/settlements/$id/innovations"
    }

    @PostMapping("/settlements/{id}/innovations/{iid}/delete")
    fun delete(
        @PathVariable id: Long,
        @PathVariable iid: Long,
    ): String {
        locator.require(id)
        innovationRepository.delete(requireInnovation(id, iid))
        return "redirect:/settlements/$id/innovations"
    }

    private fun fillList(
        settlementId: Long,
        model: Model,
    ) {
        model.addAttribute("settlementId", settlementId)
        model.addAttribute(
            "innovations",
            innovationRepository.findAllBySettlementIdOrderByUnlockedInYearDescNameAsc(settlementId),
        )
    }

    private fun requireInnovation(
        settlementId: Long,
        iid: Long,
    ): Innovation =
        innovationRepository.findById(iid).filter { it.settlementId == settlementId }.orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Innovation $iid not found")
        }

    private fun InnovationForm.toEntity(settlementId: Long) =
        Innovation().also {
            applyTo(it)
            it.settlementId = settlementId
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

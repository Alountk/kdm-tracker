package io.kdmtracker.web

import io.kdmtracker.domain.Resource
import io.kdmtracker.domain.ResourceCategory
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.web.form.ResourceForm
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

@Controller
class ResourceController(
    private val locator: SettlementLocator,
    private val resourceRepository: ResourceRepository,
) {
    @GetMapping("/settlements/{id}/resources")
    fun list(
        @PathVariable id: Long,
        model: Model,
    ): String {
        locator.require(id)
        fillList(id, model)
        model.addAttribute(FORM, ResourceForm())
        return "resources"
    }

    @PostMapping("/settlements/{id}/resources")
    fun create(
        @PathVariable id: Long,
        @Valid @ModelAttribute(FORM) form: ResourceForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        locator.require(id)
        fillList(id, model)
        if (bindingResult.hasErrors()) {
            return "resources"
        }
        resourceRepository.save(form.toEntity(id))
        return "redirect:/settlements/$id/resources"
    }

    @PostMapping("/settlements/{id}/resources/{rid}/adjust")
    fun adjust(
        @PathVariable id: Long,
        @PathVariable rid: Long,
        @RequestParam delta: Int,
    ): String {
        locator.require(id)
        val resource = requireResource(id, rid)
        resource.quantity = (resource.quantity + delta).coerceAtLeast(0)
        resourceRepository.save(resource)
        return "redirect:/settlements/$id/resources"
    }

    @GetMapping("/settlements/{id}/resources/{rid}/edit")
    fun editForm(
        @PathVariable id: Long,
        @PathVariable rid: Long,
        model: Model,
    ): String {
        locator.require(id)
        model.addAttribute("settlementId", id)
        model.addAttribute(FORM, requireResource(id, rid).toForm())
        return "resource-form"
    }

    @PostMapping("/settlements/{id}/resources/{rid}")
    fun update(
        @PathVariable id: Long,
        @PathVariable rid: Long,
        @Valid @ModelAttribute(FORM) form: ResourceForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        locator.require(id)
        model.addAttribute("settlementId", id)
        val resource = requireResource(id, rid)
        form.id = rid
        if (bindingResult.hasErrors()) {
            return "resource-form"
        }
        form.applyTo(resource)
        resourceRepository.save(resource)
        return "redirect:/settlements/$id/resources"
    }

    @PostMapping("/settlements/{id}/resources/{rid}/delete")
    fun delete(
        @PathVariable id: Long,
        @PathVariable rid: Long,
    ): String {
        locator.require(id)
        resourceRepository.delete(requireResource(id, rid))
        return "redirect:/settlements/$id/resources"
    }

    private fun fillList(
        settlementId: Long,
        model: Model,
    ) {
        model.addAttribute("settlementId", settlementId)
        model.addAttribute("resources", resourceRepository.findAllBySettlementIdOrderByNameAsc(settlementId))
    }

    private fun requireResource(
        settlementId: Long,
        rid: Long,
    ): Resource =
        resourceRepository.findById(rid).filter { it.settlementId == settlementId }.orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Resource $rid not found")
        }

    private fun ResourceForm.toEntity(settlementId: Long) =
        Resource().also {
            applyTo(it)
            it.settlementId = settlementId
        }

    private fun ResourceForm.applyTo(target: Resource) {
        target.name = name
        target.category = ResourceCategory.entries.firstOrNull { it.name == category } ?: ResourceCategory.BASIC
        target.quantity = quantity.coerceAtLeast(0)
    }

    private fun Resource.toForm() =
        ResourceForm(
            id = id,
            name = name,
            category = category.name,
            quantity = quantity,
        )

    companion object {
        private const val FORM = "form"
    }
}

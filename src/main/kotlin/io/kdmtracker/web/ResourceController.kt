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
    private val resourceRepository: ResourceRepository,
) {
    @GetMapping("/resources")
    fun list(model: Model): String {
        fillList(model)
        model.addAttribute(FORM, ResourceForm())
        return "resources"
    }

    @PostMapping("/resources")
    fun create(
        @Valid @ModelAttribute(FORM) form: ResourceForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        if (bindingResult.hasErrors()) {
            fillList(model)
            return "resources"
        }
        resourceRepository.save(form.toEntity())
        return "redirect:/resources"
    }

    @PostMapping("/resources/{id}/adjust")
    fun adjust(
        @PathVariable id: Long,
        @RequestParam delta: Int,
    ): String {
        val resource = requireResource(id)
        resource.quantity = (resource.quantity + delta).coerceAtLeast(0)
        resourceRepository.save(resource)
        return "redirect:/resources"
    }

    @GetMapping("/resources/{id}/edit")
    fun editForm(
        @PathVariable id: Long,
        model: Model,
    ): String {
        model.addAttribute(FORM, requireResource(id).toForm())
        return "resource-form"
    }

    @PostMapping("/resources/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @ModelAttribute(FORM) form: ResourceForm,
        bindingResult: BindingResult,
    ): String {
        val resource = requireResource(id)
        form.id = id
        if (bindingResult.hasErrors()) {
            return "resource-form"
        }
        form.applyTo(resource)
        resourceRepository.save(resource)
        return "redirect:/resources"
    }

    @PostMapping("/resources/{id}/delete")
    fun delete(
        @PathVariable id: Long,
    ): String {
        if (resourceRepository.existsById(id)) {
            resourceRepository.deleteById(id)
        }
        return "redirect:/resources"
    }

    private fun fillList(model: Model) {
        model.addAttribute("resources", resourceRepository.findAllByOrderByNameAsc())
    }

    private fun requireResource(id: Long): Resource =
        resourceRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Resource $id not found")
        }

    private fun ResourceForm.toEntity() =
        Resource().also {
            applyTo(it)
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

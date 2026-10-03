package io.kdmtracker.web

import io.kdmtracker.domain.Sex
import io.kdmtracker.domain.Survivor
import io.kdmtracker.repository.SurvivorRepository
import io.kdmtracker.web.form.SurvivorForm
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
class SurvivorController(
    private val survivorRepository: SurvivorRepository,
) {
    @GetMapping("/survivors")
    fun list(model: Model): String {
        model.addAttribute("survivors", survivorRepository.findAllByOrderByAliveDescNameAsc())
        model.addAttribute(FORM, SurvivorForm())
        return "survivors"
    }

    @GetMapping("/survivors/new")
    fun createForm(model: Model): String {
        model.addAttribute(FORM, SurvivorForm())
        return "survivor-form"
    }

    @PostMapping("/survivors")
    fun create(
        @Valid @ModelAttribute(FORM) form: SurvivorForm,
        bindingResult: BindingResult,
    ): String {
        if (bindingResult.hasErrors()) {
            return "survivor-form"
        }
        survivorRepository.save(form.toEntity())
        return "redirect:/survivors"
    }

    @GetMapping("/survivors/{id}/edit")
    fun editForm(
        @PathVariable id: Long,
        model: Model,
    ): String {
        model.addAttribute(FORM, requireSurvivor(id).toForm())
        return "survivor-form"
    }

    @PostMapping("/survivors/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @ModelAttribute(FORM) form: SurvivorForm,
        bindingResult: BindingResult,
    ): String {
        val survivor = requireSurvivor(id)
        form.id = id
        if (bindingResult.hasErrors()) {
            return "survivor-form"
        }
        form.applyTo(survivor)
        survivorRepository.save(survivor)
        return "redirect:/survivors"
    }

    @PostMapping("/survivors/{id}/delete")
    fun delete(
        @PathVariable id: Long,
    ): String {
        if (survivorRepository.existsById(id)) {
            survivorRepository.deleteById(id)
        }
        return "redirect:/survivors"
    }

    private fun requireSurvivor(id: Long): Survivor =
        survivorRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Survivor $id not found")
        }

    private fun SurvivorForm.toEntity() =
        Survivor().also { form ->
            applyTo(form)
        }

    private fun SurvivorForm.applyTo(target: Survivor) {
        target.name = name
        target.sex = Sex.entries.firstOrNull { it.name == sex }
        target.age = age
        target.movement = movement
        target.speed = speed
        target.accuracy = accuracy
        target.strength = strength
        target.dexterity = dexterity
        target.evasion = evasion
        target.armor = armor
        target.insanity = insanity
        target.courage = courage
        target.understanding = understanding
        target.xp = xp
        target.alive = alive
        target.notes = notes
    }

    private fun Survivor.toForm() =
        SurvivorForm(
            id = id,
            name = name,
            sex = sex?.name,
            age = age,
            movement = movement,
            speed = speed,
            accuracy = accuracy,
            strength = strength,
            dexterity = dexterity,
            evasion = evasion,
            armor = armor,
            insanity = insanity,
            courage = courage,
            understanding = understanding,
            xp = xp,
            alive = alive,
            notes = notes,
        )

    companion object {
        private const val FORM = "form"
    }
}

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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException

@Controller
class SurvivorController(
    private val locator: SettlementLocator,
    private val survivorRepository: SurvivorRepository,
) {
    @GetMapping("/settlements/{id}/survivors")
    fun list(
        @PathVariable id: Long,
        model: Model,
    ): String {
        locator.require(id)
        model.addAttribute("settlementId", id)
        model.addAttribute("survivors", survivorRepository.findAllBySettlementIdOrderByAliveDescNameAsc(id))
        model.addAttribute(FORM, SurvivorForm())
        return "survivors"
    }

    @GetMapping("/settlements/{id}/survivors/new")
    fun createForm(
        @PathVariable id: Long,
        model: Model,
    ): String {
        locator.require(id)
        model.addAttribute("settlementId", id)
        model.addAttribute(FORM, SurvivorForm())
        return "survivor-form"
    }

    @PostMapping("/settlements/{id}/survivors")
    fun create(
        @PathVariable id: Long,
        @Valid @ModelAttribute(FORM) form: SurvivorForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        locator.require(id)
        model.addAttribute("settlementId", id)
        if (bindingResult.hasErrors()) {
            return "survivor-form"
        }
        survivorRepository.save(form.toEntity(id))
        return "redirect:/settlements/$id/survivors"
    }

    @GetMapping("/settlements/{id}/survivors/{sid}/edit")
    fun editForm(
        @PathVariable id: Long,
        @PathVariable sid: Long,
        model: Model,
    ): String {
        locator.require(id)
        model.addAttribute("settlementId", id)
        model.addAttribute(FORM, requireSurvivor(id, sid).toForm())
        return "survivor-form"
    }

    @PostMapping("/settlements/{id}/survivors/{sid}")
    fun update(
        @PathVariable id: Long,
        @PathVariable sid: Long,
        @Valid @ModelAttribute(FORM) form: SurvivorForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        locator.require(id)
        model.addAttribute("settlementId", id)
        val survivor = requireSurvivor(id, sid)
        form.id = sid
        if (bindingResult.hasErrors()) {
            return "survivor-form"
        }
        form.applyTo(survivor)
        survivorRepository.save(survivor)
        return "redirect:/settlements/$id/survivors"
    }

    @PostMapping("/settlements/{id}/survivors/{sid}/dev-update")
    fun devUpdate(
        @PathVariable id: Long,
        @PathVariable sid: Long,
        @RequestParam(defaultValue = "0") courage: Int,
        @RequestParam(defaultValue = "0") understanding: Int,
        @RequestParam(defaultValue = "0") insanity: Int,
    ): String {
        locator.require(id)
        // Step 1 · Update survivors: dev gains from the hunt, clamped at 0 like every
        // other numeric bookkeeping input. Severe injuries live in the survivor's notes
        // until an injury entity exists (see the dashboard step-1 TODO).
        val survivor = requireSurvivor(id, sid)
        survivor.courage = courage.coerceAtLeast(0)
        survivor.understanding = understanding.coerceAtLeast(0)
        survivor.insanity = insanity.coerceAtLeast(0)
        survivorRepository.save(survivor)
        // PRG back to the dashboard: the caller stays on the current step (step 1).
        return "redirect:/settlements/$id"
    }

    @PostMapping("/settlements/{id}/survivors/{sid}/delete")
    fun delete(
        @PathVariable id: Long,
        @PathVariable sid: Long,
    ): String {
        locator.require(id)
        survivorRepository.delete(requireSurvivor(id, sid))
        return "redirect:/settlements/$id/survivors"
    }

    private fun requireSurvivor(
        settlementId: Long,
        sid: Long,
    ): Survivor =
        survivorRepository.findById(sid).filter { it.settlementId == settlementId }.orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Survivor $sid not found")
        }

    private fun SurvivorForm.toEntity(settlementId: Long) =
        Survivor().also { survivor ->
            applyTo(survivor)
            survivor.settlementId = settlementId
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

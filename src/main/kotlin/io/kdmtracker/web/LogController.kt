package io.kdmtracker.web

import io.kdmtracker.domain.LogEntry
import io.kdmtracker.repository.LogEntryRepository
import io.kdmtracker.web.form.LogForm
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
class LogController(
    private val logEntryRepository: LogEntryRepository,
) {
    @GetMapping("/log")
    fun list(model: Model): String {
        fillList(model)
        model.addAttribute(FORM, LogForm())
        return "log"
    }

    @PostMapping("/log")
    fun create(
        @Valid @ModelAttribute(FORM) form: LogForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        if (bindingResult.hasErrors()) {
            fillList(model)
            return "log"
        }
        logEntryRepository.save(form.toEntity())
        return "redirect:/log"
    }

    @GetMapping("/log/{id}/edit")
    fun editForm(
        @PathVariable id: Long,
        model: Model,
    ): String {
        model.addAttribute(FORM, requireEntry(id).toForm())
        return "log-form"
    }

    @PostMapping("/log/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @ModelAttribute(FORM) form: LogForm,
        bindingResult: BindingResult,
    ): String {
        val entry = requireEntry(id)
        form.id = id
        if (bindingResult.hasErrors()) {
            return "log-form"
        }
        form.applyTo(entry)
        logEntryRepository.save(entry)
        return "redirect:/log"
    }

    @PostMapping("/log/{id}/delete")
    fun delete(
        @PathVariable id: Long,
    ): String {
        if (logEntryRepository.existsById(id)) {
            logEntryRepository.deleteById(id)
        }
        return "redirect:/log"
    }

    private fun fillList(model: Model) {
        model.addAttribute("entries", logEntryRepository.findAllByOrderByLanternYearDescCreatedAtDesc())
    }

    private fun requireEntry(id: Long): LogEntry =
        logEntryRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Log entry $id not found")
        }

    private fun LogForm.toEntity() =
        LogEntry(
            lanternYear = lanternYear,
            title = title,
            body = body,
        )

    private fun LogForm.applyTo(target: LogEntry) {
        target.lanternYear = lanternYear
        target.title = title
        target.body = body
    }

    private fun LogEntry.toForm() =
        LogForm(
            id = id,
            lanternYear = lanternYear,
            title = title,
            body = body,
        )

    companion object {
        private const val FORM = "form"
    }
}

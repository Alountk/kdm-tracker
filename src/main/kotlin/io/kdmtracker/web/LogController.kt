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
    private val locator: SettlementLocator,
    private val logEntryRepository: LogEntryRepository,
) {
    @GetMapping("/settlements/{id}/log")
    fun list(
        @PathVariable id: Long,
        model: Model,
    ): String {
        locator.require(id)
        fillList(id, model)
        model.addAttribute(FORM, LogForm())
        return "log"
    }

    @GetMapping("/settlements/{id}/log/new")
    fun newEntry(
        @PathVariable id: Long,
        model: Model,
    ): String {
        // Standalone "new entry" URL: same page as the log list — the create form lives
        // there. Step 9 links here to record a special showdown's aftermath.
        locator.require(id)
        fillList(id, model)
        model.addAttribute(FORM, LogForm())
        return "log"
    }

    @PostMapping("/settlements/{id}/log")
    fun create(
        @PathVariable id: Long,
        @Valid @ModelAttribute(FORM) form: LogForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        locator.require(id)
        fillList(id, model)
        if (bindingResult.hasErrors()) {
            return "log"
        }
        logEntryRepository.save(form.toEntity(id))
        return "redirect:/settlements/$id/log"
    }

    @GetMapping("/settlements/{id}/log/{lid}/edit")
    fun editForm(
        @PathVariable id: Long,
        @PathVariable lid: Long,
        model: Model,
    ): String {
        locator.require(id)
        model.addAttribute("settlementId", id)
        model.addAttribute(FORM, requireEntry(id, lid).toForm())
        return "log-form"
    }

    @PostMapping("/settlements/{id}/log/{lid}")
    fun update(
        @PathVariable id: Long,
        @PathVariable lid: Long,
        @Valid @ModelAttribute(FORM) form: LogForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        locator.require(id)
        model.addAttribute("settlementId", id)
        val entry = requireEntry(id, lid)
        form.id = lid
        if (bindingResult.hasErrors()) {
            return "log-form"
        }
        form.applyTo(entry)
        logEntryRepository.save(entry)
        return "redirect:/settlements/$id/log"
    }

    @PostMapping("/settlements/{id}/log/{lid}/delete")
    fun delete(
        @PathVariable id: Long,
        @PathVariable lid: Long,
    ): String {
        locator.require(id)
        logEntryRepository.delete(requireEntry(id, lid))
        return "redirect:/settlements/$id/log"
    }

    private fun fillList(
        settlementId: Long,
        model: Model,
    ) {
        model.addAttribute("settlementId", settlementId)
        model.addAttribute(
            "entries",
            logEntryRepository.findAllBySettlementIdOrderByLanternYearDescCreatedAtDesc(settlementId),
        )
    }

    private fun requireEntry(
        settlementId: Long,
        lid: Long,
    ): LogEntry =
        logEntryRepository.findById(lid).filter { it.settlementId == settlementId }.orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Log entry $lid not found")
        }

    private fun LogForm.toEntity(settlementId: Long) =
        LogEntry(
            lanternYear = lanternYear,
            title = title,
            body = body,
        ).also {
            it.settlementId = settlementId
        }

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

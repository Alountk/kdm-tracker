package io.kdmtracker.web

import io.kdmtracker.domain.Innovation
import io.kdmtracker.domain.LogEntry
import io.kdmtracker.domain.Resource
import io.kdmtracker.domain.SettlementPhase
import io.kdmtracker.domain.Survivor
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.repository.LogEntryRepository
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.repository.SurvivorRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@Controller
class TransferController(
    private val settlementRepository: SettlementRepository,
    private val survivorRepository: SurvivorRepository,
    private val resourceRepository: ResourceRepository,
    private val innovationRepository: InnovationRepository,
    private val logEntryRepository: LogEntryRepository,
    private val transferService: TransferService,
    private val homeViewBuilder: HomeViewBuilder,
) {
    private val objectMapper: ObjectMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    @GetMapping("/api/export")
    fun export(): ResponseEntity<String> {
        val document =
            ExportDocument(
                schemaVersion = SCHEMA_VERSION,
                exportedAt = Instant.now().toString(),
                settlements = buildSettlements(),
            )
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"kdm-tracker-export-${LocalDate.now()}.json\"",
            ).body(objectMapper.writeValueAsString(document))
    }

    @PostMapping("/api/import")
    fun importFile(
        @RequestParam("file") file: MultipartFile,
        model: Model,
    ): String {
        if (file.isEmpty) {
            return importError(model, "Import failed: the uploaded file is empty.")
        }
        val document =
            try {
                objectMapper.readValue(file.inputStream, ExportDocument::class.java)
            } catch (e: Exception) {
                return importError(model, "Import failed: the uploaded file is not valid JSON.")
            }
        validate(document)?.let { return importError(model, it) }
        try {
            transferService.replaceAll(document)
        } catch (e: Exception) {
            return importError(model, "Import failed: the uploaded data could not be stored (${e.message}).")
        }
        return "redirect:/"
    }

    private fun buildSettlements(): List<ExportSettlement> {
        val survivors = survivorRepository.findAll()
        val resources = resourceRepository.findAll()
        val innovations = innovationRepository.findAll()
        val logs = logEntryRepository.findAll()
        return settlementRepository.findAllByOrderByIdAsc().map { settlement ->
            ExportSettlement(
                name = settlement.name,
                lanternYear = settlement.lanternYear,
                notes = settlement.notes,
                phase = settlement.phase.name,
                currentStep = settlement.currentStep,
                survivors = survivors.filter { it.settlementId == settlement.id }.map { it.toExport() },
                resources = resources.filter { it.settlementId == settlement.id }.map { it.toExport() },
                innovations = innovations.filter { it.settlementId == settlement.id }.map { it.toExport() },
                log = logs.filter { it.settlementId == settlement.id }.map { it.toExport() },
            )
        }
    }

    private fun validate(document: ExportDocument): String? {
        if (document.schemaVersion != SCHEMA_VERSION) {
            return "Import failed: unsupported schemaVersion (expected $SCHEMA_VERSION)."
        }
        val settlements = document.settlements ?: return "Import failed: the settlements array is missing."
        settlements.forEachIndexed { index, settlement ->
            if (settlement.name.isNullOrBlank()) {
                return "Import failed: settlements[$index].name is required."
            }
            settlement.phase?.let { phase ->
                if (SettlementPhase.entries.none { it.name.equals(phase, ignoreCase = true) }) {
                    return "Import failed: settlements[$index].phase '$phase' is unknown."
                }
            }
            settlement.survivors.orEmpty().forEachIndexed { i, survivor ->
                if (survivor.name.isNullOrBlank()) {
                    return "Import failed: settlements[$index].survivors[$i].name is required."
                }
            }
            settlement.resources.orEmpty().forEachIndexed { i, resource ->
                if (resource.name.isNullOrBlank()) {
                    return "Import failed: settlements[$index].resources[$i].name is required."
                }
            }
            settlement.innovations.orEmpty().forEachIndexed { i, innovation ->
                if (innovation.name.isNullOrBlank()) {
                    return "Import failed: settlements[$index].innovations[$i].name is required."
                }
            }
            settlement.log.orEmpty().forEachIndexed { i, entry ->
                if (entry.title.isNullOrBlank()) {
                    return "Import failed: settlements[$index].log[$i].title is required."
                }
                entry.createdAt?.let { raw ->
                    try {
                        LocalDateTime.parse(raw)
                    } catch (e: Exception) {
                        return "Import failed: settlements[$index].log[$i].createdAt '$raw' is not a valid date-time."
                    }
                }
            }
        }
        return null
    }

    private fun importError(
        model: Model,
        message: String,
    ): String {
        model.addAttribute("errorMessage", message)
        homeViewBuilder.addToModel(model)
        return "home"
    }

    private fun Survivor.toExport() =
        ExportSurvivor(
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

    private fun Resource.toExport() = ExportResource(name = name, category = category.name, quantity = quantity)

    private fun Innovation.toExport() = ExportInnovation(name = name, unlockedInYear = unlockedInYear, notes = notes)

    private fun LogEntry.toExport() =
        ExportLogEntry(lanternYear = lanternYear, title = title, body = body, createdAt = createdAt.toString())

    companion object {
        const val SCHEMA_VERSION = 1
    }
}

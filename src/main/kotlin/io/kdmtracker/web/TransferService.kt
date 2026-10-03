package io.kdmtracker.web

import io.kdmtracker.domain.Innovation
import io.kdmtracker.domain.LogEntry
import io.kdmtracker.domain.Resource
import io.kdmtracker.domain.ResourceCategory
import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.SettlementPhase
import io.kdmtracker.domain.Sex
import io.kdmtracker.domain.Survivor
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.repository.LogEntryRepository
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.repository.SurvivorRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class TransferService(
    private val settlementRepository: SettlementRepository,
    private val survivorRepository: SurvivorRepository,
    private val resourceRepository: ResourceRepository,
    private val innovationRepository: InnovationRepository,
    private val logEntryRepository: LogEntryRepository,
) {
    /**
     * Replaces ALL data with the uploaded document. Runs in one transaction: any
     * failure rolls back to the previous dataset instead of leaving a partial import.
     */
    @Transactional
    fun replaceAll(document: ExportDocument) {
        survivorRepository.deleteAllInBatch()
        resourceRepository.deleteAllInBatch()
        innovationRepository.deleteAllInBatch()
        logEntryRepository.deleteAllInBatch()
        settlementRepository.deleteAllInBatch()

        document.settlements.orEmpty().forEach { exported ->
            val settlement =
                settlementRepository.save(
                    Settlement(
                        name = exported.name.orEmpty(),
                        lanternYear = exported.lanternYear ?: 1,
                        notes = exported.notes.orEmpty(),
                        phase = exported.phase.toPhase(),
                        currentStep = (exported.currentStep ?: 1).coerceIn(1, SettlementSituation.OFFICIAL_STEPS),
                    ),
                )
            val settlementId = settlement.id!!
            exported.survivors.orEmpty().forEach { survivorRepository.save(it.toEntity(settlementId)) }
            exported.resources.orEmpty().forEach { resourceRepository.save(it.toEntity(settlementId)) }
            exported.innovations.orEmpty().forEach { innovationRepository.save(it.toEntity(settlementId)) }
            exported.log.orEmpty().forEach { logEntryRepository.save(it.toEntity(settlementId)) }
        }
    }

    private fun String?.toPhase(): SettlementPhase =
        SettlementPhase.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: SettlementPhase.SETTLEMENT

    private fun ExportSurvivor.toEntity(settlementId: Long) =
        Survivor(
            name = name.orEmpty(),
            sex = Sex.entries.firstOrNull { it.name == sex },
            age = age ?: 12,
            movement = movement ?: 5,
            speed = speed ?: 4,
            accuracy = accuracy ?: 5,
            strength = strength ?: 1,
            dexterity = dexterity ?: 8,
            evasion = evasion ?: 0,
            armor = armor ?: 0,
            insanity = insanity ?: 0,
            courage = courage ?: 0,
            understanding = understanding ?: 0,
            xp = xp ?: 0,
            alive = alive ?: true,
            notes = notes.orEmpty(),
            settlementId = settlementId,
        )

    private fun ExportResource.toEntity(settlementId: Long) =
        Resource(
            name = name.orEmpty(),
            category = ResourceCategory.entries.firstOrNull { it.name == category } ?: ResourceCategory.BASIC,
            quantity = quantity ?: 0,
            settlementId = settlementId,
        )

    private fun ExportInnovation.toEntity(settlementId: Long) =
        Innovation(
            name = name.orEmpty(),
            unlockedInYear = unlockedInYear ?: 1,
            notes = notes.orEmpty(),
            settlementId = settlementId,
        )

    private fun ExportLogEntry.toEntity(settlementId: Long) =
        LogEntry(
            lanternYear = lanternYear ?: 1,
            title = title.orEmpty(),
            body = body.orEmpty(),
            createdAt = createdAt?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
            settlementId = settlementId,
        )
}

package io.kdmtracker.web

import io.kdmtracker.domain.Settlement
import io.kdmtracker.repository.InnovationRepository
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.repository.SurvivorRepository
import org.springframework.stereotype.Component
import org.springframework.ui.Model

data class SettlementCardView(
    val settlement: Settlement,
    val survivorAliveCount: Long,
    val survivorTotalCount: Long,
    val innovationCount: Long,
    val resourceUnits: Int,
    val phaseTone: String,
    val statusLabel: String,
    val statusTone: String,
    val situationText: String,
)

@Component
class HomeViewBuilder(
    private val settlementRepository: SettlementRepository,
    private val survivorRepository: SurvivorRepository,
    private val resourceRepository: ResourceRepository,
    private val innovationRepository: InnovationRepository,
) {
    fun cards(): List<SettlementCardView> {
        val settlements = settlementRepository.findAllByOrderByIdAsc()
        val survivors = survivorRepository.findAll()
        val resources = resourceRepository.findAll()
        val innovations = innovationRepository.findAll()
        return settlements.map { settlement ->
            val children = survivors.filter { it.settlementId == settlement.id }
            SettlementCardView(
                settlement = settlement,
                survivorAliveCount = children.count { it.alive }.toLong(),
                survivorTotalCount = children.size.toLong(),
                innovationCount = innovations.count { it.settlementId == settlement.id }.toLong(),
                resourceUnits = resources.filter { it.settlementId == settlement.id }.sumOf { it.quantity },
                phaseTone = SettlementSituation.phaseTone(settlement.phase),
                statusLabel = SettlementSituation.statusLabel(settlement),
                statusTone = SettlementSituation.statusTone(settlement),
                situationText = SettlementSituation.situationText(settlement),
            )
        }
    }

    fun addToModel(model: Model) {
        model.addAttribute("cards", cards())
    }
}

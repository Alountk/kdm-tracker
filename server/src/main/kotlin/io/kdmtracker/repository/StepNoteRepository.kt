package io.kdmtracker.repository

import io.kdmtracker.domain.StepNote
import org.springframework.data.jpa.repository.JpaRepository

interface StepNoteRepository : JpaRepository<StepNote, Long> {
    fun findBySettlementIdAndStep(
        settlementId: Long,
        step: Int,
    ): StepNote?

    fun findAllBySettlementId(settlementId: Long): List<StepNote>
}

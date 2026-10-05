package io.kdmtracker.repository

import io.kdmtracker.domain.Principle
import io.kdmtracker.domain.PrincipleCategory
import org.springframework.data.jpa.repository.JpaRepository

interface PrincipleRepository : JpaRepository<Principle, Long> {
    fun findBySettlementIdAndCategory(
        settlementId: Long,
        category: PrincipleCategory,
    ): Principle?

    fun findAllBySettlementId(settlementId: Long): List<Principle>
}

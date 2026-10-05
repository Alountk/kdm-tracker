package io.kdmtracker.repository

import io.kdmtracker.domain.Resource
import org.springframework.data.jpa.repository.JpaRepository

interface ResourceRepository : JpaRepository<Resource, Long> {
    fun findAllBySettlementIdOrderByNameAsc(settlementId: Long): List<Resource>

    fun findAllBySettlementId(settlementId: Long): List<Resource>
}

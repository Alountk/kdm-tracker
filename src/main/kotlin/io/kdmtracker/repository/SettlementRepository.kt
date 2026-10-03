package io.kdmtracker.repository

import io.kdmtracker.domain.Settlement
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementRepository : JpaRepository<Settlement, Long>

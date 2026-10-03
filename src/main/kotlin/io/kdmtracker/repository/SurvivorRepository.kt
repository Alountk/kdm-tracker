package io.kdmtracker.repository

import io.kdmtracker.domain.Survivor
import org.springframework.data.jpa.repository.JpaRepository

interface SurvivorRepository : JpaRepository<Survivor, Long> {
    fun findAllByOrderByAliveDescNameAsc(): List<Survivor>

    fun countByAliveTrue(): Long
}

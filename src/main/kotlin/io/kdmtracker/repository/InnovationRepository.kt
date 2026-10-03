package io.kdmtracker.repository

import io.kdmtracker.domain.Innovation
import org.springframework.data.jpa.repository.JpaRepository

interface InnovationRepository : JpaRepository<Innovation, Long> {
    fun findAllByOrderByUnlockedInYearDescNameAsc(): List<Innovation>
}

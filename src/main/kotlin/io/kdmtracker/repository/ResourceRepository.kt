package io.kdmtracker.repository

import io.kdmtracker.domain.Resource
import org.springframework.data.jpa.repository.JpaRepository

interface ResourceRepository : JpaRepository<Resource, Long> {
    fun findAllByOrderByNameAsc(): List<Resource>
}

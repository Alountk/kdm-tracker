package io.kdmtracker.repository

import io.kdmtracker.domain.HuntMember
import org.springframework.data.jpa.repository.JpaRepository

interface HuntMemberRepository : JpaRepository<HuntMember, Long> {
    fun findAllByHuntIdOrderBySeat(huntId: Long): List<HuntMember>

    fun findAllByHuntIdIn(huntIds: List<Long>): List<HuntMember>
}

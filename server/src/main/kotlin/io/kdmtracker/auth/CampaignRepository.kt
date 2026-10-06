package io.kdmtracker.auth

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CampaignRepository : JpaRepository<CampaignEntity, String> {
    fun findByInviteCode(inviteCode: String): CampaignEntity?

    /**
     * Loads the campaign row under a `PESSIMISTIC_WRITE` lock. `push` takes this lock for the
     * duration of its transaction so concurrent pushes to the same campaign serialize their
     * `maxRev` reads and can never assign the same `rev`.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CampaignEntity c where c.id = :id")
    fun findWithLockById(
        @Param("id") id: String,
    ): CampaignEntity?
}

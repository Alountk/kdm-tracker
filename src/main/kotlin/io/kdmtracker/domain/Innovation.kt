package io.kdmtracker.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "innovations")
class Innovation(
    var name: String = "",
    var unlockedInYear: Int = 1,
    var notes: String = "",
    var settlementId: Long = 1L,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

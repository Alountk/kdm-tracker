package io.kdmtracker.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "settlements")
class Settlement(
    var name: String = "New Settlement",
    var lanternYear: Int = 1,
    var notes: String = "",
    @field:Id
    var id: Long = 1L,
)

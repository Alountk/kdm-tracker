package io.kdmtracker.web.form

import jakarta.validation.constraints.NotBlank

data class InnovationForm(
    var id: Long? = null,
    @field:NotBlank
    var name: String = "",
    var unlockedInYear: Int = 1,
    var notes: String = "",
)

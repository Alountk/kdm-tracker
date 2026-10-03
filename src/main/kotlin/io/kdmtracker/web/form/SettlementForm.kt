package io.kdmtracker.web.form

import jakarta.validation.constraints.NotBlank

data class SettlementForm(
    @field:NotBlank
    var name: String = "New Settlement",
    var lanternYear: Int = 1,
    var notes: String = "",
)

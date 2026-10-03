package io.kdmtracker.web.form

import jakarta.validation.constraints.NotBlank

data class LogForm(
    var id: Long? = null,
    var lanternYear: Int = 1,
    @field:NotBlank
    var title: String = "",
    var body: String = "",
)

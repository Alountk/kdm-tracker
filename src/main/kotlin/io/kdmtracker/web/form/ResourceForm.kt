package io.kdmtracker.web.form

import jakarta.validation.constraints.NotBlank

data class ResourceForm(
    var id: Long? = null,
    @field:NotBlank
    var name: String = "",
    var category: String = "BASIC",
    var quantity: Int = 0,
)

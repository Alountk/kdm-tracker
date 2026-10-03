package io.kdmtracker.web.form

import jakarta.validation.constraints.NotBlank

data class SurvivorForm(
    var id: Long? = null,
    @field:NotBlank
    var name: String = "",
    var sex: String? = null,
    var age: Int = 12,
    var movement: Int = 5,
    var speed: Int = 4,
    var accuracy: Int = 5,
    var strength: Int = 1,
    var dexterity: Int = 8,
    var evasion: Int = 0,
    var armor: Int = 0,
    var insanity: Int = 0,
    var courage: Int = 0,
    var understanding: Int = 0,
    var xp: Int = 0,
    var alive: Boolean = true,
    var notes: String = "",
)

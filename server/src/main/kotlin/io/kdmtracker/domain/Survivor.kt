package io.kdmtracker.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "survivors")
class Survivor(
    var name: String = "",
    @field:Enumerated(EnumType.STRING)
    var sex: Sex? = null,
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
    @field:Column(name = "settlement_id")
    var settlementId: Long = 1L,
    // Read-only association on the same column: declares the real foreign key to
    // settlements while settlementId stays the writable property used by the app.
    @field:ManyToOne(optional = false, fetch = FetchType.LAZY)
    @field:JoinColumn(
        name = "settlement_id",
        insertable = false,
        updatable = false,
        foreignKey = ForeignKey(name = "fk_survivors_settlement"),
    )
    var settlement: Settlement? = null,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

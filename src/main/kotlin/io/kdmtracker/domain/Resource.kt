package io.kdmtracker.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "resources")
class Resource(
    var name: String = "",
    @field:Enumerated(EnumType.STRING)
    var category: ResourceCategory = ResourceCategory.BASIC,
    var quantity: Int = 0,
    var settlementId: Long = 1L,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

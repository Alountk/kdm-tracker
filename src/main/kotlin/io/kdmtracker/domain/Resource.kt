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
@Table(name = "resources")
class Resource(
    var name: String = "",
    @field:Enumerated(EnumType.STRING)
    var category: ResourceCategory = ResourceCategory.BASIC,
    var quantity: Int = 0,
    @field:Column(name = "settlement_id")
    var settlementId: Long = 1L,
    @field:ManyToOne(optional = false, fetch = FetchType.LAZY)
    @field:JoinColumn(
        name = "settlement_id",
        insertable = false,
        updatable = false,
        foreignKey = ForeignKey(name = "fk_resources_settlement"),
    )
    var settlement: Settlement? = null,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

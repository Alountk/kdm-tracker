package io.kdmtracker.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(name = "settlements")
class Settlement(
    var name: String = "New Settlement",
    var lanternYear: Int = 1,
    var notes: String = "",
    @field:Enumerated(EnumType.STRING)
    var phase: SettlementPhase = SettlementPhase.SETTLEMENT,
    // Nullable column: rows created before multi-settlement support are backfilled
    // by DataInitializer at startup, before anything reads them.
    @field:Column(nullable = true)
    var currentStep: Int = 1,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settlement_seq")
    @field:SequenceGenerator(name = "settlement_seq", sequenceName = "settlement_seq", allocationSize = 1)
    var id: Long? = null,
)

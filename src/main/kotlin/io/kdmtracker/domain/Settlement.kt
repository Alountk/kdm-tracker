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
    // Step-cockpit columns, added nullable for the same legacy-volume reason:
    // settlement_event reads fine as NULL, endeavors is backfilled to 0 by DataInitializer.
    @field:Column(nullable = true)
    var settlementEvent: String? = null,
    @field:Column(nullable = true)
    var endeavors: Int = 0,
    // Nullable column for the same legacy-volume reason: DataInitializer backfills NULL rows
    // from the survivor count at startup. Kotlin default 0 — the official rules start a
    // settlement at 4 population, the user sets it in the step-6 panel.
    @field:Column(nullable = true)
    var population: Int? = 0,
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settlement_seq")
    @field:SequenceGenerator(name = "settlement_seq", sequenceName = "settlement_seq", allocationSize = 1)
    var id: Long? = null,
)

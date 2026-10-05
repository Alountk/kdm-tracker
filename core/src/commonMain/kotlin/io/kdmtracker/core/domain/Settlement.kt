package io.kdmtracker.core.domain

import io.kdmtracker.core.rules.SettlementStepCatalog

/**
 * A settlement: the survivors, resources and progress tracked across lantern years.
 *
 * @property lanternYear the current lantern year, one-based.
 * @property phase the phase of the lantern year currently in progress.
 * @property currentStep the Settlement Phase step in `1..SettlementStepCatalog.TOTAL`.
 * @property endeavors the unspent endeavors available this lantern year.
 * @property population the number of living survivors.
 * @property settlementEvent the settlement event drawn this year, or null when none applies.
 */
data class Settlement(
    val id: Ulid,
    val campaignId: Ulid,
    val name: String,
    val lanternYear: Int,
    val phase: SettlementPhase,
    val currentStep: Int,
    val endeavors: Int,
    val population: Int,
    val settlementEvent: String?,
    val sync: SyncMeta,
) {
    init {
        require(lanternYear >= 1) {
            "lanternYear must be at least 1, was $lanternYear"
        }
        require(currentStep in 1..SettlementStepCatalog.TOTAL) {
            "currentStep must be between 1 and ${SettlementStepCatalog.TOTAL}, was $currentStep"
        }
        require(endeavors >= 0) {
            "endeavors must not be negative, was $endeavors"
        }
        require(population >= 0) {
            "population must not be negative, was $population"
        }
    }

    companion object {
        /**
         * Creates a settlement in its opening state: lantern year 1, the Settlement Phase,
         * step 1, no endeavors and the four founding survivors.
         */
        fun new(
            id: Ulid,
            campaignId: Ulid,
            name: String,
        ): Settlement =
            Settlement(
                id = id,
                campaignId = campaignId,
                name = name,
                lanternYear = 1,
                phase = SettlementPhase.SETTLEMENT,
                currentStep = 1,
                endeavors = 0,
                population = 4,
                settlementEvent = null,
                sync = SyncMeta(HybridLogicalClock(0, 0)),
            )
    }
}

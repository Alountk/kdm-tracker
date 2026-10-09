package io.kdmtracker.core.transfer

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SurvivorMigrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun ulid(seed: Long): Ulid = Ulid.generate(seed) { 0 }

    private fun settlement(
        population: Int,
        id: Ulid = ulid(2),
    ): Settlement =
        Settlement(
            id = id,
            campaignId = ulid(1),
            name = "First Cradle",
            lanternYear = 1,
            phase = SettlementPhase.SETTLEMENT,
            currentStep = 1,
            endeavors = 0,
            population = population,
            settlementEvent = null,
            sync = SyncMeta(HybridLogicalClock(0, 0)),
        )

    private fun record(
        population: Int,
        survivors: List<Survivor> = emptyList(),
    ): SettlementRecord = SettlementRecord(settlement(population), SettlementState(survivors = survivors))

    @Test
    fun migratedSeedsTheRosterFromTheLegacyPopulation() {
        val migrated = record(population = 4).migrated { index -> Ulid.generate(index + 1L) { index } }

        assertEquals(4, migrated.state.survivors.size)
        assertEquals(4, migrated.population)
    }

    @Test
    fun migratedWithDefaultIdsIsDeterministic() {
        val source = record(population = 3)

        val first =
            source
                .migrated()
                .state.survivors
                .map { it.id }
        val second =
            source
                .migrated()
                .state.survivors
                .map { it.id }

        assertEquals(first, second)
        assertEquals(3, first.toSet().size)
    }

    @Test
    fun migratedLeavesANonEmptyRosterUntouched() {
        val existing = Survivor(id = ulid(5))
        val source = record(population = 4, survivors = listOf(existing))

        assertEquals(source, source.migrated { ulid(9) })
    }

    @Test
    fun migratedLeavesAnEmptyPopulationAlone() {
        val source = record(population = 0)

        assertEquals(source, source.migrated { ulid(9) })
    }

    @Test
    fun v6DocumentWithoutRosterDecodesAndSeeds() {
        val campaignId = ulid(1).value
        val settlementId = ulid(2).value
        val v6 =
            """
            {
              "schemaVersion": 6,
              "exportedAt": "2025-01-01T00:00:00Z",
              "campaigns": [
                {
                  "id": "$campaignId",
                  "name": "Grey Havens",
                  "rulesetId": "core-1.6",
                  "settlementIds": ["$settlementId"],
                  "settlements": [
                    { "id": "$settlementId", "campaignId": "$campaignId", "name": "First Cradle", "population": 3 }
                  ]
                }
              ]
            }
            """.trimIndent()

        val document = json.decodeFromString<TransferDocument>(v6)
        val (_, records) = TransferMapper.toDomain(document.campaigns.single())

        assertEquals(
            3,
            records
                .single()
                .state.survivors.size,
        )
        assertEquals(3, records.single().population)
    }

    @Test
    fun v8RoundTripCarriesTheRoster() {
        val campaign =
            Campaign(
                id = ulid(1),
                name = "Grey Havens",
                rulesetId = "core-1.6",
                rulesetOverrides = RulesetOverrides(),
                settlementIds = listOf(ulid(2)),
                sync = SyncMeta(HybridLogicalClock(1_700_000_000_000L, 4), rev = 7),
            )
        val records =
            listOf(
                SettlementRecord(
                    settlement(2),
                    SettlementState(survivors = listOf(Survivor(id = ulid(7), name = "Eli"), Survivor(id = ulid(8)))),
                ),
            )
        val document =
            TransferDocument(
                exportedAt = "2026-10-08T00:00:00Z",
                campaigns = listOf(TransferMapper.toDto(campaign, records)),
            )

        val decoded = json.decodeFromString<TransferDocument>(json.encodeToString(document))
        val (_, domainRecords) = TransferMapper.toDomain(decoded.campaigns.single())

        assertEquals(8, decoded.schemaVersion)
        assertEquals(records, domainRecords)
    }
}

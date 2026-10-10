package io.kdmtracker.core.transfer

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.MilestoneConditionSpec
import io.kdmtracker.core.rules.MilestoneMetric
import io.kdmtracker.core.ruleset.MilestoneDefinition
import io.kdmtracker.core.ruleset.PrincipleChoice
import io.kdmtracker.core.ruleset.RulesetOverrides
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransferDocumentTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun ulid(seed: Long): Ulid = Ulid.generate(seed) { 0 }

    private val campaign =
        Campaign(
            id = ulid(1),
            name = "Grey Havens",
            rulesetId = "core-1.6",
            rulesetOverrides =
                RulesetOverrides(
                    survivalLimit = 12,
                    extraMilestones =
                        listOf(
                            MilestoneDefinition(
                                Core16Milestones.GAME_OVER,
                                MilestoneConditionSpec.AtMost(MilestoneMetric.POPULATION, 0),
                                "house rule game over",
                            ),
                        ),
                    principleOverrides =
                        mapOf(
                            PrincipleCategory.DEATH to
                                listOf(
                                    PrincipleChoice(PrincipleCategory.DEATH, "House Rule", "a custom death rule"),
                                ),
                        ),
                ),
            settlementIds = listOf(ulid(2), ulid(3)),
            sync = SyncMeta(HybridLogicalClock(1_700_000_000_000L, 4), rev = 7),
        )

    private val settlements =
        listOf(
            Settlement(
                id = ulid(2),
                campaignId = campaign.id,
                name = "First Cradle",
                lanternYear = 5,
                phase = SettlementPhase.HUNT,
                currentStep = 3,
                endeavors = 2,
                population = 9,
                settlementEvent = "The Hand",
                sync = SyncMeta(HybridLogicalClock(1_700_000_000_100L, 1), rev = 3),
            ),
            Settlement(
                id = ulid(3),
                campaignId = campaign.id,
                name = "Second Cradle",
                lanternYear = 1,
                phase = SettlementPhase.SETTLEMENT,
                currentStep = 1,
                endeavors = 0,
                population = 4,
                settlementEvent = null,
                sync = SyncMeta(HybridLogicalClock(1_700_000_000_200L, 0), rev = 1, deletedAt = null),
            ),
        )

    /** A roster-bearing record matching [settlement]'s population, so a DTO round-trip is exact. */
    private fun record(settlement: Settlement): SettlementRecord {
        var n = 0L
        return SettlementRecord(
            settlement,
        ).withSurvivors(List(settlement.population) { Survivor(id = ulid(1_000L + n++)) })
    }

    @Test
    fun campaignAndSettlementsRoundTripThroughDto() {
        val records = settlements.map { record(it) }
        val dto = TransferMapper.toDto(campaign, records)

        val (domainCampaign, domainSettlements) = TransferMapper.toDomain(dto)

        assertEquals(campaign, domainCampaign)
        assertEquals(records, domainSettlements)
    }

    @Test
    fun transferDocumentRoundTripsThroughJson() {
        val document =
            TransferDocument(
                exportedAt = "2026-10-05T00:00:00Z",
                campaigns = listOf(TransferMapper.toDto(campaign, settlements.map { record(it) })),
            )

        val encoded = json.encodeToString(document)
        val decoded = json.decodeFromString<TransferDocument>(encoded)

        assertEquals(document, decoded)
    }

    @Test
    fun v2DocumentWithoutV3FieldsUsesDefaults() {
        val v2 =
            """
            {
              "schemaVersion": 2,
              "exportedAt": "2024-01-01T00:00:00Z",
              "campaigns": [
                {
                  "id": "00000000000000000000000000",
                  "name": "Legacy Campaign",
                  "rulesetId": "core-1.5",
                  "settlementIds": []
                }
              ]
            }
            """.trimIndent()

        val document = json.decodeFromString<TransferDocument>(v2)

        assertEquals(2, document.schemaVersion)
        val legacy = document.campaigns.single()
        assertEquals("Legacy Campaign", legacy.name)
        assertEquals("core-1.5", legacy.rulesetId)
        assertEquals(RulesetOverridesDto(), legacy.rulesetOverrides)
        assertTrue(legacy.settlements.isEmpty())
        assertTrue(legacy.settlementIds.isEmpty())
        assertEquals(SyncMetaDto(), legacy.sync)
    }

    @Test
    fun supportedVersionsCoverV1ThroughV12() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), TransferDocument.SUPPORTED_VERSIONS)
    }

    @Test
    fun aV11DocumentStillDecodesWithLocationsGearAndInnovationsMappedToIds() {
        // v11 (S1) exported the state with name-keyed locations and gear and name-only
        // innovations; importing it into this build must map every one of them to its id.
        val v11 =
            """
            {
              "schemaVersion": 11,
              "exportedAt": "2026-10-10T00:00:00Z",
              "campaigns": [
                {
                  "id": "campaign-1",
                  "name": "Grey Havens",
                  "rulesetId": "core-1.6",
                  "settlements": [
                    {
                      "id": "settlement-1",
                      "campaignId": "campaign-1",
                      "name": "First Cradle",
                      "state": {
                        "locations": {"built": ["Lantern Hoard"]},
                        "gear": ["Daga de Hueso"],
                        "innovations": {
                          "innovations": [
                            {"name": "Lenguaje", "description": "Se pueden hablar.", "consequences": ["Narración"]}
                          ]
                        }
                      }
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val document = json.decodeFromString<TransferDocument>(v11)
        val state =
            document.campaigns
                .single()
                .settlements
                .single()
                .state

        assertEquals(11, document.schemaVersion)
        assertEquals(listOf(LocationId("lantern_hoard")), state.locations.built)
        assertEquals(listOf(GearId("daga_de_hueso")), state.gear)
        val innovation = state.innovations.innovations.single()
        assertEquals(InnovationId("language"), innovation.id)
        assertEquals(LocalizedText(es = "Lenguaje"), innovation.names)
        assertEquals(listOf("Narración"), innovation.consequences)
    }

    @Test
    fun aVersionTenDocumentStillDecodes() {
        // v11 re-keyed the resource ledger to ids; v10 named resources by display string, and
        // those documents must keep importing rather than resetting to an empty settlement.
        val document = """{"schemaVersion":10,"exportedAt":"x","campaigns":[]}"""

        assertEquals(10, Json.decodeFromString(TransferDocument.serializer(), document).schemaVersion)
    }
}

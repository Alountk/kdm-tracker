package io.kdmtracker.core.transfer

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Timeline
import io.kdmtracker.core.domain.TimelineEntry
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementStateTransferTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun ulid(seed: Long): Ulid = Ulid.generate(seed) { 0 }

    private val campaign =
        Campaign(
            id = ulid(1),
            name = "Grey Havens",
            rulesetId = "core-1.6",
            rulesetOverrides = RulesetOverrides(),
            settlementIds = listOf(ulid(2)),
            sync = SyncMeta(HybridLogicalClock(1_700_000_000_000L, 4), rev = 7),
        )

    private val settlement =
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
        )

    private val populatedState =
        SettlementState(
            timeline =
                Timeline(
                    entries = listOf(TimelineEntry(1, "Returning Survivors"), TimelineEntry(4, "Story Event")),
                ),
            innovations =
                InnovationCollection(
                    innovations =
                        listOf(
                            Innovation(
                                name = "Lantern Oven",
                                description = "Cook at the lantern oven",
                                consequences = listOf("Cooking", "Storytelling"),
                                survivalLimitBonus = 1,
                            ),
                            Innovation(
                                name = "Paint",
                                description = "Paint the settlement",
                                consequences = listOf("Face Painting"),
                            ),
                        ),
                ),
            locations = LocationSet(built = listOf("Lantern Oven", "Organ Grinder")),
            resourceEntries =
                listOf(
                    ResourceEntry(ResourceId("Bone"), 3),
                    ResourceEntry(ResourceId("Bone"), -1),
                    ResourceEntry(ResourceId("Skull"), 2),
                ),
            deaths = 4,
            births = 2,
            survivors = List(9) { Survivor(id = Ulid.generate(100L + it) { 0 }) },
        )

    @Test
    fun mapperRoundTripsSettlementRecordWithPopulatedState() {
        val records = listOf(SettlementRecord(settlement, populatedState))

        val (domainCampaign, domainRecords) = TransferMapper.toDomain(TransferMapper.toDto(campaign, records))

        assertEquals(campaign, domainCampaign)
        assertEquals(records, domainRecords)
    }

    @Test
    fun populatedStateRoundTripsThroughTransferDocumentJson() {
        val records = listOf(SettlementRecord(settlement, populatedState))
        val document =
            TransferDocument(
                exportedAt = "2026-10-05T00:00:00Z",
                campaigns = listOf(TransferMapper.toDto(campaign, records)),
            )

        val encoded = json.encodeToString(document)
        val decoded = json.decodeFromString<TransferDocument>(encoded)
        val (domainCampaign, domainRecords) = TransferMapper.toDomain(decoded.campaigns.single())

        assertEquals(campaign, domainCampaign)
        assertEquals(records, domainRecords)
    }

    @Test
    fun v4DocumentWithoutStateDecodesToEmptyState() {
        val v4 =
            """
            {
              "schemaVersion": 4,
              "exportedAt": "2024-01-01T00:00:00Z",
              "campaigns": [
                {
                  "id": "${campaign.id.value}",
                  "name": "Grey Havens",
                  "rulesetId": "core-1.6",
                  "settlementIds": ["${settlement.id.value}"],
                  "settlements": [
                    {
                      "id": "${settlement.id.value}",
                      "campaignId": "${campaign.id.value}",
                      "name": "First Cradle"
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val document = json.decodeFromString<TransferDocument>(v4)

        assertEquals(4, document.schemaVersion)
        assertEquals(
            SettlementState(),
            document.campaigns
                .single()
                .settlements
                .single()
                .state,
        )
    }

    @Test
    fun newDocumentDefaultsToSchemaVersionEleven() {
        val document = TransferDocument(exportedAt = "2026-10-05T00:00:00Z", campaigns = emptyList())

        assertEquals(11, document.schemaVersion)
    }

    @Test
    fun supportedVersionsIncludesTen() {
        assertTrue(10 in TransferDocument.SUPPORTED_VERSIONS)
    }

    @Test
    fun v5DocumentWithoutPrinciplesDecodes() {
        val v5 =
            """
            {
              "schemaVersion": 5,
              "exportedAt": "2025-01-01T00:00:00Z",
              "campaigns": [
                {
                  "id": "${campaign.id.value}",
                  "name": "Grey Havens",
                  "rulesetId": "core-1.6",
                  "settlementIds": ["${settlement.id.value}"],
                  "settlements": [
                    {
                      "id": "${settlement.id.value}",
                      "campaignId": "${campaign.id.value}",
                      "name": "First Cradle",
                      "state": { "deaths": 2 }
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val document = json.decodeFromString<TransferDocument>(v5)
        val state =
            document.campaigns
                .single()
                .settlements
                .single()
                .state

        assertEquals(5, document.schemaVersion)
        assertTrue(state.principles.isEmpty())
        assertTrue(state.resolvedMilestones.isEmpty())
        assertEquals(2, state.deaths)
    }

    @Test
    fun v6RoundTripCarriesPrinciplesAndMilestones() {
        val state =
            populatedState.copy(
                principles = mapOf(PrincipleCategory.SOCIETY to "Collective Toil"),
                resolvedMilestones = setOf(Core16Milestones.PRINCIPLE_DEATH),
            )
        val records = listOf(SettlementRecord(settlement, state))
        val document =
            TransferDocument(
                schemaVersion = 6,
                exportedAt = "2026-10-08T00:00:00Z",
                campaigns = listOf(TransferMapper.toDto(campaign, records)),
            )

        val decoded = json.decodeFromString<TransferDocument>(json.encodeToString(document))
        val (_, domainRecords) = TransferMapper.toDomain(decoded.campaigns.single())

        assertEquals(records, domainRecords)
        assertEquals(6, decoded.schemaVersion)
    }
}

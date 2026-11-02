package io.kdmtracker.core.transfer

import io.kdmtracker.core.content.MonsterNode
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CampaignNodeChoicesTransferTest {
    private val campaign =
        Campaign(
            id = Ulid.generate(1) { 0 },
            name = "Grey Havens",
            rulesetId = "core-1.6",
            rulesetOverrides = RulesetOverrides(),
            settlementIds = emptyList(),
            sync = SyncMeta(HybridLogicalClock(0, 0), rev = 0),
        )

    @Test
    fun aCampaignWithoutChoicesHoldsNone() {
        assertTrue(campaign.nodeChoices.isEmpty())
    }

    @Test
    fun theChoicesRoundTripThroughTheTransferMapper() {
        val chosen = campaign.copy(nodeChoices = mapOf(MonsterNode.NQ1 to "Gorm", MonsterNode.NN2 to "King's Man"))

        val (roundTripped, _) = TransferMapper.toDomain(TransferMapper.toDto(chosen, emptyList()))

        assertEquals(chosen, roundTripped)
    }

    @Test
    fun aDocumentWrittenBeforeTheChoicesExistedDecodesWithNone() {
        val old = """{"id":"${campaign.id.value}","name":"Old","rulesetId":"core-1.6"}"""

        val (decoded, _) = TransferMapper.toDomain(Json.decodeFromString(CampaignDto.serializer(), old))

        assertTrue(decoded.nodeChoices.isEmpty())
    }

    @Test
    fun aNodeThisBuildDoesNotKnowIsDroppedInsteadOfFailing() {
        val dto =
            TransferMapper.toDto(campaign, emptyList()).copy(
                nodeChoices =
                    mapOf(
                        "NQ9" to "Future",
                        "NQ1" to "Gorm",
                    ),
            )

        val (decoded, _) = TransferMapper.toDomain(dto)

        assertEquals(mapOf(MonsterNode.NQ1 to "Gorm"), decoded.nodeChoices)
    }
}

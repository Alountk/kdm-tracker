package io.kdmtracker.core.transfer

import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.rules.MilestoneConditionSpec
import kotlinx.serialization.Serializable

/**
 * Top-level, versioned container for an exported campaign database.
 *
 * [schemaVersion] defaults to the current schema (10). Older v1/v2/v3/v4/v5/v6/v7 documents omit the
 * newer fields and still decode, because every added field carries a default. v4
 * replaced the v3 `MilestoneDefinitionDto.key` enum with a stable string [MilestoneDefinitionDto.id]
 * plus a typed [MilestoneConditionSpec] and a prose `description`; a v3 document that
 * carried milestone overrides is not auto-migrated. v5 carries the settlement's extra
 * [SettlementState] on each [SettlementDto]; a pre-v5 document that omits `state` decodes
 * to an empty state. v6 adds the settlement's chosen `principles` and `resolvedMilestones`
 * to [SettlementState]; a pre-v6 state that omits them decodes to empty. v7 adds the
 * settlement's survivor roster to [SettlementState]; a pre-v7 state that omits `survivors`
 * decodes to an empty roster and is seeded from the legacy [SettlementDto.population] on decode.
 * v8 adds the settlement's hunt state to [SettlementState]; a pre-v8 state that omits `hunt`
 * decodes to an empty [io.kdmtracker.core.domain.HuntState].
 */
@Serializable
data class TransferDocument(
    val schemaVersion: Int = 10,
    val exportedAt: String,
    val campaigns: List<CampaignDto>,
) {
    companion object {
        /** Schema versions this build can read. */
        val SUPPORTED_VERSIONS: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }
}

/** Transfer representation of a [io.kdmtracker.core.domain.Campaign], embedding its settlements. */
@Serializable
data class CampaignDto(
    val id: String,
    val name: String,
    val rulesetId: String,
    val rulesetOverrides: RulesetOverridesDto = RulesetOverridesDto(),
    val settlementIds: List<String> = emptyList(),
    val sync: SyncMetaDto = SyncMetaDto(),
    val settlements: List<SettlementDto> = emptyList(),
    val enabledPacks: List<String> = emptyList(),
)

/**
 * Transfer representation of a [io.kdmtracker.core.domain.Settlement].
 *
 * [state] is the settlement's extra record (timeline, innovations, locations, resource
 * ledger and counters). It defaults to an empty state so a pre-v5 document that omits it
 * decodes unchanged.
 */
@Serializable
data class SettlementDto(
    val id: String,
    val campaignId: String,
    val name: String,
    val lanternYear: Int = 1,
    val phase: SettlementPhase = SettlementPhase.SETTLEMENT,
    val currentStep: Int = 1,
    val endeavors: Int = 0,
    val population: Int = 4,
    val settlementEvent: String? = null,
    val sync: SyncMetaDto = SyncMetaDto(),
    val state: SettlementState = SettlementState(),
)

/** Transfer representation of [io.kdmtracker.core.ruleset.RulesetOverrides]. */
@Serializable
data class RulesetOverridesDto(
    val survivalLimit: Int? = null,
    val extraMilestones: List<MilestoneDefinitionDto> = emptyList(),
    val principleOverrides: Map<PrincipleCategory, List<PrincipleChoiceDto>> = emptyMap(),
)

/** Transfer representation of [io.kdmtracker.core.ruleset.MilestoneDefinition]. */
@Serializable
data class MilestoneDefinitionDto(
    val id: String,
    val condition: MilestoneConditionSpec,
    val description: String,
)

/** Transfer representation of [io.kdmtracker.core.ruleset.PrincipleChoice]. */
@Serializable
data class PrincipleChoiceDto(
    val category: PrincipleCategory,
    val label: String,
    val effect: String,
)

/** Transfer representation of [io.kdmtracker.core.domain.SyncMeta]. */
@Serializable
data class SyncMetaDto(
    val updatedAt: HybridLogicalClockDto = HybridLogicalClockDto(),
    val rev: Long = 0,
    val deletedAt: HybridLogicalClockDto? = null,
)

/** Transfer representation of [io.kdmtracker.core.domain.HybridLogicalClock]. */
@Serializable
data class HybridLogicalClockDto(
    val millis: Long = 0,
    val counter: Int = 0,
)

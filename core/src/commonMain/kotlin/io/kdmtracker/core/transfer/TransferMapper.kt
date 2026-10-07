package io.kdmtracker.core.transfer

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.MilestoneDefinition
import io.kdmtracker.core.ruleset.PrincipleChoice
import io.kdmtracker.core.ruleset.RulesetOverrides

/** Converts between the domain aggregates and their versioned transfer DTOs. */
object TransferMapper {
    /**
     * Maps a [campaign] and its settlement [records] to a single [CampaignDto].
     *
     * The records are embedded in the returned DTO, each carrying its full
     * [SettlementRecord.state]; the campaign's own [Campaign.settlementIds] are preserved
     * so the mapping stays lossless.
     */
    fun toDto(
        campaign: Campaign,
        records: List<SettlementRecord>,
    ): CampaignDto =
        CampaignDto(
            id = campaign.id.value,
            name = campaign.name,
            rulesetId = campaign.rulesetId,
            rulesetOverrides = campaign.rulesetOverrides.toDto(),
            settlementIds = campaign.settlementIds.map { it.value },
            sync = campaign.sync.toDto(),
            settlements = records.map { it.toDto() },
            enabledPacks = campaign.enabledPacks,
        )

    /**
     * Maps a [CampaignDto] back into its [Campaign] and the settlement records embedded in it.
     *
     * The returned pair is equal to the pair passed to [toDto], provided the campaign's
     * settlement ids and the embedded records describe the same set.
     */
    fun toDomain(dto: CampaignDto): Pair<Campaign, List<SettlementRecord>> {
        val campaign =
            Campaign(
                id = Ulid.parse(dto.id),
                name = dto.name,
                rulesetId = dto.rulesetId,
                rulesetOverrides = dto.rulesetOverrides.toDomain(),
                settlementIds = dto.settlementIds.map { Ulid.parse(it) },
                sync = dto.sync.toDomain(),
                enabledPacks = dto.enabledPacks,
            )
        return campaign to dto.settlements.map { it.toDomain() }
    }

    private fun SettlementRecord.toDto(): SettlementDto =
        SettlementDto(
            id = settlement.id.value,
            campaignId = settlement.campaignId.value,
            name = settlement.name,
            lanternYear = settlement.lanternYear,
            phase = settlement.phase,
            currentStep = settlement.currentStep,
            endeavors = settlement.endeavors,
            population = settlement.population,
            settlementEvent = settlement.settlementEvent,
            sync = settlement.sync.toDto(),
            state = state,
        )

    private fun SettlementDto.toDomain(): SettlementRecord =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.parse(id),
                    campaignId = Ulid.parse(campaignId),
                    name = name,
                    lanternYear = lanternYear,
                    phase = phase,
                    currentStep = currentStep,
                    endeavors = endeavors,
                    population = population,
                    settlementEvent = settlementEvent,
                    sync = sync.toDomain(),
                ),
            state = state,
        )

    private fun RulesetOverrides.toDto(): RulesetOverridesDto =
        RulesetOverridesDto(
            survivalLimit = survivalLimit,
            extraMilestones =
                extraMilestones.map {
                    MilestoneDefinitionDto(id = it.id.value, condition = it.condition, description = it.description)
                },
            principleOverrides =
                principleOverrides.mapValues { (_, choices) ->
                    choices.map { PrincipleChoiceDto(category = it.category, label = it.label, effect = it.effect) }
                },
        )

    private fun RulesetOverridesDto.toDomain(): RulesetOverrides =
        RulesetOverrides(
            survivalLimit = survivalLimit,
            extraMilestones =
                extraMilestones.map {
                    MilestoneDefinition(
                        id = MilestoneId(it.id),
                        condition = it.condition,
                        description = it.description,
                    )
                },
            principleOverrides =
                principleOverrides.mapValues { (_, choices) ->
                    choices.map { PrincipleChoice(category = it.category, label = it.label, effect = it.effect) }
                },
        )

    private fun SyncMeta.toDto(): SyncMetaDto =
        SyncMetaDto(
            updatedAt = updatedAt.toDto(),
            rev = rev,
            deletedAt = deletedAt?.toDto(),
        )

    private fun SyncMetaDto.toDomain(): SyncMeta =
        SyncMeta(
            updatedAt = updatedAt.toDomain(),
            rev = rev,
            deletedAt = deletedAt?.toDomain(),
        )

    private fun HybridLogicalClock.toDto(): HybridLogicalClockDto =
        HybridLogicalClockDto(millis = millis, counter = counter)

    private fun HybridLogicalClockDto.toDomain(): HybridLogicalClock =
        HybridLogicalClock(millis = millis, counter = counter)
}

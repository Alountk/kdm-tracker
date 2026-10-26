package io.kdmtracker.app

import io.kdmtracker.core.content.Core16Content
import io.kdmtracker.core.content.Legacy15Content
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.SyncClock
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.RulesetOverrides
import kotlin.random.Random

/** The campaign, settlement and opening record a new campaign starts with. */
internal class NewCampaignFactory(
    private val clock: SyncClock,
) {
    /** Campaign first, then settlement: each takes its own clock tick, in that order. */
    fun build(
        campaignId: Ulid,
        settlementId: Ulid,
        campaignName: String,
        settlementName: String,
    ): Pair<Campaign, Settlement> {
        val campaign =
            Campaign(
                id = campaignId,
                name = campaignName,
                rulesetId = Core16Ruleset.id,
                rulesetOverrides = RulesetOverrides(),
                settlementIds = listOf(settlementId),
                sync = SyncMeta(clock.tick(null), rev = 1),
                enabledPacks = listOf(Core16Content.pack.id, Legacy15Content.pack.id),
            )
        val settlement =
            Settlement
                .new(id = settlementId, campaignId = campaignId, name = settlementName)
                .copy(sync = SyncMeta(clock.tick(null), rev = 1))
        return campaign to settlement
    }

    /**
     * Opens with the Lantern Hoard and Armería built: Innovate and Craft refuse unbuilt locations,
     * so without them Step 7 would be unreachable.
     */
    fun startingRecord(settlement: Settlement): SettlementRecord =
        SettlementRecord(
            settlement = settlement,
            state =
                SettlementState(
                    locations = LocationSet(built = listOf(STARTING_LOCATION, CRAFT_LOCATION)),
                    innovations = InnovationCollection(STARTING_INNOVATIONS.map { it.copy(unlockedInYear = 1) }),
                ),
        ).withSurvivors(
            List(FOUNDING_SURVIVORS) {
                Survivor(id = Ulid.generate(clock.tick(null).millis) { Random.nextInt() })
            },
        )
}

/**
 * `Language` (1.6) and `Lenguaje` (1.5) share an id, so one definition is seeded carrying both
 * packs' consequences.
 */
private val STARTING_INNOVATIONS: List<Innovation> =
    listOfNotNull(
        Core16Content.pack.bundle.innovations
            .firstOrNull { it.id == InnovationId("language") }
            ?.let { seed ->
                val legacy =
                    Legacy15Content.pack.bundle.innovations
                        .firstOrNull { it.id == InnovationId("language") }
                if (legacy ==
                    null
                ) {
                    seed
                } else {
                    seed.copy(consequences = (seed.consequences + legacy.consequences).distinct())
                }
            },
    )

/** The location every settlement opens with, so Innovate is offered at a built location. */
private val STARTING_LOCATION = LocationId("lantern_hoard")

/** The location the curated 1.5 crafting recipes live on, built so Craft is reachable. */
private val CRAFT_LOCATION = LocationId("weapon_crafter")

/** The founding survivors a new settlement opens with. */
private const val FOUNDING_SURVIVORS = 4

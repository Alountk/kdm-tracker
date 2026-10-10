package io.kdmtracker.core.domain

import io.kdmtracker.core.rules.MilestoneState
import io.kdmtracker.core.ruleset.Core16Ruleset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementStateTest {
    private val settlement =
        Settlement(
            id = Ulid.generate(1) { 0 },
            campaignId = Ulid.generate(2) { 0 },
            name = "Kingsport",
            lanternYear = 1,
            phase = SettlementPhase.SETTLEMENT,
            currentStep = 1,
            endeavors = 0,
            population = 4,
            settlementEvent = null,
            sync = SyncMeta(HybridLogicalClock(0, 0)),
        )

    private fun record(
        population: Int = 4,
        deaths: Int = 0,
        births: Int = 0,
        innovations: List<Innovation> = emptyList(),
        resourceEntries: List<ResourceEntry> = emptyList(),
    ): SettlementRecord =
        SettlementRecord(
            settlement = settlement.copy(population = population),
            state =
                SettlementState(
                    innovations = InnovationCollection(innovations),
                    resourceEntries = resourceEntries,
                    deaths = deaths,
                    births = births,
                    survivors = List(population) { Survivor(id = Ulid.generate(it.toLong() + 10) { 0 }) },
                ),
        )

    @Test
    fun storageIsProjectedFromTheResourceEntries() {
        val state =
            SettlementState(
                resourceEntries =
                    listOf(
                        ResourceEntry(ResourceId("Bone"), delta = 3),
                        ResourceEntry(ResourceId("Bone"), delta = -1),
                        ResourceEntry(ResourceId("Hide"), delta = 2),
                    ),
            )

        assertEquals(
            listOf(
                StorageLine(ResourceId("Bone"), quantity = 2),
                StorageLine(ResourceId("Hide"), quantity = 2),
            ),
            state.storage(),
        )
    }

    @Test
    fun survivalLimitIsDerivedFromTheRulesetPlusInnovationBonuses() {
        val state =
            SettlementState(
                innovations =
                    InnovationCollection(
                        listOf(
                            Innovation(
                                id = InnovationId("language"),
                                names = LocalizedText(es = "Language"),
                                description = "",
                                survivalLimitBonus = 1,
                            ),
                            Innovation(
                                id = InnovationId("paint"),
                                names = LocalizedText(es = "Paint"),
                                description = "",
                                survivalLimitBonus = 1,
                            ),
                        ),
                    ),
            )

        assertEquals(Core16Ruleset.survivalLimit + 2, state.survivalLimit(Core16Ruleset))
    }

    @Test
    fun milestoneStateAdaptsTheCountersToTheSettlementPopulation() {
        val record =
            record(
                population = 7,
                deaths = 2,
                births = 3,
                innovations =
                    listOf(
                        Innovation(
                            id = InnovationId("language"),
                            names = LocalizedText(es = "Language"),
                            description = "",
                        ),
                        Innovation(id = InnovationId("paint"), names = LocalizedText(es = "Paint"), description = ""),
                    ),
            )

        assertEquals(
            MilestoneState(deaths = 2, births = 3, population = 7, innovations = 2),
            record.milestoneState(),
        )
    }

    @Test
    fun aFreshSettlementReachesNoMilestones() {
        assertTrue(record().reachedMilestones(Core16Ruleset).isEmpty())
    }

    @Test
    fun aDefaultSettlementRecordStartsWithFourSurvivorsAndReachesNoMilestones() {
        val record = record()

        assertEquals(4, record.population)
        assertTrue(record.reachedMilestones(Core16Ruleset).isEmpty())
    }

    @Test
    fun fifteenPopulationReachesSociety() {
        assertTrue(Core16Milestones.PRINCIPLE_SOCIETY in record(population = 15).reachedMilestones(Core16Ruleset))
    }

    @Test
    fun zeroPopulationReachesGameOver() {
        assertTrue(Core16Milestones.GAME_OVER in record(population = 0).reachedMilestones(Core16Ruleset))
    }

    @Test
    fun oneDeathReachesTheDeathMilestone() {
        assertTrue(Core16Milestones.PRINCIPLE_DEATH in record(deaths = 1).reachedMilestones(Core16Ruleset))
    }

    @Test
    fun fiveInnovationsReachTheHoodedKnight() {
        val innovations =
            List(5) {
                Innovation(
                    id = InnovationId("innovation_it"),
                    names = LocalizedText(es = "Innovation $it"),
                    description = "",
                )
            }

        assertTrue(Core16Milestones.HOODED_KNIGHT in record(innovations = innovations).reachedMilestones(Core16Ruleset))
    }

    @Test
    fun reachedMilestonesReturnsExactlyTheReachedSet() {
        val reached =
            record(
                population = 15,
                deaths = 1,
                births = 1,
                innovations =
                    List(5) {
                        Innovation(
                            id = InnovationId("innovation_it"),
                            names = LocalizedText(es = "Innovation $it"),
                            description = "",
                        )
                    },
            ).reachedMilestones(Core16Ruleset)

        assertEquals(
            setOf(
                Core16Milestones.PRINCIPLE_DEATH,
                Core16Milestones.PRINCIPLE_NEW_LIFE,
                Core16Milestones.PRINCIPLE_SOCIETY,
                Core16Milestones.HOODED_KNIGHT,
            ),
            reached,
        )
    }
}

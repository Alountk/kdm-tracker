package io.kdmtracker.core.ruleset

import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.rules.MilestoneConditionSpec
import io.kdmtracker.core.rules.MilestoneMetric
import io.kdmtracker.core.rules.SettlementStep
import io.kdmtracker.core.rules.SettlementStepCatalog

/**
 * Shipped Kingdom Death: Monster 1.6 rules content, the default ruleset for a
 * new campaign.
 *
 * Content condensed from `static/prototypes/research/lantern-year-steps.md`
 * (§2 steps 5–6) and the official Settlement Record Sheet "Milestone Story
 * Events". The markdown is a research artifact and is never parsed at runtime.
 */
object Core16Ruleset : Ruleset {
    override val id: String = "core-1.6"
    override val version: String = "1.6"
    override val settlementSteps: List<SettlementStep> = SettlementStepCatalog.ALL

    override val specialShowdownStep: Int = 9
    override val deathCountStep: Int = 5
    override val endeavorsPerReturnee: Int = 1

    /**
     * The 1.6 baseline survival limit for a new settlement.
     *
     * This is an editable ruleset value, not a hardcoded rule: innovations and
     * settlement locations may raise it and campaigns may override it.
     */
    override val survivalLimit: Int = 1

    override val milestones: List<MilestoneDefinition> =
        listOf(
            MilestoneDefinition(
                Core16Milestones.PRINCIPLE_DEATH,
                MilestoneConditionSpec.AtLeast(MilestoneMetric.DEATHS, 1),
                "1 death",
            ),
            MilestoneDefinition(
                Core16Milestones.PRINCIPLE_NEW_LIFE,
                MilestoneConditionSpec.AtLeast(MilestoneMetric.BIRTHS, 1),
                "first child is born",
            ),
            MilestoneDefinition(
                Core16Milestones.PRINCIPLE_SOCIETY,
                MilestoneConditionSpec.AtLeast(MilestoneMetric.POPULATION, 15),
                "population reaches 15",
            ),
            MilestoneDefinition(
                Core16Milestones.HOODED_KNIGHT,
                MilestoneConditionSpec.AtLeast(MilestoneMetric.INNOVATIONS, 5),
                "settlement has 5 innovations",
            ),
            MilestoneDefinition(
                Core16Milestones.GAME_OVER,
                MilestoneConditionSpec.AtMost(MilestoneMetric.POPULATION, 0),
                "population reaches 0",
            ),
        )

    override val principles: Map<PrincipleCategory, List<PrincipleChoice>> =
        mapOf(
            PrincipleCategory.DEATH to
                listOf(
                    PrincipleChoice(
                        PrincipleCategory.DEATH,
                        "The First Harvest → Cannibalize",
                        "Cannibalize: roll 1d10 — 1-5 all departing survivors gain +3 insanity " +
                            "and a Founding Stone; 6-10 a nominated survivor gains +1 permanent speed.",
                    ),
                    PrincipleChoice(
                        PrincipleCategory.DEATH,
                        "The First Grave → Graves",
                        "Graves: roll 1d10 — 1-5 all departing survivors gain +1 survival and +1 " +
                            "understanding; 6-10 a nominated survivor gains +1 permanent luck.",
                    ),
                ),
            PrincipleCategory.NEW_LIFE to
                listOf(
                    PrincipleChoice(
                        PrincipleCategory.NEW_LIFE,
                        "Without Mercy → Survival of the Fittest",
                        "Survival of the Fittest: roll 1d10 — 1-5 parents gain +1 permanent " +
                            "strength and +2 insanity and skip the next hunt; 6-10 the newborn gains " +
                            "+2 courage, the Tough fighting art and a random disorder.",
                    ),
                    PrincipleChoice(
                        PrincipleCategory.NEW_LIFE,
                        "With Affection → Protect the Young",
                        "Protect the Young: roll 1d10 — 1-5 parents gain +2 survival and +1 " +
                            "permanent evasion; 6-10 the newborn gains +2 understanding and the " +
                            "Strategist fighting art.",
                    ),
                ),
            PrincipleCategory.SOCIETY to
                listOf(
                    PrincipleChoice(
                        PrincipleCategory.SOCIETY,
                        "Collective Toil",
                        "Collective Toil: -1 population; add the Bone Witch story event to the " +
                            "timeline 3 years from now; roll 1d10 — 1-3 all survivors gain survival " +
                            "up to the survival limit; 4-10 gain +3 population, 2 random White Lion " +
                            "resources and 1 skull.",
                    ),
                    PrincipleChoice(
                        PrincipleCategory.SOCIETY,
                        "Accept Darkness",
                        "Accept Darkness: roll 1d10 — 1-5 nominate up to 4 survivors, each gains " +
                            "+10 insanity; 6-10 a nominated survivor becomes the leader and gains " +
                            "the Quixotic disorder plus the Leader and Orator of Death fighting arts.",
                    ),
                ),
        )
}

package io.kdmtracker.core.content

import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.SettlementLocation
import io.kdmtracker.core.domain.Timeline

/**
 * The shipped `core-1.6` content pack — a **seed**, not an exhaustive transcription
 * of the base game.
 *
 * It carries just enough typed content for a new campaign to compose against: the
 * settlement location tree, the initial innovation with its consequences, the base
 * resources with their categories, the timeline entries, and the core quarries and
 * nemeses. Full content (every card from the 1.5/1.6 sets) is ingested later by the
 * content pipeline and split across edition + expansion packs; this object exists so
 * `:core` is playable without it.
 */
object Core16Content {
    val pack: ContentPack =
        ContentPack(
            id = "core-1.6",
            version = "1.6",
            name = "Kingdom Death: Monster 1.6",
            bundle =
                ContentBundle(
                    innovations =
                        listOf(
                            Innovation(
                                name = "Language",
                                description = "The survivors can speak and record what they learn.",
                                consequences = listOf("Storytelling"),
                            ),
                        ),
                    locations =
                        listOf(
                            SettlementLocation("Lantern Hoard"),
                            SettlementLocation("Bone Smith", requires = listOf("Lantern Hoard")),
                            SettlementLocation("Skinnery"),
                            SettlementLocation("Organ Grinder"),
                            SettlementLocation("Weapon Crafter"),
                            SettlementLocation("Leather Worker"),
                            SettlementLocation("Stone Circle"),
                            SettlementLocation("Exhausted Lantern Hoard"),
                            SettlementLocation("Barber Surgeon"),
                            SettlementLocation("Blacksmith"),
                            SettlementLocation("Catarium"),
                            SettlementLocation("Dragon Armory"),
                            SettlementLocation("Gormchymist"),
                            SettlementLocation("Gormery"),
                            SettlementLocation("Mask Maker"),
                            SettlementLocation("Plumery"),
                            SettlementLocation("Silk Mill"),
                            SettlementLocation("Skyreef Sanctuary"),
                            SettlementLocation("Wet Resin Crafter"),
                        ),
                    resources =
                        listOf(
                            ResourceDefinition("Bone", ResourceCategory.BASIC),
                            ResourceDefinition("Hide", ResourceCategory.BASIC),
                            ResourceDefinition("Organ", ResourceCategory.BASIC),
                            ResourceDefinition("Scrap", ResourceCategory.BASIC),
                        ),
                    timeline = Timeline.core16().entries,
                    quarries = listOf("White Lion", "Screaming Antelope", "Phoenix"),
                    nemeses = listOf("The Butcher", "King's Man", "The Hand"),
                ),
        )
}

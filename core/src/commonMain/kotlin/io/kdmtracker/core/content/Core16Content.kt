package io.kdmtracker.core.content

import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.ResourceMaterial
import io.kdmtracker.core.domain.SettlementAction
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
                                id = InnovationId("language"),
                                names = LocalizedText(es = "Lenguaje", en = "Language"),
                                description = "The survivors can speak and record what they learn.",
                                consequences = listOf("Storytelling"),
                                category = "Sociedad",
                            ),
                        ),
                    locations =
                        listOf(
                            SettlementLocation(
                                id = LocationId("lantern_hoard"),
                                names = LocalizedText(es = "Cúmulo de Linternas", en = "Lantern Hoard"),
                                actions =
                                    listOf(
                                        SettlementAction(
                                            names = LocalizedText(es = "Innovar", en = "Innovate"),
                                            kind = ActionKind.INNOVATE,
                                            endeavorCost = 1,
                                            resourceCost =
                                                mapOf(
                                                    ResourceId("bone.monster") to 1,
                                                    ResourceId("organ.monster") to 1,
                                                    ResourceId("hide.monster") to 1,
                                                ),
                                            oncePerPhase = true,
                                            draw = 2,
                                            keep = 1,
                                            effect = "Draw 2 innovation cards, keep 1, return the other to the deck.",
                                        ),
                                    ),
                            ),
                            SettlementLocation(
                                id = LocationId("bone_smith"),
                                names = LocalizedText(es = "Taller de Huesos", en = "Bone Smith"),
                                requires = listOf(LocationId("lantern_hoard")),
                            ),
                            SettlementLocation(
                                id = LocationId("skinnery"),
                                names = LocalizedText(es = "Peletería", en = "Skinnery"),
                            ),
                            SettlementLocation(
                                id = LocationId("organ_grinder"),
                                names = LocalizedText(es = "Molino de Órganos", en = "Organ Grinder"),
                            ),
                            SettlementLocation(
                                id = LocationId("weapon_crafter"),
                                names = LocalizedText(es = "Armería", en = "Weapon Crafter"),
                            ),
                            SettlementLocation(
                                id = LocationId("leather_worker"),
                                names = LocalizedText(es = "Curtiduría", en = "Leather Worker"),
                            ),
                            SettlementLocation(
                                id = LocationId("stone_circle"),
                                names = LocalizedText(es = "Círculo de Piedra", en = "Stone Circle"),
                            ),
                            SettlementLocation(
                                id = LocationId("exhausted_lantern_hoard"),
                                names =
                                    LocalizedText(
                                        es = "Cúmulo de Linternas Extinto",
                                        en = "Exhausted Lantern Hoard",
                                    ),
                            ),
                            SettlementLocation(
                                id = LocationId("barber_surgeon"),
                                names = LocalizedText(es = "Cirujano Barbero", en = "Barber Surgeon"),
                            ),
                            SettlementLocation(
                                id = LocationId("blacksmith"),
                                names = LocalizedText(es = "Herrería", en = "Blacksmith"),
                            ),
                            SettlementLocation(
                                id = LocationId("catarium"),
                                names = LocalizedText(es = "Felinarium", en = "Catarium"),
                            ),
                            SettlementLocation(
                                id = LocationId("dragon_armory"),
                                names = LocalizedText(es = "Dragon Armory", en = "Dragon Armory"),
                                description =
                                    LocalizedText(
                                        es =
                                            "Taller de armamento pesado donde se ensamblan piezas a partir de " +
                                                "los restos de dragón.",
                                    ),
                            ),
                            SettlementLocation(
                                id = LocationId("gormchymist"),
                                names = LocalizedText(es = "Gormchymist", en = "Gormchymist"),
                                description =
                                    LocalizedText(
                                        es =
                                            "Alquimia del asentamiento: destila sustancias, ungüentos y venenos " +
                                                "con los ingredientes recogidos.",
                                    ),
                            ),
                            SettlementLocation(
                                id = LocationId("gormery"),
                                names = LocalizedText(es = "Gormery", en = "Gormery"),
                                description =
                                    LocalizedText(
                                        es =
                                            "Curtido y confección dedicados a las pieles más difíciles de " +
                                                "trabajar.",
                                    ),
                            ),
                            SettlementLocation(
                                id = LocationId("mask_maker"),
                                names = LocalizedText(es = "Artesano de Máscaras", en = "Mask Maker"),
                            ),
                            SettlementLocation(
                                id = LocationId("plumery"),
                                names = LocalizedText(es = "Plumería", en = "Plumery"),
                            ),
                            SettlementLocation(
                                id = LocationId("silk_mill"),
                                names = LocalizedText(es = "Silk Mill", en = "Silk Mill"),
                                description = LocalizedText(es = "Molino dedicado al hilado y al tejido de seda."),
                            ),
                            SettlementLocation(
                                id = LocationId("skyreef_sanctuary"),
                                names = LocalizedText(es = "Skyreef Sanctuary", en = "Skyreef Sanctuary"),
                                description =
                                    LocalizedText(
                                        es =
                                            "Santuario que sirve de refugio y lugar de retiro para los " +
                                                "supervivientes.",
                                    ),
                            ),
                            SettlementLocation(
                                id = LocationId("wet_resin_crafter"),
                                names = LocalizedText(es = "Wet Resin Crafter", en = "Wet Resin Crafter"),
                                description =
                                    LocalizedText(
                                        es =
                                            "Taller donde se trabaja la resina fresca para sellar y reforzar el " +
                                                "equipo.",
                                    ),
                            ),
                        ),
                    resources =
                        listOf(
                            ResourceDefinition(
                                id = ResourceId("bone.monster"),
                                names = LocalizedText(es = "Hueso de Monstruo", en = "Bone"),
                                category = ResourceCategory.BASIC,
                                materials = setOf(ResourceMaterial.BONE),
                            ),
                            ResourceDefinition(
                                id = ResourceId("hide.monster"),
                                names = LocalizedText(es = "Piel de Monstruo", en = "Hide"),
                                category = ResourceCategory.BASIC,
                                materials = setOf(ResourceMaterial.HIDE),
                            ),
                            ResourceDefinition(
                                id = ResourceId("organ.monster"),
                                names = LocalizedText(es = "Órgano de Monstruo", en = "Organ"),
                                category = ResourceCategory.BASIC,
                                materials = setOf(ResourceMaterial.ORGAN),
                            ),
                            ResourceDefinition(
                                id = ResourceId("scrap"),
                                names = LocalizedText(es = "Scrap", en = "Scrap"),
                                category = ResourceCategory.BASIC,
                                materials = setOf(ResourceMaterial.SCRAP),
                            ),
                        ),
                    timeline = Timeline.core16().entries,
                    quarries = listOf("White Lion", "Screaming Antelope", "Phoenix"),
                    nemeses = listOf("The Butcher", "King's Man", "The Hand", "The Watcher", "Golden Smoke Knight"),
                ),
        )
}

package io.kdmtracker.core.content

import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.ResourceId

/**
 * The bridge from a content entity's **pre-id display name** to its stable id.
 *
 * Before this change the display name *was* the identity, so every persisted ledger, every gear
 * recipe and every location build cost refers to an entity by name. Those references still
 * resolve here, and — more importantly — a settlement persisted by an older build decodes through
 * this table instead of losing its entries.
 *
 * The tables are the `spanishName`/`englishName` columns of the CSVs under
 * `tools/content-ingest/identity/`, plus the three English aliases the 1.6 seed uses for the
 * basic resources. They are **transitional** bridges: as each family re-keys to ids, only the
 * legacy decode path reads its table.
 *
 * A name that is missing here is a content defect, not a user error: `ContentIdentityTest` asserts
 * every name the catalog can produce resolves, so the gap fails the build rather than the user.
 */
object ContentIdentity {
    private val byName: Map<String, ResourceId> =
        mapOf(
            "???" to ResourceId("unknown.monster_part"),
            "Acanto Fresco" to ResourceId("herb.fresh_acanthus"),
            "Apéndice de Linterna" to ResourceId("lantern.appendage"),
            "Aureola Brillante" to ResourceId("phoenix.halo"),
            "Bigotes Dorados" to ResourceId("whisker.golden"),
            "Bigotes de Fénix" to ResourceId("whisker.phoenix"),
            "Calavera" to ResourceId("skull.monster"),
            "Calavera Negra" to ResourceId("skull.black"),
            "Cangrejo Araña" to ResourceId("vermin.spider_crab"),
            "Cerebro Bramador" to ResourceId("brain.antelope"),
            "Cola de León" to ResourceId("tail.lion"),
            "Cresta de Fénix" to ResourceId("crest.phoenix"),
            "Crisol Perfecto" to ResourceId("crucible.perfect"),
            "Cucaracha Silbante" to ResourceId("vermin.whistling_cockroach"),
            "Cuerno en Espiral" to ResourceId("horn.spiral"),
            "Cuernos Legendarios" to ResourceId("horn.legendary"),
            "Cuero" to ResourceId("hide.leather"),
            "Dedo de Fénix" to ResourceId("finger.phoenix"),
            "Dientes de Felino Anciano" to ResourceId("tooth.elder_feline"),
            "Encías Musculosas" to ResourceId("gums.muscular"),
            "Escarabajo Espada" to ResourceId("vermin.sword_beetle"),
            "Espoleta" to ResourceId("fuse"),
            "Excremento Arcoiris" to ResourceId("droppings.rainbow"),
            "Excrementos Mucosos" to ResourceId("droppings.mucous"),
            "Farol Roto" to ResourceId("lantern.broken"),
            "Filete de Bestia" to ResourceId("steak.beast"),
            "Garra de León" to ResourceId("claw.lion"),
            "Garrapata de Pesadilla" to ResourceId("vermin.nightmare_tick"),
            "Gran Diente Plano" to ResourceId("tooth.flat_large"),
            "Grandes Huesos de Felino" to ResourceId("bone.feline_large"),
            "Hierro" to ResourceId("iron"),
            "Hormiga Solitaria" to ResourceId("vermin.lonely_ant"),
            "Hueso de Monstruo" to ResourceId("bone.monster"),
            "Hueso de la Pata" to ResourceId("bone.paw"),
            "Huesos Huecos de Ala" to ResourceId("bone.hollow_wing"),
            "Liquen Negro" to ResourceId("herb.black_lichen"),
            "Mano Curiosa" to ResourceId("hand.curious"),
            "Melena Brillante" to ResourceId("mane.shimmering"),
            "Membrana Protectora" to ResourceId("membrane.protective"),
            "Mosca Cíclope" to ResourceId("vermin.cyclopean_fly"),
            "Ojo de Felino" to ResourceId("eye.feline"),
            "Ojo de Fénix" to ResourceId("eye.phoenix"),
            "Parásitos Enanos de Mano" to ResourceId("vermin.hand_dwarf_parasite"),
            "Pelaje Blanco" to ResourceId("hide.white_lion"),
            "Pellejo" to ResourceId("hide.pelt"),
            "Pico de Ave" to ResourceId("beak.bird"),
            "Piel de Monstruo" to ResourceId("hide.monster"),
            "Plumas Pequeñas" to ResourceId("feather.small"),
            "Plumas de Cola" to ResourceId("feather.tail"),
            "Pústulas" to ResourceId("pustule.butcher"),
            "Segundo Corazón" to ResourceId("heart.second"),
            "Tendón" to ResourceId("sinew"),
            "Testículos de León" to ResourceId("testes.lion"),
            "Vejiga" to ResourceId("bladder"),
            "Zumo del Amor" to ResourceId("juice.love"),
            "Órgano de Monstruo" to ResourceId("organ.monster"),
            "Scrap" to ResourceId("scrap"),
            // The 1.6 seed names the basic four in English; same resources, same ids.
            "Bone" to ResourceId("bone.monster"),
            "Hide" to ResourceId("hide.monster"),
            "Organ" to ResourceId("organ.monster"),
        )

    /** The id for a resource's pre-id display name, or `null` when no content produces that name. */
    fun resourceIdFor(name: String): ResourceId? = byName[name]

    /**
     * The id for [name], throwing when the name has none.
     *
     * For paths that must not proceed on an unknown resource — a recipe that names one — callers
     * turn the failure into a `StepResult.Blocked` rather than letting the throw escape.
     */
    fun requireResourceId(name: String): ResourceId = byName[name] ?: error("no content id for '$name'")

    private val locationByName: Map<String, LocationId> =
        mapOf(
            // The Spanish names the 1.5 location cards carry.
            "Armería" to LocationId("weapon_crafter"),
            "Artesano de Máscaras" to LocationId("mask_maker"),
            "Cirujano Barbero" to LocationId("barber_surgeon"),
            "Círculo de Piedra" to LocationId("stone_circle"),
            "Curtiduría" to LocationId("leather_worker"),
            "Cúmulo de Linternas" to LocationId("lantern_hoard"),
            "Cúmulo de Linternas Extinto" to LocationId("exhausted_lantern_hoard"),
            "Felinarium" to LocationId("catarium"),
            "Herrería" to LocationId("blacksmith"),
            "Molino de Órganos" to LocationId("organ_grinder"),
            "Peletería" to LocationId("skinnery"),
            "Plumería" to LocationId("plumery"),
            "Taller de Huesos" to LocationId("bone_smith"),
            // The same locations named in English — by the 1.6 seed and by the CSV's englishName
            // column — plus the six sites only the seed declares.
            "Barber Surgeon" to LocationId("barber_surgeon"),
            "Blacksmith" to LocationId("blacksmith"),
            "Bone Smith" to LocationId("bone_smith"),
            "Catarium" to LocationId("catarium"),
            "Dragon Armory" to LocationId("dragon_armory"),
            "Exhausted Lantern Hoard" to LocationId("exhausted_lantern_hoard"),
            "Gormchymist" to LocationId("gormchymist"),
            "Gormery" to LocationId("gormery"),
            "Lantern Hoard" to LocationId("lantern_hoard"),
            "Leather Worker" to LocationId("leather_worker"),
            "Mask Maker" to LocationId("mask_maker"),
            "Organ Grinder" to LocationId("organ_grinder"),
            "Plumery" to LocationId("plumery"),
            "Silk Mill" to LocationId("silk_mill"),
            "Skinnery" to LocationId("skinnery"),
            "Skyreef Sanctuary" to LocationId("skyreef_sanctuary"),
            "Stone Circle" to LocationId("stone_circle"),
            "Wet Resin Crafter" to LocationId("wet_resin_crafter"),
            "Weapon Crafter" to LocationId("weapon_crafter"),
        )

    /** The id for a location's pre-id display name, or `null` when no content produces that name. */
    fun locationIdFor(name: String): LocationId? = locationByName[name]
}

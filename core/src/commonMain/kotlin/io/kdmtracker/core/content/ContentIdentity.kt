package io.kdmtracker.core.content

import io.kdmtracker.core.domain.ResourceId

/**
 * The bridge from a resource's **pre-id display name** to its stable id.
 *
 * Before this change the display name *was* the identity, so every persisted ledger, every gear
 * recipe and every location build cost refers to a resource by name. Those references still
 * resolve here, and — more importantly — a settlement persisted by an older build decodes through
 * this table instead of losing its resource entries.
 *
 * The table is the `spanishName` column of `tools/content-ingest/identity/resources.csv` plus the
 * three English aliases the 1.6 seed uses for the basic four. It is a **transitional** bridge:
 * S2 re-keys recipes and locations to ids, and after that only the legacy decode path reads it.
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
}

package io.kdmtracker.core.content

/**
 * The shipped `core-1.6` content pack — a **seed**, not an exhaustive transcription
 * of the base game.
 *
 * It carries just enough content for a new campaign to compose against: the
 * settlement location tree, the initial innovation, the base resources, and the
 * core quarries and nemeses. Full content (every card from the 1.5/1.6 sets) is
 * ingested later by the content pipeline and split across edition + expansion
 * packs; this object exists so `:core` is playable without it.
 */
object Core16Content {
    val pack: ContentPack =
        ContentPack(
            id = "core-1.6",
            version = "1.6",
            name = "Kingdom Death: Monster 1.6",
            provideInnovations = listOf("Language"),
            provideLocations =
                listOf(
                    "Lantern Hoard",
                    "Bone Smith",
                    "Skinnery",
                    "Organ Grinder",
                    "Weapon Crafter",
                    "Leather Worker",
                    "Stone Circle",
                    "Exhausted Lantern Hoard",
                    "Barber Surgeon",
                    "Blacksmith",
                    "Catarium",
                    "Dragon Armory",
                    "Gormchymist",
                    "Gormery",
                    "Mask Maker",
                    "Plumery",
                    "Silk Mill",
                    "Skyreef Sanctuary",
                    "Wet Resin Crafter",
                ),
            provideQuarries = listOf("White Lion", "Screaming Antelope", "Phoenix"),
            provideNemeses = listOf("The Butcher", "King's Man", "The Hand"),
            provideResources = listOf("Bone", "Hide", "Organ", "Scrap"),
        )
}

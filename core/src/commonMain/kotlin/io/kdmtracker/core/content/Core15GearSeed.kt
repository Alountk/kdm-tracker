package io.kdmtracker.core.content

import io.kdmtracker.core.domain.Gear
import io.kdmtracker.core.domain.GearRecipe
import io.kdmtracker.core.domain.SettlementLocation

/**
 * A **curated** seed of 1.5 gear + crafting recipes, transcribed by hand from the
 * location cards (see `tools/content-ingest/README.md`).
 *
 * This is deliberately small: the automatic extractor could not read this content at
 * acceptable fidelity (`pdftotext` strips spaces in `-raw` and splits columns in
 * `-layout`, cards are duplicated, and the stats are icon/positional), so bulk gear
 * extraction is deferred to a dedicated content slice. Only the recipes whose costs
 * resolve against the ingested 1.5 resources are modeled here; gear `keywords`/`rules`
 * and `modifiers` are `[gap]` for now.
 */
object Core15GearSeed {
    val bundle: ContentBundle =
        ContentBundle(
            gear =
                listOf(
                    Gear("Hacha de Contrapeso"),
                    Gear("Maza Silbadora"),
                    Gear("Zanbato"),
                    Gear("Vaina de Sangre"),
                    Gear("Dedo de Dios"),
                    Gear("Katana del Arcoíris"),
                ),
            locations =
                listOf(
                    SettlementLocation(
                        name = "Armería",
                        recipes =
                            listOf(
                                GearRecipe(
                                    "Hacha de Contrapeso",
                                    resourceCost =
                                        mapOf(
                                            "Hueso de Monstruo" to 2,
                                            "Piel de Monstruo" to 1,
                                            "Órgano de Monstruo" to 1,
                                        ),
                                ),
                                GearRecipe(
                                    "Maza Silbadora",
                                    resourceCost = mapOf("Hueso de Monstruo" to 2, "Órgano de Monstruo" to 1),
                                ),
                                GearRecipe(
                                    "Zanbato",
                                    resourceCost =
                                        mapOf("Grandes Huesos de Felino" to 1, "Piel de Monstruo" to 2),
                                ),
                                GearRecipe(
                                    "Vaina de Sangre",
                                    resourceCost =
                                        mapOf(
                                            "Órgano de Monstruo" to 5,
                                            "Huesos Huecos de Ala" to 1,
                                            "Excrementos Mucosos" to 1,
                                        ),
                                ),
                                GearRecipe(
                                    "Dedo de Dios",
                                    resourceCost = mapOf("Dedo de Fénix" to 1, "Hueso de Monstruo" to 4),
                                ),
                                GearRecipe(
                                    "Katana del Arcoíris",
                                    resourceCost =
                                        mapOf(
                                            "Pico de Ave" to 1,
                                            "Excremento Arcoiris" to 1,
                                            "Hierro" to 1,
                                            "Hueso de Monstruo" to 6,
                                        ),
                                    requires = listOf("Calor"),
                                ),
                            ),
                    ),
                ),
        )
}

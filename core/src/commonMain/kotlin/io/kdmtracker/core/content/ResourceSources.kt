package io.kdmtracker.core.content

/**
 * Maps a resource name to its **source** (the monster/deck it comes from), used only to group
 * resources in the UI.
 *
 * Derived from the 1.5 resource names by keyword — the basic four, the León/Fénix/Antílope
 * monsters, and the insect cards — and anything unrecognised falls back to `"Otros"`. This is
 * presentation-only: the ledger keys resources by name, never by source, so a wrong group never
 * changes a rule.
 */
object ResourceSources {
    private val basics = setOf("Hueso de Monstruo", "Piel de Monstruo", "Órgano de Monstruo", "Scrap")

    private val rules: List<Pair<List<String>, String>> =
        listOf(
            listOf("león", "felino") to "León Blanco",
            listOf("fénix", "ave", "pluma", "plumas", "pico", "cresta", "bigote", "bigotes") to "Fénix",
            listOf("cuerno", "cuernos", "antílope", "tendón", "pellejo", "filete", "vejiga") to "Antílope Bramador",
            listOf("pústulas", "segundo", "corazón") to "Carnicero",
            listOf("araña", "cucaracha", "escarabajo", "hormiga", "mosca", "garrapata", "parásitos") to "Insectos",
        )

    /**
     * The source group for [name], or `"Otros"` when no token matches a whole word of the name.
     *
     * Matching is **word-boundary**, not substring — `"Ave"` matches `"Pico de Ave"` but not
     * `"Calavera"`.
     */
    fun sourceOf(name: String): String {
        if (name in basics) return "Básicos"
        val words = name.lowercase().split(" ")
        val rule = rules.firstOrNull { (tokens, _) -> tokens.any { token -> token in words } }
        return rule?.second ?: "Otros"
    }
}

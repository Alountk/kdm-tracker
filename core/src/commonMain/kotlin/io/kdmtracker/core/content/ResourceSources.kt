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
            listOf("León", "Felino") to "León Blanco",
            listOf("Fénix", "Ave", "Plumas", "Cresta") to "Fénix",
            listOf("Cuerno", "Antílope", "Tendón", "Pellejo") to "Antílope Bramador",
            listOf("Araña", "Cucaracha", "Escarabajo", "Hormiga", "Mosca", "Garrapata", "Parásitos") to "Insectos",
        )

    /** The source group for [name], or `"Otros"` when it matches nothing. */
    fun sourceOf(name: String): String {
        if (name in basics) return "Básicos"
        val rule = rules.firstOrNull { (tokens, _) -> tokens.any { name.contains(it, ignoreCase = true) } }
        return rule?.second ?: "Otros"
    }
}

package io.kdmtracker.core.content

/**
 * The combinations the third-party "Expansion synergy and compatibility chart" (v0) flags as a problem,
 * keyed by the pack ids the expansions will ship under (see `docs/content-packs.md`).
 *
 * Nobody has confirmed the chart against the official rules, so every relation carries
 * [RelationSource.CHART]: it warns and never refuses. A relation whose packs a build does not ship is
 * ignored by [PackRegistry], so these take effect pack by pack as expansions land.
 *
 * Only the chart's red ("incompatible") and orange ("compatibility issues") marks are relations. Node
 * overlap is modelled by [MonsterNode] and is not a conflict; a requirement is a pack's `requires`.
 */
object ExpansionRelations {
    val chart: List<PackRelation> =
        relations(
            RelationKind.INCOMPATIBLE,
            "green-knight-armour" to "inverted-mountain",
            "ivory-dragon" to "lion-god",
            "ivory-dragon" to "screaming-god",
            "ivory-dragon" to "silver-city",
            "lion-god" to "screaming-god",
            "screaming-god" to "silver-city",
        ) +
            relations(
                RelationKind.COMPATIBILITY_ISSUES,
                "death-armour" to "green-knight-armour",
                "dragon-king" to "inverted-mountain",
                "dung-beetle-knight" to "inverted-mountain",
                "first-hero" to "green-knight-armour",
                "flower-knight" to "inverted-mountain",
                "flower-knight" to "lonely-tree",
                "gorm" to "inverted-mountain",
                "honeycomb-weaver" to "inverted-mountain",
                "inverted-mountain" to "ivory-dragon",
                "inverted-mountain" to "lion-god",
                "inverted-mountain" to "lion-knight",
                "inverted-mountain" to "manhunter",
                "inverted-mountain" to "screaming-god",
                "inverted-mountain" to "slenderman",
                "inverted-mountain" to "spidicules",
                "inverted-mountain" to "sunstalker",
                "inverted-mountain" to "silver-city",
            )

    private fun relations(
        kind: RelationKind,
        vararg pairs: Pair<String, String>,
    ) = pairs.map { (a, b) -> PackRelation(a, b, kind, RelationSource.CHART) }
}

package io.kdmtracker.app.ui

import io.kdmtracker.core.content.ContentPack

/** An expansion the player may add to a new campaign. */
data class ExpansionOption(
    val id: String,
    val name: String,
)

/** One choice per optional pack, named after the pack. */
fun expansionOptions(optional: List<ContentPack>): List<ExpansionOption> =
    optional.map { ExpansionOption(it.id, it.name) }

/** [selected] with [id] added, or removed when it was already there. */
fun toggledExpansion(
    selected: Set<String>,
    id: String,
): Set<String> = if (id in selected) selected - id else selected + id

package io.kdmtracker.core.content

/** Reads node choices stored by node name; a node this build does not know is dropped rather than failing. */
object NodeChoices {
    fun decode(stored: Map<String, String>): Map<MonsterNode, String> =
        stored.entries
            .mapNotNull { (name, monster) ->
                MonsterNode.entries.firstOrNull { it.name == name }?.to(monster)
            }.toMap()
}

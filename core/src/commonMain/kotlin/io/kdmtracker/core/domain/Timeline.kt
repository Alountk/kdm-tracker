package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

@Serializable
data class TimelineEntry(
    val lanternYear: Int,
    val label: String,
)

data class Timeline(
    val entries: List<TimelineEntry> = emptyList(),
    val length: Int = 40,
) {
    fun withEvent(
        year: Int,
        label: String,
    ): Timeline {
        if (year !in 1..length) {
            return this
        }
        val entry = TimelineEntry(year, label)
        if (entry in entries) {
            return this
        }
        return copy(entries = entries + entry)
    }

    fun entriesFor(year: Int): List<TimelineEntry> = entries.filter { it.lanternYear == year }

    companion object {
        fun core16(length: Int = 40): Timeline {
            val seed =
                listOf(
                    1 to "Returning Survivors",
                    4 to "Story Event",
                    9 to "Story Event",
                    13 to "Story Event",
                    16 to "Nemesis: The Butcher Lvl 2",
                    19 to "Nemesis: King's Man Lvl 2",
                    23 to "Nemesis: The Butcher Lvl 3",
                    25 to "Story Event",
                    28 to "Nemesis: King's Man Lvl 3",
                    30 to "Story Event",
                )
            return seed.fold(Timeline(length = length)) { timeline, (year, label) ->
                timeline.withEvent(year, label)
            }
        }
    }
}

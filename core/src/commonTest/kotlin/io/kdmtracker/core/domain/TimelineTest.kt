package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TimelineTest {
    @Test
    fun defaultsToFortyYearsAndNoEntries() {
        val timeline = Timeline()

        assertEquals(40, timeline.length)
        assertTrue(timeline.entries.isEmpty())
    }

    @Test
    fun withEventAddsAnEntry() {
        val timeline = Timeline().withEvent(3, "Settlement Event")

        assertEquals(listOf(TimelineEntry(3, "Settlement Event")), timeline.entries)
    }

    @Test
    fun withEventIsIdempotentForTheSameYearAndLabel() {
        val once = Timeline().withEvent(4, "Story Event")
        val twice = once.withEvent(4, "Story Event")

        assertEquals(once, twice)
        assertEquals(listOf(TimelineEntry(4, "Story Event")), twice.entries)
    }

    @Test
    fun withEventKeepsDifferentLabelsInTheSameYear() {
        val timeline =
            Timeline()
                .withEvent(4, "Story Event")
                .withEvent(4, "Nemesis: The Butcher Lvl 2")

        assertEquals(2, timeline.entries.size)
        assertEquals(2, timeline.entriesFor(4).size)
    }

    @Test
    fun withEventIgnoresYearsOutsideOneToLength() {
        val base = Timeline()

        assertSame(base, base.withEvent(0, "before"))
        assertSame(base, base.withEvent(-1, "before"))
        assertSame(base, base.withEvent(41, "after"))
        assertTrue(base.withEvent(0, "before").entries.isEmpty())
        assertTrue(base.withEvent(41, "after").entries.isEmpty())
    }

    @Test
    fun withEventAcceptsTheBoundaryYears() {
        val timeline = Timeline().withEvent(1, "first").withEvent(40, "last")

        assertEquals(listOf(TimelineEntry(1, "first"), TimelineEntry(40, "last")), timeline.entries)
    }

    @Test
    fun entriesForFiltersByYear() {
        val timeline =
            Timeline()
                .withEvent(4, "Story Event")
                .withEvent(4, "Nemesis: The Butcher Lvl 2")
                .withEvent(16, "Nemesis: The Butcher Lvl 2")

        assertEquals(
            listOf(TimelineEntry(4, "Story Event"), TimelineEntry(4, "Nemesis: The Butcher Lvl 2")),
            timeline.entriesFor(4),
        )
        assertEquals(listOf(TimelineEntry(16, "Nemesis: The Butcher Lvl 2")), timeline.entriesFor(16))
        assertTrue(timeline.entriesFor(5).isEmpty())
    }

    @Test
    fun core16SeedsTheFixedSheetEntries() {
        val timeline = Timeline.core16()

        assertEquals(40, timeline.length)
        assertEquals(
            setOf(
                TimelineEntry(1, "Returning Survivors"),
                TimelineEntry(4, "Story Event"),
                TimelineEntry(9, "Story Event"),
                TimelineEntry(13, "Story Event"),
                TimelineEntry(16, "Nemesis: The Butcher Lvl 2"),
                TimelineEntry(19, "Nemesis: King's Man Lvl 2"),
                TimelineEntry(23, "Nemesis: The Butcher Lvl 3"),
                TimelineEntry(25, "Story Event"),
                TimelineEntry(28, "Nemesis: King's Man Lvl 3"),
                TimelineEntry(30, "Story Event"),
            ),
            timeline.entries.toSet(),
        )
        assertEquals(10, timeline.entries.size)
    }

    @Test
    fun core16HonoursAShorterLength() {
        val timeline = Timeline.core16(length = 10)

        assertEquals(10, timeline.length)
        assertEquals(
            setOf(
                TimelineEntry(1, "Returning Survivors"),
                TimelineEntry(4, "Story Event"),
                TimelineEntry(9, "Story Event"),
            ),
            timeline.entries.toSet(),
        )
    }

    @Test
    fun core16IsDeterministic() {
        assertEquals(Timeline.core16(), Timeline.core16())
    }
}

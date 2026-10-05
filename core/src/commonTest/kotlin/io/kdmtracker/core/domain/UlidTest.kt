package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UlidTest {
    private val crockford = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    @Test
    fun generateProduces26CharCrockfordString() {
        val ulid = Ulid.generate(clockMillis = 1L, random = { 0 })

        assertEquals(26, ulid.value.length)
        assertTrue(ulid.value.all { it in crockford })
    }

    @Test
    fun generateEncodesClockMillisInFirstTenChars() {
        assertEquals("0000000001", Ulid.generate(1L) { 0 }.value.take(10))
        assertEquals("0000000010", Ulid.generate(32L) { 0 }.value.take(10))
        assertEquals("000000000Z", Ulid.generate(31L) { 0 }.value.take(10))
        assertEquals("01HF7YAT00", Ulid.generate(1_700_000_000_000L) { 0 }.value.take(10))
        assertEquals("7ZZZZZZZZZ", Ulid.generate(281_474_976_710_655L) { 0 }.value.take(10))
    }

    @Test
    fun generateTakesFiveBitsFromEachRandomValue() {
        assertEquals("Z".repeat(16), Ulid.generate(0L) { 0x1F }.value.takeLast(16))
        assertEquals("1".repeat(16), Ulid.generate(0L) { 33 }.value.takeLast(16))
    }

    @Test
    fun generateIsDeterministicForFixedInputs() {
        assertEquals(
            Ulid.generate(12345L) { 7 }.value,
            Ulid.generate(12345L) { 7 }.value,
        )
    }

    @Test
    fun orderingFollowsClockMillis() {
        val earlier = Ulid.generate(1_000L) { 0 }
        val later = Ulid.generate(2_000L) { 0 }

        assertTrue(earlier < later)
        assertTrue(later > earlier)
    }

    @Test
    fun sortsAscendingByClockMillis() {
        val earliest = Ulid.generate(1_000L) { 0 }
        val middle = Ulid.generate(2_000L) { 0 }
        val latest = Ulid.generate(3_000L) { 0 }

        assertEquals(listOf(earliest, middle, latest), listOf(latest, earliest, middle).sorted())
    }

    @Test
    fun parseAcceptsAValidUlid() {
        val value = Ulid.generate(1_700_000_000_000L) { 7 }.value

        assertEquals(value, Ulid.parse(value).value)
    }

    @Test
    fun parseRejectsWrongLength() {
        assertFailsWith<IllegalArgumentException> { Ulid.parse("0123456789") }
        assertFailsWith<IllegalArgumentException> { Ulid.parse("0".repeat(27)) }
        assertFailsWith<IllegalArgumentException> { Ulid.parse("") }
    }

    @Test
    fun parseRejectsCharactersOutsideCrockfordAlphabet() {
        assertFailsWith<IllegalArgumentException> { Ulid.parse("0".repeat(25) + "I") }
        assertFailsWith<IllegalArgumentException> { Ulid.parse("0".repeat(25) + "L") }
        assertFailsWith<IllegalArgumentException> { Ulid.parse("0".repeat(25) + "O") }
        assertFailsWith<IllegalArgumentException> { Ulid.parse("0".repeat(25) + "U") }
        assertFailsWith<IllegalArgumentException> { Ulid.parse("0".repeat(25) + "a") }
    }
}

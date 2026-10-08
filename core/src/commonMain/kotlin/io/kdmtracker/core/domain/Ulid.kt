package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A 26-character, lexicographically sortable identifier generated on the client.
 *
 * The first 10 characters encode a 48-bit millisecond timestamp (big-endian) and the
 * remaining 16 characters encode 80 bits of randomness, using the Crockford base32 alphabet.
 * Because every value has the same length, [compareTo] over the raw [value] yields time order.
 */
@Serializable
@JvmInline
value class Ulid(
    val value: String,
) : Comparable<Ulid> {
    override fun compareTo(other: Ulid): Int = value.compareTo(other.value)

    companion object {
        private const val LENGTH = 26
        private const val TIME_CHARS = 10
        private const val TIMESTAMP_MAX = 1L shl 48
        private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

        /**
         * Generates a [Ulid] for [clockMillis], drawing the 80 random bits from [random].
         *
         * For each of the 16 random characters the lower 5 bits of the [Int] returned by
         * [random] are used. The result is deterministic for a fixed [clockMillis] and [random].
         */
        fun generate(
            clockMillis: Long,
            random: () -> Int,
        ): Ulid {
            require(clockMillis in 0 until TIMESTAMP_MAX) {
                "clockMillis must be a 48-bit unsigned value, was $clockMillis"
            }

            val chars = CharArray(LENGTH)
            var remaining = clockMillis
            for (index in TIME_CHARS - 1 downTo 0) {
                chars[index] = ALPHABET[(remaining and 0x1F).toInt()]
                remaining = remaining ushr 5
            }
            for (index in TIME_CHARS until LENGTH) {
                chars[index] = ALPHABET[random() and 0x1F]
            }

            return Ulid(chars.concatToString())
        }

        /**
         * Parses [value], requiring exactly [LENGTH] Crockford base32 characters.
         *
         * @throws IllegalArgumentException when the length or any character is invalid.
         */
        fun parse(value: String): Ulid {
            require(value.length == LENGTH) {
                "Ulid must be $LENGTH characters long, was ${value.length}"
            }
            require(value.all { it in ALPHABET }) {
                "Ulid contains characters outside the Crockford base32 alphabet: $value"
            }
            return Ulid(value)
        }
    }
}

package io.kdmtracker.core.domain

/**
 * The hunt-event roll: two d10 read as a d100, one die for the tens and one for the units.
 * A roll is stored as `1..100`; both dice showing 0 (`00`) is 100.
 */
object HuntRoll {
    val VALID: IntRange = 1..100

    /** Combines the two dice (each `0..9`) into the stored `1..100` value. */
    fun of(
        tens: Int,
        units: Int,
    ): Int {
        require(tens in 0..9 && units in 0..9) { "each die is 0..9" }
        return (tens * 10 + units).takeIf { it != 0 } ?: 100
    }

    /** The tens die (`0..9`) of a stored [roll]. */
    fun tens(roll: Int): Int = roll % 100 / 10

    /** The units die (`0..9`) of a stored [roll]. */
    fun units(roll: Int): Int = roll % 10

    /** The two-digit face value as read on the dice: `07`, `42`, `00` for 100. */
    fun label(roll: Int): String = (roll % 100).toString().padStart(2, '0')
}

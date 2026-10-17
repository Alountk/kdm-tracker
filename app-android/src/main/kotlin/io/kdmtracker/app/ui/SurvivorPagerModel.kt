package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.Sex
import io.kdmtracker.core.domain.Survivor

/**
 * The index the single prev button lands on: one step forward, wrapping to the first.
 *
 * The pager's rules are kept pure so the UI never holds the arithmetic. The design ships **one**
 * prev button whose handler cycles with modulo arithmetic (`(idx + 1) % len`), not a prev/next
 * pair — see `.superpowers/design/ficha/ficha.html`. These functions are that behaviour, plus the
 * gender chip's label and the empty/overflow guards the design's mock does not have to consider.
 */
fun advance(
    index: Int,
    size: Int,
): Int =
    if (size <= 0) {
        0
    } else {
        (index + 1) % size
    }

/** The design's gender chip beside the name: `H` / `M`, with `OTHER` as `O`. */
fun genderLabel(sex: Sex): String =
    when (sex) {
        Sex.MALE -> "H"
        Sex.FEMALE -> "M"
        Sex.OTHER -> "O"
    }

/** Whether the roster is empty, which selects the empty state over the pager. */
fun isEmptyRoster(survivors: List<Survivor>): Boolean = survivors.isEmpty()

/** Keeps [index] inside a roster of [size]: a shrinking roster resets an out-of-range slot. */
fun clampIndex(
    index: Int,
    size: Int,
): Int =
    if (size <= 0) {
        0
    } else {
        index.coerceIn(0, size - 1)
    }

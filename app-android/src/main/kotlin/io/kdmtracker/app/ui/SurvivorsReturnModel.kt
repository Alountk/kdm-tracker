package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.WoundState

/**
 * The step-2 survivor card's status line, read off the wound boxes.
 *
 * A hit location's `light` box is the sheet's *herida firme* and `heavy` its *herida grave*; the
 * design's `Sin lesiones permanentes` line maps onto exactly these two boxes (the model has no
 * separate permanent-injury field — recorded derivation), while `armor` is a hit-location stat and
 * never moves the line. The counts are per marked box, so one location can contribute to both.
 */
fun survivorStatusLine(wounds: Map<HitLocation, WoundState>): String {
    val light = wounds.values.count { it.light }
    val heavy = wounds.values.count { it.heavy }
    return if (light == 0 && heavy == 0) {
        "Sin lesiones permanentes"
    } else {
        "Heridas leves $light · graves $heavy"
    }
}

package io.kdmtracker.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner shapes from the rendered design: the default is a 4dp radius (Tailwind's `rounded`) and
 * the large step is 8dp (`rounded-lg`). The soft 0dp radius the `designMd` prose claims is not what
 * the screen renders.
 */
val KdmShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(8.dp),
        extraLarge = RoundedCornerShape(8.dp),
    )

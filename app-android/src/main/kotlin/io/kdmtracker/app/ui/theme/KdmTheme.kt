package io.kdmtracker.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The Atramentum Lantern theme. Every screen reads its colours, type and shapes from Material 3
 * roles, so mounting this at the root restyles the whole app without editing any screen.
 */
@Composable
fun KdmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KdmScheme,
        typography = KdmTypography,
        shapes = KdmShapes,
        content = content,
    )
}

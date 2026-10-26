package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.ui.theme.KdmType

/** A small status pill: label-caps on a tinted container with an optional hairline. */
@Composable
internal fun Pill(
    text: String,
    container: Color,
    content: Color,
    border: Color? = null,
) {
    val shape = MaterialTheme.shapes.small
    val modifier =
        if (border != null) {
            Modifier.background(container, shape).border(1.dp, border, shape)
        } else {
            Modifier.background(container, shape)
        }
    Text(
        text = text,
        style = KdmType.labelCaps,
        color = content,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** A small cost chip: "N Nombre". */
@Composable
internal fun Chip(text: String) {
    Text(
        text = text,
        style = microBold,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

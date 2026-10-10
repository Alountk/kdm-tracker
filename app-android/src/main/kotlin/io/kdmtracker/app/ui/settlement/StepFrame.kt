package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The design's per-step frame: the step's icon, number and name, its text, and the footer's advance
 * and restore actions. `Restaurar Esfuerzos` is drawn but disabled — the action is not modelled.
 */
@Composable
fun StepFrame(
    model: StepFrameModel,
    onAdvance: () -> Unit,
    onRestore: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Symbol(model.icon, MaterialTheme.colorScheme.secondary, 18.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = model.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Text(
        text = model.summary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onAdvance,
            modifier = Modifier.weight(1f),
        ) {
            Text(model.advanceLabel)
            Spacer(Modifier.width(4.dp))
            Symbol("arrow_forward", MaterialTheme.colorScheme.onPrimary, 16.sp)
        }
        OutlinedButton(onClick = onRestore, enabled = false) {
            Symbol("restart_alt", MaterialTheme.colorScheme.onSurfaceVariant, 16.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Restaurar Esfuerzos",
                style = KdmType.labelCaps,
            )
        }
    }
}

@Composable
private fun Symbol(
    name: String,
    color: Color,
    size: androidx.compose.ui.unit.TextUnit,
) {
    Text(
        text = KdmSymbols.glyph(name),
        fontFamily = KdmSymbols.family,
        fontSize = size,
        color = color,
    )
}

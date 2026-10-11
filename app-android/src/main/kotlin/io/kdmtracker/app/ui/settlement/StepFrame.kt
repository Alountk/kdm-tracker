package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The design's per-step frame: the step's icon, number and name, its text, and the footer's advance
 * and restore actions. `Restaurar Esfuerzos` is drawn but disabled — the action is not modelled.
 */
@Composable
fun StepFrameHeader(model: StepFrameModel) {
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
}

/**
 * The step's flow actions, at the end of its content: the advance in the design's `secondary`, and
 * the (inert) restore below it. Both are full width and stacked, and only the labels change from
 * step to step — this is the component every settlement step reuses.
 */
@Composable
fun StepFrameFooter(
    model: StepFrameModel,
    endeavors: Int,
    onAdvance: () -> Unit,
    onRestore: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onAdvance,
            modifier = Modifier.fillMaxWidth().shadow(elevation = 4.dp, shape = MaterialTheme.shapes.small),
            shape = MaterialTheme.shapes.small,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
        ) {
            Text(
                text = model.advanceLabel.uppercase(),
                style = KdmType.labelCaps,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            Symbol("arrow_forward", MaterialTheme.colorScheme.onSecondary, 18.sp)
        }
        Button(
            onClick = onRestore,
            // Not modelled: the design draws it, the action does not exist yet.
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) {
            Symbol("restart_alt", MaterialTheme.colorScheme.onSurfaceVariant, 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Restaurar Esfuerzos ($endeavors)".uppercase(),
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

package io.kdmtracker.app.ui.hunt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmPalette

/** The per-step 2d10 roll, shown for the selected step and editable by hand (`roll_edit`). */
@Composable
internal fun StepRoll(
    stepRoll: Int?,
    onEditRoll: (Int) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(stepRoll) { mutableStateOf(stepRoll?.toString() ?: "") }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(KdmPalette.surfaceLow)
                .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Tirada de la casilla".uppercase(),
                color = KdmPalette.ash,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = stepRoll?.let { "$it" } ?: "—",
                color = if (stepRoll != null) KdmPalette.gold else KdmPalette.outline,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        OutlinedButton(onClick = { editing = true }) { Text("Editar") }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Tirada 2d10") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.filter(Char::isDigit).take(2) },
                    label = { Text("Valor (2–20)") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        draft.toIntOrNull()?.let { if (it in 2..20) onEditRoll(it) }
                        editing = false
                    },
                ) { Text("Guardar") }
            },
            dismissButton = { OutlinedButton(onClick = { editing = false }) { Text("Cancelar") } },
        )
    }
}

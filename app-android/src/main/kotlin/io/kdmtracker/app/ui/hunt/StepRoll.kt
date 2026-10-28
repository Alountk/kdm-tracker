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
import io.kdmtracker.core.domain.HuntRoll

/**
 * The per-step hunt roll — two d10 read as a d100 (one die for the tens, one for the units) —
 * shown for the selected step and editable by hand (`roll_edit`).
 */
@Composable
internal fun StepRoll(
    stepRoll: Int?,
    onEditRoll: (Int) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var tens by remember(stepRoll) { mutableStateOf(stepRoll?.let { HuntRoll.tens(it).toString() } ?: "") }
    var units by remember(stepRoll) { mutableStateOf(stepRoll?.let { HuntRoll.units(it).toString() } ?: "") }

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
                text = stepRoll?.let { HuntRoll.label(it) } ?: "—",
                color = if (stepRoll != null) KdmPalette.gold else KdmPalette.outline,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            stepRoll?.let {
                Text(
                    text = "Decenas ${HuntRoll.tens(it)} · Unidades ${HuntRoll.units(it)}",
                    color = KdmPalette.ash,
                    fontSize = 11.sp,
                )
            }
        }
        OutlinedButton(onClick = { editing = true }) { Text("Editar") }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Tirada 2d10 (d100)") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tens,
                        onValueChange = { tens = it.filter(Char::isDigit).take(1) },
                        label = { Text("Decenas (0–9)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = units,
                        onValueChange = { units = it.filter(Char::isDigit).take(1) },
                        label = { Text("Unidades (0–9)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = tens.isNotEmpty() && units.isNotEmpty(),
                    onClick = {
                        onEditRoll(HuntRoll.of(tens.toInt(), units.toInt()))
                        editing = false
                    },
                ) { Text("Guardar") }
            },
            dismissButton = { OutlinedButton(onClick = { editing = false }) { Text("Cancelar") } },
        )
    }
}

package io.kdmtracker.app.ui.hunt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.Symbol
import io.kdmtracker.app.ui.theme.KdmPalette

/** Region 7 — the hunt action buttons: a full-width advance, a 2-up pair, then beast controls. */
@Composable
internal fun ActionButtons(
    onAdvance: () -> Unit,
    onMoveMonster: () -> Unit,
    onRetreatMonster: () -> Unit,
    onShowdown: () -> Unit,
    advanceBlocked: Boolean = false,
) {
    Button(
        onClick = onAdvance,
        enabled = !advanceBlocked,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = KdmPalette.surfaceHigh,
                contentColor = KdmPalette.onCanvas,
                disabledContainerColor = KdmPalette.surfaceHighest,
                disabledContentColor = KdmPalette.ash,
            ),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol("arrow_forward", KdmPalette.gold, 20)
            Text(
                text =
                    if (advanceBlocked) {
                        "Resuelve el evento para avanzar".uppercase()
                    } else {
                        "Avanzar Supervivientes (+1 Casilla)".uppercase()
                    },
                fontSize = 13.sp,
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Mover Bestia: filled `surface-container-low`, ink on the right.
        Button(
            onClick = onMoveMonster,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = KdmPalette.surfaceLow,
                    contentColor = KdmPalette.ash,
                ),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Symbol("directions_walk", KdmPalette.blood, 18)
                Text(text = "Mover Bestia".uppercase(), fontSize = 12.sp)
            }
        }
        // Enfrentamiento: the design's red filled action.
        Button(
            onClick = onShowdown,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = HuntDeepRed, // design's on-tertiary-fixed-variant #8E1214
                    contentColor = KdmPalette.crimsonContainer, // #FFDAD6
                ),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Symbol("swords", KdmPalette.crimsonContainer, 18)
                Text(text = "Enfrentamiento".uppercase(), fontSize = 12.sp)
            }
        }
    }
    // S8: a hunt event can make the beast step one space back — a manual counterpart of Mover Bestia.
    OutlinedButton(
        onClick = onRetreatMonster,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol("arrow_back", KdmPalette.ash, 16)
            Text(text = "Mover Bestia Atrás".uppercase(), fontSize = 12.sp, color = KdmPalette.ash)
        }
    }
}

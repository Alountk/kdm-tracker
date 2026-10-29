package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmType

/** The step-5 body's static design copy, kept where `SettlementStepBodiesTest` can pin it. */
internal object DeathCountStepCopy {
    const val TITLE = "Bajas en la Primera Historia"

    const val NOTE =
        "Marca una casilla vacía por cada caído en combate o por eventos de historia " +
            "durante la partida."
}

/**
 * Step 5 "Contador de Muertes" body: the design's deaths row card.
 *
 * `add` is the modeled death record ([io.kdmtracker.core.usecase.RecordDeaths] via the frame's
 * controller, +1 per tap — the usecase refuses `count < 1`), so `remove` is drawn disabled (backlog:
 * undo a recorded death). The subtitle is the design's zero-deaths copy and renders only while
 * [io.kdmtracker.app.UiState.deaths] is 0 ([deathsRowSubtitle]); no designed copy exists for a
 * nonzero count. The births counter and the step-5 principle card are gone — the same
 * principle choice lives in step 6's milestone cards — so neither `RecordBirths` nor
 * `onChoosePrinciple` appears here. The frame owns the step title and the opening paragraph.
 */
@Composable
fun DeathCountStepScreen(
    state: UiState,
    onRecordDeaths: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Meta row: the design's header-row `Total` pill.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total:".uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.deaths.toString(),
                    style = KdmType.labelCaps.copy(fontSize = 14.sp, lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = DeathCountStepCopy.TITLE,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                deathsRowSubtitle(state.deaths)?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Disabled: RecordDeaths refuses count < 1, so an undo needs a new usecase (backlog).
                IconButton(
                    onClick = {},
                    enabled = false,
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small),
                ) {
                    Symbol("remove", MaterialTheme.colorScheme.onSurfaceVariant, 18)
                }
                IconButton(
                    onClick = { onRecordDeaths(1) },
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small),
                ) {
                    Symbol("add", MaterialTheme.colorScheme.error, 18)
                }
            }
        }

        Text(
            text = DeathCountStepCopy.NOTE,
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

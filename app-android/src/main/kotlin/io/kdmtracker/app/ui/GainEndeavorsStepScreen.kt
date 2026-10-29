package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

/** The step-3 body's static design copy, kept where `SettlementStepBodiesTest` can pin it. */
internal object GainEndeavorsStepCopy {
    const val HINT = "Toca una ficha para alternar su consumo"
}

/**
 * Step 3 "Ganar Esfuerzos" body: the design's meta row and endeavor token tray.
 *
 * The tray is drawn from state — one disabled token per living survivor (the same basis
 * [io.kdmtracker.core.usecase.GainEndeavors] funds), labeled with their first name. Consuming a
 * token and the manual `remove`/`add` adjustment have no model behind them, so the tokens and both
 * buttons are drawn disabled (backlog). The frame owns `Paso 3. Ganar Esfuerzos` and the opening
 * paragraph, so neither is rendered here.
 */
@Composable
fun GainEndeavorsStepScreen(state: UiState) {
    val tokens = endeavorTokenNames(state.survivors)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Meta row: the design's header-row chip and right stat.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            MaterialTheme.shapes.small,
                        ).padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Auto-calculado".uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Reserva Activa".uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.endeavors.toString(),
                        style = KdmType.statDisplay,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = " / ${state.endeavorBreakdown.total}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Token tray: one inert token per living survivor plus the disabled manual adjusters.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Supervivientes que regresan (${tokens.size})".uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "+${tokens.size} ESFUERZOS",
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            tokens.chunked(4).forEach { rowTokens ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowTokens.forEach { name ->
                        EndeavorToken(name = name, modifier = Modifier.weight(1f))
                    }
                    repeat(4 - rowTokens.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = GainEndeavorsStepCopy.HINT,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    InertGlyphButton(name = "remove")
                    InertGlyphButton(name = "add")
                }
            }
        }
    }
}

/** One token tile: the design's filled `flare` plus the survivor's first name, drawn disabled. */
@Composable
private fun EndeavorToken(
    name: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Symbol("flare", MaterialTheme.colorScheme.secondary, 22, fill = true)
        Text(
            text = name,
            style = KdmType.labelCaps.copy(fontSize = 9.sp, lineHeight = 12.sp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** The design's tiny `remove`/`add` endeavor buttons; the manual adjustment is not modeled. */
@Composable
private fun InertGlyphButton(name: String) {
    IconButton(
        onClick = {},
        enabled = false,
        modifier =
            Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small),
    ) {
        Symbol(name, MaterialTheme.colorScheme.onSurfaceVariant, 14)
    }
}

package io.kdmtracker.app.ui.hunt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.Symbol
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.core.domain.HuntState

/** Region 2 — the quarry plinth: `Presa Activa`, level, quarry, variant, distance and stat matrix. */
@Composable
internal fun QuarryPlinth(
    state: UiState,
    level: Int,
) {
    // The design renders this whole block in one card (`surface-container-low`), with the
    // `Distancia` badge pinned to the top-right of the header row.
    HuntPanel {
        val hunt = state.hunt ?: HuntState()
        val frame = HuntPlinthFrame.from(hunt.copy(level = level))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KdmPalette.surfaceHighest),
                contentAlignment = Alignment.Center,
            ) {
                Symbol("pets", KdmPalette.gold, 26)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = frame.eyebrow.uppercase(),
                        color = KdmPalette.gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(KdmPalette.gold),
                    )
                    Text(text = frame.levelLabel.uppercase(), color = KdmPalette.ash, fontSize = 14.sp)
                }
                Text(
                    text = frame.quarry,
                    color = KdmPalette.onCanvas,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = KdmFontFamilies.playfair,
                )
                Text(text = frame.variant, color = KdmPalette.ash, fontSize = 11.sp)
            }
            DistanceBadge(distance = frame.distance)
        }
        StatMatrix()
        InstinctRule()
    }
}

/** The plinth's top-right `Distancia` badge: label, stat display, `casillas` unit, stacked. */
@Composable
private fun DistanceBadge(distance: String) {
    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(KdmPalette.surfaceHighest)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Distancia".uppercase(), color = KdmPalette.gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(text = distance, color = KdmPalette.onCanvas, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(text = "casillas", color = KdmPalette.ash, fontSize = 11.sp)
    }
}

/** Region 2b — the monster stat matrix `Mov 6 / Res 8 / Vel - / Daño -`, a 4-cell grid on its own
 *  darker track, exactly as the design's `surface-container-lowest` wrapper renders it. */
@Composable
private fun StatMatrix() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(KdmPalette.surfaceLowest)
                .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatCell("Mov", "6", Modifier.weight(1f))
        StatCell("Res", "8", Modifier.weight(1f))
        StatCell("Vel", "-", Modifier.weight(1f), muted = true)
        StatCell("Daño", "-", Modifier.weight(1f), muted = true)
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .background(KdmPalette.surfaceLow)
                .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = if (muted) KdmPalette.ash else KdmPalette.onCanvas,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            color = if (muted) KdmPalette.ash.copy(alpha = 0.7f) else KdmPalette.onCanvas,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Region 3 — the active-instinct rule, rendered inside the plinth card (as in the design). */
@Composable
private fun InstinctRule() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(KdmPalette.surfaceContainer)
                .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Symbol("psychology", KdmPalette.gold, 16)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Instinto: Olfateo".uppercase(),
                color = KdmPalette.onCanvas,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Text(
                text =
                    "El León Blanco olfatea el aire y finaliza su turno. " +
                        "Todos los supervivientes pasan a ser considerados amenazas activas.",
                color = KdmPalette.ash,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

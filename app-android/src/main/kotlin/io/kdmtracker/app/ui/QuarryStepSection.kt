package io.kdmtracker.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.R
import io.kdmtracker.app.ui.theme.KdmType

/** The design's `Presa Asignada` row and quarry monster card (the names are inert constants). */
@Composable
internal fun QuarrySection(quarryCount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Symbol("pets", MaterialTheme.colorScheme.secondary, 14)
                Text(
                    text = "Presa Asignada".uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            // Quarry re-selection is not modeled at this step → disabled (backlog).
            TextButton(onClick = {}, enabled = false) {
                Text(text = "Canteras ($quarryCount)", style = KdmType.labelCaps)
                Spacer(Modifier.width(4.dp))
                Symbol("swap_horiz", MaterialTheme.colorScheme.onSurfaceVariant, 13)
            }
        }
        QuarryCard()
    }
}

/**
 * The packaged quarry photo with the design's overlays, level chip, names and inert stat boxes.
 * Kicker/name/subtitle are the design's inert constants ([PrepareDepartingStepCopy.QUARRY_KICKER],
 * [PrepareDepartingStepCopy.QUARRY_NAME], [PrepareDepartingStepCopy.QUARRY_SUBTITLE] — no
 * assigned-quarry model exists before hunt setup, so nothing is derived from state here).
 */
@Composable
private fun QuarryCard() {
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(surface),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(112.dp)) {
            Image(
                painter = painterResource(R.drawable.settlement_step8_quarry),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.35f,
                modifier = Modifier.fillMaxSize(),
            )
            // The design's two scrims: solid to the top edge and to both side edges.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.5f to surface.copy(alpha = 0.8f),
                                1f to surface,
                            ),
                        ),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to surface,
                                0.5f to Color.Transparent,
                                1f to surface.copy(alpha = 0.6f),
                            ),
                        ),
            )
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f),
                            MaterialTheme.shapes.small,
                        ).padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Symbol("star", MaterialTheme.colorScheme.secondary, 13)
                Text(
                    text = PrepareDepartingStepCopy.QUARRY_LEVEL,
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = PrepareDepartingStepCopy.QUARRY_KICKER.uppercase(),
                        style = KdmType.labelCaps,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = PrepareDepartingStepCopy.QUARRY_NAME,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = PrepareDepartingStepCopy.QUARRY_SUBTITLE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuarryStatBox(PrepareDepartingStepCopy.QUARRY_STAT_LIFE_LABEL, QUARRY_LIFE_AI)
                    QuarryStatBox(PrepareDepartingStepCopy.QUARRY_STAT_TOUGH_LABEL, QUARRY_TOUGHNESS)
                }
            }
        }
    }
}

/** One inert quarry stat box (`Vida / IA` / `Dureza`) with its verbatim design value. */
@Composable
private fun QuarryStatBox(
    label: String,
    value: Int,
) {
    Column(
        modifier =
            Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
                    MaterialTheme.shapes.small,
                ).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(),
            style = KdmType.labelCaps.copy(fontSize = 9.sp, lineHeight = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value.toString(),
            style = KdmType.statDisplay.copy(fontSize = 15.sp, lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

package io.kdmtracker.app.ui.hunt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.Symbol
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.core.domain.HuntSpace
import io.kdmtracker.core.domain.HuntSpaceKind

/** Region 6 — the active hunt-event card, with its title, description and result table verbatim. */
@Composable
internal fun HuntEventCard(
    space: HuntSpace,
    index: Int,
    partySpace: HuntSpace,
    huntEvents: List<io.kdmtracker.core.domain.HuntEvent>,
    drawnEvent: io.kdmtracker.core.domain.HuntEvent?,
    atStart: Boolean,
    stepRoll: Int?,
    onEditRoll: (Int) -> Unit,
    onAssign: (String) -> Unit,
    onResolve: (String) -> Unit,
    onMarkKind: (HuntSpaceKind) -> Unit,
) {
    HuntPanel {
        // At the start step there is no event yet — only the hunt begins.
        if (atStart) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Symbol("flag", KdmPalette.green, 16)
                Text(
                    text = "Inicia la cacería".uppercase(),
                    color = KdmPalette.green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Text(
                text = "El grupo aún no ha avanzado: no se roba carta hasta el primer paso.",
                color = KdmPalette.ash,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            return@HuntPanel
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Symbol("auto_stories", KdmPalette.gold, 16)
            Text(
                text = "Evento de Caza #$index".uppercase(),
                color = KdmPalette.gold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text = "Tirada d10",
                color = KdmPalette.onCanvas,
                fontSize = 11.sp,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(KdmPalette.surfaceHighest)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }

        StepRoll(stepRoll = stepRoll, onEditRoll = onEditRoll)

        // The card the group drew on their last advance: its real name and rules text.
        if (drawnEvent != null) {
            Text(
                text = drawnEvent.name,
                color = KdmPalette.onCanvas,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = KdmFontFamilies.playfair,
            )
            // The design's "Flujo de Casillas" card — an italic quote intro + toned d10 blocks.
            val card = HuntEventCardModel.parse(drawnEvent.text)
            if (card.intro.isNotEmpty()) {
                Text(
                    text = "\"${card.intro}\"",
                    color = KdmPalette.ash.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontStyle = FontStyle.Italic,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawRect(
                                    KdmPalette.outlineVariant,
                                    topLeft = Offset.Zero,
                                    size = Size(2.dp.toPx(), size.height),
                                )
                            }.padding(start = 10.dp, top = 2.dp, bottom = 2.dp),
                )
            }
            card.ranges.forEach { range ->
                val style = range.tone.style()
                val chipBg =
                    when (style.chipBg) {
                        ChipBg.DANGER -> KdmPalette.bloodContainer
                        ChipBg.GOLD -> KdmPalette.goldContainer
                        ChipBg.NEUTRAL -> KdmPalette.surfaceHighest
                    }
                val chipText =
                    when (style.chipText) {
                        ChipText.DANGER -> KdmPalette.blood
                        ChipText.GOLD -> KdmPalette.gold
                        ChipText.NEUTRAL -> KdmPalette.onCanvas
                    }
                val borderColor =
                    when (style.border) {
                        BorderAccent.DANGER -> KdmPalette.blood
                        BorderAccent.GOLD -> KdmPalette.gold
                        BorderAccent.NEUTRAL -> KdmPalette.onCanvas.copy(alpha = 0.5f)
                    }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(KdmPalette.surfaceLow)
                            .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            // the design's 3px tone accent down the left edge of each block
                            .drawBehind {
                                drawRect(
                                    borderColor,
                                    topLeft = Offset.Zero,
                                    size = Size(3.dp.toPx(), size.height),
                                )
                            }.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Range chip with the design's tone fill (red / gold / neutral).
                    Text(
                        text = range.label,
                        color = chipText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(chipBg)
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                    Text(
                        text = range.text,
                        color = KdmPalette.onCanvas,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            // Resolve by hand: record the drawn card on the party's current space and mark it
            // resolved. The button always acts on the current space (partySpace), independent of
            // which space is being viewed (space / index).
            val onParty = drawnEvent.name in partySpace.events
            val isResolved = drawnEvent.name in partySpace.resolved
            OutlinedButton(
                onClick = {
                    if (!onParty) onAssign(drawnEvent.name)
                    if (!isResolved) onResolve(drawnEvent.name)
                },
                enabled = !isResolved,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isResolved) "Resuelta ✓" else "Marcar resuelta")
            }
        } else {
            Text(
                text = "Avanza el grupo para robar una carta de evento de caza.",
                color = KdmPalette.ash,
                fontSize = 12.sp,
            )
        }

        // The selected space's resolved history: visible when navigating backward.
        if (space.events.isNotEmpty()) {
            Text(
                text = "Historial de la casilla $index".uppercase(),
                color = KdmPalette.onCanvas,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp),
            )
            space.events.forEach { name ->
                val resolved = name in space.resolved
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(KdmPalette.surfaceHighest)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        color = if (resolved) KdmPalette.ash else KdmPalette.onCanvas,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (resolved) "Resuelta" else "Pendiente",
                        color = if (resolved) KdmPalette.gold else KdmPalette.blood,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

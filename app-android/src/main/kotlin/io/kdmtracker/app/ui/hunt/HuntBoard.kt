package io.kdmtracker.app.ui.hunt

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.Symbol
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.core.domain.HuntState

/**
 * The 13-space hunt board, rebuilt 1:1 from the Stitch design: a horizontal track of stone cells
 * over the [HuntBoardFrame] view (which pins each cell's index, icon and label). The survivor
 * party's cell glows gold, the monster's glows red, and each carries the design's position lozenge.
 */
@Composable
fun HuntBoard(
    hunt: HuntState,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
) {
    val frame = HuntBoardFrame.from(hunt)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KdmPalette.surfaceLow)
                .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Symbol("route", KdmPalette.gold, 16)
                Text(
                    text = "Tablero de Caza".uppercase(),
                    color = KdmPalette.onCanvas,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Text(text = frame.rangeLabel, color = KdmPalette.ash, fontSize = 11.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            frame.spaces.forEach { view ->
                HuntSpaceCell(
                    view = view,
                    selected = view.index == selectedIndex,
                    onClick = { onSelectIndex(view.index) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MarkerRow(color = KdmPalette.gold, text = frame.partyMarker)
            MarkerRow(color = KdmPalette.blood, text = frame.monsterMarker)
        }
    }
}

/** One board cell: the index on top, the icon medallion, the label, and the position lozenge. */
@Composable
private fun HuntSpaceCell(
    view: HuntSpaceView,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Design anatomy (Stitch `estados`): the tone drives the cell border, medallion and label.
    val tone = view.tone
    val isBeast = view.isBeast || view.icon == "cruelty_free"
    val isSpecialEvent = view.icon == "visibility"
    val isFilledMedallion = view.isParty || isBeast // groups (party) / paw (beast) are solid

    // Accent = the tone's border/label colour; SURFACE and OUTLINE have no accent.
    val accent: Color? =
        when (tone) {
            SpaceTone.GOLD -> KdmPalette.gold
            SpaceTone.BLOOD -> KdmPalette.blood
            SpaceTone.VIOLET -> KdmPalette.violet
            SpaceTone.AMBER -> KdmPalette.amber
            SpaceTone.GREEN -> KdmPalette.green
            SpaceTone.SURFACE, SpaceTone.OUTLINE -> if (selected) KdmPalette.gold else null
        }
    val numberColor = accent ?: KdmPalette.ash
    val cellBg = KdmPalette.surfaceLow
    val medallionSize = 44.dp // the design's `w-11 h-11` round medallion, always round
    // Border weight: the active/party and the beast cells carry the design's 2px accent border.
    val borderWidth = if (view.isParty || isBeast || isSpecialEvent) 2.dp else 1.dp

    // Every card is the SAME fixed size — the design's `w-[84px] h-[134px]`, so the track
    // has one uniform card height regardless of label wrap.
    val cellWidth = 72.dp
    val cellHeight = 132.dp // the design's `h-[134px]`; fits a 2-line label + caption without clipping

    Box(
        modifier =
            Modifier
                .width(cellWidth)
                .height(cellHeight) // uniform height — never content-driven
                .clip(RoundedCornerShape(8.dp))
                .background(cellBg)
                .border(
                    borderWidth,
                    accent ?: KdmPalette.outlineVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp),
                ).clickable(onClick = onClick)
                .padding(vertical = 4.dp, horizontal = 6.dp), // tight top pad so the row hugs the edge
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top row: number at the left + the type badge/ping at the right, flush to the
            // card's top edge (the row itself touches the border — no stray gap above it).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (view.showNumber) {
                    Text(
                        text = view.index.toString().padStart(2, '0'),
                        color = numberColor,
                        fontWeight = if (accent != null) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp,
                    )
                } else {
                    // Even with no number, the row keeps its left edge so the mark stays flush right.
                    Box(modifier = Modifier.size(10.dp))
                }
                // The party's step swaps its type badge for the animated ping dot.
                if (view.partyPing) {
                    PartyPingDot(color = accent ?: KdmPalette.green)
                } else {
                    BadgeMark(name = view.badge, tint = accent ?: KdmPalette.ash)
                }
            }
            Box(
                modifier =
                    Modifier
                        .size(medallionSize)
                        .clip(CircleShape) // always round
                        // The quarry's eye sits on the dark container with a gold ring + gold eye —
                        // never a solid gold disc (the gold-on-gold eye would be invisible).
                        .background(if (isSpecialEvent) KdmPalette.surfaceContainer else medallionBg(tone))
                        .border(
                            if (isFilledMedallion || tone == SpaceTone.OUTLINE) 1.dp else 2.dp,
                            accent ?: KdmPalette.outlineVariant.copy(alpha = 0.5f),
                            CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                // Ink: filled medallions get dark ink on their accent; the rest get the accent itself.
                val ink =
                    when {
                        view.isParty -> KdmPalette.green // emerald medallion -> ink on the wash
                        tone == SpaceTone.BLOOD && isBeast -> KdmPalette.blood // red paw on the container
                        isSpecialEvent -> KdmPalette.gold // gold eye
                        tone == SpaceTone.VIOLET -> KdmPalette.violet
                        tone == SpaceTone.AMBER -> KdmPalette.amber
                        tone == SpaceTone.GREEN -> KdmPalette.green
                        else -> KdmPalette.ash
                    }
                if (view.icon == "E") {
                    Text(
                        text = "E",
                        color = ink.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                } else {
                    Symbol(
                        name = view.icon,
                        color = ink,
                        size = 24,
                        // Filled for the solid party/beast discs and for the quarry's gold eye
                        // (`FILL 1` in the design); outline for the neutral round marks.
                        fill = isFilledMedallion || isSpecialEvent,
                    )
                }
            }
            Text(
                text = view.label,
                color = accent ?: KdmPalette.ash,
                fontWeight = if (accent != null) FontWeight.Bold else FontWeight.Normal,
                fontSize = 9.5.sp,
                lineHeight = 11.sp, // tight leading so a 2-line label (Oscuridad Abrumadora) fits
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (view.subLabel.isNotEmpty()) {
                Text(
                    text = view.subLabel,
                    color = (accent ?: KdmPalette.outline).copy(alpha = 0.75f),
                    fontSize = 7.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The small top-right type badge: a material glyph when it maps to a symbol, else its text. */
@Composable
private fun BadgeMark(
    name: String,
    tint: Color,
) {
    if (name.isBlank()) return
    if (name == "GEN") {
        Text(text = "GEN", color = tint.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
    } else {
        Symbol(name = name, color = tint, size = 12, fill = false)
    }
}

/** The `pulse-lantern` / `pulse-beast` phase: a 0f→1f triangle wave for the ping ring. */
@Composable
private fun pingPhase(): Float {
    val transition = rememberInfiniteTransition(label = "ping")
    val phase by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart),
            label = "pingPhase",
        )
    return phase
}

/**
 * The party's top-right mark — a small circle that **switches on, grows small→large and
 * vanishes, looping forever**, staying **inside the step** (its max size barely exceeds the other
 * icons, never overflowing the card). The design's `animate-ping` dot at 6px base.
 */
@Composable
private fun PartyPingDot(color: Color) {
    val phase = pingPhase()
    // 0→1: grows small→large then fades out; a solid dot always anchors the centre so it reads as
    // "on" even at the low end of the cycle (never a blank gap).
    val dotSize = 6.dp // the design's `w-1.5 h-1.5` base
    val maxGrowth = 5.dp // grows to ~11dp — barely past the other icons, bounded inside the step
    val grow = dotSize + (maxGrowth * phase)
    val ringAlpha = (1f - phase) * 0.6f
    Box(modifier = Modifier.size(dotSize + maxGrowth), contentAlignment = Alignment.Center) {
        // The expanding, fading ring (the ping). Drawn as a growing, fading solid dot.
        Box(
            modifier =
                Modifier
                    .size(grow)
                    .clip(CircleShape)
                    .background(color.copy(alpha = ringAlpha)),
        )
        // The solid centre dot: always visible (the "on" state).
        Box(
            modifier =
                Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color),
        )
    }
}

/** The medallion fill by tone: filled accents are solid; the rest sit on the container tile. */
private fun medallionBg(tone: SpaceTone): Color =
    when (tone) {
        SpaceTone.GOLD -> KdmPalette.gold
        SpaceTone.BLOOD -> KdmPalette.bloodContainer
        SpaceTone.VIOLET -> KdmPalette.violetContainer
        SpaceTone.AMBER -> KdmPalette.amberContainer
        SpaceTone.GREEN -> KdmPalette.greenWash
        SpaceTone.SURFACE, SpaceTone.OUTLINE -> KdmPalette.surfaceContainer
    }

/** One of the two board markers: a coloured dot plus its label. */
@Composable
private fun MarkerRow(
    color: Color,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Text(text = text, color = KdmPalette.ash, fontSize = 11.sp)
    }
}

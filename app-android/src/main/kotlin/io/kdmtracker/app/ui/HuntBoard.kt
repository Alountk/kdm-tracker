package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // The beast's space is a paw; the quarry's special event is a gold eye; the rest are grey.
    val isBeast = view.icon == "pets"
    val isSpecialEvent = view.icon == "visibility"
    val isFilledMedallion = view.isParty || isBeast // star (party) / paw (beast) are solid
    val accent =
        when {
            isBeast -> KdmPalette.blood
            view.isParty -> KdmPalette.gold
            isSpecialEvent -> KdmPalette.gold
            selected -> KdmPalette.gold
            else -> null
        }
    val numberColor = accent ?: KdmPalette.ash
    val cellBg =
        when {
            isBeast -> KdmPalette.surfaceHigh
            view.isParty -> KdmPalette.surfaceHigh
            else -> KdmPalette.surfaceHighest
        }
    val medallionSize = if (isFilledMedallion) 32.dp else 28.dp

    Box(
        modifier =
            Modifier
                .width(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(cellBg)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = view.index.toString(),
                color = numberColor,
                fontWeight = if (accent != null) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp,
            )
            Box(
                modifier =
                    Modifier
                        .size(medallionSize)
                        .clip(if (isFilledMedallion) CircleShape else RoundedCornerShape(4.dp))
                        .background(accent ?: KdmPalette.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                // Filled medallions (party / beast) need ink on the accent, not the accent itself.
                val ink =
                    when {
                        view.isParty -> KdmPalette.onGold // gold medallion -> dark ink
                        isBeast -> KdmPalette.onBlood // blood medallion -> dark ink
                        isSpecialEvent -> KdmPalette.gold // gold eye on a dark tile
                        else -> KdmPalette.ash
                    }
                if (view.icon == "E") {
                    Text(
                        text = "E",
                        color = ink.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                } else {
                    Symbol(
                        name = view.icon,
                        color = ink,
                        size = if (isFilledMedallion) 18 else 16,
                        fill = isFilledMedallion,
                    )
                }
            }
            Text(
                text = view.label,
                color = accent ?: KdmPalette.ash,
                fontWeight = if (accent != null) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // The design's rotated position lozenge, only on the party / beast cells.
            if (view.isParty || isBeast) {
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .rotate(45f)
                            .background(accent ?: KdmPalette.gold),
                )
            }
        }
    }
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

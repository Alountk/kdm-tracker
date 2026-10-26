package io.kdmtracker.app.ui.hunt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.Symbol
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid

/** Region 4 — the expedition: `Cazadores en Marcha` with a chip per departing survivor. */
@Composable
internal fun ExpeditionChips(
    party: List<Survivor>,
    onAssignPlayer: (Ulid, Int) -> Unit,
    players: Int,
) {
    // The design renders this region directly on the `surface` (no wrapper card): a header row
    // with the label in `on-surface-variant` and the lantern count in `secondary` (gold), then a
    // grid of independent `surface-container-low` chips separated by gaps.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Symbol("groups", KdmPalette.ash, 14)
                Text(
                    text = "Cazadores en Marcha".uppercase(),
                    color = KdmPalette.ash,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            Text(text = "${party.size} Linternas encendidas", color = KdmPalette.gold, fontSize = 11.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            party.take(4).forEach { survivor ->
                SurvivorChip(
                    survivor = survivor,
                    selected = true,
                    onClick = { onAssignPlayer(survivor.id, players) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SurvivorChip(
    survivor: Survivor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initials =
        survivor.name.trim().let {
            if (it.isEmpty()) {
                "??"
            } else {
                it
                    .split(Regex("\\s+"))
                    .map { w ->
                        w.first().uppercase()
                    }.take(2)
                    .joinToString("")
            }
        }
    // The design's expedition chip: `surface-container-low` tile, a `surface-container-highest`
    // avatar with the survivor's initials, the name in `text-primary`, and the insanity (psychology)
    // / survival (favorite) counters in `text-secondary` / `text-error`.
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(KdmPalette.surfaceLow)
                .clickable(onClick = onClick)
                .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(KdmPalette.surfaceHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = initials, color = KdmPalette.onCanvas, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Text(
            text = survivor.name.ifBlank { "Unnamed" },
            color = KdmPalette.onCanvas,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol("psychology", KdmPalette.gold, 11)
            Text(
                text = "${survivor.insanity}",
                color = KdmPalette.onCanvas,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Symbol("favorite", KdmPalette.blood, 11)
            Text(
                text = "${survivor.survival}",
                color = KdmPalette.onCanvas,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

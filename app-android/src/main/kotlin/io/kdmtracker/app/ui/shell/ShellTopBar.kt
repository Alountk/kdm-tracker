package io.kdmtracker.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType

private val BarHeight = 80.dp
private val AvatarSize = 32.dp

/**
 * The design's fixed top bar: the lantern year, the view's name and the settlement on the left; the
 * settlement's stat cluster and the account avatar on the right.
 *
 * The design blurs the backdrop behind the bar. Compose has no backdrop filter — `Modifier.blur`
 * blurs a layer's own content — so the bar uses a near-opaque surface and an elevation shadow
 * instead. That is the one visual departure, and it is recorded as an accepted deviation.
 *
 * @param title the selected view's name (the design names each screen in the bar).
 */
@Composable
fun ShellTopBar(
    title: String,
    lanternYear: Int,
    settlementName: String,
    population: Int,
    deaths: Int,
    onAccount: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                .shadow(elevation = 8.dp)
                .height(BarHeight)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Symbol("light_mode", MaterialTheme.colorScheme.secondary, 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Año Linterna $lanternYear".uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = settlementName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCluster(population = population, deaths = deaths)
            Avatar(onClick = onAccount)
        }
    }
}

@Composable
private fun StatCluster(
    population: Int,
    deaths: Int,
) {
    Row(
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ShellStats.of(population = population, deaths = deaths).forEachIndexed { index, stat ->
            if (index > 0) {
                Divider()
            }
            StatEntry(stat = stat)
        }
    }
}

@Composable
private fun StatEntry(stat: ShellStat) {
    val color =
        when {
            stat.unbacked -> MaterialTheme.colorScheme.outlineVariant
            stat.icon == "skull" -> MaterialTheme.colorScheme.error
            stat.icon == "flare" -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurface
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Symbol(stat.icon, color, 14.sp)
        Spacer(Modifier.width(4.dp))
        Text(text = stat.value, style = KdmType.labelCaps, color = color)
    }
}

@Composable
private fun Divider() {
    Box(
        modifier =
            Modifier
                .width(1.dp)
                .height(12.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}

@Composable
private fun Avatar(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(AvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Symbol("person", MaterialTheme.colorScheme.onPrimary, 18.sp)
    }
}

@Composable
private fun Symbol(
    name: String,
    color: Color,
    size: androidx.compose.ui.unit.TextUnit,
) {
    Text(
        text = KdmSymbols.glyph(name),
        fontFamily = KdmSymbols.family,
        fontSize = size,
        color = color,
    )
}

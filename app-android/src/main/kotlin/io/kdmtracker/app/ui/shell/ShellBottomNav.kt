package io.kdmtracker.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.Screen
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType

/** The active-tab rule, kept beside the bar it drives. */
object ShellBottomNav {
    /** The tab showing [screen], or `null` when the screen lives outside the shell. */
    fun activeTab(screen: Screen): ShellTab? = ShellTab.of(screen)
}

private val NavHeight = 64.dp

/**
 * The design's fixed bottom navigation: the four in-campaign views, the current one in gold.
 *
 * As with the top bar, the design's backdrop blur is not reproducible in Compose, so the bar is a
 * near-opaque `surface` with an elevation shadow — the same accepted deviation.
 */
@Composable
fun ShellBottomNav(
    selected: ShellTab,
    onSelect: (ShellTab) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
                .shadow(elevation = 8.dp)
                .height(NavHeight)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        ShellTab.entries.forEach { tab ->
            Tab(
                tab = tab,
                selected = tab == selected,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
private fun Tab(
    tab: ShellTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color =
        if (selected) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Column(
        modifier =
            Modifier
                .widthIn(min = 56.dp)
                .height(44.dp)
                .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = KdmSymbols.glyph(tab.icon),
            fontFamily = KdmSymbols.family,
            fontSize = 20.sp,
            color = color,
        )
        Text(
            text = tab.label.uppercase(),
            style = KdmType.labelCaps,
            color = color,
        )
    }
}

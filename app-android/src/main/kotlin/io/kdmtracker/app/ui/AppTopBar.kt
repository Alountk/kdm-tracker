package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The single global top bar: the title, a tappable sync-status chip, Home (on the phases) and the
 * Account control. Stateless: everything it shows comes from [chrome] and every tap is reported
 * through a callback, so the bar owns no rules and no state.
 *
 * Home and Account are plain text actions rather than icons — the icons artifact failed to dex in
 * this build (see the ledger), and words are more discoverable than glyphs anyway.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    chrome: AppChrome,
    onChipAction: (ChipAction) -> Unit,
    onHome: () -> Unit,
    onAccount: () -> Unit,
) {
    TopAppBar(
        title = { Text(chrome.title) },
        actions = {
            StatusChip(chrome = chrome, onClick = { onChipAction(chrome.chipAction) })
            if (chrome.showHome) {
                TextButton(onClick = onHome) { Text("Home") }
            }
            if (chrome.showAccount) {
                TextButton(onClick = onAccount) { Text("Account") }
            }
        },
    )
}

@Composable
private fun StatusChip(
    chrome: AppChrome,
    onClick: () -> Unit,
) {
    val (dotColor, container) = chipColors(chrome.chipTone)
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(end = 4.dp),
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
            Text(text = chrome.chipLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** The chip's dot and container colours per tone. */
private fun chipColors(tone: ChipTone): Pair<Color, Color> =
    when (tone) {
        ChipTone.CONNECTED -> Color(0xFF137333) to Color(0xFFE6F4EA)
        ChipTone.SYNCING -> Color(0xFF1A73E8) to Color(0xFFE8F0FE)
        ChipTone.ERROR -> Color(0xFFB06000) to Color(0xFFFEF7E0)
        ChipTone.SIGNED_OUT -> Color(0xFF9AA0A6) to Color(0xFFF1F3F4)
    }

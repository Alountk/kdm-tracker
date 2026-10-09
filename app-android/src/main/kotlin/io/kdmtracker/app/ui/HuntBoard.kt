package io.kdmtracker.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.core.domain.HuntSpace
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntState

/**
 * The 13-space hunt board: a horizontal, scrollable row of the spaces, each showing its kind and how
 * many of its events are resolved. The current space and the selected space are highlighted.
 */
@Composable
fun HuntBoard(
    hunt: HuntState,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        hunt.spaces.forEachIndexed { index, space ->
            HuntSpaceCell(
                index = index,
                space = space,
                current = index == hunt.position,
                selected = index == selectedIndex,
                onClick = { onSelectIndex(index) },
            )
        }
    }
}

@Composable
private fun HuntSpaceCell(
    index: Int,
    space: HuntSpace,
    current: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container =
        when {
            selected -> MaterialTheme.colorScheme.secondaryContainer
            current -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    Surface(
        onClick = onClick,
        color = container,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = space.kind.marker(), style = MaterialTheme.typography.labelLarge)
            Text(text = "$index", style = MaterialTheme.typography.bodySmall)
            Text(text = "${space.resolved.size}/${space.events.size}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun HuntSpaceKind.marker(): String =
    when (this) {
        HuntSpaceKind.START -> "★"
        HuntSpaceKind.NORMAL -> "•"
        HuntSpaceKind.MONSTER_E -> "E"
        HuntSpaceKind.OVERWHELMING_DARKNESS -> "◎"
        HuntSpaceKind.STARVATION -> "☠"
        HuntSpaceKind.MONSTER -> "◆"
    }

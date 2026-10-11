package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.StorageLine

internal enum class DevelopTab { RECURSOS, INNOVAR, FABRICAR }

/**
 * The Step 7 "Develop" body: a small tab switcher over [RecursosTab], [InnovarTab] and
 * [FabricarTab], inside the cockpit's chrome. The selected tab is hoisted to the caller
 * ([selectedTab]/[onTabSelected]) so the Settlement screen's panels can open a tab — the
 * locations panel's Innovar action jumps here; every action is reported through a callback and
 * every list comes from [state].
 */
@Composable
internal fun DevelopStepScreen(
    state: UiState,
    onInnovate: (String) -> Unit,
    onBuild: (String) -> Unit,
    onSpendEndeavor: () -> Unit,
    onAddResource: (ResourceEntry) -> Unit,
    onCraft: (String, String) -> Unit = { _, _ -> },
    selectedTab: DevelopTab = DevelopTab.RECURSOS,
    onTabSelected: (DevelopTab) -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.actionError?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChoiceChip("Recursos", selectedTab == DevelopTab.RECURSOS) { onTabSelected(DevelopTab.RECURSOS) }
            ChoiceChip("Innovar", selectedTab == DevelopTab.INNOVAR) { onTabSelected(DevelopTab.INNOVAR) }
            ChoiceChip("Fabricar", selectedTab == DevelopTab.FABRICAR) { onTabSelected(DevelopTab.FABRICAR) }
        }

        Text(
            text = "Endeavors ${state.endeavors} · ${storageText(state.storageLines, state.resourceDefinitions)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (selectedTab) {
            DevelopTab.RECURSOS -> RecursosTab(state = state, onAddResource = onAddResource)
            DevelopTab.INNOVAR -> InnovarTab(state = state, onInnovate = onInnovate)
            DevelopTab.FABRICAR ->
                FabricarTab(
                    state = state,
                    onBuild = onBuild,
                    onCraft = onCraft,
                    onSpendEndeavor = onSpendEndeavor,
                )
        }
    }
}

/** A small selectable chip, shared by the Develop tabs. */
@Composable
internal fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color =
            if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        contentColor =
            if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private fun storageText(
    lines: List<StorageLine>,
    definitions: List<ResourceDefinition>,
): String {
    if (lines.isEmpty()) return "Storage: empty"
    val byId = definitions.associateBy { it.id }
    return "Storage: " + lines.joinToString(" · ") { "${byId[it.id]?.names?.es ?: it.id.value} ${it.quantity}" }
}

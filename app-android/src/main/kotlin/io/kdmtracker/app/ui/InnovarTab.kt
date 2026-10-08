package io.kdmtracker.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.rules.InnovationStatus
import io.kdmtracker.core.rules.InnovationView

private enum class InnovarFilter { TODAS, DISPONIBLES, DESBLOQUEADAS, SIN_ACCESO }

private val Unlocked = Color(0xFF2ECC71)
private val Available = Color(0xFF3B82F6)

/**
 * The Innovar tab: the innovation unlock tree by access state, with the filter chips
 * Todas / Disponibles / Desbloqueadas / Sin acceso — green = unlocked, blue = available (its parent
 * is unlocked), grey = no access (shows the parent it still needs).
 */
@Composable
internal fun InnovarTab(
    state: UiState,
    onInnovate: (String) -> Unit,
) {
    var filter by remember { mutableStateOf(InnovarFilter.TODAS) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ChoiceChip("Todas", filter == InnovarFilter.TODAS) { filter = InnovarFilter.TODAS }
            ChoiceChip("Disponibles", filter == InnovarFilter.DISPONIBLES) { filter = InnovarFilter.DISPONIBLES }
            ChoiceChip("Desbloqueadas", filter == InnovarFilter.DESBLOQUEADAS) { filter = InnovarFilter.DESBLOQUEADAS }
            ChoiceChip("Sin acceso", filter == InnovarFilter.SIN_ACCESO) { filter = InnovarFilter.SIN_ACCESO }
        }

        state.innovationViews.filter { it.matches(filter) }.forEach { view ->
            InnovarRow(view = view, onInnovate = onInnovate)
        }
    }
}

private fun InnovationView.matches(filter: InnovarFilter): Boolean =
    when (filter) {
        InnovarFilter.TODAS -> true
        InnovarFilter.DISPONIBLES -> status == InnovationStatus.AVAILABLE
        InnovarFilter.DESBLOQUEADAS -> status == InnovationStatus.UNLOCKED
        InnovarFilter.SIN_ACCESO -> status == InnovationStatus.NO_ACCESS
    }

@Composable
private fun InnovarRow(
    view: InnovationView,
    onInnovate: (String) -> Unit,
) {
    val color =
        when (view.status) {
            InnovationStatus.UNLOCKED -> Unlocked
            InnovationStatus.AVAILABLE -> Available
            InnovationStatus.NO_ACCESS -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val subtitle =
        when (view.status) {
            InnovationStatus.UNLOCKED -> "desbloqueada"
            InnovationStatus.AVAILABLE -> "disponible · ${actionCost(view)}"
            InnovationStatus.NO_ACCESS -> "sin acceso — requiere ${view.missingParent ?: "?"}"
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text =
                    if (view.status ==
                        InnovationStatus.UNLOCKED
                    ) {
                        "${view.innovation.name} ✓"
                    } else {
                        view.innovation.name
                    },
                style = MaterialTheme.typography.bodyLarge,
                color = color,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (view.status == InnovationStatus.AVAILABLE) {
            Button(onClick = { onInnovate(view.innovation.name) }, enabled = !view.used) {
                Text(if (view.used) "Usada esta fase" else "Innovar")
            }
        }
    }
}

private fun actionCost(view: InnovationView): String {
    val parts = mutableListOf("${view.endeavorCost} esfuerzo")
    view.resourceCost.entries
        .sortedBy { it.key }
        .forEach { (name, count) -> parts += "$count $name" }
    return parts.joinToString(" + ")
}

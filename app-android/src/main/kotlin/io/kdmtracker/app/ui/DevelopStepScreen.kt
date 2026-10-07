package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.SettlementAction
import io.kdmtracker.core.domain.SettlementLocation
import io.kdmtracker.core.domain.StorageLine
import io.kdmtracker.core.rules.EndeavorBreakdown

/**
 * The Step 7 "Develop" body.
 *
 * Three affordances with their real costs: **Innovate** (the catalog's Innovate location, with its
 * cost and once-per-phase limit), **Build** (the not-yet-built locations with their cost and
 * prerequisites) and a generic **Endeavor** (−1). The current storage ledger and endeavor pool are
 * shown beside them, and every action's rules text is displayed verbatim — effects are resolved by
 * the players, never executed here. Stateless: content comes from [state] and every action is
 * reported through a callback; no rules live here.
 */
@Composable
fun DevelopStepScreen(
    state: UiState,
    onInnovate: (String) -> Unit,
    onBuild: (String) -> Unit,
    onSpendEndeavor: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = state.stepTitle, style = MaterialTheme.typography.headlineMedium)
        Text(
            text =
                "Lantern Year ${state.lanternYear} · Population ${state.population} · " +
                    "Endeavors ${state.endeavors}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.actionError?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        InnovateSection(state = state, onInnovate = onInnovate)
        BuildSection(state = state, onBuild = onBuild)
        EndeavorSection(state = state, onSpendEndeavor = onSpendEndeavor)
        StorageSection(lines = state.storageLines, breakdown = state.endeavorBreakdown)
    }
}

@Composable
private fun InnovateSection(
    state: UiState,
    onInnovate: (String) -> Unit,
) {
    val hoard =
        state.buildableLocations.firstOrNull { location ->
            location.actions.any { it.kind == ActionKind.INNOVATE }
        }
    DevelopSection(title = "Innovate") {
        if (hoard == null) {
            Text(text = "No location offers Innovate", style = MaterialTheme.typography.bodySmall)
            return@DevelopSection
        }
        val action = hoard.actions.first { it.kind == ActionKind.INNOVATE }
        val used = state.record?.state?.usedInPhase(hoard.name) == true
        Text(text = hoard.name, style = MaterialTheme.typography.titleSmall)
        Text(text = "Cost: ${actionCost(action)}", style = MaterialTheme.typography.bodySmall)
        Text(
            text = if (action.oncePerPhase) "Once per Settlement Phase" else "Repeatable this phase",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (used) {
            Text(
                text = "Used this phase",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (action.effect.isNotBlank()) {
            Text(text = action.effect, style = MaterialTheme.typography.bodySmall)
        }
        state.innovationDefinitions.forEach { innovation ->
            Button(
                onClick = { onInnovate(innovation.name) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Innovate ${innovation.name}")
            }
            if (innovation.description.isNotBlank()) {
                Text(text = innovation.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BuildSection(
    state: UiState,
    onBuild: (String) -> Unit,
) {
    val buildable = state.buildableLocations.filter { it.name !in state.builtLocations }
    DevelopSection(title = "Build") {
        if (buildable.isEmpty()) {
            Text(text = "Nothing left to build", style = MaterialTheme.typography.bodySmall)
            return@DevelopSection
        }
        buildable.forEach { location ->
            Text(text = location.name, style = MaterialTheme.typography.titleSmall)
            Text(text = "Cost: ${buildCost(location)}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Requires: ${location.requires.ifEmpty { listOf("None") }.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { onBuild(location.name) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Build ${location.name}")
            }
        }
    }
}

@Composable
private fun EndeavorSection(
    state: UiState,
    onSpendEndeavor: () -> Unit,
) {
    DevelopSection(title = "Endeavor") {
        Text(
            text = "A generic action spends 1 endeavor; resolve its effect with the group.",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = onSpendEndeavor,
            enabled = state.endeavors >= 1,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Spend Endeavor (−1)")
        }
    }
}

@Composable
private fun StorageSection(
    lines: List<StorageLine>,
    breakdown: EndeavorBreakdown,
) {
    DevelopSection(title = "Storage") {
        if (lines.isEmpty()) {
            Text(text = "Storage is empty", style = MaterialTheme.typography.bodySmall)
        } else {
            lines.forEach { line ->
                Text(
                    text = "${line.name} (${line.category}): ${line.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(
            text = "Endeavors: ${breakdown.total} (base ${breakdown.base})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        breakdown.bonuses.forEach { (name, value) ->
            Text(
                text = "$name +$value",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DevelopSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

private fun actionCost(action: SettlementAction): String {
    val endeavors = "${action.endeavorCost} " + if (action.endeavorCost == 1) "endeavor" else "endeavors"
    val resources = action.resourceCost.entries.joinToString(", ") { "${it.key} ×${it.value}" }
    return if (resources.isEmpty()) endeavors else "$endeavors + $resources"
}

private fun buildCost(location: SettlementLocation): String {
    val endeavors = "${location.buildEndeavors} " + if (location.buildEndeavors == 1) "endeavor" else "endeavors"
    val resources = location.buildCost.entries.joinToString(", ") { "${it.key} ×${it.value}" }
    return if (resources.isEmpty()) endeavors else "$endeavors + $resources"
}

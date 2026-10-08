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
import io.kdmtracker.app.CraftRecipeView
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.GearRecipe

private val Craftable = Color(0xFF2ECC71)

/**
 * The Fabricar tab: build locations, then craft gear grouped by **localización** or by **tipo**.
 * A recipe you can craft now is green with an enabled *Crear*; otherwise it is dimmed and shows
 * what is missing.
 */
@Composable
internal fun FabricarTab(
    state: UiState,
    onBuild: (String) -> Unit,
    onCraft: (String, String) -> Unit,
    onSpendEndeavor: () -> Unit,
) {
    var groupByType by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val buildable = state.catalogLocations.filter { it.name !in state.builtLocations }
        if (buildable.isNotEmpty()) {
            Text(text = "Construir", style = MaterialTheme.typography.titleSmall)
            buildable.forEach { location ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = location.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Cost: ${buildCostText(location)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Requires: ${location.requires.ifEmpty { listOf("None") }.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = { onBuild(location.name) }) { Text("Build") }
                }
            }
        }

        Text(text = "Crear equipo", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ChoiceChip("Por localización", !groupByType) { groupByType = false }
            ChoiceChip("Por tipo", groupByType) { groupByType = true }
        }

        state.craftRecipes
            .groupBy { if (groupByType) it.type.ifBlank { "Otros" } else it.locationName }
            .toList()
            .sortedBy { (group, _) -> group }
            .forEach { (group, recipes) ->
                Text(text = group, style = MaterialTheme.typography.titleSmall)
                recipes.forEach { view -> CraftRow(view = view, onCraft = onCraft) }
            }

        Button(onClick = onSpendEndeavor) { Text("Gastar endeavor (−1)") }
    }
}

@Composable
private fun CraftRow(
    view: CraftRecipeView,
    onCraft: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = view.recipe.gearName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (view.check.craftable) Craftable else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    costText(view.recipe) +
                        (view.check.missing?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = { onCraft(view.locationName, view.recipe.gearName) }, enabled = view.check.craftable) {
            Text("Crear")
        }
    }
}

private fun costText(recipe: GearRecipe): String =
    recipe.resourceCost.entries
        .sortedBy { it.key }
        .joinToString(", ") { (name, count) -> "$count $name" }

private fun buildCostText(location: io.kdmtracker.core.domain.SettlementLocation): String {
    val parts = mutableListOf<String>()
    if (location.buildEndeavors > 0) parts += "${location.buildEndeavors} endeavors"
    location.buildCost.entries
        .sortedBy { it.key }
        .forEach { (name, count) -> parts += "$count $name" }
    return if (parts.isEmpty()) "free" else parts.joinToString(", ")
}

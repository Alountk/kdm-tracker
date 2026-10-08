package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid

/**
 * Step 8 "Prepare Departing Survivors" body.
 *
 * Toggles up to four departing survivors and assigns the settlement's crafted gear to each. The
 * cap lives in `:core` (`MarkDeparting`), so an over-selection surfaces as an action error rather
 * than being silently dropped here. Stateless beyond the open editor.
 */
@Composable
fun PrepareDepartingStepScreen(
    state: UiState,
    onDepartingChange: (List<Ulid>) -> Unit,
    onAssignGear: (Ulid, String) -> Unit,
    onUnassignGear: (Ulid, String) -> Unit,
    onSaveSurvivor: (Survivor) -> Unit,
) {
    var editing by remember { mutableStateOf<Survivor?>(null) }
    val departing = state.survivors.filter { it.status == SurvivorStatus.DEPARTING }.map { it.id }
    val limit = state.population.coerceAtMost(4)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = state.stepTitle, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Pick up to $limit departing survivors (${departing.size} selected) and load their gear.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.survivors.filter { it.status != SurvivorStatus.DEAD }.forEach { survivor ->
            val isDeparting = survivor.id in departing
            SurvivorSummaryCard(survivor = survivor, onEdit = { editing = survivor }) {
                OutlinedButton(
                    onClick = {
                        onDepartingChange(if (isDeparting) departing - survivor.id else departing + survivor.id)
                    },
                ) {
                    Text(if (isDeparting) "Departing" else "Depart")
                }
            }
            if (isDeparting) {
                GearAssignment(
                    survivor = survivor,
                    settlementGear = state.gear,
                    onAssign = onAssignGear,
                    onUnassign = onUnassignGear,
                )
            }
        }
    }

    editing?.let { survivor ->
        SurvivorEditorDialog(
            survivor = survivor,
            survivalLimit = state.survivalLimit,
            onSave = {
                onSaveSurvivor(it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun GearAssignment(
    survivor: Survivor,
    settlementGear: List<String>,
    onAssign: (Ulid, String) -> Unit,
    onUnassign: (Ulid, String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = "Gear", style = MaterialTheme.typography.titleSmall)
            if (settlementGear.isEmpty()) {
                Text(text = "No crafted gear in the settlement.", style = MaterialTheme.typography.bodySmall)
            }
            survivor.gear.forEach { gear ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = gear, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { onUnassign(survivor.id, gear) }) { Text("Remove") }
                }
            }
            settlementGear.filter { it !in survivor.gear }.forEach { gear ->
                OutlinedButton(
                    onClick = { onAssign(survivor.id, gear) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Assign $gear")
                }
            }
        }
    }
}

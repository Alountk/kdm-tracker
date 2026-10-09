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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid

/**
 * Step 2 "Survivors Return" body: a bulk heal and the filterable roster of expandable cards.
 *
 * `Heal & clear tokens` resets the light/heavy injury boxes and clears tokens for every living
 * survivor; everything a survivor brought back is recorded through the sheet editor.
 */
@Composable
fun SurvivorsReturnStepScreen(
    state: UiState,
    onHeal: () -> Unit,
    onSaveSurvivor: (Survivor) -> Unit,
    onAssignGear: (Ulid, String) -> Unit = { _, _ -> },
    onUnassignGear: (Ulid, String) -> Unit = { _, _ -> },
) {
    var filter by remember { mutableStateOf(SurvivorFilter.ALL) }
    var expandedId by remember { mutableStateOf<Ulid?>(null) }
    var editing by remember { mutableStateOf<Survivor?>(null) }
    var pending by remember { mutableStateOf<Survivor?>(null) }

    // Close the editor only once the save actually landed; a refused save keeps it open.
    LaunchedEffect(state.survivors, pending) {
        val draft = pending
        if (draft != null && state.survivors.any { it.id == draft.id && it == draft }) {
            editing = null
            pending = null
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = state.stepTitle, style = MaterialTheme.typography.headlineMedium)
        state.actionError?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = onHeal, modifier = Modifier.fillMaxWidth()) {
            Text("Heal & clear tokens")
        }
        RosterFilterChips(selected = filter, onSelect = { filter = it })
        filterSurvivors(state.survivors, filter).forEach { survivor ->
            SurvivorCard(
                survivor = survivor,
                survivalLimit = state.survivalLimit,
                expanded = expandedId == survivor.id,
                onToggle = { expandedId = if (expandedId == survivor.id) null else survivor.id },
                onEdit = { editing = survivor },
                huntXpTrackMax = state.huntXpTrackMax,
                insanityThreshold = state.insanityThreshold,
                settlementGear = state.gear,
                onSave = onSaveSurvivor,
                onAssignGear = onAssignGear,
                onUnassignGear = onUnassignGear,
            )
        }
    }

    editing?.let { survivor ->
        SurvivorEditorDialog(
            survivor = survivor,
            survivalLimit = state.survivalLimit,
            error = state.actionError,
            onSave = {
                onSaveSurvivor(it)
                pending = it
            },
            onDismiss = {
                editing = null
                pending = null
            },
        )
    }
}

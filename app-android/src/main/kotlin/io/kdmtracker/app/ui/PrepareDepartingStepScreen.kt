package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid

/**
 * Step 8 "Prepare Departing Survivors" body.
 *
 * Toggles up to four departing survivors and, on an expanded departing card, the 3×3 gear grid that
 * assigns the settlement's crafted gear. The cap lives in `:core` (`MarkDeparting`), so an
 * over-selection surfaces as an action error. Dead survivors are shown greyed and cannot depart.
 */
@Composable
fun PrepareDepartingStepScreen(
    state: UiState,
    onDepartingChange: (List<Ulid>) -> Unit,
    onAssignGear: (Ulid, String) -> Unit,
    onUnassignGear: (Ulid, String) -> Unit,
    onSaveSurvivor: (Survivor) -> Unit,
) {
    var filter by remember { mutableStateOf(SurvivorFilter.ALL) }
    var expandedId by remember { mutableStateOf<Ulid?>(null) }
    var editing by remember { mutableStateOf<Survivor?>(null) }
    var pending by remember { mutableStateOf<Survivor?>(null) }
    val departing = state.survivors.filter { it.status == SurvivorStatus.DEPARTING }.map { it.id }
    val limit = state.population.coerceAtMost(4)

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
        Text(
            text = "Pick up to $limit departing survivors (${departing.size} selected) and load their gear.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RosterFilterChips(selected = filter, onSelect = { filter = it })
        filterSurvivors(state.survivors, filter).forEach { survivor ->
            val isDeparting = survivor.id in departing
            val isDead = survivor.status == SurvivorStatus.DEAD
            SurvivorCard(
                survivor = survivor,
                survivalLimit = state.survivalLimit,
                expanded = expandedId == survivor.id,
                onToggle = { expandedId = if (expandedId == survivor.id) null else survivor.id },
                onEdit = { editing = survivor },
                muted = isDead,
                trailing = {
                    if (!isDead) {
                        OutlinedButton(
                            onClick = {
                                onDepartingChange(if (isDeparting) departing - survivor.id else departing + survivor.id)
                            },
                        ) {
                            Text(if (isDeparting) "Departing" else "Depart")
                        }
                    }
                },
                expandedExtra = {
                    if (isDeparting) {
                        GearGrid(
                            assigned = survivor.gear,
                            settlementGear = state.gear,
                            onAssign = { onAssignGear(survivor.id, it) },
                            onUnassign = { onUnassignGear(survivor.id, it) },
                        )
                    }
                },
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

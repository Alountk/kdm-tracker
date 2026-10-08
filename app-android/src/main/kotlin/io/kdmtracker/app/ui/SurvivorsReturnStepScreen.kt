package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.Survivor

/**
 * Step 2 "Survivors Return" body: a bulk heal and the editable roster.
 *
 * `Heal & clear tokens` resets the light/heavy injury boxes and clears tokens for every living
 * survivor; everything a survivor brought back (permanent stat changes, new disorders, XP) is
 * recorded through the Survivor Editor. Stateless beyond the open editor.
 */
@Composable
fun SurvivorsReturnStepScreen(
    state: UiState,
    onHeal: () -> Unit,
    onSaveSurvivor: (Survivor) -> Unit,
) {
    var editing by remember { mutableStateOf<Survivor?>(null) }
    var pending by remember { mutableStateOf<Survivor?>(null) }

    // Close the editor only once the save actually landed (the record now holds the draft); a
    // refused save keeps the editor open with the draft and surfaces the action error.
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
        Text(
            text =
                "Healing resets light/heavy injuries and clears tokens. Survival, insanity and " +
                    "progression stay — edit them per survivor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.survivors.forEach { survivor ->
            SurvivorSummaryCard(survivor = survivor, onEdit = { editing = survivor })
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

/** A compact read-only summary of a survivor, with the entry into the editor. */
@Composable
internal fun SurvivorSummaryCard(
    survivor: Survivor,
    onEdit: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${survivor.name.ifBlank { "Unnamed" }} · ${survivor.sex.name} · ${survivor.status.name}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text =
                    "Survival ${survivor.survival} · Insanity ${survivor.insanity} · Hunt XP ${survivor.huntXp} · " +
                        "Courage ${survivor.courage} · Understanding ${survivor.understanding}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text =
                    "MOV ${survivor.attributes.movement} · ACC ${survivor.attributes.accuracy} · " +
                        "STR ${survivor.attributes.strength} · EVA ${survivor.attributes.evasion} · " +
                        "LUCK ${survivor.attributes.luck} · SPD ${survivor.attributes.speed}",
                style = MaterialTheme.typography.bodySmall,
            )
            val injured =
                HitLocation.entries.mapNotNull { location ->
                    val wound = survivor.wounds[location]
                    when {
                        wound == null -> null
                        wound.heavy -> "${location.name} heavy"
                        wound.light -> "${location.name} light"
                        else -> null
                    }
                }
            if (injured.isNotEmpty()) {
                Text(text = "Injuries: ${injured.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
            if (survivor.tokens.isNotEmpty()) {
                Text(text = "Tokens: ${survivor.tokens.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
            if (survivor.gear.isNotEmpty()) {
                Text(text = "Gear: ${survivor.gear.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                trailing()
                OutlinedButton(onClick = onEdit) { Text("Edit") }
            }
        }
    }
}

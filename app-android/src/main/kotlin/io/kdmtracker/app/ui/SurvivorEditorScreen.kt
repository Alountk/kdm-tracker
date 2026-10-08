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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.Sex
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.WoundState

/**
 * The Survivor Editor: edits the whole record sheet and reports the result through [onSave].
 *
 * Owns only the draft; every limit (disorders/fighting arts ≤ 3, non-negative counters) is enforced
 * by `:core`'s `UpdateSurvivor` on save, so a rejected edit surfaces as an action error and the
 * stored survivor is untouched.
 */
@Composable
fun SurvivorEditorDialog(
    survivor: Survivor,
    survivalLimit: Int,
    onSave: (Survivor) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(survivor) { mutableStateOf(survivor) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "Edit survivor", style = MaterialTheme.typography.headlineSmall)

                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(text = "Sex", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sex.values().forEach { sex ->
                        OutlinedButton(onClick = { draft = draft.copy(sex = sex) }) {
                            Text(if (sex == draft.sex) "• ${sex.name}" else sex.name)
                        }
                    }
                }

                ToggleRow("+1 survival when named applied", draft.nameBonusApplied) {
                    draft = draft.copy(nameBonusApplied = it)
                }
                ToggleRow("Retired", draft.retired) { draft = draft.copy(retired = it) }
                ToggleRow("Brain trauma", draft.brainTrauma) { draft = draft.copy(brainTrauma = it) }

                Stepper("Survival", draft.survival) { draft = draft.copy(survival = it) }
                Text(
                    text = "Survival limit $survivalLimit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Stepper("Insanity", draft.insanity) { draft = draft.copy(insanity = it) }

                Text(text = "Attributes", style = MaterialTheme.typography.titleSmall)
                Stepper("Movement", draft.attributes.movement) {
                    draft = draft.copy(attributes = draft.attributes.copy(movement = it))
                }
                Stepper("Accuracy", draft.attributes.accuracy) {
                    draft = draft.copy(attributes = draft.attributes.copy(accuracy = it))
                }
                Stepper("Strength", draft.attributes.strength) {
                    draft = draft.copy(attributes = draft.attributes.copy(strength = it))
                }
                Stepper("Evasion", draft.attributes.evasion) {
                    draft = draft.copy(attributes = draft.attributes.copy(evasion = it))
                }
                Stepper("Luck", draft.attributes.luck) {
                    draft = draft.copy(attributes = draft.attributes.copy(luck = it))
                }
                Stepper("Speed", draft.attributes.speed) {
                    draft = draft.copy(attributes = draft.attributes.copy(speed = it))
                }

                Text(text = "Armor & wounds", style = MaterialTheme.typography.titleSmall)
                HitLocation.entries.forEach { location ->
                    val wound = draft.wounds[location] ?: WoundState()
                    Text(text = location.name, style = MaterialTheme.typography.labelLarge)
                    Stepper("Armor", wound.armor) {
                        draft = draft.copy(wounds = draft.wounds + (location to wound.copy(armor = it)))
                    }
                    ToggleRow("Light", wound.light) {
                        draft = draft.copy(wounds = draft.wounds + (location to wound.copy(light = it)))
                    }
                    ToggleRow("Heavy", wound.heavy) {
                        draft = draft.copy(wounds = draft.wounds + (location to wound.copy(heavy = it)))
                    }
                }

                TextListEditor("Tokens", draft.tokens.toList(), max = 20) { tokens ->
                    draft = draft.copy(tokens = tokens.toSet())
                }

                Text(text = "Progression", style = MaterialTheme.typography.titleSmall)
                Stepper("Hunt XP", draft.huntXp) { draft = draft.copy(huntXp = it) }
                OutlinedTextField(
                    value = draft.weapon.type,
                    onValueChange = { draft = draft.copy(weapon = draft.weapon.copy(type = it)) },
                    label = { Text("Weapon type") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Stepper("Weapon XP", draft.weapon.xp) { draft = draft.copy(weapon = draft.weapon.copy(xp = it)) }
                ToggleRow("Specialist", draft.weapon.specialist) {
                    draft = draft.copy(weapon = draft.weapon.copy(specialist = it))
                }
                ToggleRow("Master", draft.weapon.master) { draft = draft.copy(weapon = draft.weapon.copy(master = it)) }
                Stepper("Courage", draft.courage) { draft = draft.copy(courage = it) }
                Stepper("Understanding", draft.understanding) { draft = draft.copy(understanding = it) }

                TextListEditor("Disorders", draft.disorders, max = 3) {
                    draft = draft.copy(disorders = it)
                }
                TextListEditor("Fighting arts", draft.fightingArts, max = 3) {
                    draft = draft.copy(fightingArts = it)
                }

                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { draft = draft.copy(notes = it) },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = { onSave(draft) }, modifier = Modifier.weight(1f)) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onChange(value - 1) }) { Text("−") }
        Text(text = value.toString(), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { onChange(value + 1) }) { Text("+") }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TextListEditor(
    label: String,
    values: List<String>,
    max: Int,
    onChange: (List<String>) -> Unit,
) {
    var entry by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = "$label (${values.size}/$max)", style = MaterialTheme.typography.titleSmall)
            values.forEach { value ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = value, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { onChange(values - value) }) { Text("Remove") }
                }
            }
            if (values.size < max) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = entry,
                        onValueChange = { entry = it },
                        label = { Text("Add") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            if (entry.isNotBlank()) {
                                onChange(values + entry.trim())
                                entry = ""
                            }
                        },
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

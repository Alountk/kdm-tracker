package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.ui.components.AttributeStat
import io.kdmtracker.app.ui.components.PipTrack
import io.kdmtracker.app.ui.components.SheetPanel
import io.kdmtracker.app.ui.components.StoneButton
import io.kdmtracker.app.ui.theme.KdmType
import io.kdmtracker.core.domain.Survivor

/**
 * The survivor record sheet, built from tonal panels on the Atramentum theme.
 *
 * Stateless: every control computes the next survivor through [SurvivorSheetEdits] and reports it
 * through [onSave]; the labels are the design's Spanish copy.
 *
 * [showHeader] lets the paged tab (which renders its own pager bar — prev button, name, gender
 * chip and `Equipo`, per the design's sub-header) suppress this sheet's built-in header, so the
 * two never stack. The expanded roster card keeps the default `true`.
 */
@Composable
fun SurvivorSheet(
    survivor: Survivor,
    survivalLimit: Int,
    huntXpTrackMax: Int,
    insanityThreshold: Int,
    settlementGear: List<String>,
    onSave: (Survivor) -> Unit,
    onAssignGear: (String) -> Unit,
    onUnassignGear: (String) -> Unit,
    onEditSheet: () -> Unit,
    showHeader: Boolean = true,
) {
    val model =
        SurvivorSheetModel.from(
            survivor = survivor,
            survivalLimit = survivalLimit,
            huntXpTrackMax = huntXpTrackMax,
            insanityThreshold = insanityThreshold,
        )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showHeader) {
            SurvivorHeader(model = model, onEditSheet = onEditSheet)
        }

        SheetPanel(title = "PE de Caza (Experiencia)") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${model.huntXp} / ${model.huntXpMax} PE",
                    style = KdmType.statDisplay,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { onSave(SurvivorSheetEdits.huntXp(survivor, model.huntXpMax, 1)) },
                    enabled = model.huntXp < model.huntXpMax,
                ) {
                    Text("+")
                }
            }
            PipTrack(filled = model.huntXp, total = model.huntXpMax)
        }

        SheetPanel(title = "Supervivencia", icon = "flare") {
            CounterRow(
                label = "Límite: ${model.survivalLimit}",
                value = model.survival,
                onMinus = { onSave(SurvivorSheetEdits.survival(survivor, model.survivalLimit, -1)) },
                onPlus = { onSave(SurvivorSheetEdits.survival(survivor, model.survivalLimit, 1)) },
                plusEnabled = model.canSpendSurvival,
                minusEnabled = model.survival > 0,
            )
            if (!model.canSpendSurvival) {
                Text(
                    text = "No puede gastar",
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            ChipRow(
                chips = model.tokenChips,
                onToggle = { onSave(SurvivorSheetEdits.toggleToken(survivor, it)) },
            )
        }

        SheetPanel(title = "Locura (Cerebro)", icon = "psychology") {
            CounterRow(
                label = "Estado Mental",
                value = model.insanity,
                onMinus = { onSave(SurvivorSheetEdits.insanity(survivor, -1)) },
                onPlus = { onSave(SurvivorSheetEdits.insanity(survivor, 1)) },
                badge = if (model.mentalState == MentalState.INSANE) "Loco" else "Cuerdo",
                minusEnabled = model.insanity > 0,
            )
            Text(
                text = "Si locura es 3+ estás loco.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SheetPanel(title = "Atributos Primarios") {
            Text(
                text = "Base + Modificadores",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                model.attributes.forEachIndexed { index, row ->
                    AttributeStat(
                        label = row.label,
                        value = row.value,
                        icon = ATTRIBUTE_ICONS.getOrElse(index) { FALLBACK_ICON },
                    )
                }
            }
        }

        SheetPanel(title = "Localizaciones de Impacto", icon = "security") {
            Text(
                text = "Armadura / Daño / Lesión",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            model.hitLocations.forEach { row ->
                HitLocationRow(
                    row = row,
                    onArmorMinus = { onSave(SurvivorSheetEdits.armor(survivor, row.location, -1)) },
                    onArmorPlus = { onSave(SurvivorSheetEdits.armor(survivor, row.location, 1)) },
                    onLight = { onSave(SurvivorSheetEdits.toggleLight(survivor, row.location)) },
                    onHeavy = { onSave(SurvivorSheetEdits.toggleHeavy(survivor, row.location)) },
                )
            }
        }

        SheetPanel(title = "Cuadrícula de Equipo (3x3)", icon = "grid_view") {
            GearGrid(
                assigned = survivor.gear,
                settlementGear = settlementGear,
                onAssign = onAssignGear,
                onUnassign = onUnassignGear,
            )
        }

        SheetPanel(title = "Arma Melé") {
            OutlinedButton(onClick = onEditSheet, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text =
                        survivor.weapon.type.ifBlank { "Desarmado" } +
                            " · ${survivor.weapon.xp} PE",
                )
            }
        }
    }
}

/** The six attribute icons, in the design's order (MOV, PREC, FZA, EVA, SUE, VEL). */
private const val FALLBACK_ICON = "crop_square"

private val ATTRIBUTE_ICONS =
    listOf("directions_walk", "target", "fitness_center", "airline_stops", "casino", "bolt")

@Composable
private fun SurvivorHeader(
    model: SurvivorSheetModel,
    onEditSheet: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .border(1.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = model.initial,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = model.name.ifBlank { "Sin nombre" },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = onEditSheet) { Text("Equipo") }
    }
}

@Composable
private fun CounterRow(
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    plusEnabled: Boolean = true,
    minusEnabled: Boolean = true,
    badge: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (badge != null) {
                Text(text = badge, style = MaterialTheme.typography.bodyMedium)
            }
        }
        OutlinedButton(onClick = onMinus, enabled = minusEnabled) { Text("−") }
        Text(
            text = value.toString(),
            style = KdmType.statDisplay,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp),
        )
        OutlinedButton(onClick = onPlus, enabled = plusEnabled) { Text("+") }
    }
}

@Composable
private fun HitLocationRow(
    row: HitLocationRowModel,
    onArmorMinus: () -> Unit,
    onArmorPlus: () -> Unit,
    onLight: () -> Unit,
    onHeavy: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        StoneButton(label = "−", onClick = onArmorMinus, enabled = row.armor > 0)
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = row.armor.toString(), style = MaterialTheme.typography.bodyMedium)
        }
        StoneButton(label = "+", onClick = onArmorPlus)
        StoneButton(label = "F", onClick = onLight, checked = row.light)
        StoneButton(label = "G", onClick = onHeavy, checked = row.heavy)
    }
}

@Composable
private fun ChipRow(
    chips: List<TokenChip>,
    onToggle: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        chips.forEach { chip ->
            OutlinedButton(onClick = { onToggle(chip.token) }) {
                Text(
                    text = chip.label,
                    style = KdmType.labelCaps,
                    fontWeight = if (chip.selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

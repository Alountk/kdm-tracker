package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType
import io.kdmtracker.core.domain.Survivor

/**
 * The survivor record sheet, built from tonal panels on the Atramentum theme.
 *
 * Stateless: every control computes the next survivor through [SurvivorSheetEdits] and reports it
 * through [onSave]; the labels are the design's Spanish copy.
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
) {
    val model =
        SurvivorSheetModel.from(
            survivor = survivor,
            survivalLimit = survivalLimit,
            huntXpTrackMax = huntXpTrackMax,
            insanityThreshold = insanityThreshold,
        )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SurvivorHeader(model = model, onEditSheet = onEditSheet)

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
private fun SheetPanel(
    title: String,
    icon: String? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Symbol(icon, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = title.uppercase(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
        content()
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

/**
 * The Hunt XP track: one cell per point, scrolled horizontally. Cells past [filled] stay unlit.
 */
@Composable
private fun PipTrack(
    filled: Int,
    total: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .background(
                            if (index < filled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (index < filled) {
                    Text(
                        text = (index + 1).toString(),
                        style = KdmType.labelCaps,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AttributeStat(
    label: String,
    value: Int,
    icon: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Symbol(icon, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value.toString(), style = KdmType.statDisplay)
        Text(
            text = label,
            style = KdmType.labelCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

/**
 * The colour of a stone box's label. Checked must stay legible on the panel background —
 * `onPrimary` (the obvious choice for a filled button) is nearly invisible there, so the checked
 * state uses the panel-safe [androidx.compose.material3.ColorScheme.secondary].
 */
internal fun woundBoxColor(
    checked: Boolean,
    scheme: androidx.compose.material3.ColorScheme,
): androidx.compose.ui.graphics.Color = if (checked) scheme.secondary else scheme.onSurfaceVariant

/**
 * A compact 28dp stone square — the design's "minimalist stone square with a centered numeral".
 *
 * It has a fixed size on purpose: a row of Material buttons squeezed the neighbouring label down to
 * one character per line, so every control in a crowded row is explicitly sized.
 */
@Composable
private fun StoneButton(
    label: String,
    onClick: () -> Unit,
    checked: Boolean = false,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .border(
                    1.dp,
                    if (checked) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    MaterialTheme.shapes.small,
                ).background(
                    if (checked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f) else Color.Transparent,
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = KdmType.labelCaps,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
            color =
                if (enabled) {
                    woundBoxColor(checked, MaterialTheme.colorScheme)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
        )
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

@Composable
private fun Symbol(
    name: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Text(
        text = KdmSymbols.glyph(name),
        fontFamily = KdmSymbols.family,
        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        color = color,
    )
}

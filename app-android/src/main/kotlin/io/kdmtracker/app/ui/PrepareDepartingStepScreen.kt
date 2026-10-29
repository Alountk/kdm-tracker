package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmType
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid

/** The step-8 body's static design copy, kept where `SettlementStepBodiesTest` can pin it. */
internal object PrepareDepartingStepCopy {
    const val QUARRY_LEVEL = "Nivel 1"
    const val QUARRY_KICKER = "Bestia Blanca"
    const val QUARRY_SUBTITLE = "Prólogo / Presa Estándar"

    /**
     * The quarry card's kicker, name and subtitle are **inert design constants**:
     * the assigned quarry is chosen at hunt setup and has no model before it, so deriving the name
     * from `state.quarries.firstOrNull()` was arbitrary — it rendered `Fénix` under the fixed
     * `Bestia Blanca` kicker. Only the disabled `Canteras ({n})` count still derives from state.
     */
    const val QUARRY_NAME = "León Blanco"
    const val QUARRY_STAT_LIFE_LABEL = "Vida / IA"
    const val QUARRY_STAT_TOUGH_LABEL = "Dureza"
    const val ARMOR_TITLE = "Ajuste de Armaduras y Equipo"
    const val ARMOR_PARAGRAPH =
        "Asegúrate de que todas las bonificaciones de equipo, fichas y armadura adquiridas están " +
            "reflejadas en las 5 localizaciones de impacto de cada hoja de superviviente."
    const val ARMOR_CHIP_WAIST = "Cintura +1 (Tela)"
    const val ARMOR_CHIP_SURVIVAL = "Puntos de Supervivencia"
    const val CAMPAIGN_RULE_LABEL = "Regla Oficial de Campaña"
    const val CAMPAIGN_RULE_QUOTE =
        "\"Sáltate el paso 9 (Enfrentamiento Especial) en la primera fase de asentamiento.\""
    const val CAMPAIGN_RULE_SOURCE = "Reglamento KDM pág. 35"

    /** The design tooltips on the two card buttons — reused as the picker's dialog copy. */
    const val SWAP_BUTTON = "Cambiar Superviviente"
    const val GEAR_BUTTON = "Editar Cuadrícula 3x3"
}

/** Which departing slot the open picker replaces, or `null` when it fills an empty slot. */
private data class PickerRequest(
    val slot: Int?,
)

/**
 * Step 8 "Supervivientes que Parten" body: the design's quarry card, the four-slot hunt party and
 * the inert armor/campaign-rule callouts.
 *
 * The section header row's title and icon belong to the frame, and the design's opening quote is
 * the catalog summary, so neither is rendered here. The `Canteras` re-selection button is
 * drawn **disabled** (quarry swap is not modeled at this step → backlog) and the quarry's
 * `Vida / IA` / `Dureza` boxes are inert design values ([QUARRY_LIFE_AI], [QUARRY_TOUGHNESS] — no
 * monster-stat model → backlog). The party is the design's own selection control: `sync_alt` opens
 * the swap picker over the alive survivors and reports the new full list through
 * [onDepartingChange] (`MarkDeparting` is the only path to `DEPARTING`, so this stays wired), and
 * `grid_view` expands the gear chips wired to [onAssignGear]/[onUnassignGear]. Empty `Sin asignar`
 * cards are the derived empty state so a fresh settlement can reach the designed `4/4`; the role
 * chips are omitted (role has no domain field). `RosterFilterChips`, the full roster and the
 * survivor editor are gone — survivor editing stays in the Supervivientes tab and
 * `state.actionError` is already surfaced by the cockpit. Dead survivors never appear.
 */
@Composable
fun PrepareDepartingStepScreen(
    state: UiState,
    onDepartingChange: (List<Ulid>) -> Unit,
    onAssignGear: (Ulid, String) -> Unit,
    onUnassignGear: (Ulid, String) -> Unit,
) {
    val departing = departingIds(state.survivors)
    val limit = departingLimit(state.population)
    val slots = departing.mapNotNull { id -> state.survivors.firstOrNull { it.id == id } }
    var picker by remember { mutableStateOf<PickerRequest?>(null) }
    var gearEditId by remember { mutableStateOf<Ulid?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuarrySection(quarryCount = state.quarries.size)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Symbol("groups", MaterialTheme.colorScheme.secondary, 14)
                    Text(
                        text = "Grupo de Caza ($limit Supervivientes)".uppercase(),
                        style = KdmType.labelCaps,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(
                    text = "${departing.size}/$limit Asignados",
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            slots.forEachIndexed { index, survivor ->
                DepartingCard(
                    survivor = survivor,
                    gearExpanded = gearEditId == survivor.id,
                    onSwap = { picker = PickerRequest(slot = index) },
                    onToggleGear = { gearEditId = if (gearEditId == survivor.id) null else survivor.id },
                    settlementGear = state.gear,
                    onAssignGear = onAssignGear,
                    onUnassignGear = onUnassignGear,
                )
            }
            repeat(emptySlotCount(limit, departing.size)) {
                EmptySlotCard(onSwap = { picker = PickerRequest(slot = null) })
            }
        }

        ArmorCallout()
        CampaignRuleBox()
    }

    picker?.let { request ->
        SwapPickerDialog(
            candidates = swapPickerCandidates(state.survivors),
            onPick = { candidate ->
                onDepartingChange(
                    request.slot?.let { slot -> swapDepartingSlot(departing, slot, candidate.id) }
                        ?: addToDeparting(departing, candidate.id),
                )
                picker = null
            },
            onDismiss = { picker = null },
        )
    }
}

/**
 * One departing survivor's design card: avatar, name, the survival/insanity line, the two card
 * buttons and the gear strip, plus the inline gear-chip editor behind `grid_view`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepartingCard(
    survivor: Survivor,
    gearExpanded: Boolean,
    onSwap: () -> Unit,
    onToggleGear: () -> Unit,
    settlementGear: List<String>,
    onAssignGear: (Ulid, String) -> Unit,
    onUnassignGear: (Ulid, String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Symbol("person", MaterialTheme.colorScheme.secondary, 20, fill = true)
                }
                Column {
                    Text(
                        text = survivor.name.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text =
                            buildAnnotatedString {
                                append("Supervivencia: ")
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                ) {
                                    append(survivor.survival.toString())
                                }
                                append(" • ")
                                append("Locura: ")
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                ) {
                                    append(survivor.insanity.toString())
                                }
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardIconButton(
                    name = "sync_alt",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = onSwap,
                )
                CardIconButton(
                    name = "grid_view",
                    tint = MaterialTheme.colorScheme.secondary,
                    container = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    onClick = onToggleGear,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                    .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Equipo Portado:".uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = gearGridLabel(survivor.gear),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                survivor.gear.forEach { item ->
                    GearStripChip(item = item)
                }
            }
        }

        if (gearExpanded) {
            GearChipEditor(
                survivor = survivor,
                settlementGear = settlementGear,
                onAssignGear = onAssignGear,
                onUnassignGear = onUnassignGear,
            )
        }
    }
}

/** The derived empty state: one `Sin asignar` card per unfilled slot, adding through the picker. */
@Composable
private fun EmptySlotCard(onSwap: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Symbol("person", MaterialTheme.colorScheme.onSurfaceVariant, 20, fill = true)
            }
            Text(
                text = "Sin asignar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CardIconButton(
            name = "sync_alt",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            onClick = onSwap,
        )
    }
}

/** The design's square card button (`sync_alt` / `grid_view`), drawn enabled. */
@Composable
private fun CardIconButton(
    name: String,
    tint: Color,
    container: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .background(container, MaterialTheme.shapes.small)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Symbol(name, tint, 16)
    }
}

/** The swap picker: the design's own selection control, over the alive non-departing survivors. */
@Composable
private fun SwapPickerDialog(
    candidates: List<Survivor>,
    onPick: (Survivor) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(PrepareDepartingStepCopy.SWAP_BUTTON, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                candidates.forEach { candidate ->
                    TextButton(
                        onClick = { onPick(candidate) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = candidate.name.ifBlank { "Sin nombre" },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmType
import io.kdmtracker.core.domain.ResourceId

/** The step-10 body's static design copy, kept where `SettlementStepBodiesTest` can pin it. */
internal object ArchiveStepCopy {
    const val META_CHIP = "Cúmulo"
    const val GEAR_HEADER = "Equipo Almacenado"
    const val ADD_ITEM = "Añadir Ítem"
}

/**
 * Step 10 "Anotar y Almacenar Recursos" body: the design's resource card and stored-gear block.
 *
 * The card starts at the 2-column grid — its design paragraph is the catalog summary, so it
 * is not repeated here. Each of the four basic resources ([resourceGridRows]) shows its count from
 * [io.kdmtracker.app.UiState.storageLines] and `remove`/`add` wired to the summary's adjust
 * callback; `remove` is inert at zero ([ResourceGridRow.canDecrement], the summary's clamp
 * semantics). `Añadir Ítem` is drawn disabled (no item-add model → backlog) and each distinct
 * stored item renders as one chip with its count ([gearChips]). The read-only `Storage` list
 * is gone; storage lines beyond the four basics have no designed surface here (backlog). The frame
 * owns the step title and the opening paragraph.
 */
@Composable
fun ArchiveStepScreen(
    state: UiState,
    onAdjust: (ResourceId, Int) -> Unit,
) {
    val rows = resourceGridRows(definitions = state.resourceDefinitions, storage = state.storageLines)
    val chips = gearChips(state.gear)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Meta row: the design's header-row chip (the numbered box and title belong to the frame).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = ArchiveStepCopy.META_CHIP.uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // Resource card: the 2-column grid of the four basic resources.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                    .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rows.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    pair.forEach { row ->
                        ResourceCell(row = row, onAdjust = onAdjust, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Stored gear block: header, the disabled add action and the distinct-item chips.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Symbol("shield", MaterialTheme.colorScheme.onSurfaceVariant, 14)
                    Text(
                        text = ArchiveStepCopy.GEAR_HEADER.uppercase(),
                        style = KdmType.labelCaps,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Disabled: no item-add model exists yet (backlog).
                TextButton(onClick = {}, enabled = false) {
                    Symbol("add", MaterialTheme.colorScheme.secondary, 12)
                    Spacer(Modifier.width(4.dp))
                    Text(text = ArchiveStepCopy.ADD_ITEM.uppercase(), style = KdmType.labelCaps)
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                chips.forEach { chip -> GearCountChip(chip) }
            }
        }
    }
}

/**
 * One basic-resource cell: icon box + labels on top, the `remove`/add stepper below.
 *
 * Compact so the labels never ellipsize at 360 dp width: the body is 328 dp wide there (page
 * padding 16 dp), the card padding 10 dp and the grid gap 6 dp leave each cell 151 dp — 135 dp
 * inside its 8/6 dp padding. The label column takes the rest as `weight(1f)` (≈ 103 dp after the
 * 26 dp icon box and its 6 dp gap) against ≈ 62 dp for the longest label (`CHATARRA` in
 * [KdmType.labelCaps]) — so `maxLines = 1` with [TextOverflow.Visible] draws the full word instead
 * of an ellipsis. The stepper cannot share that row (icon + label + two ≥ 36 dp buttons exceed the
 * 135 dp), so it wraps to its own line, right-aligned like the design; its drawn buttons are
 * shrunk to 22 dp inside 36 dp touch targets.
 */
@Composable
private fun ResourceCell(
    row: ResourceGridRow,
    onAdjust: (ResourceId, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(26.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Symbol(row.glyph, row.tone.accent(), 15)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.label,
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                )
                Text(
                    text = row.subLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Inert at zero: the ledger refuses an overdraw and the summary's stepper does the same.
            AdjustIcon(name = "remove", enabled = row.canDecrement, onClick = { onAdjust(row.id, -1) })
            Text(
                text = row.count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.width(40.dp),
            )
            AdjustIcon(name = "add", enabled = true, onClick = { onAdjust(row.id, 1) })
        }
    }
}

/**
 * The design's `remove`/`add` button — `enabled = false` dims it, exactly like the summary's. The
 * drawn box is shrunk to 22 dp so the cell stays compact; the clickable region keeps the 36 dp
 * touch target floor.
 */
@Composable
private fun AdjustIcon(
    name: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(36.dp).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                    .alpha(if (enabled) 1f else 0.35f),
            contentAlignment = Alignment.Center,
        ) {
            Symbol(name, MaterialTheme.colorScheme.onSurfaceVariant, 12)
        }
    }
}

/** One stored-gear chip: the distinct item and how many the settlement holds (no per-item icon). */
@Composable
private fun GearCountChip(chip: GearChip) {
    Row(
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = chip.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "×${chip.count}",
            style = KdmType.labelCaps.copy(lineHeight = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The design's icon-box tone per resource: accent for the named basics, dim for the rare one. */
@Composable
private fun ResourceGridTone.accent(): Color =
    when (this) {
        ResourceGridTone.PRIMARY -> MaterialTheme.colorScheme.primary
        ResourceGridTone.SECONDARY -> MaterialTheme.colorScheme.secondary
        ResourceGridTone.DIM -> MaterialTheme.colorScheme.onSurfaceVariant
    }

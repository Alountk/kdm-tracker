package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmSymbols

/**
 * The design's "Supervivientes" tab: one survivor's paged sheet, no roster list.
 *
 * Stateless in its data — it renders [state] and reports survivor edits through the callbacks.
 * The pager index is the only local state: `rememberSaveable`, pure UI, reset when the tab
 * recomposes onto a fresh [UiState]. The design ships a single prev button that cycles through
 * the roster (`.superpowers/design/ficha/ficha.html`), so this mirrors that: prev advances one
 * step with wrap-around, and the sheet's own header is suppressed in favour of the pager bar.
 */
@Composable
fun SurvivorsScreen(
    state: UiState,
    onSave: (io.kdmtracker.core.domain.Survivor) -> Unit,
    onAssignGear: (io.kdmtracker.core.domain.Ulid, String) -> Unit,
    onUnassignGear: (io.kdmtracker.core.domain.Ulid, String) -> Unit,
) {
    var index by rememberSaveable { mutableStateOf(0) }
    var editing by rememberSaveable { mutableStateOf(false) }

    if (isEmptyRoster(state.survivors)) {
        EmptyRosterState()
        return
    }

    // pagerFrame clamps first, so a roster that shrank between frames never strands the index.
    val survivor = pagerFrame(state.survivors, index).survivor ?: state.survivors.first()
    val safeIndex = state.survivors.indexOf(survivor)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SurvivorPagerBar(
            survivor = survivor,
            onPrev = { index = advance(safeIndex, state.survivors.size) },
            onEditSheet = { editing = true },
        )

        SurvivorSheet(
            survivor = survivor,
            survivalLimit = state.survivalLimit,
            huntXpTrackMax = state.huntXpTrackMax,
            insanityThreshold = state.insanityThreshold,
            settlementGear = state.gear,
            onSave = onSave,
            onAssignGear = { onAssignGear(survivor.id, it) },
            onUnassignGear = { onUnassignGear(survivor.id, it) },
            onEditSheet = { editing = true },
            showHeader = false,
        )
    }

    if (editing) {
        SurvivorEditorDialog(
            survivor = survivor,
            survivalLimit = state.survivalLimit,
            error = state.actionError,
            onSave = {
                onSave(it)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

/**
 * The design's sub-header: a `surface-container-low` bar carrying the single prev button, the
 * name + gender chip column, and the `Equipo` button on the right.
 */
@Composable
private fun SurvivorPagerBar(
    survivor: io.kdmtracker.core.domain.Survivor,
    onPrev: () -> Unit,
    onEditSheet: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // The design's single prev button (aria-label "Anterior superviviente").
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                        .clickable(onClick = onPrev),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = KdmSymbols.glyph("chevron_left"),
                    fontFamily = KdmSymbols.family,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = survivor.name.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    GenderChip(sex = survivor.sex)
                }
            }
        }

        // The design's right-side Equipo button.
        Row(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                    .clickable(onClick = onEditSheet)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = KdmSymbols.glyph("grid_view"),
                fontFamily = KdmSymbols.family,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = "Equipo",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

/** The design's gender chip beside the name: `H` / `M` (and `O` for other). */
@Composable
private fun GenderChip(sex: io.kdmtracker.core.domain.Sex) {
    Box(
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.extraSmall)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = genderLabel(sex),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/** The empty state the maintainer chose: a simple centred message, no action. */
@Composable
private fun EmptyRosterState() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "Aún no hay supervivientes.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.settlement.common.PanelTitle
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.core.rules.InnovationStatus

/** The kind of cell in the design's 2x2 slot grid. */
enum class SlotKind { FILLED, EMPTY, ACTIVE_DECK }

/**
 * One cell of the design's 2x2 slot grid: a chosen principle, a slot still to discover, or the
 * location feeding the draw. Every field is presentational — the grid is layout, not rules.
 */
data class SlotCardData(
    val kind: SlotKind,
    val state: SlotState = SlotState.EMPTY,
    val label: String,
    val icon: String,
    val iconColor: Color,
    val iconSize: TextUnit,
    val title: String,
    val note: String?,
    val footer: String,
    val footerCentered: Boolean,
    val centerIcon: String? = null,
)

/**
 * The Develop step's Innovaciones panel: the outer gate (access, then resources), the milestone
 * grid, the draw action and the offer, resolving to the consumed state once the choice is made.
 */
@Composable
fun InnovationsPanel(
    model: InnovationsModel,
    onConfirm: (String) -> Unit,
) {
    var catalogueOpen by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<String?>(null) }
    var resolved by remember { mutableStateOf<Pair<String, String>?>(null) }
    var drawn by remember { mutableStateOf(false) }
    var acquisition by remember { mutableStateOf(Acquisition.ALL) }
    var category by remember { mutableStateOf<String?>(null) }
    val shape = MaterialTheme.shapes.small

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.3f), shape)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PanelTitle(icon = "auto_stories", title = "Innovaciones", trailing = "Mazo: ${model.deckSize} cartas")

        if (catalogueOpen) {
            // The catalogue replaces the panel body: the design's estados B/C are a separate view
            // over the panel, reached only through the entrance row (spec decision 9).
            CatalogueView(
                model = model,
                acquisition = acquisition,
                onAcquisition = { acquisition = it },
                category = category,
                onCategory = { category = it },
                onClose = { catalogueOpen = false },
            )
        } else {
            when {
                // Consumed wins over everything: after the draw the pool is empty, and empty must not
                // masquerade as "no resources" while the resolution of the draw is still owed.
                model.phaseState == PhaseState.USED_THIS_PHASE ->
                    ConsumedBlock(
                        model = model,
                        resolution = resolved,
                        onOpenCatalogue = { catalogueOpen = true },
                    )
                model.gate == InnovarGate.NO_ACCESS -> NoAccessBlock()
                model.gate == InnovarGate.SHORT_ON_RESOURCES -> CostBlock(cost = model.cost)
                model.phaseState == PhaseState.NO_CARDS ->
                    NoCardsBlock(model = model, onOpenCatalogue = { catalogueOpen = true })
                else ->
                    ReadyBody(
                        model = model,
                        picked = picked,
                        onPick = { picked = it },
                        drawn = drawn,
                        onDraw = { drawn = true },
                        onOpenCatalogue = { catalogueOpen = true },
                        onConfirm = { chosen ->
                            // Capture the resolution before the commit changes the model's hand.
                            resolved = chosen to (model.hand.firstOrNull { it.name != chosen }?.name ?: "")
                            picked = null
                            onConfirm(chosen)
                        },
                    )
            }
        }
    }
}

/** The design's 2x2 grid: up to three principle slots plus the deck slot that feeds the draw. */
@Composable
internal fun slotCards(model: InnovationsModel): List<SlotCardData> {
    val positions = listOf("Principio", "Ranura 2", "Ranura 3")
    val cards = mutableListOf<SlotCardData>()
    val filled = model.slots.take(positions.size)

    filled.forEachIndexed { index, slot ->
        cards +=
            SlotCardData(
                kind = SlotKind.FILLED,
                state = slot.state,
                label = positions[index],
                icon = "verified",
                iconColor =
                    if (slot.state ==
                        SlotState.NEW_THIS_YEAR
                    ) {
                        InnovarPalette.green
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                iconSize = 14.sp,
                title = slot.name,
                note = null,
                footer = if (slot.state == SlotState.NEW_THIS_YEAR) "Nueva · Desbloqueado" else "Activo Permanente",
                footerCentered = false,
            )
    }
    repeat(positions.size - filled.size) { index ->
        cards +=
            SlotCardData(
                kind = SlotKind.EMPTY,
                state = SlotState.EMPTY,
                label = positions.getOrElse(filled.size + index) { "Ranura ${filled.size + index + 1}" },
                icon = "crop_square",
                iconColor = MaterialTheme.colorScheme.outline,
                iconSize = 14.sp,
                title = if (filled.isEmpty() && index == 0) "Por Descubrir" else "Futuro",
                note = null,
                footer = if (filled.isEmpty() && index == 0) "Sin asignar" else "Vacío",
                footerCentered = true,
                centerIcon = if (filled.isEmpty() && index == 0) "style" else "hourglass_bottom",
            )
    }
    model.activeDeck?.let { deck ->
        cards +=
            SlotCardData(
                kind = SlotKind.ACTIVE_DECK,
                state = SlotState.OWNED,
                label = "Mazo Activo",
                icon = "flare",
                iconColor = MaterialTheme.colorScheme.secondary,
                iconSize = 16.sp,
                title = deck.name,
                note = deck.note,
                footer = "${model.catalogue.count { it.status == InnovationStatus.AVAILABLE }} disponibles",
                footerCentered = false,
            )
    }
    return cards
}

package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType

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
 * The Develop step's Innovations panel. `Innovar` is drawn and disabled: the design's flow draws
 * several cards and keeps one, and that flow is not modelled — the Develop body below is where an
 * innovation is actually unlocked.
 */
@Composable
fun InnovationsPanel(
    model: InnovationsModel,
    onConfirm: (String) -> Unit,
) {
    var catalogueOpen by remember { mutableStateOf(true) }
    var picked by remember { mutableStateOf<String?>(null) }
    var resolved by remember { mutableStateOf<Pair<String, String>?>(null) }
    var drawn by remember { mutableStateOf(false) }
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

        when {
            // Consumed wins over everything: after the draw the pool is empty, and empty must not
            // masquerade as "no resources" while the resolution of the draw is still owed.
            model.phaseState == PhaseState.USED_THIS_PHASE ->
                ConsumedBlock(
                    model = model,
                    resolution = resolved,
                    onToggleCatalogue = { catalogueOpen = !catalogueOpen },
                )
            model.gate == InnovarGate.NO_ACCESS -> NoAccessBlock()
            model.gate == InnovarGate.SHORT_ON_RESOURCES -> CostBlock(cost = model.cost)
            model.phaseState == PhaseState.NO_CARDS ->
                NoCardsBlock(model = model, onToggleCatalogue = { catalogueOpen = !catalogueOpen })
            else ->
                ReadyBody(
                    model = model,
                    picked = picked,
                    onPick = { picked = it },
                    drawn = drawn,
                    onDraw = { drawn = true },
                    catalogueOpen = catalogueOpen,
                    onToggleCatalogue = { catalogueOpen = !catalogueOpen },
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

/** Everything below the gate once the settlement can both reach Innovar and pay for it. */
@Composable
private fun ReadyBody(
    model: InnovationsModel,
    picked: String?,
    onPick: (String) -> Unit,
    drawn: Boolean,
    onDraw: () -> Unit,
    catalogueOpen: Boolean,
    onToggleCatalogue: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val shape = MaterialTheme.shapes.small

    slotCards(model, catalogueOpen).chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.forEach { card ->
                SlotCard(card = card, modifier = Modifier.weight(1f), onToggle = onToggleCatalogue)
            }
        }
    }

    if (!drawn) {
        // The design's cost row: no wrapper around the row itself, full width — the label
        // sits left and the minified price is boxed on the right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "COSTE", style = microLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${model.costChip.endeavors}*",
                    style = KdmType.labelCaps,
                    color = InnovarPalette.gold,
                )
                Text(
                    text = "| ${model.costChip.abbreviated}",
                    style = microLabel,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        KdmActionButton(
            text = "Robar cartas para innovar",
            onClick = onDraw,
            icon = "style",
            variant = KdmActionVariant.GOLD,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // The offer, exactly as the HTML draws it: its own container, its own gold border.
    if (drawn) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OfferHeader(model = model)

            if (drawn) {
                model.hand.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { card ->
                            InnovationCardView(
                                card = card,
                                phaseState = model.phaseState,
                                selected = picked == card.name,
                                onPick = onPick,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            if (drawn) {
                picked?.let { chosen ->
                    KdmActionButton(
                        text = "ACEPTAR ELECCIÓN",
                        onClick = { onConfirm(chosen) },
                        icon = "check_circle",
                        variant = KdmActionVariant.GOLD,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** The design's `ESTADO 2 - CONSUMADA`: the resolution of the draw and the year's gold note. */
@Composable
private fun ConsumedBlock(
    model: InnovationsModel,
    resolution: Pair<String, String>?,
    onToggleCatalogue: () -> Unit,
) {
    val shape = MaterialTheme.shapes.small

    slotCards(model, true).chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.forEach { card ->
                SlotCard(card = card, modifier = Modifier.weight(1f), onToggle = onToggleCatalogue)
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.3f), shape)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Symbol("sync_alt", InnovarPalette.green, 16.sp)
            Text(
                text = "Resolución del robo de cartas",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Completado",
                style = microLabel,
                color = InnovarPalette.green,
                modifier =
                    Modifier
                        .background(InnovarPalette.greenWash, shape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        if (resolution != null) {
            Text(
                text = "${resolution.first} · ${resolution.second} devuelta al fondo del mazo de innovación",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = "1 CARTA DEVUELTA", style = microLabel, color = MaterialTheme.colorScheme.outline)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(InnovarPalette.goldWash, shape)
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), shape)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Symbol("check_circle", MaterialTheme.colorScheme.secondary, 16.sp)
            Text(
                text = "Innovación Realizada en este Año Linterna",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text =
                "El esfuerzo gastado y los 3 recursos (1 Hueso, 1 Órgano, 1 Pellejo) " +
                    "ya fueron deducidos del almacén.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The design's `ESTADO 6 - SIN CARTAS EN EL MAZO`: the grid is unchanged — an empty deck says
 * nothing about what the settlement owns — and only the part below it differs.
 */
@Composable
private fun NoCardsBlock(
    model: InnovationsModel,
    onToggleCatalogue: () -> Unit,
) {
    val shape = MaterialTheme.shapes.small

    slotCards(model, true).chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.forEach { card ->
                SlotCard(card = card, modifier = Modifier.weight(1f), onToggle = onToggleCatalogue)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The board draws `swap_horizontal_circle`; it is not in the bundled subset.
        Symbol("swap_horizontal_circle", MaterialTheme.colorScheme.outline, 28.sp)
        Text(
            text = "No quedan innovaciones en el mazo",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text =
                "No hay cartas con acceso directo en el mazo de robo actual: todas las " +
                    "consecuencias requieren innovaciones previas que todavía no se han desbloqueado.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                    .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.5f), shape)
                    .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Symbol("lightbulb", InnovarPalette.gold, 16.sp)
                Text(
                    text = "¿Cómo desbloquear nuevas cartas?",
                    style = KdmType.labelCaps,
                    color = InnovarPalette.gold,
                )
            }
            listOf(
                "Construye localizaciones que añadan consecuencias al mazo.",
                "Supera hitos de población para desbloquear nuevos principios.",
                "Usa eventos de historia para introducir cartas raras.",
            ).forEach { line ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "•", style = microLabel, color = InnovarPalette.gold.copy(alpha = 0.7f))
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        KdmActionButton(
            text = "Innovar no disponible",
            onClick = {},
            icon = "lock",
            variant = KdmActionVariant.BLOCKED,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The design's `ESTADO 1 - SIN ACCESO`: the only thing on screen when Innovar cannot run at all. */
@Composable
private fun NoAccessBlock() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Symbol("lock", KdmPalette.outline, 28.sp)
        Text(
            text = "Acción de Innovar no disponible",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text =
                "El asentamiento aún no ha construido el Cúmulo de Linternas " +
                    "o la fase actual prohíbe el desarrollo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                    .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Innovar no disponible",
                style = microLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

/** The design's 2x2 grid: up to three principle slots plus the deck slot that feeds the draw. */
@Composable
private fun slotCards(
    model: InnovationsModel,
    catalogueOpen: Boolean,
): List<SlotCardData> {
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
                footer = if (catalogueOpen) "Desplegado" else "Replegado",
                footerCentered = false,
            )
    }
    return cards
}

@Composable
private fun PanelTitle(
    icon: String,
    title: String,
    trailing: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Symbol(icon, MaterialTheme.colorScheme.secondary, 18.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = trailing.uppercase(),
            style = KdmType.labelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.shapes.small)
                    .border(1.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun SlotCard(
    card: SlotCardData,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit = {},
) {
    val shape = MaterialTheme.shapes.small
    val filled = card.kind == SlotKind.FILLED
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
                .background(
                    if (filled) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else if (card.kind == SlotKind.ACTIVE_DECK) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape,
                ).border(
                    1.dp,
                    when {
                        card.kind == SlotKind.ACTIVE_DECK -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                        card.state == SlotState.OWNED -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        card.state == SlotState.NEW_THIS_YEAR -> InnovarPalette.green.copy(alpha = 0.6f)
                        else -> KdmPalette.outlineVariant.copy(alpha = 0.3f)
                    },
                    shape,
                ).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            if (card.kind == SlotKind.ACTIVE_DECK) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = card.label,
                        style = KdmType.labelCaps,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape),
                    )
                }
            } else {
                Text(
                    text = card.label,
                    style = KdmType.labelCaps,
                    color =
                        when {
                            card.state == SlotState.NEW_THIS_YEAR -> InnovarPalette.green
                            card.state == SlotState.OWNED -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            Symbol(card.icon, card.iconColor, card.iconSize)
        }

        when {
            filled || card.kind == SlotKind.ACTIVE_DECK ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = card.title,
                        style = slotTitle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    card.note?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (card.kind == SlotKind.ACTIVE_DECK) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            else ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Symbol(card.centerIcon ?: card.icon, MaterialTheme.colorScheme.outline, 22.sp)
                    Text(
                        text = card.title,
                        style = KdmType.labelCaps,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
        }

        if (card.kind == SlotKind.ACTIVE_DECK) {
            KdmActionButton(
                text = card.footer,
                onClick = onToggle,
                icon = "visibility",
                variant = KdmActionVariant.GOLD,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = card.footer,
                style = microLabel,
                color =
                    when (card.state) {
                        SlotState.OWNED -> MaterialTheme.colorScheme.secondary
                        SlotState.NEW_THIS_YEAR -> InnovarPalette.green
                        SlotState.EMPTY -> MaterialTheme.colorScheme.outline
                    },
                modifier =
                    if (card.footerCentered) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier
                            .background(
                                when (card.state) {
                                    SlotState.OWNED -> InnovarPalette.goldWash
                                    SlotState.NEW_THIS_YEAR -> InnovarPalette.greenWash
                                    SlotState.EMPTY -> Color.Transparent
                                },
                                MaterialTheme.shapes.small,
                            ).padding(horizontal = 4.dp, vertical = 2.dp)
                    },
                textAlign = if (card.footerCentered) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun Symbol(
    name: String,
    color: Color,
    size: androidx.compose.ui.unit.TextUnit,
) {
    Text(
        text = KdmSymbols.glyph(name),
        // Google's hosted Material Symbols CSS pins `line-height: 1`; without it the symbol
        // font's own ~1.7em line box pads every row that carries an icon (see Phase 1).
        style =
            androidx.compose.ui.text.TextStyle(
                fontFamily = KdmSymbols.family,
                fontSize = size,
                lineHeight = size,
            ),
        color = color,
    )
}

/** The offer's header: what it costs today and the rule behind it. */
@Composable
private fun OfferHeader(model: InnovationsModel) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Symbol("style", MaterialTheme.colorScheme.secondary, 16.sp)
                Text(
                    text = model.offerTitle.uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = model.ruleLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * State 5, exactly as the board draws it: a red panel with a `FALTAN RECURSOS` pill, four resource
 * boxes each showing `have / need` and `FALTA`/`OK`, and the `Motivo:` sentence underneath.
 */
@Composable
private fun CostBlock(cost: CostView) {
    val shape = MaterialTheme.shapes.small
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(InnovarPalette.redBg, shape)
                .border(1.dp, InnovarPalette.red.copy(alpha = 0.5f), shape)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // The board draws `error`; the bundled subset has no such glyph and the font is not
            // re-subsettable here, so the nearest mapped one stands in. See docs/backlog.md.
            Symbol("error", InnovarPalette.red, 16.sp)
            Text(
                text = "Coste de Innovación Insuficiente",
                style = MaterialTheme.typography.bodySmall,
                color = InnovarPalette.red,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Faltan Recursos",
                style = microLabel,
                color = InnovarPalette.red,
                modifier =
                    Modifier
                        .background(InnovarPalette.redBg, shape)
                        .border(1.dp, InnovarPalette.red.copy(alpha = 0.4f), shape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (listOf(cost.endeavors) + cost.resources).forEach { need ->
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                            .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = need.name,
                        style = microLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${need.have} / ${need.need}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (need.ok) "OK" else "FALTA",
                        style = microLabel,
                        color = if (need.ok) InnovarPalette.green else InnovarPalette.red,
                    )
                }
            }
        }

        Text(
            text = "Motivo: ${cost.reason}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CostLine(
    label: String,
    ok: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = if (ok) "OK" else "FALTA",
            style = KdmType.labelCaps,
            color = if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        )
    }
}

/** The slot grid's title: `font-headline-sm text-[14px] font-bold` — Playfair, not Inter. */
private val slotTitle =
    TextStyle(
        fontFamily = KdmFontFamilies.playfair,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
    )

/** The slot grid's footers: `font-label-caps text-[9px]` with wide tracking. */
private val microLabel =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1f.em,
    )

/** A 1dp hairline across the top of the box — the mock's `border-t`. */
private fun Modifier.topHairline(color: Color): Modifier =
    drawBehind {
        drawLine(
            color = color,
            start = Offset(0f, 0.5f),
            end = Offset(size.width, 0.5f),
            strokeWidth = 1.dp.toPx(),
        )
    }

/** A 1dp hairline across the bottom of the box — the mock's `border-b`. */
private fun Modifier.bottomHairline(color: Color): Modifier =
    drawBehind {
        drawLine(
            color = color,
            start = Offset(0f, size.height - 0.5f),
            end = Offset(size.width, size.height - 0.5f),
            strokeWidth = 1.dp.toPx(),
        )
    }

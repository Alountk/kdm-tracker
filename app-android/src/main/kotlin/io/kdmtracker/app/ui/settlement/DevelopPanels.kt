package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
    onSelect: (String) -> Unit,
) {
    var catalogueOpen by remember { mutableStateOf(true) }
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

        when (model.gate) {
            InnovarGate.NO_ACCESS -> NoAccessBlock()
            InnovarGate.SHORT_ON_RESOURCES -> CostBlock(cost = model.cost)
            InnovarGate.READY ->
                ReadyBody(
                    model = model,
                    onSelect = onSelect,
                    catalogueOpen = catalogueOpen,
                    onToggleCatalogue = { catalogueOpen = !catalogueOpen },
                )
        }
    }
}

/** Everything below the gate once the settlement can both reach Innovar and pay for it. */
@Composable
private fun ReadyBody(
    model: InnovationsModel,
    onSelect: (String) -> Unit,
    catalogueOpen: Boolean,
    onToggleCatalogue: () -> Unit,
) {
    val shape = MaterialTheme.shapes.small

    slotCards(model, catalogueOpen).chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.forEach { card ->
                SlotCard(
                    card = card,
                    modifier = Modifier.weight(1f),
                    onToggle = onToggleCatalogue,
                )
            }
        }
    }

    // The offer, exactly as the HTML draws it: its own container, its own gold border.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), shape)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OfferHeader(model = model)

        model.hand.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { card ->
                    InnovationCardView(
                        card = card,
                        phaseState = model.phaseState,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .topHairline(KdmPalette.outlineVariant.copy(alpha = 0.2f))
                    .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DeckRow(deckSize = model.deckSize, open = catalogueOpen, onToggle = onToggleCatalogue)
            ChipRow(chips = model.acquisitionChips)
            ChipRow(chips = model.categoryChips)
        }
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

    model.principleSlots.forEachIndexed { index, chosen ->
        cards +=
            SlotCardData(
                kind = SlotKind.FILLED,
                label = positions[index],
                icon = "verified",
                iconColor = MaterialTheme.colorScheme.outline,
                iconSize = 14.sp,
                title = chosen,
                note = null,
                footer = "Activo Permanente",
                footerCentered = false,
            )
    }
    repeat(model.emptySlots) { index ->
        val position = model.principleSlots.size + index
        cards +=
            SlotCardData(
                kind = SlotKind.EMPTY,
                label = positions.getOrElse(position) { "Ranura ${position + 1}" },
                icon = "crop_square",
                iconColor = MaterialTheme.colorScheme.outline,
                iconSize = 14.sp,
                title = if (index == 0) "Por Descubrir" else "Futuro",
                note = null,
                footer = if (index == 0) "Sin asignar" else "Vacío",
                footerCentered = true,
                centerIcon = if (index == 0) "style" else "hourglass_bottom",
            )
    }
    model.activeDeck?.let { deck ->
        cards +=
            SlotCardData(
                kind = SlotKind.ACTIVE_DECK,
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
                    if (card.kind == SlotKind.ACTIVE_DECK) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    } else {
                        KdmPalette.outlineVariant.copy(alpha = 0.3f)
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
                        if (filled) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.outline,
                modifier = if (card.footerCentered) Modifier.fillMaxWidth() else Modifier,
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

/** `Mazo de Innovación (N Descubribles)` with the toggle that shows or hides the chip groups. */
@Composable
private fun DeckRow(
    deckSize: Int,
    open: Boolean,
    onToggle: () -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.2f), shape)
                .clickable(onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Symbol("grid_view", MaterialTheme.colorScheme.outline, 15.sp)
            Text(
                text = "Mazo de Innovación ($deckSize Descubribles)".uppercase(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "Catálogo", style = microLabel, color = MaterialTheme.colorScheme.outline)
            Symbol(
                name = if (open) "arrow_drop_up" else "arrow_drop_down",
                color = MaterialTheme.colorScheme.outline,
                size = 14.sp,
            )
        }
    }
}

/**
 * One chip group. The reference draws these as counts over the deck, not as controls over the
 * hand: the hand only ever holds not-yet-acquired cards, so an acquisition filter would only
 * empty it, and neither the reference nor this model carries a catalogue list to narrow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(chips: List<Pair<String, Int>>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        chips.forEach { (label, count) ->
            Text(
                text = "$label ($count)",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** The offer's header: what it costs today and the rule behind it. */
@Composable
private fun OfferHeader(model: InnovationsModel) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .bottomHairline(KdmPalette.outlineVariant.copy(alpha = 0.2f))
                .padding(bottom = 8.dp),
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
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text(
                text = model.ruleLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(text = "COSTE", style = microLabel, color = MaterialTheme.colorScheme.outline)
            Text(
                text =
                    "-${model.cost.endeavors.need} Esfuerzo · " +
                        "-${model.cost.resources.size} Recursos",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** State 3: the action is spent for this phase and says so, with the reference's reason. */
@Composable
private fun UsedThisPhaseBanner(year: Int) {
    val shape = MaterialTheme.shapes.small
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.3f), shape)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Symbol("lock", MaterialTheme.colorScheme.onSurface, 16.sp)
            Text(
                text = "Acción de Innovar Agotada",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(text = "1 DE 1 USADO", style = KdmType.labelCaps, color = MaterialTheme.colorScheme.secondary)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "YA UTILIZADO (AÑO $year)",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            shape,
                        ).padding(horizontal = 4.dp, vertical = 2.dp),
            )
            Text(text = "BLOQUEO TEMPORAL", style = KdmType.labelCaps, color = MaterialTheme.colorScheme.outline)
        }
        Text(
            text =
                "Motivo visible: La acción de desarrollo de Innovación solo puede ejecutarse una vez " +
                    "por cada fase de asentamiento. Se reactivará de forma automática al alcanzar " +
                    "el Paso 7 del siguiente Año Linterna.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            Symbol("warning", InnovarPalette.red, 16.sp)
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

/** State 4: nothing is offered, and the reference says what to do about it. */
@Composable
private fun EmptyDeckState() {
    val shape = MaterialTheme.shapes.small
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.3f), shape)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "No Quedan Innovaciones en el Mazo",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text =
                "No hay cartas con acceso directo en el mazo de robo actual o todas las consecuencias " +
                    "requieren innovaciones previas aún no dominadas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "¿Cómo desbloquear nuevas cartas?",
            style = KdmType.labelCaps,
            color = MaterialTheme.colorScheme.secondary,
        )
        listOf(
            "Construye localizaciones que añadan consecuencias (ej. Taller de Huesos, Armería).",
            "Supera hitos de población para agregar nuevos principios de sociedad.",
            "Usa eventos de historia para introducir cartas raras en el mazo.",
        ).forEach { bullet ->
            Text(
                text = "•  $bullet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
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

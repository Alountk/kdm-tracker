package io.kdmtracker.app.ui.settlement

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.core.rules.InnovationStatus

/*
 * The catalogue browser's UI (the design's `Innovaciones: Catálogo — entrada y consulta`, cached at
 * `.superpowers/design/innovar-flow/catalogo.html`): the single entrance row (estado A), the open
 * browser with its two chip groups (estado B), and the filtered state with the count bar
 * (estado C).
 *
 * The entrance row is the *only* affordance — the deck slot's old toggle button became a static
 * count line. Filters live in the panel's `remember` state, never in [InnovationsModel]: the model
 * is rebuilt on every recomposition and cannot hold interaction state.
 */

/** The design's entrance row (estado A): one row that opens the catalogue as a separate view. */
@Composable
fun CatalogueEntrance(
    deckSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                    .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.6f), shape)
                    .clickable(onClick = onClick)
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(InnovarPalette.goldPillBg, MaterialTheme.shapes.small)
                            .border(1.dp, InnovarPalette.gold.copy(alpha = 0.4f), MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Symbol("manage_search", InnovarPalette.gold, 19.sp)
                }
                Column {
                    Text(
                        text = "Mazo de Innovación",
                        style = entranceTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "($deckSize Descubribles)",
                        style = entranceSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Catálogo",
                    style = microLabel,
                    color = InnovarPalette.gold,
                    modifier =
                        Modifier
                            .background(InnovarPalette.goldPillBg, RoundedCornerShape(4.dp))
                            .border(1.dp, InnovarPalette.gold.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Symbol("unfold_more", MaterialTheme.colorScheme.onSurfaceVariant, 18.sp)
            }
        }
        Text(
            text = "Abre el catálogo completo para ver lo que ya posees y lo que puedes descubrir.",
            style =
                TextStyle(
                    fontFamily = KdmFontFamilies.inter,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontStyle = FontStyle.Italic,
                ),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
    }
}

/**
 * The open catalogue (estados B and C) replacing the panel body: breadcrumb, the expanded deck
 * row, the two chip groups — acquisition first, categories second (spec decision 4) — the count
 * bar while a filter is active, and the two-column card grid.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogueView(
    model: InnovationsModel,
    acquisition: Acquisition,
    onAcquisition: (Acquisition) -> Unit,
    category: String?,
    onCategory: (String?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val browser = InnovationCatalog.browser(model.catalogue, acquisition, category)
    val summaryHighlighted = browser.countBar != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Breadcrumb back affordance.
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClose).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol("chevron_left", InnovarPalette.gold, 16.sp)
            Text(
                text = "Volver a Innovaciones",
                style = MaterialTheme.typography.bodyMedium,
                color = InnovarPalette.gold,
            )
        }

        // The deck row, expanded: same box as the entrance, gold-bordered with the collapse icon.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                    .border(1.dp, InnovarPalette.gold.copy(alpha = 0.6f), shape)
                    .clickable(onClick = onClose)
                    .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(InnovarPalette.goldPillBg, MaterialTheme.shapes.small)
                            .border(1.dp, InnovarPalette.gold, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Symbol("manage_search", InnovarPalette.gold, 19.sp)
                }
                Column {
                    Text(
                        text = "Mazo de Innovación",
                        style = entranceTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = browser.subtitle,
                        style = entranceSubtitle,
                        fontWeight = FontWeight.SemiBold,
                        color = InnovarPalette.gold,
                    )
                }
            }
            Symbol("expand_less", InnovarPalette.gold, 20.sp)
        }

        // Chip group 1: the acquisition chips, always before the categories.
        val acquisitionGroup = browser.groups[0]
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape)
                    .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.6f), shape)
                    .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = acquisitionGroup.title,
                    style = groupTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = browser.summary,
                    style = microLabel,
                    color =
                        if (summaryHighlighted) {
                            InnovarPalette.gold
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                acquisitionGroup.chips.forEach { chip ->
                    val candidate = Acquisition.entries.firstOrNull { it.label == chip.label }
                    AcquisitionChip(
                        chip = chip,
                        onClick = { candidate?.let(onAcquisition) },
                    )
                }
            }
        }

        // Chip group 2: one chip per distinct category, over the acquisition filter's result.
        val categoryGroup = browser.groups[1]
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape)
                    .border(1.dp, KdmPalette.outlineVariant.copy(alpha = 0.6f), shape)
                    .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = categoryGroup.title, style = groupTitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                categoryGroup.chips.forEach { chip ->
                    CategoryChip(
                        chip = chip,
                        onClick = { onCategory(if (category == chip.label) null else chip.label) },
                    )
                }
            }
        }

        browser.countBar?.let { bar -> CountBar(text = bar) }

        // The card grid, bounded and scrollable: 42 cards do not fit a phone, and the design's
        // estado C scrolls at exactly this height.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 305.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            browser.cards.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { card ->
                        CatalogueCardView(card = card, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** The acquisition chip: solid gold while selected (the design's estado B/C active state). */
@Composable
private fun AcquisitionChip(
    chip: CatalogChip,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = "${chip.label} (${chip.count})",
        style =
            TextStyle(
                fontFamily = KdmFontFamilies.inter,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = if (chip.selected) FontWeight.Bold else FontWeight.Medium,
            ),
        color =
            if (chip.selected) {
                KdmPalette.canvas
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        modifier =
            Modifier
                .clip(shape)
                .background(
                    if (chip.selected) {
                        InnovarPalette.gold
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    shape,
                ).border(
                    1.dp,
                    if (chip.selected) {
                        InnovarPalette.gold
                    } else {
                        KdmPalette.outlineVariant.copy(alpha = 0.6f)
                    },
                    shape,
                ).clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** The category chip: gold wash while selected, smaller than the acquisition chips. */
@Composable
private fun CategoryChip(
    chip: CatalogChip,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = "${chip.label} (${chip.count})",
        style =
            TextStyle(
                fontFamily = KdmFontFamilies.inter,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = if (chip.selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        color =
            if (chip.selected) {
                InnovarPalette.gold
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        modifier =
            Modifier
                .clip(shape)
                .background(
                    if (chip.selected) {
                        InnovarPalette.goldPillBg
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    shape,
                ).border(
                    1.dp,
                    if (chip.selected) {
                        InnovarPalette.gold.copy(alpha = 0.4f)
                    } else {
                        KdmPalette.outlineVariant.copy(alpha = 0.6f)
                    },
                    shape,
                ).clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** The estado C count bar: how many cards the filters show, with the scroll hint. */
@Composable
private fun CountBar(text: String) {
    val shape = RoundedCornerShape(8.dp)
    val transition = rememberInfiniteTransition(label = "countBar")
    val dotAlpha by
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "dotAlpha",
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(InnovarPalette.goldPillBg, shape)
                .border(1.dp, InnovarPalette.gold.copy(alpha = 0.4f), shape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .background(InnovarPalette.gold.copy(alpha = dotAlpha), CircleShape),
            )
            Text(
                text = text,
                style =
                    TextStyle(
                        fontFamily = KdmFontFamilies.inter,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = InnovarPalette.gold,
            )
        }
        Symbol("swap_vert", InnovarPalette.gold, 14.sp)
    }
}

/**
 * One catalogue card preview (the design's fixed-height card): category chip, name, two-line
 * effect, and the status footer — gold `ADQUIRIDA`, green `DISPONIBLE`, or grey `SIN ACCESO` with
 * the parent it still requires.
 */
@Composable
private fun CatalogueCardView(
    card: CatalogCard,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val accent =
        when (card.status) {
            InnovationStatus.UNLOCKED -> InnovarPalette.gold
            InnovationStatus.AVAILABLE -> InnovarPalette.green
            InnovationStatus.NO_ACCESS -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val container =
        if (card.status == InnovationStatus.NO_ACCESS) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }

    Column(
        modifier =
            modifier
                .height(142.dp)
                .clip(shape)
                .background(container, shape)
                .border(
                    1.dp,
                    when (card.status) {
                        InnovationStatus.UNLOCKED -> InnovarPalette.gold.copy(alpha = 0.6f)
                        InnovationStatus.AVAILABLE -> InnovarPalette.green.copy(alpha = 0.5f)
                        InnovationStatus.NO_ACCESS -> KdmPalette.outlineVariant
                    },
                    shape,
                ).padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = card.category.uppercase(),
                style = microLabel,
                color =
                    if (card.status == InnovationStatus.NO_ACCESS) {
                        InnovarPalette.gold.copy(alpha = 0.7f)
                    } else {
                        InnovarPalette.gold
                    },
                modifier =
                    Modifier
                        .background(
                            if (card.status == InnovationStatus.NO_ACCESS) {
                                InnovarPalette.goldPillBg.copy(alpha = 0.5f)
                            } else {
                                InnovarPalette.goldPillBg
                            },
                            MaterialTheme.shapes.small,
                        ).padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Text(
                text = card.name,
                style = cardTitle,
                color =
                    if (card.status == InnovationStatus.NO_ACCESS) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.effect,
                style =
                    TextStyle(
                        fontFamily = KdmFontFamilies.inter,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                    ),
                color =
                    if (card.status == InnovationStatus.NO_ACCESS) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(modifier = Modifier.topHairline(KdmPalette.outlineVariant.copy(alpha = 0.5f)).padding(top = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = card.footer, style = microLabel, color = accent)
                Symbol(footerIcon(card.status), accent, 12.sp)
            }
            if (card.requires != null) {
                Text(
                    text = "Requiere: ${card.requires}",
                    style =
                        TextStyle(
                            fontFamily = KdmFontFamilies.inter,
                            fontSize = 8.sp,
                            lineHeight = 11.sp,
                        ),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun footerIcon(status: InnovationStatus): String =
    when (status) {
        InnovationStatus.UNLOCKED -> "check_circle"
        InnovationStatus.AVAILABLE -> "lock_open"
        InnovationStatus.NO_ACCESS -> "lock"
    }

/** The single-glyph text the panel composables draw with — file-private, as in the sibling files. */
@Composable
private fun Symbol(
    name: String,
    color: Color,
    size: TextUnit,
) {
    Text(
        text = KdmSymbols.glyph(name),
        // Google's hosted Material Symbols CSS pins `line-height: 1`; without it the symbol
        // font's own ~1.7em line box pads every row that carries an icon (see Phase 1).
        style =
            TextStyle(
                fontFamily = KdmSymbols.family,
                fontSize = size,
                lineHeight = size,
            ),
        color = color,
    )
}

/** The entrance row's title: 14px medium Inter. */
private val entranceTitle =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 14.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
    )

/** The entrance row's subtitle / deck-row subtitle: 11px Inter. */
private val entranceSubtitle =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )

/** The chip groups' section titles: `text-[10px] uppercase font-bold tracking-wider`. */
private val groupTitle =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.05f.em,
    )

/** The chip label: `font-label-caps text-[9px]` with wide tracking. */
private val microLabel =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1f.em,
    )

/** The catalogue card's title: `font-headline-sm text-[13px] font-bold` — Playfair, not Inter. */
private val cardTitle =
    TextStyle(
        fontFamily = KdmFontFamilies.playfair,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
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

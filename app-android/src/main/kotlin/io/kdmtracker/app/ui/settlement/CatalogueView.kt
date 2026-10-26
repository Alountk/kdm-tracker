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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.settlement.common.Symbol
import io.kdmtracker.app.ui.settlement.common.microLabel
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette

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

/** The chip groups' section titles: `text-[10px] uppercase font-bold tracking-wider`. */
private val groupTitle =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.05f.em,
    )

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

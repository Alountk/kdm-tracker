package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The step-9 body's verbatim design copy (`pasos_9_11_pretty.html`): the year-1 exemption card is
 * static Spanish the render cannot derive from state, so it lives here where
 * `SettlementStepBodiesTest` can pin it against the design source. The inner block's bold term is
 * split out of the sentence because the design highlights it.
 */
internal object SpecialShowdownStepCopy {
    const val META_CHIP = "Nemesis / Evento"
    const val LABEL = "Regla de Año Linterna 1"
    const val HEADLINE = "Sáltate el Paso 9"
    const val CHIP = "Automático"

    const val PARAGRAPH =
        "Durante la primera fase de asentamiento, los supervivientes aún no conocen las bestias " +
            "némesis. Este paso queda ignorado por orden del códice."

    const val INNER_LABEL = "Efecto en Años Posteriores"
    const val INNER_PREFIX = "Tras resolver un combate especial, regresa de inmediato al "
    const val INNER_HIGHLIGHT = "Paso 5"
    const val INNER_SUFFIX =
        ". Cura a estos combatientes y retira fichas temporales. No computan como supervivientes " +
            "que regresan."
}

/**
 * Step 9 "Enfrentamiento Especial" body: the design's year-1 exemption card.
 *
 * The card renders only during the first settlement phase ([showYearOneExemption],
 * `lanternYear <= 2` — the first phase at step 9 always runs under year 2); the designs give
 * no later-year content, so past the first settlement phase the body is the meta row alone.
 * Everything on
 * the card is inert copy (the year rule is unbacked). The `Special Showdown this year`
 * switch and the whole `EncounterSetup`/`EncounterPanel` branch are removed — `SetSpecialShowdown`
 * and friends are unreachable from this screen (backlog). The frame owns the step title and the
 * opening paragraph.
 */
@Composable
fun SpecialShowdownStepScreen(state: UiState) {
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
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            MaterialTheme.shapes.small,
                        ).padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = SpecialShowdownStepCopy.META_CHIP.uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (showYearOneExemption(state.lanternYear)) {
            YearOneExemptionCard()
        }
    }
}

/** The design's `Sáltate el Paso 9` card: left error bar, rule header and the later-years note. */
@Composable
private fun YearOneExemptionCard() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(
            modifier =
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error),
        )
        Column(
            modifier = Modifier.weight(1f).padding(start = 8.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Symbol("verified_user", MaterialTheme.colorScheme.error, 18)
                        Text(
                            text = SpecialShowdownStepCopy.LABEL.uppercase(),
                            style = KdmType.labelCaps,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = SpecialShowdownStepCopy.HEADLINE,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = SpecialShowdownStepCopy.CHIP.uppercase(),
                        style = KdmType.labelCaps.copy(fontSize = 9.sp, lineHeight = 12.sp),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Text(
                text = SpecialShowdownStepCopy.PARAGRAPH,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                        .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Symbol("menu_book", MaterialTheme.colorScheme.secondary, 15)
                    Text(
                        text = SpecialShowdownStepCopy.INNER_LABEL.uppercase(),
                        style = KdmType.labelCaps.copy(fontSize = 10.sp, lineHeight = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text =
                        buildAnnotatedString {
                            append(SpecialShowdownStepCopy.INNER_PREFIX)
                            withStyle(
                                SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            ) {
                                append(SpecialShowdownStepCopy.INNER_HIGHLIGHT)
                            }
                            append(SpecialShowdownStepCopy.INNER_SUFFIX)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

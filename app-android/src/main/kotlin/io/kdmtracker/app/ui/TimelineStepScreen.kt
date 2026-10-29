package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The step-4 body's verbatim design copy (`pasos_3_6_pretty.html`): the story event block and its
 * three checked rules checkpoints are year-1 inert copy the render cannot derive from state, so
 * they live here where `SettlementStepBodiesTest` can pin them against the design source. The
 * checklist's bold halves are split out of the rows because the design highlights them.
 */
internal object TimelineStepCopy {
    const val CHECKED_LABEL = "Marcado"
    const val STORY_LABEL = "Evento de Historia Activo"
    const val STORY_TITLE = "Primer Día (First Day)"
    const val STORY_SUBTITLE = "Los supervivientes contemplan el brillo y descubren la palabra hablada."
    const val STORY_CHIP = "Obligatorio"
    const val NEXT_YEAR = "Próximo año: Aparición de Carroñeros"

    const val CHECK_INNOVATION_PREFIX = "Añadir innovación: "
    const val CHECK_INNOVATION_HIGHLIGHT = "Lenguaje"
    const val CHECK_LOCATION_PREFIX = "Añadir localización: "
    const val CHECK_LOCATION_HIGHLIGHT = "Cúmulo de Linternas"
    const val CHECK_DECK_ROW = "Crear mazo de Innovaciones del Asentamiento"
}

/**
 * Step 4 "Línea de Tiempo" body: the design's timeline card.
 *
 * Only the two year numbers derive from state ([io.kdmtracker.app.UiState.lanternYear] and its
 * successor); the story event block and the checklist are the design's inert year-1 copy, drawn
 * checked and not toggleable. The settlement-event deck UI is gone — the `Draw a settlement
 * event` button, the recorded-event card, the deck list and the `This Year` section are removed
 * (their usecases stay in `:core`, unreachable from here → backlog). The frame owns the step title
 * and the opening paragraph.
 */
@Composable
fun TimelineStepScreen(state: UiState) {
    val year = state.lanternYear
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Meta row: the design's header-row year pill.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Año $year",
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Timeline card: the marked year, its story event and the locked next year.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(20.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = year.toString(),
                            style = KdmType.labelCaps,
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                    Text(
                        text = "Año Linterna $year",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Symbol("check_box", MaterialTheme.colorScheme.secondary, 16, fill = true)
                    Text(
                        text = TimelineStepCopy.CHECKED_LABEL.uppercase(),
                        style = KdmType.labelCaps,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            StoryEventBlock()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (year + 1).toString(),
                            style = KdmType.labelCaps.copy(fontSize = 9.sp, lineHeight = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = TimelineStepCopy.NEXT_YEAR,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Symbol("lock", MaterialTheme.colorScheme.onSurfaceVariant, 16)
            }
        }
    }
}

/** The design's `Primer Día` story-event block with its three checked, non-clickable checkpoints. */
@Composable
private fun StoryEventBlock() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol("menu_book", MaterialTheme.colorScheme.secondary, 16)
            Text(
                text = TimelineStepCopy.STORY_LABEL.uppercase(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = TimelineStepCopy.STORY_TITLE,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = TimelineStepCopy.STORY_SUBTITLE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier =
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            MaterialTheme.shapes.small,
                        ).padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = TimelineStepCopy.STORY_CHIP.uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        HighlightedCheckRow(
            prefix = TimelineStepCopy.CHECK_INNOVATION_PREFIX,
            highlight = TimelineStepCopy.CHECK_INNOVATION_HIGHLIGHT,
        )
        HighlightedCheckRow(
            prefix = TimelineStepCopy.CHECK_LOCATION_PREFIX,
            highlight = TimelineStepCopy.CHECK_LOCATION_HIGHLIGHT,
        )
        CheckedRow(text = TimelineStepCopy.CHECK_DECK_ROW)
    }
}

/** A checked rules checkpoint whose tail is highlighted (the design's bold term). */
@Composable
private fun HighlightedCheckRow(
    prefix: String,
    highlight: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Symbol("check_box", MaterialTheme.colorScheme.secondary, 16, fill = true)
        Text(
            text =
                buildAnnotatedString {
                    append(prefix)
                    withStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    ) {
                        append(highlight)
                    }
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A checked rules checkpoint with no highlighted term. */
@Composable
private fun CheckedRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Symbol("check_box", MaterialTheme.colorScheme.secondary, 16, fill = true)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

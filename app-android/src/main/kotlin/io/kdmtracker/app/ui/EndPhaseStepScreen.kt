package io.kdmtracker.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.R
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The step-11 body's verbatim design copy (`pasos_9_11_pretty.html`): the closing card's overlay
 * and checklist are static Spanish the render cannot derive from state, so they live here where
 * `SettlementStepBodiesTest` can pin them against the design source. The campaign note's bold term
 * is split out of the sentence because the design highlights it.
 */
internal object EndPhaseStepCopy {
    const val META_CHIP = "Cierre"
    const val OVERLAY = "Fin de la Fase de Asentamiento"

    /** The design's three checked checkpoints, verbatim, as title to subtitle. */
    val CHECKLIST: List<Pair<String, String>> =
        listOf(
            "Limpieza de mesa completada" to
                "Cartas de innovación recogidas y recursos no gastados asegurados.",
            "Hojas de registro actualizadas" to
                "Equipo adquirido registrado en las fichas de los supervivientes que parten.",
            "Grupo de Caza Designado" to
                "4 supervivientes listos con sus cuadrículas de equipo ajustadas.",
        )

    const val NOTE_PREFIX = "Los jugadores han concluido la "
    const val NOTE_HIGHLIGHT = "Primera Historia"
    const val NOTE_SUFFIX = ". La campaña avanza hacia las tinieblas de la estepa de rostros."
}

/**
 * Step 11 "El Fin del Principio" body: the design's closing card.
 *
 * The packaged banner photo carries the overlay row; the three checklists are drawn checked and
 * non-clickable (records, not controls → backlog) and the campaign note is inert year-1 copy. The
 * unspent-endeavors discard warning and the `Summary` card are removed — the count stays
 * visible in the footer's `Restaurar Esfuerzos (N)`, population and deaths in the top bar. Nothing
 * on this body derives from state: the design's closing is year-1 inert copy. The frame owns the
 * step title and the opening paragraph.
 */
@Composable
fun EndPhaseStepScreen(state: UiState) {
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
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            MaterialTheme.shapes.small,
                        ).padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = EndPhaseStepCopy.META_CHIP.uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ClosingBanner()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EndPhaseStepCopy.CHECKLIST.forEach { (title, subtitle) ->
                    ChecklistRow(title = title, subtitle = subtitle)
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
                        .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Symbol("bookmark", MaterialTheme.colorScheme.secondary, 20)
                Text(
                    text =
                        buildAnnotatedString {
                            append(EndPhaseStepCopy.NOTE_PREFIX)
                            withStyle(
                                SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            ) {
                                append(EndPhaseStepCopy.NOTE_HIGHLIGHT)
                            }
                            append(EndPhaseStepCopy.NOTE_SUFFIX)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The packaged banner photo with the design's scrim and its bottom overlay row. */
@Composable
private fun ClosingBanner() {
    val surface = MaterialTheme.colorScheme.surfaceContainer
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(128.dp)
                .clip(MaterialTheme.shapes.medium),
    ) {
        Image(
            painter = painterResource(R.drawable.settlement_step11_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.6f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to surface.copy(alpha = 0.4f),
                            1f to surface,
                        ),
                    ),
        )
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = EndPhaseStepCopy.OVERLAY,
                style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.primary,
            )
            Symbol("hotel_class", MaterialTheme.colorScheme.secondary, 20)
        }
    }
}

/**
 * One rules checkpoint, drawn checked and inert: the design's checkboxes record what the campaign
 * already did, so they are not toggleable (step-11 checklists → backlog).
 */
@Composable
private fun ChecklistRow(
    title: String,
    subtitle: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.padding(top = 2.dp)) {
            Symbol("check_box", MaterialTheme.colorScheme.secondary, 16, fill = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

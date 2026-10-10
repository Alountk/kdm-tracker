package io.kdmtracker.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType

/** One chip of the phase carousel: the official step, its icon and the design's short label. */
data class CarouselStep(
    val number: Int,
    val icon: String,
    val shortLabel: String,
    val isSpecial: Boolean,
)

/**
 * The design's "Secuencia de Fase Oficial" strip: the eleven settlement steps in order, with the
 * Special Showdown step flagged so it can be drawn in crimson.
 */
object PhaseCarouselModel {
    /** The Special Showdown step, drawn in crimson rather than the active bone. */
    const val SPECIAL_STEP = 9

    /** The full strip, in the rulebook's order. */
    val steps: List<CarouselStep> =
        listOf(
            CarouselStep(1, "group_add", "Prep.", isSpecial = false),
            CarouselStep(2, "account_balance", "Regreso", isSpecial = false),
            CarouselStep(3, "emergency", "Esfuerzo", isSpecial = false),
            CarouselStep(4, "hourglass_empty", "Tiempo", isSpecial = false),
            CarouselStep(5, "skull", "Muertes", isSpecial = false),
            CarouselStep(6, "check_box_outline_blank", "Hitos", isSpecial = false),
            CarouselStep(7, "handyman", "Desarrollo", isSpecial = false),
            CarouselStep(8, "shield", "Partida", isSpecial = false),
            CarouselStep(9, "swords", "Especial", isSpecial = true),
            CarouselStep(10, "edit_note", "Almacén", isSpecial = false),
            CarouselStep(11, "flare", "Fin", isSpecial = false),
        )

    /** The step the strip should highlight, clamped so an out-of-range record never breaks it. */
    fun active(number: Int): Int = number.coerceIn(1, steps.size)
}

/** The Special Showdown's crimson, the only chip that is not bone or `surfaceContainer`. */
private val SpecialCrimson = Color(0xFF8E1214)

private val ChipWidth = 64.dp
private val ChipGap = 8.dp

/**
 * The phase carousel. Tapping a chip reports its step through [onSelectStep]; the active chip is
 * centred when [currentStep] changes.
 */
@Composable
fun PhaseCarousel(
    currentStep: Int,
    onSelectStep: (Int) -> Unit,
) {
    val active = PhaseCarouselModel.active(currentStep)
    val scrollState = rememberScrollState()
    val pitch = with(LocalDensity.current) { (ChipWidth + ChipGap).toPx() }

    LaunchedEffect(active) {
        scrollState.animateScrollTo(((active - 1) * pitch).toInt())
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Secuencia de Fase Oficial".uppercase(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Paso $active de ${PhaseCarouselModel.steps.size}",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(ChipGap),
        ) {
            PhaseCarouselModel.steps.forEach { step ->
                Chip(
                    step = step,
                    selected = step.number == active,
                    onClick = { onSelectStep(step.number) },
                )
            }
        }
    }
}

@Composable
private fun Chip(
    step: CarouselStep,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background =
        when {
            selected -> MaterialTheme.colorScheme.primary
            step.isSpecial -> SpecialCrimson
            else -> MaterialTheme.colorScheme.surfaceContainer
        }
    val content =
        when {
            selected -> MaterialTheme.colorScheme.onPrimary
            step.isSpecial -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Column(
        modifier =
            Modifier
                .width(ChipWidth)
                .background(background, MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
                .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = KdmSymbols.glyph(step.icon),
            fontFamily = KdmSymbols.family,
            fontSize = 18.sp,
            color = content,
        )
        Text(
            text = step.number.toString(),
            style = KdmType.labelCaps,
            color = content,
        )
        Text(
            text = step.shortLabel.uppercase(),
            style = KdmType.labelCaps,
            fontSize = 9.sp,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

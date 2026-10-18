package io.kdmtracker.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.components.woundBoxColor
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmScheme
import io.kdmtracker.app.ui.theme.KdmShapes
import io.kdmtracker.app.ui.theme.KdmType
import io.kdmtracker.app.ui.theme.KdmTypography
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the Atramentum Lantern tokens to the values the Stitch design renders. Every role, type step
 * and shape the design defines is enumerated, so a future edit that drifts fails here instead of
 * shipping.
 */
class KdmThemeTest {
    @Test
    fun everyColourRoleCarriesTheRenderedToken() {
        val expected =
            mapOf(
                "background" to Color(0xFF131315),
                "onBackground" to Color(0xFFE5E1E4),
                "surface" to Color(0xFF131315),
                "onSurface" to Color(0xFFE5E1E4),
                "surfaceVariant" to Color(0xFF353437),
                "onSurfaceVariant" to Color(0xFFC8C7BE),
                "surfaceDim" to Color(0xFF131315),
                "surfaceBright" to Color(0xFF39393B),
                "surfaceTint" to Color(0xFFC8C6C0),
                "surfaceContainerLowest" to Color(0xFF0E0E10),
                "surfaceContainerLow" to Color(0xFF1B1B1D),
                "surfaceContainer" to Color(0xFF201F21),
                "surfaceContainerHigh" to Color(0xFF2A2A2C),
                "surfaceContainerHighest" to Color(0xFF353437),
                "primary" to Color(0xFFFFFFFF),
                "onPrimary" to Color(0xFF30312C),
                "primaryContainer" to Color(0xFFE4E2DB),
                "onPrimaryContainer" to Color(0xFF65655F),
                "inversePrimary" to Color(0xFF5F5F59),
                "secondary" to Color(0xFFE9C349),
                "onSecondary" to Color(0xFF3C2F00),
                "secondaryContainer" to Color(0xFFAF8D11),
                "onSecondaryContainer" to Color(0xFF342800),
                "tertiary" to Color(0xFFFFFFFF),
                "onTertiary" to Color(0xFF690007),
                "tertiaryContainer" to Color(0xFFFFDAD6),
                "onTertiaryContainer" to Color(0xFFB9332E),
                "error" to Color(0xFFFFB4AB),
                "onError" to Color(0xFF690005),
                "errorContainer" to Color(0xFF93000A),
                "onErrorContainer" to Color(0xFFFFDAD6),
                "outline" to Color(0xFF919189),
                "outlineVariant" to Color(0xFF474741),
                "inverseSurface" to Color(0xFFE5E1E4),
                "inverseOnSurface" to Color(0xFF313032),
            )
        val actual =
            mapOf(
                "background" to KdmScheme.background,
                "onBackground" to KdmScheme.onBackground,
                "surface" to KdmScheme.surface,
                "onSurface" to KdmScheme.onSurface,
                "surfaceVariant" to KdmScheme.surfaceVariant,
                "onSurfaceVariant" to KdmScheme.onSurfaceVariant,
                "surfaceDim" to KdmScheme.surfaceDim,
                "surfaceBright" to KdmScheme.surfaceBright,
                "surfaceTint" to KdmScheme.surfaceTint,
                "surfaceContainerLowest" to KdmScheme.surfaceContainerLowest,
                "surfaceContainerLow" to KdmScheme.surfaceContainerLow,
                "surfaceContainer" to KdmScheme.surfaceContainer,
                "surfaceContainerHigh" to KdmScheme.surfaceContainerHigh,
                "surfaceContainerHighest" to KdmScheme.surfaceContainerHighest,
                "primary" to KdmScheme.primary,
                "onPrimary" to KdmScheme.onPrimary,
                "primaryContainer" to KdmScheme.primaryContainer,
                "onPrimaryContainer" to KdmScheme.onPrimaryContainer,
                "inversePrimary" to KdmScheme.inversePrimary,
                "secondary" to KdmScheme.secondary,
                "onSecondary" to KdmScheme.onSecondary,
                "secondaryContainer" to KdmScheme.secondaryContainer,
                "onSecondaryContainer" to KdmScheme.onSecondaryContainer,
                "tertiary" to KdmScheme.tertiary,
                "onTertiary" to KdmScheme.onTertiary,
                "tertiaryContainer" to KdmScheme.tertiaryContainer,
                "onTertiaryContainer" to KdmScheme.onTertiaryContainer,
                "error" to KdmScheme.error,
                "onError" to KdmScheme.onError,
                "errorContainer" to KdmScheme.errorContainer,
                "onErrorContainer" to KdmScheme.onErrorContainer,
                "outline" to KdmScheme.outline,
                "outlineVariant" to KdmScheme.outlineVariant,
                "inverseSurface" to KdmScheme.inverseSurface,
                "inverseOnSurface" to KdmScheme.inverseOnSurface,
            )

        assertEquals(expected.keys, actual.keys)
        expected.forEach { (role, value) -> assertEquals(value, actual.getValue(role), role) }
    }

    @Test
    fun everyTypeStepCarriesTheRenderedMetrics() {
        val expected =
            listOf(
                Triple(KdmTypography.displaySmall, "displaySmall", 38.sp to 44.sp),
                Triple(KdmTypography.headlineLarge, "headlineLarge", 26.sp to 32.sp),
                Triple(KdmTypography.headlineMedium, "headlineMedium", 22.sp to 28.sp),
                Triple(KdmTypography.headlineSmall, "headlineSmall", 19.sp to 24.sp),
                Triple(KdmTypography.titleLarge, "titleLarge", 16.sp to 22.sp),
                Triple(KdmTypography.bodyLarge, "bodyLarge", 15.sp to 22.sp),
                Triple(KdmTypography.bodyMedium, "bodyMedium", 13.sp to 19.sp),
                Triple(KdmTypography.bodySmall, "bodySmall", 11.sp to 16.sp),
                Triple(KdmType.statDisplay, "statDisplay", 24.sp to 28.sp),
                Triple(KdmType.labelCaps, "labelCaps", 10.sp to 14.sp),
                Triple(KdmType.ruleQuote, "ruleQuote", 13.sp to 18.sp),
            )

        expected.forEach { (style, name, size) ->
            assertEquals(size.first, style.fontSize, "$name fontSize")
            assertEquals(size.second, style.lineHeight, "$name lineHeight")
        }
        assertEquals(FontWeight.Bold, KdmTypography.displaySmall.fontWeight)
        assertEquals(FontWeight.SemiBold, KdmTypography.headlineLarge.fontWeight)
        assertEquals(FontWeight.SemiBold, KdmTypography.titleLarge.fontWeight)
        assertEquals(FontWeight.Normal, KdmTypography.bodyMedium.fontWeight)
        assertEquals(0.02f.em, KdmTypography.titleLarge.letterSpacing)
        assertEquals(0.01f.em, KdmTypography.headlineSmall.letterSpacing)
        assertEquals((-0.02f).em, KdmType.statDisplay.letterSpacing)
        assertEquals(0.08f.em, KdmType.labelCaps.letterSpacing)
    }

    @Test
    fun everyShapeCarriesTheRenderedRadius() {
        val four = RoundedCornerShape(4.dp)
        val eight = RoundedCornerShape(8.dp)

        assertEquals(four, KdmShapes.extraSmall)
        assertEquals(four, KdmShapes.small)
        assertEquals(eight, KdmShapes.medium)
        assertEquals(eight, KdmShapes.large)
        assertEquals(eight, KdmShapes.extraLarge)
    }

    @Test
    fun theCheckedWoundColourIsLegibleOnThePanel() {
        val panel = KdmScheme.surfaceContainerLow

        // The bug this pins: the first implementation used `onPrimary` for the checked box, which
        // sits on the panel (an OutlinedButton has no container) and is nearly invisible there.
        assertTrue(
            contrastRatio(KdmScheme.onPrimary, panel) < 4.5f,
            "onPrimary is legible on the panel — this test no longer guards anything",
        )
        assertTrue(
            contrastRatio(woundBoxColor(checked = true, scheme = KdmScheme), panel) >= 3f,
            "the checked wound box must stay readable on the panel",
        )
    }

    /**
     * The Innovar-flow board carries colours the Atramentum Lantern palette does not: a gold that
     * is not [KdmPalette.gold], plus a green and a red the theme has no role for. They are pinned
     * here so the section cannot drift from the design it was built from. The choice of these over
     * the theme's own gold/red is under review — see `docs/backlog.md`.
     */
    @Test
    fun theInnovarFlowTokensMatchTheBoard() {
        val expected =
            mapOf(
                "gold" to Color(0xFFE5B842),
                "goldLight" to Color(0xFFF5D376),
                "goldDark" to Color(0xFF9B7617),
                "goldWash" to Color(0x1FE5B842),
                "goldPillBg" to Color(0xFF342800),
                "green" to Color(0xFF10B981),
                "greenWash" to Color(0x1F10B981),
                "red" to Color(0xFFEF4444),
                "redBg" to Color(0x1AEF4444),
                "outlineBorder" to Color(0x14FFFFFF),
            )
        val actual =
            mapOf(
                "gold" to InnovarPalette.gold,
                "goldLight" to InnovarPalette.goldLight,
                "goldDark" to InnovarPalette.goldDark,
                "goldWash" to InnovarPalette.goldWash,
                "goldPillBg" to InnovarPalette.goldPillBg,
                "green" to InnovarPalette.green,
                "greenWash" to InnovarPalette.greenWash,
                "red" to InnovarPalette.red,
                "redBg" to InnovarPalette.redBg,
                "outlineBorder" to InnovarPalette.outlineBorder,
            )
        assertEquals(expected, actual, "the Innovar board's tokens must not drift")
    }
}

/** WCAG relative-luminance contrast between two opaque colours. */
private fun contrastRatio(
    a: Color,
    b: Color,
): Float {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

private fun relativeLuminance(color: Color): Float {
    fun channel(value: Float): Float = if (value <= 0.03928f) value / 12.92f else ((value + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
}

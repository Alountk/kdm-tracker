package io.kdmtracker.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmScheme
import io.kdmtracker.app.ui.theme.KdmShapes
import io.kdmtracker.app.ui.theme.KdmType
import io.kdmtracker.app.ui.theme.KdmTypography
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the Atramentum Lantern tokens to the values the Stitch design renders, so a later edit that
 * drifts from the design fails here instead of shipping.
 */
class KdmThemeTest {
    @Test
    fun colourRolesCarryTheRenderedTokens() {
        assertEquals(Color(0xFF131315), KdmScheme.background)
        assertEquals(Color(0xFFE5E1E4), KdmScheme.onSurface)
        assertEquals(Color(0xFF1B1B1D), KdmScheme.surfaceContainerLow)
        assertEquals(Color(0xFF2A2A2C), KdmScheme.surfaceContainerHigh)
        assertEquals(Color(0xFFE9C349), KdmScheme.secondary)
        assertEquals(Color(0xFF919189), KdmScheme.outline)
        assertEquals(Color(0xFF474741), KdmScheme.outlineVariant)
    }

    @Test
    fun typeScaleCarriesTheRenderedSteps() {
        val headline = KdmTypography.headlineLarge

        assertEquals(26.sp, headline.fontSize)
        assertEquals(32.sp, headline.lineHeight)
        assertEquals(FontWeight.SemiBold, headline.fontWeight)
        assertEquals(10.sp, KdmType.labelCaps.fontSize)
        assertEquals(0.08f.em, KdmType.labelCaps.letterSpacing)
    }

    @Test
    fun shapesMatchTheRenderedRadii() {
        assertEquals(RoundedCornerShape(4.dp), KdmShapes.small)
        assertEquals(RoundedCornerShape(8.dp), KdmShapes.large)
    }
}

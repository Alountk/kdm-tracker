package io.kdmtracker.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.R

/**
 * The two families the design uses. Both ship as variable fonts, so each weight the design needs is
 * registered as its own entry pinned with [FontVariation] — on API 26+ that selects the axis value;
 * below it the font falls back to its default instance.
 */
object KdmFontFamilies {
    val playfair: FontFamily =
        FontFamily(
            playfairFace(FontWeight.Normal, 400, FontStyle.Normal),
            playfairFace(FontWeight.SemiBold, 600, FontStyle.Normal),
            playfairFace(FontWeight.Bold, 700, FontStyle.Normal),
            Font(
                R.font.playfair_display_italic,
                weight = FontWeight.Normal,
                style = FontStyle.Italic,
                variationSettings = FontVariation.Settings(FontVariation.weight(400)),
            ),
        )

    val inter: FontFamily =
        FontFamily(
            interFace(FontWeight.Normal, 400),
            interFace(FontWeight.SemiBold, 600),
            interFace(FontWeight.Bold, 700),
        )

    private fun playfairFace(
        weight: FontWeight,
        wght: Int,
        style: FontStyle,
    ): Font =
        Font(
            R.font.playfair_display,
            weight = weight,
            style = style,
            variationSettings = FontVariation.Settings(FontVariation.weight(wght)),
        )

    private fun interFace(
        weight: FontWeight,
        wght: Int,
    ): Font =
        Font(
            R.font.inter,
            weight = weight,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(wght)),
        )
}

/** The design's type scale, mapped onto the Material 3 roles that fit it. */
val KdmTypography =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = KdmFontFamilies.playfair,
                fontSize = 38.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02f).em,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = KdmFontFamilies.playfair,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = KdmFontFamilies.playfair,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = KdmFontFamilies.playfair,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.01f.em,
            ),
        titleLarge =
            TextStyle(
                fontFamily = KdmFontFamilies.playfair,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.02f.em,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = KdmFontFamilies.inter,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = KdmFontFamilies.inter,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodySmall =
            TextStyle(
                fontFamily = KdmFontFamilies.inter,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
            ),
    )

/**
 * The three design steps Material 3 has no role for, exposed by name rather than forced into a
 * wrong role.
 */
object KdmType {
    /** Numeric stats: 24/28 bold, tight tracking. */
    val statDisplay: TextStyle =
        TextStyle(
            fontFamily = KdmFontFamilies.inter,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.02f).em,
        )

    /** Uppercase micro-labels: 10/14 bold, wide tracking. */
    val labelCaps: TextStyle =
        TextStyle(
            fontFamily = KdmFontFamilies.inter,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08f.em,
        )

    /** Serif rules quotations: 13/18 regular. */
    val ruleQuote: TextStyle =
        TextStyle(
            fontFamily = KdmFontFamilies.playfair,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
        )
}

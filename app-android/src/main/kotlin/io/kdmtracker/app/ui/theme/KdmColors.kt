package io.kdmtracker.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The "Atramentum Lantern" palette, copied from the tokens the Stitch design actually renders.
 *
 * These are the values in the screen's embedded Tailwind config, not the `designMd` brand prose:
 * the canvas is [131315] (not `#0c0c0e`) and the gold is [E9C349] (not `#d4af37`).
 */
object KdmPalette {
    val canvas = Color(0xFF131315)
    val onCanvas = Color(0xFFE5E1E4)
    val bone = Color(0xFFFFFFFF)
    val onBone = Color(0xFF30312C)
    val boneContainer = Color(0xFFE4E2DB)
    val onBoneContainer = Color(0xFF65655F)
    val inverseBone = Color(0xFF5F5F59)
    val ash = Color(0xFFC8C7BE)
    val gold = Color(0xFFE9C349)
    val onGold = Color(0xFF3C2F00)
    val goldContainer = Color(0xFFAF8D11)
    val onGoldContainer = Color(0xFF342800)
    val crimson = Color(0xFF690007)
    val onCrimson = Color(0xFFFFFFFF)
    val crimsonContainer = Color(0xFFFFDAD6)
    val onCrimsonContainer = Color(0xFFB9332E)
    val blood = Color(0xFFFFB4AB)
    val onBlood = Color(0xFF690005)
    val bloodContainer = Color(0xFF93000A)
    val onBloodContainer = Color(0xFFFFDAD6)
    val outline = Color(0xFF919189)
    val outlineVariant = Color(0xFF474741)
    val surfaceLowest = Color(0xFF0E0E10)
    val surfaceLow = Color(0xFF1B1B1D)
    val surfaceContainer = Color(0xFF201F21)
    val surfaceHigh = Color(0xFF2A2A2C)
    val surfaceHighest = Color(0xFF353437)
    val surfaceBright = Color(0xFF39393B)
    val tint = Color(0xFFC8C6C0)
    val inverseCanvas = Color(0xFFE5E1E4)
    val inverseOnCanvas = Color(0xFF313032)
}

/** The Material 3 colour roles for the Atramentum Lantern design system. */
val KdmScheme =
    darkColorScheme(
        primary = KdmPalette.bone,
        onPrimary = KdmPalette.onBone,
        primaryContainer = KdmPalette.boneContainer,
        onPrimaryContainer = KdmPalette.onBoneContainer,
        inversePrimary = KdmPalette.inverseBone,
        secondary = KdmPalette.gold,
        onSecondary = KdmPalette.onGold,
        secondaryContainer = KdmPalette.goldContainer,
        onSecondaryContainer = KdmPalette.onGoldContainer,
        tertiary = KdmPalette.bone,
        onTertiary = KdmPalette.crimson,
        tertiaryContainer = KdmPalette.crimsonContainer,
        onTertiaryContainer = KdmPalette.onCrimsonContainer,
        background = KdmPalette.canvas,
        onBackground = KdmPalette.onCanvas,
        surface = KdmPalette.canvas,
        onSurface = KdmPalette.onCanvas,
        surfaceVariant = KdmPalette.surfaceHighest,
        onSurfaceVariant = KdmPalette.ash,
        surfaceTint = KdmPalette.tint,
        inverseSurface = KdmPalette.inverseCanvas,
        inverseOnSurface = KdmPalette.inverseOnCanvas,
        error = KdmPalette.blood,
        onError = KdmPalette.onBlood,
        errorContainer = KdmPalette.bloodContainer,
        onErrorContainer = KdmPalette.onBloodContainer,
        outline = KdmPalette.outline,
        outlineVariant = KdmPalette.outlineVariant,
        surfaceDim = KdmPalette.canvas,
        surfaceBright = KdmPalette.surfaceBright,
        surfaceContainerLowest = KdmPalette.surfaceLowest,
        surfaceContainerLow = KdmPalette.surfaceLow,
        surfaceContainer = KdmPalette.surfaceContainer,
        surfaceContainerHigh = KdmPalette.surfaceHigh,
        surfaceContainerHighest = KdmPalette.surfaceHighest,
    )

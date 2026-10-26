package io.kdmtracker.app.ui.settlement

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmFontFamilies

/** The mock's row title: `font-headline-sm text-[13px] font-bold` — Playfair, not Inter. */
internal val rowTitle =
    TextStyle(
        fontFamily = KdmFontFamilies.playfair,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
    )

/** The mock's micro type: 10px Inter, plain and bold, without label-caps' tracking. */
internal val microNormal =
    TextStyle(fontFamily = KdmFontFamilies.inter, fontSize = 10.sp, lineHeight = 14.sp)

internal val microBold =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
    )

/** `tracking-widest`: label-caps' 0.08em widened to the mock's 0.1em. */
internal val TextStyle.widest: TextStyle
    get() = copy(letterSpacing = 0.1f.em)

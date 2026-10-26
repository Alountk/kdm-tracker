package io.kdmtracker.app

import io.kdmtracker.app.ui.hunt.BorderAccent
import io.kdmtracker.app.ui.hunt.ChipBg
import io.kdmtracker.app.ui.hunt.ChipText
import io.kdmtracker.app.ui.hunt.HuntEventCardModel
import io.kdmtracker.app.ui.hunt.RangeTone
import io.kdmtracker.app.ui.hunt.style
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S9: the hunt-event card splits its rules text into an italic intro and one block per d10 result
 * range, each with the design's tone (danger / advantage / neutral).
 */
class HuntEventCardModelTest {
    @Test
    fun splitsIntroAndRangeBlocksFromARealLionCard() {
        // "Cachorro de León Blanco" verbatim rules text (Core15HuntEvents)
        val text =
            "Los supervivientes encuentran un cachorro de León Blanco. Deben decidir si matan " +
                "al cachorro. Si lo hacen, cada superviviente gana 1 recurso básico aleatorio. El " +
                "revelador de eventos tira en la tabla de abajo. 1-3 La leona madre aparece. " +
                "Sobreponiéndose al dolor, ataca. El León Blanco comienza el enfrentamiento con " +
                "Enfurecido en juego. (Construye el mazo de IA normalmente.) 4-7 El león padre " +
                "aparece. Comienza el enfrentamiento de inmediato. 8 - 10 No ocurre nada. Si " +
                "dejan libre al cachorro: Haz una tirada de evento de caza aleatorio"

        val model = HuntEventCardModel.parse(text)

        // intro is everything before the first range and is rendered in italic
        assertTrue(model.intro.contains("revelador de eventos tira en la tabla de abajo"))
        assertTrue(!model.intro.contains("1-3"))
        // three range blocks, in order, with their verbatim text
        assertEquals(listOf("1 - 3", "4 - 7", "8 - 10"), model.ranges.map { it.label })
        assertTrue(model.ranges[0].text.contains("La leona madre aparece"))
        assertTrue(model.ranges[1].text.contains("El león padre aparece"))
        assertTrue(model.ranges[2].text.startsWith("No ocurre nada"))
    }

    @Test
    fun marksDangerAndAdvantageTonesFromTheRangeText() {
        val text =
            "Intro. 1-3 Todos sufren 1 de daño al Cerebro. 4-7 Nada. 8-10 +1 de Supervivencia y avanzáis."
        val model = HuntEventCardModel.parse(text)

        assertEquals(RangeTone.DANGER, model.ranges[0].tone)
        assertEquals(RangeTone.NEUTRAL, model.ranges[1].tone)
        assertEquals(RangeTone.ADVANTAGE, model.ranges[2].tone)
    }

    @Test
    fun keepsAPlainTextCardAsASingleItalicIntroWithoutRanges() {
        val model = HuntEventCardModel.parse("Un evento sin tabla de tiradas.")
        assertTrue(model.ranges.isEmpty())
        assertEquals("Un evento sin tabla de tiradas.", model.intro.trim())
    }

    /** S9 (design): each tone maps to the design's chip fill, chip text and left border. */
    @Test
    fun eachToneMapsToTheDesignsChipAndBorderRoles() {
        val danger = RangeTone.DANGER.style()
        val advantage = RangeTone.ADVANTAGE.style()
        val neutral = RangeTone.NEUTRAL.style()

        assertEquals(ChipBg.DANGER, danger.chipBg)
        assertEquals(ChipText.DANGER, danger.chipText)
        assertEquals(BorderAccent.DANGER, danger.border)

        assertEquals(ChipBg.GOLD, advantage.chipBg)
        assertEquals(ChipText.GOLD, advantage.chipText)
        assertEquals(BorderAccent.GOLD, advantage.border)

        assertEquals(ChipBg.NEUTRAL, neutral.chipBg)
        assertEquals(ChipText.NEUTRAL, neutral.chipText)
        assertEquals(BorderAccent.NEUTRAL, neutral.border)
    }
}

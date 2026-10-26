package io.kdmtracker.app.ui.hunt

/**
 * A d10 result range of a hunt-event card, split out of the card's plain rules text.
 *
 * @property label the display range (e.g. `1 - 3`), normalized from the raw `1-3`/`8 - 10` forms.
 * @property text the range's outcome text.
 * @property tone the design's color role for this block (danger / advantage / neutral).
 */
data class HuntEventRange(
    val label: String,
    val text: String,
    val tone: RangeTone,
)

/** The design's color role for a result block: damage (red), advantage (gold), or neutral. */
enum class RangeTone { DANGER, ADVANTAGE, NEUTRAL }

/**
 * The design's per-tone visual roles for a range block, mirroring the `Flujo de Casillas` card in
 * `.superpowers/design` + the Stitch `flujo` screen: each tone has a **filled chip background**,
 * a **chip text** colour and a **3px left border** accent, all of the same hue family.
 *
 * The roles are semantic (not `androidx` colours) so the model stays Compose-free and testable;
 * the render maps them to `KdmPalette` colours.
 */
data class RangeStyle(
    val chipBg: ChipBg,
    val chipText: ChipText,
    val border: BorderAccent,
)

enum class ChipBg { DANGER, GOLD, NEUTRAL }

enum class ChipText { DANGER, GOLD, NEUTRAL }

enum class BorderAccent { DANGER, GOLD, NEUTRAL }

/**
 * The parsed model of a hunt-event card's rules text, for the design's "Flujo de Casillas" card
 * (S9): an **italic** intro (everything before the first d10 range) plus one block per result
 * range, each with the design's tone.
 *
 * The decks store plain rules text (not structured ranges), so ranges are parsed from the text.
 * A card with no `N - M` table is a single intro with no ranges.
 */
data class HuntEventCardModel(
    val intro: String,
    val ranges: List<HuntEventRange>,
) {
    companion object {
        /** Matches a d10 range token like `1-3`, `4 - 7`, or `8 - 10` at a word boundary. */
        private val RANGE =
            Regex("""(?<![\p{L}\d])(\d{1,2})\s*-\s*(\d{1,2})(?![\p{L}\d])""")

        /** Tone keywords: damage words -> danger; bonus/advance words -> advantage. */
        private val DANGER = Regex("""(?i)daño|daña|muere|muerte|derrib|locura|cerebro|trauma|pérdid|pierde""")

        private val ADVANTAGE =
            Regex("""(?i)\+\d|ventaja|avanz|supervivencia|recurso|gana|bonif|investig""")

        /** Splits [text] into an italic intro and one toned block per d10 result range. */
        fun parse(text: String): HuntEventCardModel {
            val matches = RANGE.findAll(text).toList()
            if (matches.isEmpty()) {
                return HuntEventCardModel(intro = text, ranges = emptyList())
            }
            val intro = text.substring(0, matches.first().range.first).trim()
            val ranges =
                matches.mapIndexed { i, m ->
                    val start = m.range.last + 1
                    val end = matches.getOrNull(i + 1)?.range?.first ?: text.length
                    val body = text.substring(start, end).trim()
                    HuntEventRange(
                        label = "${m.groupValues[1].toInt()} - ${m.groupValues[2].toInt()}",
                        text = body,
                        tone = toneFor(body),
                    )
                }
            return HuntEventCardModel(intro = intro, ranges = ranges)
        }

        private fun toneFor(body: String): RangeTone =
            when {
                DANGER.containsMatchIn(body) -> RangeTone.DANGER
                ADVANTAGE.containsMatchIn(body) -> RangeTone.ADVANTAGE
                else -> RangeTone.NEUTRAL
            }
    }
}

/**
 * The design's visual style per tone. `danger` -> red chip on a red-tinted fill with a red border;
 * `advantage` (the design's resource/gain rows) -> gold; `neutral` -> the primary-tinted row.
 * Mirrors the `1-3` / `4-7` / `8-10` blocks of the design's `Flujo de Casillas` card.
 */
fun RangeTone.style(): RangeStyle =
    when (this) {
        RangeTone.DANGER -> RangeStyle(ChipBg.DANGER, ChipText.DANGER, BorderAccent.DANGER)
        RangeTone.ADVANTAGE -> RangeStyle(ChipBg.GOLD, ChipText.GOLD, BorderAccent.GOLD)
        RangeTone.NEUTRAL -> RangeStyle(ChipBg.NEUTRAL, ChipText.NEUTRAL, BorderAccent.NEUTRAL)
    }

package io.kdmtracker.app

import io.kdmtracker.app.ui.HuntBoardFrame
import io.kdmtracker.app.ui.SpaceTone
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.core.domain.HuntSpace
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the pure 13-space hunt board frame: each space's index, icon and label by `HuntSpaceKind`,
 * and the two position markers. This is the render `HuntBoard` composes, extracted so it can be
 * tested without a Compose harness.
 */
class HuntBoardFrameTest {
    private fun hunt(): HuntState =
        HuntState(
            quarry = "White Lion",
            level = 1,
            players = 1,
            spaces =
                List(13) { index ->
                    HuntSpace(
                        kind =
                            when (index) {
                                0 -> HuntSpaceKind.START
                                12 -> HuntSpaceKind.MONSTER
                                5 -> HuntSpaceKind.MONSTER_E
                                6 -> HuntSpaceKind.OVERWHELMING_DARKNESS
                                7 -> HuntSpaceKind.STARVATION
                                else -> HuntSpaceKind.NORMAL
                            },
                    )
                },
            position = 3,
            monsterPosition = 12,
        )

    @Test
    fun boardHasThirteenIndexedSpaces() {
        val frame = HuntBoardFrame.from(hunt())

        assertEquals(13, frame.spaces.size)
        assertEquals((0..12).toList(), frame.spaces.map { it.index })
        assertEquals("Casillas 0 - 12", frame.rangeLabel)
    }

    @Test
    fun eachSpaceCarriesItsKindIconAndLabel() {
        val frame = HuntBoardFrame.from(hunt())
        val byIndex = frame.spaces.associateBy { it.index }

        // START -> foundation Asent.
        assertEquals("foundation", byIndex.getValue(0).icon)
        assertEquals("Asent.", byIndex.getValue(0).label)
        // NORMAL free space -> E Evento (an "E" space with no monster event assigned yet)
        assertEquals("E", byIndex.getValue(1).icon)
        assertEquals("Evento", byIndex.getValue(1).label)
        // MONSTER_E -> the quarry special event `E. León` (S4)
        assertEquals("visibility", byIndex.getValue(5).icon)
        assertEquals("E. León", byIndex.getValue(5).label)
        // OVERWHELMING_DARKNESS -> its kind's label (the fixed board space "Oscuridad Abrumadora")
        assertEquals("Oscuridad Abrumadora", byIndex.getValue(6).label)
        // STARVATION -> its kind's label (the fixed board space "Hambruna")
        assertEquals("Hambruna", byIndex.getValue(7).label)
        // MONSTER -> cruelty_free paw + the quarry's name (the beast, not the lair skull)
        assertEquals("cruelty_free", byIndex.getValue(12).icon)
        assertEquals("White Lion", byIndex.getValue(12).label)
    }

    @Test
    fun thePartysSpaceShowsTheGroupMarker() {
        val frame = HuntBoardFrame.from(hunt())

        assertEquals(3, frame.spaces.first { it.isParty }.index)
        assertEquals("groups", frame.spaces.first { it.isParty }.icon)
        assertEquals("Grupo", frame.spaces.first { it.isParty }.label)
    }

    @Test
    fun positionMarkersNameThePartyAndMonsterSpaces() {
        val frame = HuntBoardFrame.from(hunt())

        assertEquals("Supervivientes (Casilla 3)", frame.partyMarker)
        assertEquals("Monstruo (Casilla 12)", frame.monsterMarker)
    }

    /** S-design: each space carries the design's caption and colour tone (estados anatomy). */
    @Test
    fun eachSpaceCarriesTheDesignsCaptionAndTone() {
        val frame = HuntBoardFrame.from(hunt())
        val byIndex = frame.spaces.associateBy { it.index }

        // Inicio (00): neutral surface, caption "Inicio"
        assertEquals(SpaceTone.SURFACE, byIndex.getValue(0).tone)
        assertEquals("Inicio", byIndex.getValue(0).subLabel)
        // Free event space (01): outline tone, caption "Tablero"
        assertEquals(SpaceTone.OUTLINE, byIndex.getValue(1).tone)
        assertEquals("Tablero", byIndex.getValue(1).subLabel)
        // Quarry special event (05): gold tone, caption "Ojo León"
        assertEquals(SpaceTone.GOLD, byIndex.getValue(5).tone)
        assertEquals("Ojo León", byIndex.getValue(5).subLabel)
        // Overwhelming Darkness (06): violet tone, caption "Divisoria"
        assertEquals(SpaceTone.VIOLET, byIndex.getValue(6).tone)
        assertEquals("Divisoria", byIndex.getValue(6).subLabel)
        // Starvation (07): amber tone, caption "Desgaste"
        assertEquals(SpaceTone.AMBER, byIndex.getValue(7).tone)
        assertEquals("Desgaste", byIndex.getValue(7).subLabel)
        // The beast's space (12): blood tone, caption "En Acecho"
        assertEquals(SpaceTone.BLOOD, byIndex.getValue(12).tone)
        assertEquals("En Acecho", byIndex.getValue(12).subLabel)
        // The party's space (03): emerald tone (design decision L2), caption "Activo"
        assertEquals(SpaceTone.GREEN, byIndex.getValue(3).tone)
        assertEquals("Activo", byIndex.getValue(3).subLabel)
    }

    /** S2: a fixed space keeps its own mark even when the party stands on it. */
    @Test
    fun fixedSpacesKeepTheirMarkWhenThePartyStandsThere() {
        val starvation = hunt().copy(position = 7) // party on the Starvation space
        val dark = hunt().copy(position = 6) // party on Overwhelming Darkness

        assertEquals(
            "Hambruna",
            HuntBoardFrame
                .from(starvation)
                .spaces
                .getValue(7)
                .label,
        )
        assertEquals(
            "Oscuridad Abrumadora",
            HuntBoardFrame
                .from(dark)
                .spaces
                .getValue(6)
                .label,
        )
        // neither fixed space is repainted as the party's Grupo
        assertEquals(
            emptyList(),
            HuntBoardFrame.from(starvation).spaces.filter { it.index == 7 && it.label == "Grupo" },
        )
    }

    /** S4: the beast's space is a paw + the quarry name, not the lair skull. */
    @Test
    fun theBeastSpaceShowsAPawAndTheQuarryName() {
        val beast = hunt().copy(position = 0) // party off the monster space
        val view = HuntBoardFrame.from(beast).spaces.getValue(12)

        assertEquals("cruelty_free", view.icon)
        assertEquals("White Lion", view.label)
        assertEquals(12, HuntBoardFrame.from(beast).monsterMarker.let { 12 }) // lair stays index 12
    }

    /** S4: the lair skull moves to the real MONSTER kind; a normal free space stays a grey E. */
    @Test
    fun normalEventSpacesStayGreyE() {
        val frame = HuntBoardFrame.from(hunt())
        val free = frame.spaces.getValue(1)

        assertEquals("E", free.icon)
        assertEquals("Evento", free.label)
        // the quarry special event (MONSTER_E) is a gold eye, not a plain E
        assertEquals("visibility", frame.spaces.getValue(5).icon)
        assertEquals("E. León", frame.spaces.getValue(5).label)
    }

    /** S3: the fixed OD/Starvation icons must be bundled glyphs (glyph() must not throw). */
    @Test
    fun fixedSpaceIconsAreBundledGlyphs() {
        val frame = HuntBoardFrame.from(hunt())

        listOf(6, 7, 12).forEach { index ->
            val icon = frame.spaces.getValue(index).icon
            // resolving the glyph throws for any unmapped name — that is the crash we are fixing;
            // calling it here fails the test if the icon is not bundled.
            KdmSymbols.glyph(icon)
        }
    }

    /** L5(e): top-right mark by step — ping dot on the party, star on E, no mark on normal. */
    @Test
    fun eachStepCarriesItsTopRightTypeBadge() {
        val frame = HuntBoardFrame.from(hunt())

        assertEquals("flag", frame.spaces.getValue(0).badge) // Inicio
        assertEquals("", frame.spaces.getValue(1).badge) // ordinary event step: NO top-right mark (L5e)
        assertEquals("star", frame.spaces.getValue(5).badge) // quarry special event: a star, not the eye (L5e)
        assertEquals("nights_stay", frame.spaces.getValue(6).badge) // Overwhelming Darkness
        assertEquals("warning", frame.spaces.getValue(7).badge) // Starvation
        // the beast (position 12) wins: swords, even though kind 12 is MONSTER/final
        assertEquals("swords", frame.spaces.getValue(12).badge)
        // with the beast gone, the final step (kind MONSTER) shows skull
        assertEquals(
            "skull",
            HuntBoardFrame
                .from(hunt().copy(monsterPosition = -1))
                .spaces
                .getValue(12)
                .badge,
        )
    }

    /** L5(b): the party's step replaces its type badge with the ping dot; others have none. */
    @Test
    fun onlyThePartysStepCarriesThePingDot() {
        val frame = HuntBoardFrame.from(hunt())
        val byIndex = frame.spaces.associateBy { it.index }

        // the party (03): ping dot, no type badge
        assertEquals(true, byIndex.getValue(3).partyPing)
        assertEquals("", byIndex.getValue(3).badge)
        // every other step: no ping dot
        listOf(0, 1, 5, 6, 7, 12).forEach { idx ->
            assertEquals(false, byIndex.getValue(idx).partyPing, "ping at $idx")
        }
    }

    /** S1: the start (00) and final (12) steps carry no number; the rest show theirs. */
    @Test
    fun startAndFinalStepsHaveNoNumberButTheRestDo() {
        val frame = HuntBoardFrame.from(hunt())

        assertEquals(false, frame.spaces.getValue(0).showNumber) // Inicio
        assertEquals(false, frame.spaces.getValue(12).showNumber) // final (beast here, no number)
        assertEquals(true, frame.spaces.getValue(1).showNumber)
        assertEquals(true, frame.spaces.getValue(7).showNumber)
    }

    private fun <T> List<T>.getValue(index: Int): T = this[index]
}

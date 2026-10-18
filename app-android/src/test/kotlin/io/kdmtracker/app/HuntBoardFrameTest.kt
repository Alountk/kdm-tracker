package io.kdmtracker.app

import io.kdmtracker.app.ui.HuntBoardFrame
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

        // START -> home Asent.
        assertEquals("home", byIndex.getValue(0).icon)
        assertEquals("Asent.", byIndex.getValue(0).label)
        // NORMAL free space -> E Evento (an "E" space with no monster event assigned yet)
        assertEquals("E", byIndex.getValue(1).icon)
        assertEquals("Evento", byIndex.getValue(1).label)
        // MONSTER_E -> E Evento
        assertEquals("E", byIndex.getValue(5).icon)
        assertEquals("Evento", byIndex.getValue(5).label)
        // OVERWHELMING_DARKNESS -> its kind's label (the fixed board space "Oscuridad Abrumadora")
        assertEquals("Oscuridad Abrumadora", byIndex.getValue(6).label)
        // STARVATION -> its kind's label (the fixed board space "Hambruna")
        assertEquals("Hambruna", byIndex.getValue(7).label)
        // MONSTER -> skull Guarida
        assertEquals("skull", byIndex.getValue(12).icon)
        assertEquals("Guarida", byIndex.getValue(12).label)
    }

    @Test
    fun thePartysSpaceShowsTheGroupMarker() {
        val frame = HuntBoardFrame.from(hunt())

        assertEquals(3, frame.spaces.first { it.isParty }.index)
        assertEquals("flare", frame.spaces.first { it.isParty }.icon)
        assertEquals("Grupo", frame.spaces.first { it.isParty }.label)
    }

    @Test
    fun positionMarkersNameThePartyAndMonsterSpaces() {
        val frame = HuntBoardFrame.from(hunt())

        assertEquals("Supervivientes (Casilla 3)", frame.partyMarker)
        assertEquals("Monstruo (Casilla 12)", frame.monsterMarker)
    }
}

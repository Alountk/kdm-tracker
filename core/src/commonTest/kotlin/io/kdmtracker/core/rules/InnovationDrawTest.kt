package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The hand is a pure function of (campaign, classified cards): same campaign and same
 * available set ⇒ same cards in the same order, on every device, for as long as the user
 * looks at it. Nothing here may depend on the order the views arrive in.
 */
class InnovationDrawTest {
    private fun innovation(id: String) = Innovation(InnovationId(id), LocalizedText(es = id), "Rules text.")

    private fun view(
        id: String,
        status: InnovationStatus,
    ) = InnovationView(innovation(id), status)

    private val available = listOf("pintura", "simposio", "choza", "amoníaco", "tambores", "linterna")
    private val wholeTree =
        listOf(view("lenguaje", InnovationStatus.UNLOCKED)) +
            available.map { view(it, InnovationStatus.AVAILABLE) } +
            listOf(view("escultura", InnovationStatus.NO_ACCESS))

    private fun ids(hand: List<Innovation>) = hand.map { it.id.value }

    @Test
    fun theHandIsExactlyTheRequestedSize() {
        val hand = InnovationDraw.of("campaign-a", wholeTree, draw = 4)

        assertEquals(4, hand.size)
    }

    @Test
    fun theHandIsNeverLongerThanWhatIsAvailable() {
        val oneAvailable = listOf(view("pintura", InnovationStatus.AVAILABLE))

        val hand = InnovationDraw.of("campaign-a", oneAvailable, draw = 2)

        assertEquals(listOf("pintura"), ids(hand))
    }

    @Test
    fun theHandOnlyOffersAvailableCards() {
        val hand = InnovationDraw.of("campaign-a", wholeTree, draw = 6)

        assertTrue(hand.isNotEmpty())
        assertTrue(ids(hand).all { it in available }, "hand was ${ids(hand)}")
        assertTrue("lenguaje" !in ids(hand), "an unlocked card can never be offered again")
        assertTrue("escultura" !in ids(hand), "a card whose parent is missing is not offered")
    }

    @Test
    fun theHandIsEmptyWhenNothingIsUnlocked() {
        val locked = listOf(view("lenguaje", InnovationStatus.UNLOCKED))

        assertEquals(emptyList(), InnovationDraw.of("campaign-a", locked, draw = 2))
    }

    @Test
    fun theHandIsEmptyWithoutACampaign() {
        val hand = InnovationDraw.of(null, wholeTree, draw = 4)

        assertEquals(emptyList(), hand)
    }

    @Test
    fun theHandIsStableForTheSameCampaign() {
        val first = InnovationDraw.of("campaign-a", wholeTree, draw = 4)
        val second = InnovationDraw.of("campaign-a", wholeTree, draw = 4)

        assertEquals(first, second)
    }

    @Test
    fun theHandIgnoresTheOrderTheViewsArriveIn() {
        val reversed = InnovationDraw.of("campaign-a", wholeTree.reversed(), draw = 4)

        assertEquals(InnovationDraw.of("campaign-a", wholeTree, draw = 4), reversed)
    }

    @Test
    fun theHandDiffersBetweenCampaigns() {
        val a = ids(InnovationDraw.of("campaign-a", wholeTree, draw = 4))
        val b = ids(InnovationDraw.of("campaign-b", wholeTree, draw = 4))

        assertTrue(a != b, "two campaigns must not be handed the same offer: $a")
    }
}

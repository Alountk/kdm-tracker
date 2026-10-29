package io.kdmtracker.app

import io.kdmtracker.app.ui.activeMilestoneCount
import io.kdmtracker.app.ui.deathsRowSubtitle
import io.kdmtracker.app.ui.endeavorTokenNames
import io.kdmtracker.app.ui.milestoneFlavor
import io.kdmtracker.app.ui.milestoneGlyph
import io.kdmtracker.app.ui.milestoneStatus
import io.kdmtracker.app.ui.milestoneTitle
import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * S4–S7: the step-bodies' pure derivations — the endeavor token tray's living-survivor basis, the
 * milestone card's derived status chip/glyph/flavor, and the step-5 zero-deaths subtitle gate.
 * Verbatim design copy is pinned separately in `SettlementStepBodiesTest`; these tests only pin
 * the derivations' behavior.
 */
class StepBodies3To6ModelTest {
    private fun survivor(
        seed: Long,
        name: String,
        status: SurvivorStatus = SurvivorStatus.ALIVE,
    ): Survivor =
        Survivor(
            id = Ulid.generate(seed) { 0 },
            name = name,
            status = status,
        )

    private fun view(
        reached: Boolean,
        resolved: Boolean,
    ): MilestoneView =
        MilestoneView(
            id = MilestoneId("test"),
            description = "",
            reached = reached,
            resolved = resolved,
            choices = emptyList(),
        )

    // -- S4: the endeavor token tray's basis (the same living-survivor count GainEndeavors funds) --

    @Test
    fun noLivingSurvivorYieldsNoEndeavorTokens() {
        assertEquals(emptyList(), endeavorTokenNames(emptyList()))
        assertEquals(
            emptyList(),
            endeavorTokenNames(listOf(survivor(1, "Erza", SurvivorStatus.DEAD))),
        )
    }

    @Test
    fun everyLivingSurvivorContributesOneFirstNameToken() {
        val survivors =
            listOf(
                survivor(1, "Zachary Conley"),
                survivor(2, "Lucy", SurvivorStatus.DEPARTING),
                survivor(3, "Erza", SurvivorStatus.DEAD),
            )

        assertEquals(listOf("Zachary", "Lucy"), endeavorTokenNames(survivors))
    }

    // -- S7: the `{n} Activos` meta chip --

    @Test
    fun noActiveMilestoneWhenNoneAreReached() {
        assertEquals(0, activeMilestoneCount(listOf(view(reached = false, resolved = false))))
    }

    @Test
    fun onlyReachedUnresolvedMilestonesAreActive() {
        val views =
            listOf(
                view(reached = true, resolved = false),
                view(reached = true, resolved = true),
                view(reached = false, resolved = false),
            )

        assertEquals(1, activeMilestoneCount(views))
    }

    // -- S7: the derived status chip --

    @Test
    fun resolvedMilestonesChipAsResuelta() {
        assertEquals("Resuelta", milestoneStatus(reached = true, resolved = true).chip)
        assertEquals("Resuelta", milestoneStatus(reached = false, resolved = true).chip)
    }

    @Test
    fun reachedUnresolvedMilestonesChipAsAlcanzada() {
        assertEquals("Alcanzada", milestoneStatus(reached = true, resolved = false).chip)
    }

    @Test
    fun unreachedMilestonesChipAsPendiente() {
        assertEquals("Pendiente", milestoneStatus(reached = false, resolved = false).chip)
    }

    // -- S7: the milestone glyph mapping (the design draws only Death + Society) --

    @Test
    fun theDeathMilestoneKeepsTheDesignsBookmarkGlyph() {
        assertEquals("bookmark_border", milestoneGlyph(Core16Milestones.PRINCIPLE_DEATH))
    }

    @Test
    fun theSocietyMilestoneMapsToFamilyRestroom() {
        assertEquals("family_restroom", milestoneGlyph(Core16Milestones.PRINCIPLE_SOCIETY))
    }

    @Test
    fun milestonesTheDesignDoesNotNameMapToBookmarkBorder() {
        assertEquals("bookmark_border", milestoneGlyph(Core16Milestones.PRINCIPLE_NEW_LIFE))
        assertEquals("bookmark_border", milestoneGlyph(Core16Milestones.HOODED_KNIGHT))
    }

    // -- S7: the design's own Spanish titles for the two milestones it names --

    @Test
    fun theNamedMilestonesUseTheDesignsSpanishTitles() {
        assertEquals(
            "Principio: Muerte",
            milestoneTitle(Core16Milestones.PRINCIPLE_DEATH, "Death principle"),
        )
        assertEquals(
            "Principio: Sociedad",
            milestoneTitle(Core16Milestones.PRINCIPLE_SOCIETY, "Society principle"),
        )
    }

    @Test
    fun milestonesTheDesignDoesNotNameKeepTheirDescription() {
        assertEquals(
            "First child is born",
            milestoneTitle(Core16Milestones.PRINCIPLE_NEW_LIFE, "First child is born"),
        )
        assertEquals(
            "Hooded Knight",
            milestoneTitle(Core16Milestones.HOODED_KNIGHT, "Hooded Knight"),
        )
        assertEquals(
            "Game over",
            milestoneTitle(Core16Milestones.GAME_OVER, "Game over"),
        )
    }

    // -- S7: the flavor paragraph appears only for the two milestones the design names --

    @Test
    fun onlyTheDesignNamedMilestonesCarryFlavor() {
        assertNotNull(milestoneFlavor(Core16Milestones.PRINCIPLE_DEATH))
        assertNotNull(milestoneFlavor(Core16Milestones.PRINCIPLE_SOCIETY))
        assertNull(milestoneFlavor(Core16Milestones.PRINCIPLE_NEW_LIFE))
        assertNull(milestoneFlavor(Core16Milestones.HOODED_KNIGHT))
        assertNull(milestoneFlavor(Core16Milestones.GAME_OVER))
    }

    // -- S6: the step-5 subtitle exists only while nobody has died --

    @Test
    fun theZeroDeathsSubtitleIsPresentOnlyAtZeroDeaths() {
        assertNotNull(deathsRowSubtitle(0))
        assertNull(deathsRowSubtitle(1))
        assertNull(deathsRowSubtitle(3))
    }
}

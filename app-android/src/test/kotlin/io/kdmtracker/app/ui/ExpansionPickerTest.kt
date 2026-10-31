package io.kdmtracker.app.ui

import io.kdmtracker.core.content.ContentPack
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpansionPickerTest {
    private val gorm = ContentPack(id = "gorm", version = "1", name = "Gorm")
    private val other = ContentPack(id = "other", version = "1", name = "Other")

    @Test
    fun everyOptionalPackBecomesAChoiceNamedAfterThePack() {
        assertEquals(
            listOf(ExpansionOption("gorm", "Gorm"), ExpansionOption("other", "Other")),
            expansionOptions(listOf(gorm, other)),
        )
    }

    @Test
    fun noOptionalPackMeansNoChoiceToShow() {
        assertEquals(emptyList(), expansionOptions(emptyList()))
    }

    @Test
    fun tappingAChoiceSelectsItAndTappingAgainClearsIt() {
        val once = toggledExpansion(emptySet(), "gorm")
        val twice = toggledExpansion(once, "gorm")

        assertEquals(setOf("gorm"), once)
        assertEquals(emptySet(), twice)
    }
}

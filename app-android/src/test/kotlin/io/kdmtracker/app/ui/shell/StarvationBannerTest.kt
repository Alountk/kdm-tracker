package io.kdmtracker.app.ui.shell

import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.StorageLine
import kotlin.test.Test
import kotlin.test.assertEquals

class StarvationBannerTest {
    private val bone = ResourceId("bone.monster")
    private val hide = ResourceId("hide.monster")
    private val lost = ResourceId("unknown.thing")
    private val definitions =
        listOf(ResourceDefinition(id = bone, names = LocalizedText(es = "Hueso"), category = ResourceCategory.BASIC))

    @Test
    fun onlyStoredResourcesAreOfferedAndNamedFromTheCatalog() {
        val choices =
            starvationChoices(listOf(StorageLine(bone, 3), StorageLine(hide, 0), StorageLine(lost, 1)), definitions)

        assertEquals(listOf(bone, lost), choices.map { it.id })
        assertEquals("Hueso ×3", choices[0].label)
        assertEquals("unknown.thing ×1", choices[1].label, "a resource the catalog lacks keeps its id as the name")
    }
}

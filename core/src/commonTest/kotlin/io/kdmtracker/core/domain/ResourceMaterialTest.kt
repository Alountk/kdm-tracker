package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceMaterialTest {
    @Test
    fun thereAreExactlyTheFourSummaryBuckets() {
        assertEquals(4, ResourceMaterial.entries.size)
    }

    @Test
    fun everyMaterialCarriesTheDesignsLabelInBothLanguages() {
        assertEquals("Hueso", ResourceMaterial.BONE.names.inLanguage(Language.ES))
        assertEquals("Bone", ResourceMaterial.BONE.names.inLanguage(Language.EN))

        assertEquals("Órgano", ResourceMaterial.ORGAN.names.inLanguage(Language.ES))
        assertEquals("Organ", ResourceMaterial.ORGAN.names.inLanguage(Language.EN))

        assertEquals("Pellejo", ResourceMaterial.HIDE.names.inLanguage(Language.ES))
        assertEquals("Hide", ResourceMaterial.HIDE.names.inLanguage(Language.EN))

        assertEquals("Chatarra", ResourceMaterial.SCRAP.names.inLanguage(Language.ES))
        assertEquals("Scrap", ResourceMaterial.SCRAP.names.inLanguage(Language.EN))
    }
}

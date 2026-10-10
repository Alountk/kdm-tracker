package io.kdmtracker.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizedTextTest {
    @Test
    fun spanishLanguageReturnsTheSpanishName() {
        val text = LocalizedText(es = "Daga de Hueso", en = "Bone Dagger")

        assertEquals("Daga de Hueso", text.inLanguage(Language.ES))
    }

    @Test
    fun englishLanguageReturnsTheEnglishName() {
        val text = LocalizedText(es = "Daga de Hueso", en = "Bone Dagger")

        assertEquals("Bone Dagger", text.inLanguage(Language.EN))
    }

    @Test
    fun englishLanguageFallsBackToSpanishWhenEnglishIsMissing() {
        val text = LocalizedText(es = "Hueso de Monstruo")

        assertEquals("Hueso de Monstruo", text.inLanguage(Language.EN))
    }
}

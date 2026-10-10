package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * A content name in every supported language.
 *
 * Two named fields rather than a map: the project has exactly two languages, and the fallback is
 * then explicit and testable instead of an implicit lookup miss.
 *
 * @property es the Spanish name. Always present — the content packs are transcribed in Spanish,
 *   so it is the one name every entity is guaranteed to have.
 * @property en the English name, absent until that entity is translated. The catalog grows it
 *   incrementally, starting with the entities the screens actually show.
 */
@Serializable
data class LocalizedText(
    val es: String,
    val en: String? = null,
) {
    /**
     * The name in [language], falling back to Spanish when there is no English one.
     *
     * Never returns an empty string or an id: [es] is non-null, so `EN` on an untranslated entity
     * yields the Spanish name rather than a blank label.
     */
    fun inLanguage(language: Language): String =
        when (language) {
            Language.ES -> es
            Language.EN -> en ?: es
        }
}

package io.kdmtracker.core.domain

/**
 * The language a user-facing content name is rendered in.
 *
 * Spanish is the default: it is the language the content packs are transcribed in, and the only
 * one that is complete. A name that has no English translation falls back to its Spanish one.
 */
enum class Language {
    ES,
    EN,
}

package io.kdmtracker.core.domain

/**
 * A principle story event's category.
 *
 * The vocabulary is open in spirit: packs may introduce further principles, and
 * `CONVICTION` (the Era 12 principle) ships here so the base set is complete.
 */
enum class PrincipleCategory {
    DEATH,
    NEW_LIFE,
    SOCIETY,
    CONVICTION,
}

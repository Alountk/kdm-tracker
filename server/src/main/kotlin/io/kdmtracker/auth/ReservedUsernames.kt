package io.kdmtracker.auth

/**
 * Usernames the server reserves for internal accounts. Registration rejects these so a real user
 * cannot claim a name the server later reuses as an owner (e.g. the legacy migration account).
 */
object ReservedUsernames {
    /** Owner of the deterministic legacy-migration campaign (see `LegacyToChangeLogMigration`). */
    const val LEGACY_MIGRATION = "legacy-migration"

    /** Every reserved username, for registration-time rejection. */
    val ALL: Set<String> = setOf(LEGACY_MIGRATION)
}

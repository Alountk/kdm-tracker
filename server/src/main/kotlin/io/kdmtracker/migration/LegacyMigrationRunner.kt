package io.kdmtracker.migration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Startup invocation path for the one-shot [LegacyToChangeLogMigration]. It is deliberately gated by
 * `sync.migration.run-on-startup` (default `false`) so ordinary boots — including the test suite and
 * fresh deployments — never migrate silently. The production runbook sets
 * `SYNC_MIGRATION_RUN_ON_STARTUP=true` for a single boot, then removes it.
 */
@Component
class LegacyMigrationRunner(
    private val migration: LegacyToChangeLogMigration,
    @Value("\${sync.migration.run-on-startup:false}") private val runOnStartup: Boolean,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (runOnStartup) {
            migration.migrate()
        }
    }
}

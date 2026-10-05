package io.kdmtracker.core.domain

import io.kdmtracker.core.ruleset.RulesetOverrides

/**
 * A campaign groups the settlements played under a single ruleset.
 *
 * @property rulesetId the identifier of the base [io.kdmtracker.core.ruleset.Ruleset] in effect.
 * @property rulesetOverrides per-campaign adjustments applied on top of the base ruleset.
 * @property settlementIds the settlements belonging to this campaign, in no particular order.
 */
data class Campaign(
    val id: Ulid,
    val name: String,
    val rulesetId: String,
    val rulesetOverrides: RulesetOverrides,
    val settlementIds: List<Ulid>,
    val sync: SyncMeta,
)

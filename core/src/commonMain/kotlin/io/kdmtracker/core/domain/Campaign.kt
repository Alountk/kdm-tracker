package io.kdmtracker.core.domain

import io.kdmtracker.core.content.MonsterNode
import io.kdmtracker.core.ruleset.RulesetOverrides

/**
 * A campaign groups the settlements played under a single ruleset.
 *
 * @property rulesetId the identifier of the base [io.kdmtracker.core.ruleset.Ruleset] in effect.
 * @property rulesetOverrides per-campaign adjustments applied on top of the base ruleset.
 * @property settlementIds the settlements belonging to this campaign, in no particular order.
 * @property enabledPacks the ids of the [io.kdmtracker.core.content.ContentPack]s selected for this
 *   campaign. Defaults to empty: content ingestion selects the packs later.
 * @property nodeChoices the monster the players picked for a node that several enabled packs contest. A node
 *   with no entry falls to the monster of the latest pack.
 */
data class Campaign(
    val id: Ulid,
    val name: String,
    val rulesetId: String,
    val rulesetOverrides: RulesetOverrides,
    val settlementIds: List<Ulid>,
    val sync: SyncMeta,
    val enabledPacks: List<String> = emptyList(),
    val nodeChoices: Map<MonsterNode, String> = emptyMap(),
)

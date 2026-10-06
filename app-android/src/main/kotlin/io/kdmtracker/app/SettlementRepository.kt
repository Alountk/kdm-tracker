package io.kdmtracker.app

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.CampaignSnapshot
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.persistence.SyncClock
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.sync.CampaignBadge
import io.kdmtracker.core.sync.CampaignBadges
import io.kdmtracker.core.sync.CampaignBanner
import io.kdmtracker.core.sync.CampaignSummary
import io.kdmtracker.core.sync.MembershipStore
import kotlin.random.Random

/**
 * One row of the Home campaign list: the campaign's settlement headline joined with its sharing
 * [banner].
 *
 * [name]/[lanternYear]/[population] come from the stored settlement (the campaign's live state);
 * [banner] is derived from the cached membership, falling back to an owned banner when the
 * membership has not been synced yet.
 */
data class CampaignCard(
    val campaignId: String,
    val name: String,
    val lanternYear: Int,
    val population: Int,
    val banner: CampaignBanner,
)

/**
 * Holder of the campaign and settlement the UI currently renders, backed by a [CampaignStore].
 *
 * On construction it loads the first stored snapshot. If the store is empty — or a campaign exists
 * with no live settlement, which can only result from an interrupted seed — it seeds a single
 * "Walking Skeleton" campaign and its first settlement atomically through the store. [update]
 * stamps the incoming settlement with a fresh [SyncClock] value and an incremented revision before
 * writing, so last-write-wins has an ordering signal. No rules live here; the controller drives the
 * loop.
 *
 * The held [campaign]/[settlement] can be swapped by [select] (Ruling AJ: selecting a campaign from
 * Home re-scopes the cockpit) and by [create] (which opens the new campaign). [memberships] is the
 * optional offline membership cache used to badge [listCards] and to record the caller's `OWNER`
 * membership on create; when omitted (a test that does not exercise sharing) every campaign is
 * badged as owned.
 *
 * [createCampaign] is the hub round-trip [create] calls to obtain a server-owned campaign id. It is
 * injected (in production, `HttpSyncApi::createCampaign`) so the repository stays JVM-testable; the
 * client never mints an id it intends to sync, because the change-log endpoints only accept
 * server-issued campaign ids.
 */
class SettlementRepository(
    private val store: CampaignStore,
    private val clock: SyncClock = SyncClock.system(),
    private val memberships: MembershipStore? = null,
    private val createCampaign: (String) -> CampaignSummary = { name ->
        throw IllegalStateException("Cannot create campaign '$name': no sync hub is configured")
    },
) {
    var campaign: Campaign
        private set

    var settlement: Settlement
        private set

    init {
        val existing = store.loadSnapshots().firstOrNull()
        val snapshot = if (existing == null || existing.settlements.isEmpty()) seed() else existing
        campaign = snapshot.campaign
        settlement = snapshot.settlements.first()
    }

    /**
     * Every stored campaign as a Home card, ordered as the store returns them.
     *
     * A card needs a live settlement (its year and population); a campaign whose settlement is
     * missing is skipped rather than rendered with placeholder numbers. The badge is the stored
     * membership's [CampaignBadges] banner, defaulting to `OWNED`/`"Tuyo"` for a campaign whose
     * membership has not been synced yet.
     */
    fun listCards(): List<CampaignCard> =
        store.loadSnapshots().mapNotNull { snapshot ->
            val settlement = snapshot.settlements.firstOrNull() ?: return@mapNotNull null
            CampaignCard(
                campaignId = snapshot.campaign.id.value,
                name = settlement.name,
                lanternYear = settlement.lanternYear,
                population = settlement.population,
                banner = bannerFor(snapshot.campaign.id.value),
            )
        }

    /**
     * Swaps the held campaign and settlement to [campaignId]'s, so the cockpit reads that campaign
     * only.
     *
     * Returns `false` and changes nothing when no stored campaign has that id, or when it has no
     * live settlement to open. Campaigns are isolated by id: the previously held campaign's rows and
     * sync cursor are never touched (Review Focus 3).
     */
    fun select(campaignId: String): Boolean {
        val snapshot = store.loadSnapshots().firstOrNull { it.campaign.id.value == campaignId } ?: return false
        val selected = snapshot.settlements.firstOrNull() ?: return false
        campaign = snapshot.campaign
        settlement = selected
        return true
    }

    /**
     * Creates a campaign named [name] on the hub, then persists and opens its local state.
     *
     * The hub round-trip happens **first**: it owns campaign-id generation, so only the id it
     * returns is accepted by the change-log endpoints. An offline or unauthenticated create throws
     * before any write, so a failure leaves no half-created local campaign behind (Review Focus 4).
     * The returned [CampaignSummary] carries the fresh `OWNER` role, which is cached as the local
     * membership.
     *
     * The campaign, settlement and membership are written inside one [CampaignStore.transaction], so
     * a store failure cannot leave a campaign row with no settlement (or an unbadged campaign)
     * behind. The returned card reflects the freshly created state; the campaign is also listed by
     * [listCards] from now on.
     */
    fun create(name: String): CampaignCard {
        val summary = createCampaign(name)
        val campaignId = Ulid.parse(summary.id)
        val settlementId = Ulid.generate(clock.tick(null).millis) { Random.nextInt() }
        val campaign =
            Campaign(
                id = campaignId,
                name = summary.name,
                rulesetId = Core16Ruleset.id,
                rulesetOverrides = RulesetOverrides(),
                settlementIds = listOf(settlementId),
                sync = SyncMeta(clock.tick(null), rev = 1),
            )
        val settlement =
            Settlement
                .new(id = settlementId, campaignId = campaignId, name = summary.name)
                .copy(sync = SyncMeta(clock.tick(null), rev = 1))
        store.transaction {
            store.upsertCampaign(campaign)
            store.upsertSettlement(settlement)
            memberships?.upsertMembership(summary)
        }
        this.campaign = campaign
        this.settlement = settlement
        return CampaignCard(
            campaignId = campaignId.value,
            name = settlement.name,
            lanternYear = settlement.lanternYear,
            population = settlement.population,
            banner = CampaignBadges.of(summary),
        )
    }

    /**
     * Re-reads the canonical snapshot from the store and refreshes the held settlement.
     *
     * Called after a sync pass applies remote changes (Ruling AE). Without it the repository keeps
     * the pre-sync settlement, the UI never shows the remote edit, and the next local mutation
     * writes the stale value back over the already-advanced remote row. Only the **selected**
     * campaign is refreshed, so a sync of another campaign cannot yank the cockpit to a different
     * settlement.
     */
    fun reload() {
        val selectedId = campaign.id.value
        val snapshot = store.loadSnapshots().firstOrNull { it.campaign.id.value == selectedId } ?: return
        campaign = snapshot.campaign
        settlement = snapshot.settlements.firstOrNull() ?: return
    }

    fun update(next: Settlement) {
        val previous = settlement.sync
        val stamped =
            next.copy(
                sync =
                    next.sync.copy(
                        updatedAt = clock.tick(previous.updatedAt),
                        rev = previous.rev + 1,
                    ),
            )
        store.upsertSettlement(stamped)
        settlement = stamped
    }

    private fun bannerFor(campaignId: String): CampaignBanner =
        memberships?.membership(campaignId)?.let { CampaignBadges.of(it) }
            ?: CampaignBanner(CampaignBadge.OWNED, "Tuyo")

    /**
     * Writes the sample campaign and settlement in one transaction and returns them as the initial
     * snapshot.
     *
     * The campaign and its settlement commit together, so a crash cannot leave a campaign row with
     * no settlement behind. A later construction that finds such an orphan reseeds over it rather
     * than crashing.
     */
    private fun seed(): CampaignSnapshot {
        val campaignId = Ulid.generate(0L) { 0 }
        val settlementId = Ulid.generate(1L) { 1 }
        val campaign =
            Campaign(
                id = campaignId,
                name = "Walking Skeleton",
                rulesetId = Core16Ruleset.id,
                rulesetOverrides = RulesetOverrides(),
                settlementIds = listOf(settlementId),
                sync = SyncMeta(clock.tick(null), rev = 1),
            )
        val settlement =
            Settlement
                .new(id = settlementId, campaignId = campaignId, name = "New Settlement")
                .copy(sync = SyncMeta(clock.tick(null), rev = 1))
        store.transaction {
            store.upsertCampaign(campaign)
            store.upsertSettlement(settlement)
        }
        return CampaignSnapshot(campaign, listOf(settlement))
    }
}

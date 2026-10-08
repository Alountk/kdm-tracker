package io.kdmtracker.app

import io.kdmtracker.core.content.CatalogResult
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentCatalogComposer
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.content.Core16Content
import io.kdmtracker.core.content.Legacy15Content
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
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
 * stamps the incoming record's settlement with a fresh [SyncClock] value and an incremented
 * revision before writing, so last-write-wins has an ordering signal. No rules live here; the
 * controller drives the loop.
 *
 * The held [campaign]/[record] can be swapped by [select] (Ruling AJ: selecting a campaign from
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

    /**
     * The canonical held state: the settlement scalars together with its [SettlementState].
     *
     * [settlement] is the read-only compatibility accessor for callers that only need the scalars
     * (the controller and the existing loop tests); the state is read and written through [record]
     * and the [update] overloads.
     */
    var record: SettlementRecord
        private set

    /** The held settlement scalars; a compatibility view over [record]'s settlement. */
    val settlement: Settlement get() = record.settlement

    /** The shipped packs this app knows how to resolve, keyed by their ids. */
    private val knownPacks: Map<String, ContentPack> =
        listOf(Core16Content.pack, Legacy15Content.pack).associateBy { it.id }

    /**
     * The catalog composed from the held campaign's [Campaign.enabledPacks].
     *
     * Known pack ids (`core-1.6`, `core-1.5`) resolve to their shipped [ContentPack]s; the
     * shipped default seed enables no packs, which composes an empty catalog. An unknown id,
     * or a pack whose requirement is not enabled, does not crash: [catalogError] carries the
     * reason and [catalog] falls back to an empty catalog, so the screens render nothing
     * rather than the app failing to load.
     */
    val catalog: ContentCatalog
        get() = (catalogResult as? CatalogResult.Ok)?.catalog ?: EMPTY_CATALOG

    /** Why [catalog] is empty, or `null` when it composed cleanly. */
    val catalogError: String?
        get() {
            val missing = catalogResult as? CatalogResult.MissingRequirement ?: return null
            return if (missing.packId == missing.missing) {
                "Cannot compose the campaign catalog: unknown content pack '${missing.packId}'"
            } else {
                "Cannot compose the campaign catalog: content pack '${missing.packId}' requires '${missing.missing}'"
            }
        }

    /**
     * Resolves the held campaign's enabled packs and composes them.
     *
     * An unknown id short-circuits to a [CatalogResult.MissingRequirement] (there is no pack to
     * compose), otherwise the composition reports a genuine missing requirement or `Ok`.
     */
    private val catalogResult: CatalogResult
        get() {
            val enabled = campaign.enabledPacks
            val unknown = enabled.firstOrNull { it !in knownPacks }
            return if (unknown != null) {
                CatalogResult.MissingRequirement(packId = unknown, missing = unknown)
            } else {
                ContentCatalogComposer.compose(enabled.map(knownPacks::getValue))
            }
        }

    init {
        val existing = store.loadSnapshots().firstOrNull()
        val snapshot = if (existing == null || existing.settlements.isEmpty()) seed() else existing
        campaign = snapshot.campaign
        record = storedRecord(snapshot.campaign.id.value, snapshot.settlements.first())
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
        record = storedRecord(campaignId, selected)
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
                enabledPacks = listOf(Core16Content.pack.id, Legacy15Content.pack.id),
            )
        val settlement =
            Settlement
                .new(id = settlementId, campaignId = campaignId, name = summary.name)
                .copy(sync = SyncMeta(clock.tick(null), rev = 1))
        store.transaction {
            store.upsertCampaign(campaign)
            store.upsertSettlement(startingRecord(settlement))
            memberships?.upsertMembership(summary)
        }
        this.campaign = campaign
        this.record = startingRecord(settlement)
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
        val selected = snapshot.settlements.firstOrNull() ?: return
        record = storedRecord(selectedId, selected)
    }

    /**
     * Writes [next] as the canonical record, stamping a fresh sync first.
     *
     * The record's settlement is stamped with `updatedAt` from `clock.tick(previous.updatedAt)` and
     * `rev = previous.rev + 1`, where `previous` is the currently held record's sync, so
     * last-write-wins has an ordering signal. The record's [SettlementState] is written alongside
     * the scalars, so a state-carrying update is never lost.
     */
    fun update(next: SettlementRecord) {
        val previous = record.settlement.sync
        val stamped =
            next.copy(
                settlement =
                    next.settlement.copy(
                        sync =
                            next.settlement.sync.copy(
                                updatedAt = clock.tick(previous.updatedAt),
                                rev = previous.rev + 1,
                            ),
                    ),
            )
        store.upsertSettlement(stamped)
        record = stamped
    }

    /**
     * Compatibility overload: updates the settlement scalars, preserving the held record's state.
     *
     * Delegates to [update] with the current [record] and only [next] swapped in, so a caller that
     * only knows the settlement (the controller) cannot clobber the stored state.
     */
    fun update(next: Settlement) {
        update(record.copy(settlement = next))
    }

    /**
     * The state-carrying record for [fallback] as stored under [campaignId].
     *
     * Reads the canonical `loadRecords` path; if a store somehow lists a settlement but returns no
     * record for it, falls back to an empty-state record over the scalar settlement rather than
     * dropping the settlement.
     */
    private fun storedRecord(
        campaignId: String,
        fallback: Settlement,
    ): SettlementRecord =
        store.loadRecords(campaignId).firstOrNull { it.settlement.id == fallback.id }
            ?: SettlementRecord(fallback)

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
                enabledPacks = listOf(Core16Content.pack.id, Legacy15Content.pack.id),
            )
        val settlement =
            Settlement
                .new(id = settlementId, campaignId = campaignId, name = "New Settlement")
                .copy(sync = SyncMeta(clock.tick(null), rev = 1))
        store.transaction {
            store.upsertCampaign(campaign)
            store.upsertSettlement(startingRecord(settlement))
        }
        return CampaignSnapshot(campaign, listOf(settlement))
    }

    /**
     * The opening [SettlementRecord] for a new settlement: its starting locations are already built.
     *
     * A settlement opens with the Lantern Hoard and the Armería built, so Innovate and Craft are
     * offered at locations the settlement actually has (both use-cases refuse an unbuilt location).
     * Without this the develop economy is unreachable — the Step 7 actions would all be `Blocked`.
     * The Armería carries the curated 1.5 crafting recipes (see `Core15GearSeed`).
     */
    private fun startingRecord(settlement: Settlement): SettlementRecord =
        SettlementRecord(
            settlement = settlement,
            state =
                SettlementState(
                    locations = LocationSet(built = listOf(STARTING_LOCATION, CRAFT_LOCATION)),
                    innovations = InnovationCollection(STARTING_INNOVATIONS),
                ),
        )
}

/** The game's initial innovation(s), seeded so the unlock tree has a root to grow from. */
private val STARTING_INNOVATIONS: List<Innovation> =
    listOfNotNull(
        Core16Content.pack.bundle.innovations
            .firstOrNull { it.name == "Language" },
        Legacy15Content.pack.bundle.innovations
            .firstOrNull { it.name == "Lenguaje" },
    )

/** The location every settlement opens with, so Innovate is offered at a built location. */
private const val STARTING_LOCATION = "Lantern Hoard"

/** The location the curated 1.5 crafting recipes live on, built so Craft is reachable. */
private const val CRAFT_LOCATION = "Armería"

/** A catalog with no packs, used when composition fails so the screens render nothing. */
private val EMPTY_CATALOG = ContentCatalog(emptyList())

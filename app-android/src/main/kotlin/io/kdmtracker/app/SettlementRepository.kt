package io.kdmtracker.app

import io.kdmtracker.core.content.CatalogResult
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.PackRegistry
import io.kdmtracker.core.content.PackSelection
import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.persistence.CampaignSnapshot
import io.kdmtracker.core.persistence.CampaignStore
import io.kdmtracker.core.persistence.SyncClock
import io.kdmtracker.core.sync.CampaignBadge
import io.kdmtracker.core.sync.CampaignBadges
import io.kdmtracker.core.sync.CampaignBanner
import io.kdmtracker.core.sync.CampaignSummary
import io.kdmtracker.core.sync.MembershipStore
import kotlin.random.Random

/** One Home-list row: the campaign's settlement headline plus its sharing [banner]. */
data class CampaignCard(
    val campaignId: String,
    val name: String,
    val lanternYear: Int,
    val population: Int,
    val banner: CampaignBanner,
)

/**
 * Holds the campaign and settlement the UI renders, backed by a [CampaignStore].
 *
 * Seeds a sample campaign when the store is empty or holds a campaign with no settlement (an
 * interrupted seed). [memberships] is optional; without it every campaign is badged as owned.
 * [createCampaign] is the hub round-trip that returns a server-issued campaign id (the change-log
 * endpoints reject client-minted ids); it is injected to keep the repository JVM-testable.
 */
class SettlementRepository(
    private val store: CampaignStore,
    private val clock: SyncClock = SyncClock.system(),
    private val memberships: MembershipStore? = null,
    private val createCampaign: (String) -> CampaignSummary = { name ->
        throw IllegalStateException("Cannot create campaign '$name': no sync hub is configured")
    },
    private val packs: PackRegistry = PackRegistry.shipped,
) {
    var campaign: Campaign
        private set

    /** The canonical held state: the settlement scalars together with its [SettlementState]. */
    var record: SettlementRecord
        private set

    /** Scalars-only view of [record]'s settlement. */
    val settlement: Settlement get() = record.settlement

    /**
     * Composed from [Campaign.enabledPacks]. Falls back to an empty catalog when composition fails
     * (see [catalogError]) so the screens render nothing instead of the app failing to load.
     */
    val catalog: ContentCatalog
        get() = (PackCatalogs.compose(campaign.enabledPacks, packs) as? CatalogResult.Ok)?.catalog ?: PackCatalogs.EMPTY

    /** Why [catalog] is empty, or `null` when it composed cleanly. */
    val catalogError: String?
        get() = PackCatalogs.errorOf(PackCatalogs.compose(campaign.enabledPacks, packs))

    private val newCampaigns = NewCampaignFactory(clock)

    init {
        val existing = store.loadSnapshots().firstOrNull()
        val snapshot = if (existing == null || existing.settlements.isEmpty()) seed() else existing
        campaign = snapshot.campaign
        record = storedRecord(snapshot.campaign.id.value, snapshot.settlements.first())
    }

    /** Every stored campaign as a Home card; a campaign with no settlement is skipped. */
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
     * Swaps the held campaign and settlement to [campaignId]'s.
     *
     * Returns `false` and changes nothing if the id is unknown or has no settlement. The previously
     * held campaign's rows and sync cursor are never touched.
     */
    fun select(campaignId: String): Boolean {
        val snapshot = store.loadSnapshots().firstOrNull { it.campaign.id.value == campaignId } ?: return false
        val selected = snapshot.settlements.firstOrNull() ?: return false
        campaign = snapshot.campaign
        record = storedRecord(campaignId, selected)
        return true
    }

    /**
     * Creates [name] on the hub, then persists and opens it locally, with the base packs plus the
     * [expansions] the player picked.
     *
     * An expansion the registry refuses throws [IllegalArgumentException] before the hub is called, so
     * a rejected choice leaves nothing behind. The hub call otherwise runs first because it owns
     * campaign-id generation; an offline create throws
     * before any local write, so nothing is left half-created. Campaign, settlement and the `OWNER`
     * membership are written in one transaction so a store failure cannot orphan the campaign row. The
     * opening record is built once, so the held record and the stored one share their survivor ids.
     */
    fun create(
        name: String,
        expansions: Set<String> = emptySet(),
    ): CampaignCard {
        val enabled =
            when (val selection = packs.select(expansions)) {
                is PackSelection.Ok -> selection.enabled
                is PackSelection.Blocked -> throw IllegalArgumentException(selection.reason)
            }
        val summary = createCampaign(name)
        val campaignId = Ulid.parse(summary.id)
        val settlementId = Ulid.generate(clock.tick(null).millis) { Random.nextInt() }
        val (campaign, settlement) = newCampaigns.build(campaignId, settlementId, summary.name, summary.name, enabled)
        val opening = newCampaigns.startingRecord(settlement)
        store.transaction {
            store.upsertCampaign(campaign)
            store.upsertSettlement(opening)
            memberships?.upsertMembership(summary)
        }
        this.campaign = campaign
        this.record = opening
        return CampaignCard(
            campaignId = campaignId.value,
            name = settlement.name,
            lanternYear = settlement.lanternYear,
            population = settlement.population,
            banner = CampaignBadges.of(summary),
        )
    }

    /**
     * Re-reads the selected campaign from the store after a sync applied remote changes.
     *
     * Without it the next local mutation would write the stale pre-sync settlement over the newer
     * remote row. Other campaigns are ignored so a background sync cannot switch the cockpit.
     */
    fun reload() {
        val selectedId = campaign.id.value
        val snapshot = store.loadSnapshots().firstOrNull { it.campaign.id.value == selectedId } ?: return
        campaign = snapshot.campaign
        val selected = snapshot.settlements.firstOrNull() ?: return
        record = storedRecord(selectedId, selected)
    }

    /**
     * Writes [next], stamping `updatedAt` and `rev + 1` from the held record's sync so
     * last-write-wins has an ordering signal.
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

    /** Scalars-only overload: keeps the held record's state. */
    fun update(next: Settlement) {
        update(record.copy(settlement = next))
    }

    /** Falls back to an empty-state record if the store lists a settlement but has no record for it. */
    private fun storedRecord(
        campaignId: String,
        fallback: Settlement,
    ): SettlementRecord =
        (
            store.loadRecords(campaignId).firstOrNull { it.settlement.id == fallback.id }
                ?: SettlementRecord(fallback)
        ).migrated()

    /** A campaign whose membership has not been synced yet is shown as owned. */
    private fun bannerFor(campaignId: String): CampaignBanner =
        memberships?.membership(campaignId)?.let { CampaignBadges.of(it) }
            ?: CampaignBanner(CampaignBadge.OWNED, "Tuyo")

    /**
     * Writes the sample campaign and settlement in one transaction, so a crash cannot leave a
     * campaign without a settlement. Construction reseeds over such an orphan.
     */
    private fun seed(): CampaignSnapshot {
        val campaignId = Ulid.generate(0L) { 0 }
        val settlementId = Ulid.generate(1L) { 1 }
        val (campaign, settlement) =
            newCampaigns.build(
                campaignId,
                settlementId,
                "Walking Skeleton",
                "New Settlement",
                packs.baseIds,
            )
        store.transaction {
            store.upsertCampaign(campaign)
            store.upsertSettlement(newCampaigns.startingRecord(settlement))
        }
        return CampaignSnapshot(campaign, listOf(settlement))
    }
}

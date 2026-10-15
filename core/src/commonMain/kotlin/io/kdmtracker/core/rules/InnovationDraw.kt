package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.Innovation

/**
 * The cards the settlement is offered right now, **derived and never stored**.
 *
 * The whole point of the design is that there is no hand to persist: two devices reading the
 * same campaign must see the same offer, and the offer must not reshuffle while the user reads
 * it. That holds as long as [of] is a pure function of (campaign, classified cards), which is
 * what the ordering below is built to be.
 *
 * The cards arrive from [InnovationAccess.view] already classified, because that is the
 * boundary the screen actually holds — the view model carries the innovation views, never a
 * `ContentCatalog`.
 */
object InnovationDraw {
    /**
     * The up-to-[draw] cards [campaignId] would be offered: every [InnovationStatus.AVAILABLE]
     * view, ordered by [orderKey], then truncated. A null campaign, a deck with nothing
     * available and a non-positive [draw] all yield an empty hand rather than an error.
     */
    fun of(
        campaignId: String?,
        views: List<InnovationView>,
        draw: Int,
    ): List<Innovation> {
        if (campaignId == null || draw <= 0) return emptyList()
        return views
            .filter { it.status == InnovationStatus.AVAILABLE }
            .map { it.innovation }
            .sortedBy { orderKey(campaignId, it.id.value) }
            .take(draw)
    }

    /**
     * FNV-1a over `<campaign> | <card id>`, folded to 32 bits.
     *
     * Written by hand rather than leaning on `String.hashCode()` because the promise is
     * *cross-device* determinism: the byte-level algorithm is fixed by its specification,
     * while `hashCode` is whatever the running platform defines.
     */
    private fun orderKey(
        campaignId: String,
        innovationId: String,
    ): Int {
        // 2166136261, the FNV-1a 32-bit offset basis, in two's complement.
        var hash = -2128831035
        for (byte in (campaignId + '|' + innovationId).toByteArray(Charsets.UTF_8)) {
            hash = hash xor (byte.toInt() and 0xff)
            hash *= 16777619
        }
        return hash
    }
}

package io.kdmtracker.core.rules

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentIdentity
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationDeck
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.SettlementState

/** Where an innovation sits in the settlement's unlock tree. */
enum class InnovationStatus { UNLOCKED, AVAILABLE, NO_ACCESS }

/**
 * One innovation with its access state.
 *
 * @property missingParent the innovation that would unlock this one (its parent), when it is
 *   [InnovationStatus.NO_ACCESS]; `null` otherwise.
 * @property endeavorCost the cost of the Innovate action that unlocks it (content, not a UI
 *   literal); the same for every innovation.
 * @property resourceCost the resources the Innovate action needs, keyed by resource id.
 * @property used whether that action has already been used this Settlement Phase.
 */
data class InnovationView(
    val innovation: Innovation,
    val status: InnovationStatus,
    val missingParent: String? = null,
    val endeavorCost: Int = 0,
    val resourceCost: Map<ResourceId, Int> = emptyMap(),
    val used: Boolean = false,
)

/**
 * Classifies every catalog innovation for a settlement's unlock tree.
 *
 * An innovation's "parent" is the innovation that lists it among its consequences. The deck is the
 * consequences of the unlocked innovations ([InnovationDeck]), so: **UNLOCKED** when it is in the
 * collection, **AVAILABLE** when it is in the deck (its parent is unlocked), and **NO_ACCESS**
 * otherwise (with the parent it still needs). Read the same way as
 * `external-assets/arbol_de_innovaciones_kdm_1_5.md`.
 */
object InnovationAccess {
    fun view(
        state: SettlementState,
        catalog: ContentCatalog,
    ): List<InnovationView> {
        val unlocked =
            state.innovations.innovations
                .mapTo(mutableSetOf()) { it.id }
        // The deck holds consequence *names* (what the cards print); the identity table turns
        // each into an id so a Spanish deck string and an English innovation still match.
        val deck =
            InnovationDeck
                .of(state.innovations)
                .cards
                .mapNotNullTo(mutableSetOf()) { ContentIdentity.innovationIdFor(it) }
        val parentByChild = mutableMapOf<InnovationId, Innovation>()
        catalog.innovationDefinitions.forEach { innovation ->
            innovation.consequences.forEach { child ->
                ContentIdentity.innovationIdFor(child)?.let { childId ->
                    if (childId !in parentByChild) parentByChild[childId] = innovation
                }
            }
        }
        val hoard =
            catalog.locationDefinitions.firstOrNull { location ->
                location.actions.any { it.kind == ActionKind.INNOVATE }
            }
        val innovate = hoard?.actions?.firstOrNull { it.kind == ActionKind.INNOVATE }
        val used = hoard != null && state.usedInPhase(hoard.id.value)
        return catalog.innovationDefinitions
            .sortedBy { it.names.es }
            .map { innovation ->
                val status =
                    when {
                        innovation.id in unlocked -> InnovationStatus.UNLOCKED
                        innovation.id in deck -> InnovationStatus.AVAILABLE
                        else -> InnovationStatus.NO_ACCESS
                    }
                InnovationView(
                    innovation = innovation,
                    status = status,
                    missingParent =
                        if (status == InnovationStatus.NO_ACCESS) parentByChild[innovation.id]?.names?.es else null,
                    endeavorCost = innovate?.endeavorCost ?: 0,
                    resourceCost = innovate?.resourceCost ?: emptyMap(),
                    used = used,
                )
            }
    }
}

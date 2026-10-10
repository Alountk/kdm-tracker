package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/**
 * The kind of action a settlement location offers.
 *
 * The kind selects which rule use-case owns the action; it is content, never
 * inferred by the UI.
 */
enum class ActionKind {
    /** A generic action carrying the endeavor symbol; the group resolves its effect. */
    ENDEAVOR,

    /** The Lantern Hoard's Innovate action: spends the action's cost to unlock an innovation. */
    INNOVATE,
}

/**
 * An action a settlement location offers during the Settlement Phase.
 *
 * The action models only what the rules layer can enforce — its cost and its
 * per-phase limit. The [effect] is rules text shown to the player and resolved
 * by the group; this model never interprets it.
 *
 * The [kind] is the action's identity: a location offers at most one action of
 * each kind, and packs contributing the same action merge on it.
 *
 * @property names the action's localized display names.
 * @property kind the kind of action, selecting the owning use-case.
 * @property endeavorCost the number of endeavors the action costs.
 * @property resourceCost the resources the action consumes, as id → count.
 * @property oncePerPhase whether the action may be used only once per Settlement Phase.
 * @property draw the number of innovation cards the action draws, when [kind] is [ActionKind.INNOVATE].
 * @property keep the number of drawn cards the action keeps, when [kind] is [ActionKind.INNOVATE].
 * @property effect the rules text shown to the player; resolved by the group.
 */
@Serializable
data class SettlementAction(
    val names: LocalizedText,
    val kind: ActionKind = ActionKind.ENDEAVOR,
    val endeavorCost: Int = 1,
    val resourceCost: Map<ResourceId, Int> = emptyMap(),
    val oncePerPhase: Boolean = false,
    val draw: Int = 0,
    val keep: Int = 0,
    val effect: String = "",
)

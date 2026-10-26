package io.kdmtracker.app

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import io.kdmtracker.core.ruleset.Ruleset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The held state and publishing shared by the controller's action classes. */
internal class ControllerSession(
    val repository: SettlementRepository,
    val ruleset: Ruleset,
    private val catalog: ContentCatalog?,
    val newSurvivorId: () -> Ulid,
) {
    /** The injected [catalog] (tests) or the repository's composed one. */
    val content: ContentCatalog get() = catalog ?: repository.catalog

    private val mapper =
        UiStateMapper(ruleset, contentProvider = { content }, catalogError = { repository.catalogError })

    private val _state = MutableStateFlow(mapper.map(repository.record, lastTransition = null))

    val state: StateFlow<UiState> = _state.asStateFlow()

    fun publish(lastTransition: String?) {
        _state.value = mapper.map(repository.record, lastTransition)
    }

    fun republish() = publish(_state.value.lastTransition)

    fun publishBlocked(reason: String) {
        _state.value = _state.value.copy(actionError = reason)
    }

    /** Ok is persisted and republished; Blocked leaves the record untouched and surfaces its reason. */
    fun applyResult(result: StepResult<SettlementRecord>) {
        when (result) {
            is StepResult.Ok -> {
                repository.update(result.value)
                republish()
            }
            is StepResult.Blocked -> publishBlocked(result.reason)
        }
    }
}

/** Base of the per-feature action classes: gives them the session's state under short names. */
internal abstract class FeatureActions(
    protected val session: ControllerSession,
) {
    protected val repository: SettlementRepository get() = session.repository
    protected val ruleset: Ruleset get() = session.ruleset
    protected val content: ContentCatalog get() = session.content
    protected val newSurvivorId: () -> Ulid get() = session.newSurvivorId

    protected fun applyResult(result: StepResult<SettlementRecord>) = session.applyResult(result)

    protected fun publishBlocked(reason: String) = session.publishBlocked(reason)
}

/** The nemesis the current year's timeline lists, or `null`. */
internal fun SettlementRecord.currentNemesis(): String? = nemesisOf(settlement.lanternYear)

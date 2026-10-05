package io.kdmtracker.web

import io.kdmtracker.domain.Settlement
import io.kdmtracker.repository.SettlementRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class SettlementLocator(
    private val settlementRepository: SettlementRepository,
) {
    fun require(id: Long): Settlement =
        settlementRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement $id not found")
        }
}

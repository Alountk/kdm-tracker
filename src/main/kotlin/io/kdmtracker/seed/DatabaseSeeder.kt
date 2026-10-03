package io.kdmtracker.seed

import io.kdmtracker.domain.Resource
import io.kdmtracker.domain.ResourceCategory
import io.kdmtracker.domain.Settlement
import io.kdmtracker.repository.ResourceRepository
import io.kdmtracker.repository.SettlementRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Order(Ordered.LOWEST_PRECEDENCE)
@Component
class DatabaseSeeder(
    private val settlementRepository: SettlementRepository,
    private val resourceRepository: ResourceRepository,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (settlementRepository.count() == 0L) {
            settlementRepository.save(Settlement())
        }
        if (resourceRepository.count() == 0L) {
            val settlementId = settlementRepository.findAllByOrderByIdAsc().first().id!!
            val seeds =
                listOf(
                    Resource(name = "Bone", category = ResourceCategory.BASIC),
                    Resource(name = "Hide", category = ResourceCategory.BASIC),
                    Resource(name = "Sinew", category = ResourceCategory.BASIC),
                    Resource(name = "Amber", category = ResourceCategory.SPECIAL),
                    Resource(name = "Cloth", category = ResourceCategory.SPECIAL),
                    Resource(name = "Iron", category = ResourceCategory.SPECIAL),
                    Resource(name = "Pigment", category = ResourceCategory.SPECIAL),
                    Resource(name = "Scrap Iron", category = ResourceCategory.SPECIAL),
                )
            seeds.forEach {
                it.settlementId = settlementId
                resourceRepository.save(it)
            }
        }
    }
}

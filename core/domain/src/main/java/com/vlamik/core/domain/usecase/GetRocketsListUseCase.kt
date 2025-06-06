package com.vlamik.core.domain.usecase

import com.vlamik.core.domain.models.RocketListItemModel
import com.vlamik.core.domain.repository.RocketsRepository

class GetRocketsListUseCase(
    private val rocketsRepository: RocketsRepository
) {
    suspend operator fun invoke(refresh: Boolean = false): Result<List<RocketListItemModel>> {
        return rocketsRepository.getRocketsList(refresh)
    }
}

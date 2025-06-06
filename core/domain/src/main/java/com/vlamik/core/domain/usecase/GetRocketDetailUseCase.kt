package com.vlamik.core.domain.usecase

import com.vlamik.core.domain.models.RocketDetailModel
import com.vlamik.core.domain.repository.RocketsRepository

class GetRocketDetailUseCase(
    private val rocketsRepository: RocketsRepository
) {
    suspend operator fun invoke(id: String): Result<RocketDetailModel> {
        return rocketsRepository.getRocketDetail(id)
    }
}

package com.vlamik.core.domain.usecase

import com.vlamik.core.domain.models.CrewListItemModel
import com.vlamik.core.domain.repository.CrewRepository

class GetCrewListUseCase(
    private val crewRepository: CrewRepository
) {
    suspend operator fun invoke(): Result<List<CrewListItemModel>> {
        return crewRepository.getCrew()
    }
}

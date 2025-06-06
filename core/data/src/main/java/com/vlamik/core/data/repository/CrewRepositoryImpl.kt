package com.vlamik.core.data.repository

import com.vlamik.core.data.models.toCrewListItemModel
import com.vlamik.core.data.network.CrewApi
import com.vlamik.core.domain.models.CrewListItemModel
import com.vlamik.core.domain.repository.CrewRepository
import javax.inject.Inject

class CrewRepositoryImpl @Inject constructor(
    private val crewApi: CrewApi,
) : CrewRepository {
    override suspend fun getCrew(): Result<List<CrewListItemModel>> =
        crewApi.getCrew().map { crewList ->
            crewList.map { crew -> crew.toCrewListItemModel() }
        }
}
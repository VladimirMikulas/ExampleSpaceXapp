package com.vlamik.core.domain.repository

import com.vlamik.core.domain.models.RocketDetailModel
import com.vlamik.core.domain.models.RocketListItemModel

interface RocketsRepository {
    suspend fun getRocketsList(refresh: Boolean): Result<List<RocketListItemModel>>
    suspend fun getRocketDetail(id: String): Result<RocketDetailModel>
}
package com.vlamik.core.domain.repository

import com.vlamik.core.domain.models.CrewListItemModel


interface CrewRepository {
    suspend fun getCrew(): Result<List<CrewListItemModel>>
}
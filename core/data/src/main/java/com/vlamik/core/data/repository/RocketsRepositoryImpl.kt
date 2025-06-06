package com.vlamik.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vlamik.core.data.models.toRocketDetailModel
import com.vlamik.core.data.models.toRocketListItemModel
import com.vlamik.core.data.network.RocketsApi
import com.vlamik.core.domain.models.RocketDetailModel
import com.vlamik.core.domain.models.RocketListItemModel
import com.vlamik.core.domain.repository.RocketsRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class RocketsRepositoryImpl
@Inject constructor(
    private val rocketsApi: RocketsApi,
    private val dataStore: DataStore<Preferences>
) : RocketsRepository {
    override suspend fun getRocketsList(refresh: Boolean): Result<List<RocketListItemModel>> {
        val cachedRockets = getCachedRockets()
        if (cachedRockets.isNotEmpty() && !refresh) {
            return Result.success(cachedRockets)
        }
        val rocketsResult = rocketsApi.getRockets().map { rocketList ->
            rocketList.map { rocket -> rocket.toRocketListItemModel() }
        }
        rocketsResult.onSuccess {
            saveRocketsToCache(it)
        }
        return rocketsResult
    }

    private suspend fun getCachedRockets(): List<RocketListItemModel> {
        val jsonString = dataStore.data
            .map { it[rocketCacheKey] }
            .firstOrNull() ?: return emptyList()

        return try {
            json.decodeFromString<List<RocketListItemModel>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveRocketsToCache(rockets: List<RocketListItemModel>) {
        dataStore.edit { preferences ->
            preferences[rocketCacheKey] = json.encodeToString(rockets)
        }
    }

    override suspend fun getRocketDetail(id: String): Result<RocketDetailModel> =
        rocketsApi.getRocket(id).map { rocket ->
            rocket.toRocketDetailModel()
        }

    companion object {
        private val rocketCacheKey = stringPreferencesKey("LAST_ROCKET_DATA")
        private val json = Json { ignoreUnknownKeys = true }
    }
}

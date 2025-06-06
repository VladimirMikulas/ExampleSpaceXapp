package com.vlamik.spacex.di

import com.vlamik.core.data.repository.AppRepositoryImpl
import com.vlamik.core.data.repository.CrewRepositoryImpl
import com.vlamik.core.data.repository.RocketsRepositoryImpl
import com.vlamik.core.domain.usecase.AppSettingsUseCase
import com.vlamik.core.domain.usecase.ApplyRocketsFiltersUseCase
import com.vlamik.core.domain.usecase.ApplyRocketsSearchUseCase
import com.vlamik.core.domain.usecase.GetCrewListUseCase
import com.vlamik.core.domain.usecase.GetRocketDetailUseCase
import com.vlamik.core.domain.usecase.GetRocketsListUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
open class UseCaseModule {

    @Provides
    @Singleton
    fun providesAppSettingUseCase(
        repo: AppRepositoryImpl
    ): AppSettingsUseCase {
        return AppSettingsUseCase(repo)
    }

    @Provides
    @Singleton
    fun providesGetRocketDetailUseCase(
        repo: RocketsRepositoryImpl
    ): GetRocketDetailUseCase {
        return GetRocketDetailUseCase(repo)
    }

    @Provides
    @Singleton
    fun providesGetRocketsListUseCase(
        repo: RocketsRepositoryImpl
    ): GetRocketsListUseCase {
        return GetRocketsListUseCase(repo)
    }

    @Provides
    @Singleton
    fun providesGetCrewListUseCase(
        repo: CrewRepositoryImpl
    ): GetCrewListUseCase {
        return GetCrewListUseCase(repo)
    }

    @Provides
    @Singleton
    fun providesApplyRocketsFiltersUseCase(): ApplyRocketsFiltersUseCase {
        return ApplyRocketsFiltersUseCase()
    }

    @Provides
    @Singleton
    fun providesApplyRocketsSearchUseCase(): ApplyRocketsSearchUseCase {
        return ApplyRocketsSearchUseCase()
    }
}

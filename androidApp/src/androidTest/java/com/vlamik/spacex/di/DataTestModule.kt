package com.vlamik.spacex.di

import com.vlamik.datatest.MockOpenLibraryApi
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class]
)
class DataTestModule : DataModule() {

    override val baseUrl = super.baseUrl

    override fun internalHttpClientEngine(): HttpClientEngineFactory<*> =
        object : HttpClientEngineFactory<HttpClientEngineConfig> {
            override fun create(block: HttpClientEngineConfig.() -> Unit) =
                MockOpenLibraryApi.engine
        }
}

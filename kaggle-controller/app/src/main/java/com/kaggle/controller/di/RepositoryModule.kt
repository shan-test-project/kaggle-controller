package com.kaggle.controller.di

import com.kaggle.controller.data.remote.api.KaggleApiService
import com.kaggle.controller.data.repository.KaggleRepositoryImpl
import com.kaggle.controller.domain.repository.KaggleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideKaggleRepository(
        api: KaggleApiService,
        prefs: com.kaggle.controller.data.local.AppPreferences,
    ): KaggleRepository {
        return KaggleRepositoryImpl(api, prefs)
    }
}

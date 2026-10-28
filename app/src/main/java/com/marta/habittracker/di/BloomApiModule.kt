package com.marta.habittracker.di

import com.marta.habittracker.BuildConfig
import com.marta.habittracker.data.remote.BloomRecommendationDataSource
import com.marta.habittracker.data.remote.KtorRecommendationTransport
import com.marta.habittracker.data.repository.RecommendationRepositoryImpl
import com.marta.habittracker.domain.repository.RecommendationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BloomApiModule {

    @Provides
    @Singleton
    fun provideBloomRecommendationDataSource(): BloomRecommendationDataSource {
        return BloomRecommendationDataSource(
            baseUrl = BuildConfig.BLOOM_API_BASE_URL,
            transport = KtorRecommendationTransport(),
        )
    }

    @Provides
    @Singleton
    fun provideRecommendationRepository(
        dataSource: BloomRecommendationDataSource,
    ): RecommendationRepository {
        return RecommendationRepositoryImpl(dataSource)
    }
}

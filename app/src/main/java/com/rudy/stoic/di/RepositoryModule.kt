package com.rudy.stoic.di

import com.rudy.stoic.data.repository.AppRepositoryImpl
import com.rudy.stoic.data.repository.QuickSettingsRepositoryImpl
import com.rudy.stoic.domain.repository.AppRepository
import com.rudy.stoic.domain.repository.QuickSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository

    @Binds
    @Singleton
    abstract fun bindQuickSettingsRepository(impl: QuickSettingsRepositoryImpl): QuickSettingsRepository
}

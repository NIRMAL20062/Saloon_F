package com.glide.android.data

import com.glide.android.data.health.HealthRepository
import com.glide.android.data.health.NetworkHealthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds repository interfaces to their implementations. Tests replace this module with fakes. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun healthRepository(impl: NetworkHealthRepository): HealthRepository
}

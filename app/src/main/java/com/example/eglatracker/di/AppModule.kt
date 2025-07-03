package com.example.eglatracker.di

import android.content.Context
import com.egla.location.client.EGLALocationClient
import com.example.eglatracker.utils.DatabaseLogger
import com.example.eglatracker.utils.LoggingManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEGLALocationClient(@ApplicationContext context: Context): EGLALocationClient =
        EGLALocationClient(context)

    @Provides
    @Singleton
    fun provideDatabaseLogger(@ApplicationContext context: Context): DatabaseLogger =
        DatabaseLogger(context)

    @Provides
    @Singleton
    fun provideLoggingManager(): LoggingManager = LoggingManager.getInstance()
}
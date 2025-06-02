package com.nhlstenden.appdev.core.di

import com.nhlstenden.appdev.supabase.SupabaseClient
import com.nhlstenden.appdev.core.repositories.ProfileRepository
import com.nhlstenden.appdev.features.profile.repositories.ProfileRepositoryImpl
import com.nhlstenden.appdev.features.courses.repositories.CourseRepository
import com.nhlstenden.appdev.features.courses.repositories.CourseRepositoryImpl
import com.nhlstenden.appdev.core.repositories.TaskRepository
import com.nhlstenden.appdev.features.task.repositories.TaskRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository

    @Binds
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
}

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseClient()
} 
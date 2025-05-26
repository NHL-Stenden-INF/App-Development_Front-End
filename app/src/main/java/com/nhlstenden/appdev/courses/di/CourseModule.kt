package com.nhlstenden.appdev.courses.di

import com.nhlstenden.appdev.courses.data.repositories.CourseRepositoryImpl
import com.nhlstenden.appdev.courses.domain.repositories.CourseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CourseModule {
    @Binds
    @Singleton
    abstract fun bindCourseRepository(
        courseRepositoryImpl: CourseRepositoryImpl
    ): CourseRepository
} 
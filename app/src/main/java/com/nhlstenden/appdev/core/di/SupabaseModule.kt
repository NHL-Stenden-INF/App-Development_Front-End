package com.nhlstenden.appdev.core.di

import android.content.Context
import com.nhlstenden.appdev.core.repositories.ProfileRepository
import com.nhlstenden.appdev.features.profile.repositories.ProfileRepositoryImpl
import com.nhlstenden.appdev.features.course.repositories.CourseRepository
import com.nhlstenden.appdev.features.course.repositories.CourseRepositoryImpl as CourseRepoImpl
import com.nhlstenden.appdev.features.courses.repositories.CourseRepositoryImpl as CoursesRepoImpl
import com.nhlstenden.appdev.features.courses.repositories.CoursesRepository
import com.nhlstenden.appdev.features.course.utils.CourseParser as CourseParserImpl
import com.nhlstenden.appdev.features.course.utils.TaskParser as TaskParserImpl
import com.nhlstenden.appdev.core.parsers.CourseParser
import com.nhlstenden.appdev.core.parsers.TaskParser
import com.nhlstenden.appdev.features.course.utils.QuestionParser
import com.nhlstenden.appdev.core.repositories.TaskRepository
import com.nhlstenden.appdev.features.task.repositories.TaskRepositoryImpl
import com.nhlstenden.appdev.core.repositories.FriendsRepository
import com.nhlstenden.appdev.features.friends.repositories.FriendsRepositoryImpl
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.features.auth.repositories.AuthRepositoryImpl
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.features.user.repositories.UserRepositoryImpl
import com.nhlstenden.appdev.core.repositories.RewardsRepository
import com.nhlstenden.appdev.core.repositories.SettingsRepository
import com.nhlstenden.appdev.features.profile.repositories.SettingsRepositoryImpl
import com.nhlstenden.appdev.features.rewards.repositories.RewardsRepositoryImpl
import com.nhlstenden.appdev.core.repositories.AchievementRepository
import com.nhlstenden.appdev.features.rewards.repositories.AchievementRepositoryImpl
import com.nhlstenden.appdev.supabase.*
import com.nhlstenden.appdev.core.services.MusicService
import com.nhlstenden.appdev.core.services.MusicServiceImpl
import com.nhlstenden.appdev.core.services.MusicManager
import com.nhlstenden.appdev.core.services.MusicManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindRewardsRepository(impl: RewardsRepositoryImpl): RewardsRepository

    @Binds
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    abstract fun bindCourseRepository(impl: CourseRepoImpl): CourseRepository

    @Binds
    abstract fun bindCoursesRepository(impl: CoursesRepoImpl): CoursesRepository

    @Binds
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    abstract fun bindFriendsRepository(impl: FriendsRepositoryImpl): FriendsRepository

    @Binds
    abstract fun bindAchievementRepository(impl: AchievementRepositoryImpl): AchievementRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindMusicService(impl: MusicServiceImpl): MusicService

    @Binds
    abstract fun bindMusicManager(impl: MusicManagerImpl): MusicManager

    @Binds
    abstract fun bindCourseParser(impl: CourseParserImpl): CourseParser

    @Binds
    abstract fun bindTaskParser(impl: TaskParserImpl): TaskParser
}

@Module
@InstallIn(SingletonComponent::class)
object ContextModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext appContext: Context): Context {
        return appContext
    }
}


@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseClient()

    @Provides
    @Singleton
    fun provideQuestionParser(@ApplicationContext context: Context): QuestionParser = QuestionParser(context)

    @Provides
    @Singleton
    fun provideStreakManager(): com.nhlstenden.appdev.features.home.StreakManager = com.nhlstenden.appdev.features.home.StreakManager()
} 
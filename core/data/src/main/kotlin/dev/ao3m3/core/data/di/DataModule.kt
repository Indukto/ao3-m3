package dev.ao3m3.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.ao3m3.core.data.db.Ao3Dao
import dev.ao3m3.core.data.db.Ao3Database
import dev.ao3m3.core.data.prefs.UserPrefs
import dev.ao3m3.core.data.prefs.userPrefsStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): Ao3Database =
        Room.databaseBuilder(context, Ao3Database::class.java, "ao3.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideDao(database: Ao3Database): Ao3Dao = database.dao()

    @Provides
    @Singleton
    fun provideUserPrefs(@ApplicationContext context: Context): UserPrefs =
        UserPrefs(context.userPrefsStore)
}

package com.example.securityapp.di

import android.content.Context
import androidx.room.Room
import com.example.securityapp.data.db.AppDatabase
import com.example.securityapp.data.db.ScanHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "security_app_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideScanHistoryDao(database: AppDatabase): ScanHistoryDao {
        return database.scanHistoryDao()
    }

    @Provides
    fun provideBatteryUsageDao(database: AppDatabase): com.example.securityapp.data.db.BatteryUsageDao {
        return database.batteryUsageDao()
    }
}

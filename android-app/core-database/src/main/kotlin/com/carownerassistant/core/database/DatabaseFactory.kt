package com.carownerassistant.core.database

import android.content.Context
import androidx.room.Room

object DatabaseFactory {
    fun create(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.NAME,
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .addMigrations(AppDatabase.MIGRATION_4_5)
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .addMigrations(AppDatabase.MIGRATION_6_7)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
}

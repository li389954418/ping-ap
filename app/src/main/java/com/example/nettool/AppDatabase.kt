package com.example.nettool

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [IpEntry::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ipDao(): IpDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ip_table ADD COLUMN extraRemarks TEXT NOT NULL DEFAULT '{}'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ip_database"
                ).addMigrations(MIGRATION_1_2)
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

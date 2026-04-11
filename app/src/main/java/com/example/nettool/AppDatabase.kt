package com.example.nettool

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [IpEntry::class, TemplateEntry::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ipDao(): IpDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ip_table ADD COLUMN extraRemarks TEXT NOT NULL DEFAULT '{}'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS template_table (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        pattern TEXT NOT NULL,
                        targetField TEXT NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1
                    )
                """)
                // 插入默认模板
                database.execSQL("INSERT INTO template_table (name, pattern, targetField, enabled) VALUES ('IPv4地址', '\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b', 'address', 1)")
                database.execSQL("INSERT INTO template_table (name, pattern, targetField, enabled) VALUES ('域名', '\\b([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\\b', 'address', 1)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ip_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
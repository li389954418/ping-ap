package com.example.nettool

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

@Database(entities = [IpEntry::class, TemplateEntry::class], version = 6)
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
                database.execSQL("INSERT INTO template_table (name, pattern, targetField, enabled) VALUES ('IPv4地址', '\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b', 'address', 1)")
                database.execSQL("INSERT INTO template_table (name, pattern, targetField, enabled) VALUES ('域名', '\\b([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\\b', 'address', 1)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ip_table ADD COLUMN category TEXT NOT NULL DEFAULT '默认'")
                database.execSQL("""
                    CREATE TABLE template_table_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        rulesJson TEXT NOT NULL DEFAULT '[]',
                        enabled INTEGER NOT NULL DEFAULT 1
                    )
                """)
                val cursor = database.query("SELECT id, name, pattern, targetField, enabled FROM template_table")
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0)
                    val name = cursor.getString(1)
                    val pattern = cursor.getString(2)
                    val targetField = cursor.getString(3)
                    val enabled = cursor.getInt(4)
                    val rules = JSONArray()
                    val rule = JSONObject()
                    rule.put("type", "regex")
                    rule.put("pattern", pattern)
                    rule.put("targetField", targetField)
                    rules.put(rule)
                    database.execSQL(
                        "INSERT INTO template_table_new (id, name, rulesJson, enabled) VALUES (?, ?, ?, ?)",
                        arrayOf(id, name, rules.toString(), enabled)
                    )
                }
                cursor.close()
                database.execSQL("DROP TABLE template_table")
                database.execSQL("ALTER TABLE template_table_new RENAME TO template_table")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 预留，未使用
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // IpEntry 增加时间戳和软删除字段
                database.execSQL("ALTER TABLE ip_table ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE ip_table ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE ip_table ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                // TemplateEntry 增加去重和冲突策略字段
                database.execSQL("ALTER TABLE template_table ADD COLUMN duplicateKeys TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE template_table ADD COLUMN conflictStrategy TEXT NOT NULL DEFAULT '{}'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ip_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

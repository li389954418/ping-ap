package com.example.nettool

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.File

val Context.backupDataStore by preferencesDataStore(name = "backup_settings")

object BackupManager {
    private val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
    private val AUTO_BACKUP_TRIGGER = stringPreferencesKey("auto_backup_trigger")
    private val AUTO_BACKUP_TARGET = stringPreferencesKey("auto_backup_target")

    fun isAutoBackupEnabled(context: Context): Boolean {
        return runBlocking {
            runCatching {
                context.backupDataStore.data.map { it[AUTO_BACKUP_ENABLED] ?: false }.first()
            }.getOrDefault(false)
        }
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        runBlocking {
            context.backupDataStore.edit { it[AUTO_BACKUP_ENABLED] = enabled }
        }
    }

    fun getAutoBackupTrigger(context: Context): String {
        return runBlocking {
            runCatching {
                context.backupDataStore.data.map { it[AUTO_BACKUP_TRIGGER] ?: "manual" }.first()
            }.getOrDefault("manual")
        }
    }

    fun setAutoBackupTrigger(context: Context, trigger: String) {
        runBlocking {
            context.backupDataStore.edit { it[AUTO_BACKUP_TRIGGER] = trigger }
        }
    }

    fun getAutoBackupTarget(context: Context): String {
        return runBlocking {
            runCatching {
                context.backupDataStore.data.map { it[AUTO_BACKUP_TARGET] ?: "local" }.first()
            }.getOrDefault("local")
        }
    }

    fun setAutoBackupTarget(context: Context, target: String) {
        runBlocking {
            context.backupDataStore.edit { it[AUTO_BACKUP_TARGET] = target }
        }
    }

    fun saveToFile(context: Context, content: String, fileName: String): Boolean {
        return try {
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) backupDir.mkdirs()
            val file = File(backupDir, fileName)
            file.writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun listBackupFiles(context: Context): List<File> {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        return if (backupDir.exists()) {
            backupDir.listFiles()?.filter { it.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else emptyList()
    }

    fun performAutoBackup(context: Context, data: String) {
        if (!isAutoBackupEnabled(context)) return
        val target = getAutoBackupTarget(context)
        val fileName = "NetTool_AutoBackup_${System.currentTimeMillis()}.json"
        when (target) {
            "local" -> saveToFile(context, data, fileName)
            "webdav" -> {
                val config = getWebDavConfig(context)
                if (config != null) {
                    runBlocking { WebDavClient.upload(config, fileName, data) }
                }
            }
        }
    }

    private fun getWebDavConfig(context: Context): WebDavConfig? {
        return null // TODO: 实现配置读取
    }
}

data class WebDavConfig(
    val serverUrl: String,
    val username: String,
    val password: String
)

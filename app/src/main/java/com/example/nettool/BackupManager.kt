package com.example.nettool

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File

val Context.backupDataStore by preferencesDataStore(name = "backup_settings")

object BackupManager {
    private val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
    private val AUTO_BACKUP_TRIGGER = stringPreferencesKey("auto_backup_trigger")
    private val AUTO_BACKUP_TARGET = stringPreferencesKey("auto_backup_target")
    private val WEBDAV_SERVER = stringPreferencesKey("webdav_server")
    private val WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
    private val WEBDAV_PASSWORD = stringPreferencesKey("webdav_password")

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

    fun getWebDavConfig(context: Context): WebDavConfig? {
        return runBlocking {
            val prefs = context.backupDataStore.data.first()
            val server = prefs[WEBDAV_SERVER] ?: return@runBlocking null
            val username = prefs[WEBDAV_USERNAME] ?: return@runBlocking null
            val password = prefs[WEBDAV_PASSWORD] ?: return@runBlocking null
            WebDavConfig(server, username, password)
        }
    }

    fun saveWebDavConfig(context: Context, config: WebDavConfig) {
        runBlocking {
            context.backupDataStore.edit { prefs ->
                prefs[WEBDAV_SERVER] = config.serverUrl
                prefs[WEBDAV_USERNAME] = config.username
                prefs[WEBDAV_PASSWORD] = config.password
            }
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
}

data class WebDavConfig(
    val serverUrl: String,
    val username: String,
    val password: String
)

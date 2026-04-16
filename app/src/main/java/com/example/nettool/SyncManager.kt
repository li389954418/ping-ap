package com.example.nettool

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SyncManager {
    data class SyncResult(
        val uploaded: Int = 0,
        val downloaded: Int = 0,
        val conflicts: Int = 0,
        val merged: Int = 0,
        val errors: List<String> = emptyList()
    )

    data class ConflictItem(
        val local: IpEntry,
        val remote: IpEntry,
        val field: String,
        val localValue: String,
        val remoteValue: String
    )

    suspend fun syncNow(
        context: Context,
        viewModel: MainViewModel,
        onProgress: (String) -> Unit = {}
    ): SyncResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        var uploaded = 0
        var downloaded = 0
        var conflicts = 0
        var merged = 0

        try {
            onProgress("正在准备同步...")
            val currentUserName = runCatching { 
                context.dataStore.data.first()[ThemeManager.USER_NAME] ?: "" 
            }.getOrDefault("")

            // 1. 获取本地数据
            val localEntries = viewModel.getAllEntriesSync()
            
            // 2. 从云端拉取数据
            val target = BackupManager.getAutoBackupTarget(context)
            val cloudData = when (target) {
                "webdav" -> {
                    val config = BackupManager.getWebDavConfig(context)
                    if (config != null) {
                        onProgress("正在从坚果云拉取数据...")
                        WebDavClient.downloadLatest(config, "NetTool_Sync_") ?: ""
                    } else ""
                }
                else -> {
                    val files = BackupManager.listBackupFiles(context)
                    files.firstOrNull { it.name.startsWith("NetTool_Sync_") }?.readText() ?: ""
                }
            }

            // 3. 解析云端数据
            val cloudEntries = mutableListOf<IpEntry>()
            if (cloudData.isNotBlank()) {
                onProgress("正在解析云端数据...")
                try {
                    val cloudJson = JSONObject(cloudData)
                    val cloudArray = cloudJson.optJSONArray("entries") ?: JSONArray()
                    for (i in 0 until cloudArray.length()) {
                        val obj = cloudArray.getJSONObject(i)
                        cloudEntries.add(IpEntry(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name"),
                            address = obj.optString("address"),
                            extraRemarks = obj.optString("extraRemarks", "{}"),
                            category = obj.optString("category", "互联网"),
                            createdAt = obj.optLong("createdAt", 0),
                            updatedAt = obj.optLong("updatedAt", 0),
                            deleted = obj.optBoolean("deleted", false),
                            userName = obj.optString("userName", "")
                        ))
                    }
                } catch (e: Exception) {
                    errors.add("解析云端数据失败: ${e.message}")
                }
            }

            // 4. 三方对比合并
            onProgress("正在合并数据...")
            val mergedEntries = mutableListOf<IpEntry>()
            val localMap = localEntries.associateBy { it.id }
            val cloudMap = cloudEntries.associateBy { it.id }

            // 处理本地有、云端无的条目（上传）
            localEntries.forEach { local ->
                val cloud = cloudMap[local.id]
                if (cloud == null) {
                    mergedEntries.add(local)
                    uploaded++
                } else {
                    // 两边都有，检查是否冲突
                    if (local.updatedAt > cloud.updatedAt) {
                        // 本地更新，覆盖云端
                        mergedEntries.add(local)
                        uploaded++
                    } else if (cloud.updatedAt > local.updatedAt) {
                        // 云端更新，覆盖本地
                        mergedEntries.add(cloud.copy(userName = cloud.userName.ifBlank { "云端" }))
                        downloaded++
                    } else {
                        // 时间相同，无冲突
                        mergedEntries.add(local)
                    }
                }
            }

            // 处理云端有、本地无的条目（下载）
            cloudEntries.forEach { cloud ->
                if (!localMap.containsKey(cloud.id)) {
                    mergedEntries.add(cloud.copy(userName = cloud.userName.ifBlank { "云端" }))
                    downloaded++
                }
            }

            // 5. 保存合并后的数据
            onProgress("正在保存数据...")
            viewModel.replaceAllEntries(mergedEntries)

            // 6. 上传合并后的完整数据到云端
            onProgress("正在上传同步结果...")
            val exportData = viewModel.exportEntries(mergedEntries)
            val fileName = "NetTool_Sync_${System.currentTimeMillis()}.json"
            when (target) {
                "webdav" -> {
                    val config = BackupManager.getWebDavConfig(context)
                    if (config != null) {
                        WebDavClient.upload(config, fileName, exportData)
                    }
                }
                else -> {
                    BackupManager.saveToFile(context, exportData, fileName)
                }
            }

            onProgress("同步完成！上传 $uploaded 条，下载 $downloaded 条")
        } catch (e: Exception) {
            errors.add("同步失败: ${e.message}")
        }

        SyncResult(
            uploaded = uploaded,
            downloaded = downloaded,
            conflicts = conflicts,
            merged = merged,
            errors = errors
        )
    }
}

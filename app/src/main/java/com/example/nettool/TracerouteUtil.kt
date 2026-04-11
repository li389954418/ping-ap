package com.example.nettool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object TracerouteUtil {
    // 常见的 traceroute 可能存在的路径
    private val possiblePaths = listOf(
        "/system/bin/traceroute",
        "/system/xbin/traceroute",
        "/su/bin/traceroute",
        "/data/local/tmp/traceroute",
        "traceroute"  // 依赖 PATH 环境变量
    )

    private fun findTracerouteCommand(): String? {
        for (path in possiblePaths) {
            if (File(path).exists() && File(path).canExecute()) {
                return path
            }
        }
        return null
    }

    fun traceroute(host: String, maxHops: Int = 30): Flow<String> = flow {
        val commandPath = findTracerouteCommand()
        if (commandPath == null) {
            emit("错误：此设备未安装 traceroute 命令。\n部分 Android 系统默认不带该工具。")
            return@flow
        }

        val command = arrayOf(commandPath, "-m", maxHops.toString(), host)
        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line ?: "")
            }
            while (errorReader.readLine().also { line = it } != null) {
                emit("ERROR: $line")
            }

            process.waitFor()
            reader.close()
            errorReader.close()
        } catch (e: Exception) {
            emit("Traceroute 执行失败: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}

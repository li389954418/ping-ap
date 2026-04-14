package com.example.nettool

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

object IcmpPing {

    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        timeout: Int = 2000 // 单次等待响应的超时（毫秒）
    ): Flow<String> = channelFlow {
        // 1. 参数验证
        require(count > 0 || count == -1 || count == 0) { "数据包数量必须为正整数、-1或0，当前值: $count" }
        
        val isInfinity = count == -1 || count == 0
        val safeCount = if (isInfinity) 0 else count.coerceAtLeast(1).coerceAtMost(999999)

        // 2. 构建命令
        val command = buildList {
            add("ping")
            if (!isInfinity) {
                add("-c")
                add(safeCount.toString())
            }
            add("-s")
            add(packetSize.toString())
            // -W 设置单次等待响应的超时时间（秒）
            // 注意：Linux ping 的 -W 单位是秒，所以这里除以 1000 并至少为 1
            add("-W")
            add((timeout / 1000).coerceAtLeast(1).toString())
            add(host)
        }.toTypedArray()

        var process: Process? = null

        try {
            // 3. 启动进程
            process = Runtime.getRuntime().exec(command)

            // --- 立即发送“开始提示” ---
            send("正在 Ping $host [大小: ${packetSize}B, 超时: ${timeout}ms]...")

            // 4. 读取标准输出流 (实时返回 Ping 结果)
            val outputJob = launch(Dispatchers.IO) {
                try {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            trySend(line)
                        }
                    }
                } catch (e: Exception) {
                    // 忽略流中断
                }
            }

            // 5. 读取错误流 (捕获权限错误等)
            val errorJob = launch(Dispatchers.IO) {
                try {
                    process.errorStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            trySend("[Error] $line")
                        }
                    }
                } catch (e: Exception) {
                    // 忽略
                }
            }

            // 6. 监控进程存活状态
            while (isActive && process?.isAlive == true) {
                delay(500) 
            }

            outputJob.join()
            errorJob.join()

        } catch (e: Exception) {
            send("Ping 启动失败: ${e.message ?: "Unknown Error"}")
        } finally {
            process?.destroyForcibly()
        }

        awaitClose {
            process?.destroyForcibly()
        }
    }.flowOn(Dispatchers.IO)
}

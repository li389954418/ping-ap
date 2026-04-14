package com.example.nettool

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object IcmpPing {

    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        timeout: Int = 2000
    ): Flow<String> = channelFlow {
        require(count > 0 || count == -1 || count == 0) { "数据包数量必须为正整数、-1或0，当前值: $count" }
        
        val isInfinity = count == -1 || count == 0
        val safeCount = if (isInfinity) 0 else count.coerceAtLeast(1).coerceAtMost(999999)

        val command = buildList {
            add("ping")
            if (!isInfinity) {
                add("-c")
                add(safeCount.toString())
            }
            add("-s")
            add(packetSize.toString())
            add("-W")
            add((timeout / 1000).coerceAtLeast(1).toString())
            add(host)
        }.toTypedArray()

        var process: Process? = null
        var outputJob: Job? = null
        var errorJob: Job? = null

        try {
            process = Runtime.getRuntime().exec(command)
            send("正在 Ping $host [大小: ${packetSize}B, 超时: ${timeout}ms]...")

            // 用于主动超时检测
            var lastReceiveTime = System.currentTimeMillis()
            val timeoutMonitor = launch(Dispatchers.IO) {
                while (isActive) {
                    delay(1000)
                    val now = System.currentTimeMillis()
                    if (now - lastReceiveTime > timeout + 1000) {
                        send("请求超时。")
                        lastReceiveTime = now // 重置，避免重复发送
                    }
                }
            }

            // 读取标准输出（逐行读取，无缓冲延迟）
            outputJob = launch(Dispatchers.IO) {
                try {
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line = reader.readLine()
                    while (line != null) {
                        lastReceiveTime = System.currentTimeMillis()
                        send(line)
                        line = reader.readLine()
                    }
                } catch (e: Exception) {
                    // 忽略
                }
            }

            // 读取错误输出
            errorJob = launch(Dispatchers.IO) {
                try {
                    val reader = BufferedReader(InputStreamReader(process.errorStream))
                    var line = reader.readLine()
                    while (line != null) {
                        lastReceiveTime = System.currentTimeMillis()
                        send("[Error] $line")
                        line = reader.readLine()
                    }
                } catch (e: Exception) {
                    // 忽略
                }
            }

            // 等待进程结束
            while (isActive && process?.isAlive == true) {
                delay(500)
            }

            timeoutMonitor.cancel()
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

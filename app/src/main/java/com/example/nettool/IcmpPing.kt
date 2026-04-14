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
        timeout: Int = 2000
    ): Flow<String> = channelFlow {
        val command = buildList {
            add("ping")
            add("-c")
            add(count.toString())
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

            val inputStream = InputStreamReader(process.inputStream)
            val errorStream = InputStreamReader(process.errorStream)

            // 3. 读取标准输出（修正版）
            outputJob = launch(Dispatchers.IO) {
                BufferedReader(inputStream).use { reader ->
                    var line: String? = null
                    line = reader.readLine()
                    while (isActive && line != null) {
                        send(line)
                        line = reader.readLine()
                    }
                }
            }

            // 4. 读取错误输出（修正版）
            errorJob = launch(Dispatchers.IO) {
                BufferedReader(errorStream).use { reader ->
                    var line: String? = null
                    line = reader.readLine()
                    while (isActive && line != null) {
                        send(line)
                        line = reader.readLine()
                    }
                }
            }

            // 异步等待进程结束
            val waitForJob = launch {
                try {
                    val exitCode = withTimeout(10_000) {
                        process.waitFor()
                    }
                    
                    outputJob?.join()
                    errorJob?.join()
                    
                    if (exitCode != 0) {
                        send("Ping 结束，退出码: $exitCode")
                    }
                } catch (e: CancellationException) {
                    process.destroyForcibly()
                    send("--- Ping 已停止 ---")
                } catch (e: Exception) {
                    process.destroyForcibly()
                    send("Ping 异常: ${e.message}")
                }
            }

            // 监听协程取消
            awaitClose {
                waitForJob.cancel()
                process.destroyForcibly()
            }

        } catch (e: Exception) {
            send("Ping 启动失败: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}

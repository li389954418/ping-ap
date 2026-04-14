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
        // 允许0值作为无限ping标识
        require(count > 0 || count == -1 || count == 0) { "数据包数量必须为正整数、-1或0，当前值: $count" }

        val isInfinity = count == -1 || count == 0
        val safeCount = if (isInfinity) 0 else count.coerceAtLeast(1).coerceAtMost(999999)

        // 构建命令（无限ping不添加-c参数）
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
            // 启动进程
            process = Runtime.getRuntime().exec(command)

            // 读取标准输出
            val inputStream = InputStreamReader(process.inputStream)
            outputJob = launch(Dispatchers.IO) {
                BufferedReader(inputStream).use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        send(line)
                    }
                }
            }

            // 读取错误输出
            val errorStream = InputStreamReader(process.errorStream)
            errorJob = launch(Dispatchers.IO) {
                BufferedReader(errorStream).use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        send(line)
                    }
                }
            }

            // 等待进程结束或超时
            try {
                val exitCode = withTimeout(15_000) {
                    process.waitFor()
                }
                outputJob?.join()
                errorJob?.join()
                if (exitCode != 0) {
                    // send("Ping 结束，退出码: $exitCode")
                }
            } catch (e: TimeoutCancellationException) {
                process.destroyForcibly()
                send("Ping 超时")
            } catch (e: Exception) {
                process.destroyForcibly()
                send("Ping 异常: ${e.message}")
            }

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

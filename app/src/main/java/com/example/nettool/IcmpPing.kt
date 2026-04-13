package com.example.nettool

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object IcmpPing {
    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        timeout: Int = 2000,
        interval: Int = 1000
    ): Flow<String> = flow {
        val command = mutableListOf(
            "ping",
            "-c", count.toString(),
            "-s", packetSize.toString(),
            "-W", (timeout / 1000).toString(),
            "-i", (interval / 1000.0).toString()
        )
        if (count <= 0) {
            command.removeAll { it.startsWith("-c") }
        }
        command.add(host)

        var process: Process? = null
        var inputReader: BufferedReader? = null
        var errorReader: BufferedReader? = null

        try {
            process = Runtime.getRuntime().exec(command.toTypedArray())
            inputReader = BufferedReader(InputStreamReader(process.inputStream))
            errorReader = BufferedReader(InputStreamReader(process.errorStream))

            currentCoroutineContext().job.invokeOnCompletion {
                process?.destroy()
                process?.waitFor(500, TimeUnit.MILLISECONDS)
                if (process?.isAlive == true) {
                    process?.destroyForcibly()
                }
            }

            var hasOutput = false
            coroutineScope {
                val outputJob = launch(Dispatchers.IO) {
                    var line: String?
                    while (isActive && inputReader?.readLine().also { line = it } != null) {
                        hasOutput = true
                        emit(line ?: "")
                    }
                }
                val errorJob = launch(Dispatchers.IO) {
                    var line: String?
                    while (isActive && errorReader?.readLine().also { line = it } != null) {
                        hasOutput = true
                        // 将常见错误转换为 Windows 风格提示
                        val friendlyMsg = when {
                            line?.contains("unknown host", ignoreCase = true) == true ->
                                "Ping 请求找不到主机 $host。请检查该名称，然后重试。"
                            line?.contains("Network is unreachable", ignoreCase = true) == true ->
                                "网络不可达。"
                            line?.contains("Destination Host Unreachable", ignoreCase = true) == true ->
                                "来自 ${host} 的回复: 目标主机不可达。"
                            else -> "ERROR: $line"
                        }
                        emit(friendlyMsg)
                    }
                }
                val exitCode = withContext(Dispatchers.IO) { process?.waitFor() ?: -1 }
                outputJob.join()
                errorJob.join()
                if (!hasOutput && count > 0) {
                    emit("请求超时。")
                } else if (exitCode != 0 && count > 0) {
                    emit("\nPing 命令执行失败，退出码: $exitCode")
                }
            }
        } catch (e: CancellationException) {
            emit("\n--- Ping 已手动取消 ---")
            throw e
        } catch (e: Exception) {
            emit("ICMP Ping 执行失败: ${e.message ?: "未知错误"}")
        } finally {
            withContext(NonCancellable) {
                inputReader?.close()
                errorReader?.close()
                process?.destroyForcibly()
            }
        }
    }.flowOn(Dispatchers.IO)
}

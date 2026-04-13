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
        timeout: Int = 2000,
        interval: Int = 1000
    ): Flow<String> = channelFlow {
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

            invokeOnClose {
                process?.destroy()
                process?.waitFor(500, TimeUnit.MILLISECONDS)
                if (process?.isAlive == true) {
                    process?.destroyForcibly()
                }
            }

            var hasOutput = false
            coroutineScope {
                val outputJob = launch(Dispatchers.IO) {
                    while (isActive) {
                        val line = inputReader?.readLine() ?: break
                        hasOutput = true
                        send(line)
                    }
                }
                val errorJob = launch(Dispatchers.IO) {
                    while (isActive) {
                        val line = errorReader?.readLine() ?: break
                        hasOutput = true
                        val friendlyMsg = when {
                            line.contains("unknown host", ignoreCase = true) ->
                                "Ping 请求找不到主机 $host。请检查该名称，然后重试。"
                            line.contains("Network is unreachable", ignoreCase = true) ->
                                "网络不可达。"
                            line.contains("Destination Host Unreachable", ignoreCase = true) ->
                                "来自 $host 的回复: 目标主机不可达。"
                            else -> "ERROR: $line"
                        }
                        send(friendlyMsg)
                    }
                }
                val exitCode = withContext(Dispatchers.IO) { process?.waitFor() ?: -1 }
                outputJob.join()
                errorJob.join()
                if (!hasOutput && count > 0) {
                    send("请求超时。")
                } else if (exitCode != 0 && count > 0) {
                    send("\nPing 命令执行失败，退出码: $exitCode")
                }
            }
        } catch (e: CancellationException) {
            send("\n--- Ping 已手动取消 ---")
            throw e
        } catch (e: Exception) {
            send("ICMP Ping 执行失败: ${e.message ?: "未知错误"}")
        } finally {
            withContext(NonCancellable) {
                inputReader?.close()
                errorReader?.close()
                process?.destroyForcibly()
            }
        }
        awaitClose()
    }.flowOn(Dispatchers.IO)
}

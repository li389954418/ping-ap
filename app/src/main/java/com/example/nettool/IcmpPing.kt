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
        val command = buildList {
            add("ping")
            if (count > 0) {
                add("-c")
                add(count.toString())
            }
            add("-s")
            add(packetSize.toString())
            add("-W")
            add((timeout / 1000).toString())
            add(host)
        }.toTypedArray()

        var process: Process? = null
        var inputReader: BufferedReader? = null
        var errorReader: BufferedReader? = null

        try {
            process = Runtime.getRuntime().exec(command)
            inputReader = BufferedReader(InputStreamReader(process.inputStream))
            errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val job = currentCoroutineContext()[Job]
            job?.invokeOnCompletion {
                process?.destroy()
                process?.waitFor(500, TimeUnit.MILLISECONDS)
                if (process?.isAlive == true) {
                    process?.destroyForcibly()
                }
            }

            var hasOutput = false
            
            val outputJob = launch(Dispatchers.IO) {
                var line = inputReader.readLine()
                while (line != null) {
                    hasOutput = true
                    send(line)
                    line = inputReader.readLine()
                }
            }
            
            val errorJob = launch(Dispatchers.IO) {
                var line = errorReader.readLine()
                while (line != null) {
                    hasOutput = true
                    send(line)
                    line = errorReader.readLine()
                }
            }

            val exitCode = try {
                withTimeoutOrNull(10000L) {
                    process?.waitFor()
                } ?: run {
                    process?.destroyForcibly()
                    -1
                }
            } catch (e: Exception) {
                process?.destroyForcibly()
                -1
            }

            outputJob.join()
            errorJob.join()

            if (!hasOutput && count > 0) {
                send("请求超时或目标不可达")
            } else if (exitCode != null && exitCode != 0 && count > 0) {
                send("Ping 命令异常，退出码: $exitCode")
            }
        } catch (e: CancellationException) {
            send("--- Ping 已手动取消 ---")
            throw e
        } catch (e: Exception) {
            send("Ping 错误: ${e.message ?: "未知异常"}")
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

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
        timeout: Int = 2000
    ): Flow<String> = flow {
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
            // 启动超时监控
            val timeoutJob = withContext(Dispatchers.IO) {
                launch {
                    delay(10000L)
                    if (!hasOutput && count > 0) {
                        process?.destroyForcibly()
                    }
                }
            }

            withContext(Dispatchers.IO) {
                val outputJob = launch {
                    var line: String?
                    while (isActive && inputReader?.readLine().also { line = it } != null) {
                        hasOutput = true
                        emit(line!!)
                    }
                }
                val errorJob = launch {
                    var line: String?
                    while (isActive && errorReader?.readLine().also { line = it } != null) {
                        hasOutput = true
                        emit(line!!)
                    }
                }

                val exitCode = process?.waitFor() ?: -1
                timeoutJob.cancel()
                outputJob.join()
                errorJob.join()

                if (!hasOutput && count > 0) {
                    emit("请求超时或目标不可达。")
                } else if (exitCode != 0 && count > 0) {
                    emit("Ping 命令退出码: $exitCode")
                }
            }
        } catch (e: CancellationException) {
            emit("\n--- Ping 已手动取消 ---")
            throw e
        } catch (e: Exception) {
            emit("ICMP Ping 异常: ${e.message}")
        } finally {
            withContext(NonCancellable) {
                inputReader?.close()
                errorReader?.close()
                process?.destroyForcibly()
            }
        }
    }.flowOn(Dispatchers.IO)
}

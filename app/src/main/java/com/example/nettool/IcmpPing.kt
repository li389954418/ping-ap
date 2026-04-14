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
        val command = mutableListOf("ping")
        if (count > 0) {
            command.add("-c")
            command.add(count.toString())
        }
        command.add("-s")
        command.add(packetSize.toString())
        command.add("-W")
        command.add((timeout / 1000).toString())
        command.add(host)

        var process: Process? = null
        var inputReader: BufferedReader? = null
        var errorReader: BufferedReader? = null

        try {
            process = Runtime.getRuntime().exec(command.toTypedArray())
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
            val startTime = System.currentTimeMillis()
            val maxWaitTime = if (count > 0) count * 3000L + 5000 else 30000L

            coroutineScope {
                val outputJob = launch(Dispatchers.IO) {
                    var line: String? = inputReader?.readLine()
                    while (isActive && line != null) {
                        hasOutput = true
                        emit(line)
                        line = inputReader?.readLine()
                    }
                }
                val errorJob = launch(Dispatchers.IO) {
                    var line: String? = errorReader?.readLine()
                    while (isActive && line != null) {
                        hasOutput = true
                        emit(line)
                        line = errorReader?.readLine()
                    }
                }

                val exitCode = withContext(Dispatchers.IO) {
                    if (process?.waitFor(maxWaitTime, TimeUnit.MILLISECONDS) == false) {
                        process?.destroyForcibly()
                        -1
                    } else {
                        process?.exitValue() ?: -1
                    }
                }

                outputJob.cancel()
                errorJob.cancel()
                outputJob.join()
                errorJob.join()

                if (!hasOutput && count > 0) {
                    emit("请求超时或目标不可达。")
                } else if (exitCode != 0 && count > 0) {
                    emit("Ping 命令退出码: $exitCode")
                }
            }
        } catch (e: CancellationException) {
            emit("\n--- Ping 已取消 ---")
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

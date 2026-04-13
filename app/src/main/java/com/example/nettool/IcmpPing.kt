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
        val command = mutableListOf("ping")

        if (count > 0) {
            command.add("-c")
            command.add(count.toString())
        }

        command.add("-W")
        command.add((timeout / 1000).toString())

        command.add("-i")
        command.add((interval / 1000).toString())

        command.add("-s")
        command.add(packetSize.toString())

        command.add(host)

        var process: Process? = null
        var inputReader: BufferedReader? = null
        var errorReader: BufferedReader? = null

        try {
            process = Runtime.getRuntime().exec(command.toTypedArray())
            inputReader = BufferedReader(InputStreamReader(process.inputStream))
            errorReader = BufferedReader(InputStreamReader(process.errorStream))

            // 协程取消时自动杀进程
            awaitClose {
                runCatching {
                    process?.destroy()
                    process?.waitFor(300, TimeUnit.MILLISECONDS)
                    if (process?.isAlive == true) process?.destroyForcibly()
                }
                runCatching { inputReader?.close() }
                runCatching { errorReader?.close() }
            }

            // 读取输出
            launch(Dispatchers.IO) {
                var line: String?
                while (isActive && inputReader.readLine().also { line = it } != null) {
                    send(line!!)
                }
            }

            // 读取错误（关键：不可达IP的信息从这里出）
            launch(Dispatchers.IO) {
                var line: String?
                while (isActive && errorReader.readLine().also { line = it } != null) {
                    send("[错误] $line")
                }
            }

            // 等待进程结束
            process?.waitFor(30, TimeUnit.SECONDS)
            send("\n--- Ping 完成 ---")

        } catch (e: CancellationException) {
            send("\n--- 已手动停止 ---")
        } catch (e: Exception) {
            send("\n异常：${e.message ?: "未知错误"}")
        }
    }.flowOn(Dispatchers.IO)
}

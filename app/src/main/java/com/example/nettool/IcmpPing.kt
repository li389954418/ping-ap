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
            // 1. 启动进程
            process = Runtime.getRuntime().exec(command)

            // 2. 创建读取任务
            val inputStream = InputStreamReader(process.inputStream)
            val errorStream = InputStreamReader(process.errorStream)

            // 3. 读取标准输出
            outputJob = launch(Dispatchers.IO) {
                BufferedReader(inputStream).use { reader ->
                    var line: String?
                    while (isActive && (reader.readLine().also { line = it }) != null) {
                        send(line!!)
                    }
                }
            }

            // 4. 读取错误输出（关键：防止缓冲区阻塞）
            errorJob = launch(Dispatchers.IO) {
                BufferedReader(errorStream).use { reader ->
                    var line: String?
                    while (isActive && (reader.readLine().also { line = it }) != null) {
                        send(line!!)
                    }
                }
            }

            // 5. 异步等待进程结束，防止阻塞协程
            val waitForJob = launch {
                try {
                    // 等待进程结束，或者被协程取消
                    val exitCode = withTimeout(10_000) { // 总超时保护
                        process.waitFor()
                    }
                    
                    // 进程结束后，确保读取完剩余数据
                    outputJob?.join()
                    errorJob?.join()
                    
                    // 发送结束信号或状态
                    if (exitCode != 0) {
                        send("Ping 结束，退出码: $exitCode")
                    }
                } catch (e: CancellationException) {
                    // 协程被取消时，强制结束进程
                    process.destroyForcibly()
                    send("--- Ping 已停止 ---")
                } catch (e: Exception) {
                    process.destroyForcibly()
                    send("Ping 异常: ${e.message}")
                }
            }

            // 6. 监听协程取消，主动销毁进程
            awaitClose {
                waitForJob.cancel()
                process.destroyForcibly()
            }

        } catch (e: Exception) {
            send("Ping 启动失败: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}

package com.example.nettool

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

object IcmpPing {
    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        timeout: Int = 2000
    ): Flow<String> = flow {
        // 构建命令：当 count == 0 时不加 -c 参数，实现无限 ping
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

        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            // 获取当前协程的 Job，用于取消时销毁进程
            val job = currentCoroutineContext()[Job]
            job?.invokeOnCompletion {
                process.destroy()
            }

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line ?: "")
                // 检查协程是否被取消
                currentCoroutineContext().ensureActive()
            }
            while (errorReader.readLine().also { line = it } != null) {
                emit("ERROR: $line")
            }

            val exitCode = process.waitFor()
            reader.close()
            errorReader.close()

            // 非无限模式且异常退出时给出提示
            if (exitCode != 0 && count > 0) {
                emit("Ping 命令退出码: $exitCode")
            }
        } catch (e: CancellationException) {
            emit("\n--- Ping 已取消 ---")
            throw e
        } catch (e: Exception) {
            emit("ICMP Ping 失败: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}

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
        val command = listOf(
            "ping",
            "-c", if (count == 0) "0" else count.toString(),
            "-s", packetSize.toString(),
            "-W", (timeout / 1000).toString(),
            host
        ).toTypedArray()

        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            // 获取当前协程的 Job，以便取消时销毁进程
            val job = currentCoroutineContext()[Job]
            job?.invokeOnCompletion {
                process.destroy()
            }

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line ?: "")
                currentCoroutineContext().ensureActive()
            }
            while (errorReader.readLine().also { line = it } != null) {
                emit("ERROR: $line")
            }

            val exitCode = process.waitFor()
            reader.close()
            errorReader.close()

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

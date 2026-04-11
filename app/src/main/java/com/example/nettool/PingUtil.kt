package com.example.nettool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object PingUtil {
    data class PingParams(
        val count: Int = 4,
        val packetSize: Int = 56,
        val ttl: Int = 64,
        val deadline: Int = 10
    )

    fun pingWithFlow(
        address: String,
        params: PingParams = PingParams()
    ): Flow<String> = flow {
        val command = buildList {
            add("ping")
            add("-c")
            add(params.count.toString())
            add("-s")
            add(params.packetSize.toString())
            add("-t")
            add(params.ttl.toString())
            add("-w")
            add(params.deadline.toString())
            add(address)
        }.toTypedArray()

        withContext(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(command)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                // 读取标准输出
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    emit(line ?: "")
                }

                // 读取错误输出
                while (errorReader.readLine().also { line = it } != null) {
                    emit("ERROR: $line")
                }

                process.waitFor()
                reader.close()
                errorReader.close()
            } catch (e: Exception) {
                emit("Ping 失败: ${e.message}")
            }
        }
    }
}

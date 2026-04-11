package com.example.nettool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line ?: "")
            }
            while (errorReader.readLine().also { line = it } != null) {
                emit("ERROR: $line")
            }

            process.waitFor()
            reader.close()
            errorReader.close()
        } catch (e: Exception) {
            emit("Ping 失败: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)  // 关键：将整个 Flow 切换到 IO 线程
}

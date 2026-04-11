package com.example.nettool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

object TracerouteUtil {
    fun traceroute(host: String, maxHops: Int = 30): Flow<String> = flow {
        val command = arrayOf("/system/bin/traceroute", "-m", maxHops.toString(), host)
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
            emit("Traceroute 失败: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}

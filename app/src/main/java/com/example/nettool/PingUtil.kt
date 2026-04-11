package com.example.nettool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object PingUtil {
    suspend fun ping(address: String): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("ping -c 4 $address")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                result.append(line).append("\n")
            }
            process.waitFor()
            reader.close()
            result.toString().ifEmpty { "Ping 执行完成，无返回结果。" }
        } catch (e: Exception) {
            "Ping 失败: ${e.message}"
        }
    }
}

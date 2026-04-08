package com.example.nettool.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.net.InetAddress

data class PingResult(
    val success: Boolean,
    val host: String,
    val timeMs: Long? = null,
    val errorMessage: String? = null,
    val packetLoss: Int? = null,
    val minDelay: Long? = null,
    val avgDelay: Long? = null,
    val maxDelay: Long? = null
)

object Pinger {
    suspend fun ping(host: String, count: Int = 4): PingResult = withContext(Dispatchers.IO) {
        try {
            // 先检查主机是否可解析
            val address = InetAddress.getByName(host)
            
            // 使用 Android 的 ping 方法（ICMP）
            val startTime = System.currentTimeMillis()
            val isReachable = address.isReachable(5000) // 5 秒超时
            val endTime = System.currentTimeMillis()
            
            if (isReachable) {
                val timeMs = endTime - startTime
                PingResult(
                    success = true,
                    host = host,
                    timeMs = timeMs,
                    packetLoss = 0,
                    minDelay = timeMs,
                    avgDelay = timeMs,
                    maxDelay = timeMs
                )
            } else {
                PingResult(
                    success = false,
                    host = host,
                    errorMessage = "Host unreachable or timeout",
                    packetLoss = 100
                )
            }
        } catch (e: Exception) {
            PingResult(
                success = false,
                host = host,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }
}
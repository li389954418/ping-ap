package com.example.nettool

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import kotlin.math.round

object IcmpPing {
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        timeout: Int = 2000
    ): Flow<String> = flow {
        emit("正在 Ping $host (使用 TCP 探测)")

        var sequence = 0
        var transmitted = 0
        var received = 0
        val times = mutableListOf<Double>()

        // 尝试解析为 IP，如果已经是 IP 则直接使用，否则尝试通过 HTTP 连接测通断
        val address = try {
            InetAddress.getByName(host)
        } catch (e: Exception) {
            null
        }

        while (count == 0 || sequence < count) {
            sequence++
            transmitted++

            currentCoroutineContext().ensureActive()

            val start = System.nanoTime()
            val success = try {
                if (address != null) {
                    address.isReachable(timeout)
                } else {
                    // 对于域名，尝试 HEAD 请求测通断
                    val request = Request.Builder()
                        .url("http://$host")
                        .head()
                        .build()
                    client.newCall(request).execute().use { it.isSuccessful }
                }
            } catch (e: Exception) {
                false
            }
            val timeMs = (System.nanoTime() - start) / 1_000_000.0

            if (success) {
                received++
                times.add(timeMs)
                emit("来自 $host 的回复: 时间=${round(timeMs).toInt()}ms")
            } else {
                emit("请求超时。")
            }

            if (count == 0 || sequence < count) {
                delay(1000)
            }
        }

        val loss = if (transmitted > 0) (transmitted - received) * 100.0 / transmitted else 0.0
        val min = times.minOrNull() ?: 0.0
        val max = times.maxOrNull() ?: 0.0
        val avg = times.average().takeUnless { it.isNaN() } ?: 0.0

        emit("")
        emit("--- $host ping 统计 ---")
        emit("发送包数 = $transmitted，接收包数 = $received，丢失 = ${transmitted - received} (${round(loss)}% 丢失)")
        if (received > 0) {
            emit("最短 = ${round(min).toInt()}ms，最长 = ${round(max).toInt()}ms，平均 = ${round(avg).toInt()}ms")
        }
    }.flowOn(Dispatchers.IO)
}

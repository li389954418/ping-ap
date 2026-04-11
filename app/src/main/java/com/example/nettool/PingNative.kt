package com.example.nettool

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.round

object PingNative {
    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        ttl: Int = 64,
        timeout: Int = 2000
    ): Flow<String> = flow {
        val address = try {
            InetAddress.getByName(host)
        } catch (e: Exception) {
            emit("无法解析主机名: $host")
            return@flow
        }

        emit("PING $host (${address.hostAddress}) ${packetSize}(${packetSize + 8}) bytes of data.")

        var sequence = 0
        var transmitted = 0
        var received = 0
        val times = mutableListOf<Double>()

        while (count == 0 || sequence < count) {
            sequence++
            transmitted++

            val startTime = System.nanoTime()
            val success = isHostReachable(address, timeout)
            val endTime = System.nanoTime()
            val timeMs = (endTime - startTime) / 1_000_000.0

            if (success) {
                received++
                times.add(timeMs)
                emit("64 bytes from ${address.hostAddress}: icmp_seq=$sequence ttl=$ttl time=${round(timeMs).toInt()} ms")
            } else {
                emit("Request timeout for icmp_seq $sequence")
            }

            // 检查协程是否被取消
            currentCoroutineContext().ensureActive()

            // 间隔 1 秒（最后一次不延迟）
            if (count == 0 || sequence < count) {
                delay(1000)
            }
        }

        // 统计摘要
        val loss = if (transmitted > 0) (transmitted - received) * 100.0 / transmitted else 0.0
        val min = times.minOrNull() ?: 0.0
        val max = times.maxOrNull() ?: 0.0
        val avg = times.average().takeUnless { it.isNaN() } ?: 0.0

        emit("--- $host ping statistics ---")
        emit("$transmitted packets transmitted, $received received, ${round(loss)}% packet loss")
        if (received > 0) {
            emit("rtt min/avg/max = ${round(min)}/${round(avg)}/${round(max)} ms")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 检测主机是否可达（兼容性更好）
     */
    private suspend fun isHostReachable(address: InetAddress, timeout: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 方法1：系统 isReachable（可能发送 ICMP 或 TCP Echo）
                if (address.isReachable(timeout)) {
                    return@withContext true
                }
                // 方法2：尝试连接常见端口
                val ports = listOf(443, 80, 53)
                for (port in ports) {
                    try {
                        Socket().use { socket ->
                            socket.tcpNoDelay = true
                            socket.soTimeout = timeout
                            socket.connect(InetSocketAddress(address, port), timeout)
                        }
                        return@withContext true
                    } catch (e: Exception) {
                        // 继续尝试下一个端口
                    }
                }
                false
            } catch (e: Exception) {
                false
            }
        }
    }
}

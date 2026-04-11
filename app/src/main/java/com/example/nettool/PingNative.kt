package com.example.nettool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.round

object PingNative {
    data class PingResult(
        val address: InetAddress,
        val sequence: Int,
        val ttl: Int,
        val time: Double,      // 毫秒
        val received: Boolean
    )

    data class PingSummary(
        val address: InetAddress,
        val transmitted: Int,
        val received: Int,
        val lossPercent: Double,
        val minTime: Double,
        val avgTime: Double,
        val maxTime: Double
    )

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

        val results = mutableListOf<PingResult>()
        var sequence = 0

        repeat(count) {
            sequence++
            val startTime = System.nanoTime()
            val received = try {
                // 尝试 TCP 连接 7 号端口 (echo) 或 80 端口模拟连通性
                // 注意：Android 不支持原始 ICMP 套接字，我们使用 TCP 连接测延迟
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.soTimeout = timeout
                socket.connect(InetSocketAddress(address, 80), timeout)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
            val endTime = System.nanoTime()
            val timeMs = (endTime - startTime) / 1_000_000.0

            if (received) {
                emit("64 bytes from ${address.hostAddress}: icmp_seq=$sequence ttl=$ttl time=${round(timeMs).toInt()} ms")
                results.add(PingResult(address, sequence, ttl, timeMs, true))
            } else {
                emit("Request timeout for icmp_seq $sequence")
                results.add(PingResult(address, sequence, 0, 0.0, false))
            }

            if (it < count - 1) {
                Thread.sleep(1000)
            }
        }

        // 统计摘要
        val receivedCount = results.count { it.received }
        val transmitted = results.size
        val loss = (transmitted - receivedCount) * 100.0 / transmitted
        val times = results.filter { it.received }.map { it.time }
        val min = times.minOrNull() ?: 0.0
        val max = times.maxOrNull() ?: 0.0
        val avg = times.average().takeUnless { it.isNaN() } ?: 0.0

        emit("--- $host ping statistics ---")
        emit("$transmitted packets transmitted, $receivedCount received, ${round(loss)}% packet loss")
        if (receivedCount > 0) {
            emit("rtt min/avg/max = ${round(min)}/${round(avg)}/${round(max)} ms")
        }
    }.flowOn(Dispatchers.IO)
}

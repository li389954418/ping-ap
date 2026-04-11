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
            val success = try {
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
}

package com.example.nettool

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.round

object TcpPing {
    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        port: Int = 80,
        timeout: Int = 2000
    ): Flow<String> = flow {
        val address = try {
            InetAddress.getByName(host)
        } catch (e: Exception) {
            emit("Ping 请求找不到主机 $host。请检查该名称，然后重试。")
            return@flow
        }

        emit("正在 Ping $host:${port} 具有 32 字节的数据:")

        var sequence = 0
        var transmitted = 0
        var received = 0
        val times = mutableListOf<Double>()

        while (count == 0 || sequence < count) {
            sequence++
            transmitted++

            val startTime = System.nanoTime()
            val success = tcpConnect(address, port, timeout)
            val endTime = System.nanoTime()
            val timeMs = (endTime - startTime) / 1_000_000.0

            if (success) {
                received++
                times.add(timeMs)
                emit("来自 ${address.hostAddress}:$port 的回复: 时间=${round(timeMs).toInt()}ms")
            } else {
                emit("请求超时。")
            }

            currentCoroutineContext().ensureActive()

            if (count == 0 || sequence < count) {
                delay(1000)
            }
        }

        val loss = if (transmitted > 0) (transmitted - received) * 100.0 / transmitted else 0.0
        val min = times.minOrNull() ?: 0.0
        val max = times.maxOrNull() ?: 0.0
        val avg = times.average().takeUnless { it.isNaN() } ?: 0.0

        emit("\n--- $host:$port tcping 统计 ---")
        emit("发送包数 = $transmitted，接收包数 = $received，丢失 = ${transmitted - received} (${round(loss)}% 丢失)")
        if (received > 0) {
            emit("最短 = ${round(min).toInt()}ms，最长 = ${round(max).toInt()}ms，平均 = ${round(avg).toInt()}ms")
        }
    }.flowOn(Dispatchers.IO)

    private fun tcpConnect(address: InetAddress, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.soTimeout = timeout
                socket.connect(InetSocketAddress(address, port), timeout)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

    companion object {
        fun tcpConnect(address: String, port: Int, timeout: Int): Boolean {
            return try {
                java.net.Socket().use { socket ->
                    socket.tcpNoDelay = true
                    socket.soTimeout = timeout
                    socket.connect(java.net.InetSocketAddress(address, port), timeout)
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

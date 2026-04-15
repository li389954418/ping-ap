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
            emit("无法解析主机名: $host")
            return@flow
        }

        emit("TCPING $host (${address.hostAddress}) port=$port, timeout=${timeout}ms")

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
                emit("Connected to ${address.hostAddress}:$port seq=$sequence time=${round(timeMs).toInt()} ms")
            } else {
                emit("Connection timeout for seq=$sequence")
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

        emit("--- $host:$port tcping statistics ---")
        emit("$transmitted probes transmitted, $received received, ${round(loss)}% loss")
        if (received > 0) {
            emit("rtt min/avg/max = ${round(min)}/${round(avg)}/${round(max)} ms")
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

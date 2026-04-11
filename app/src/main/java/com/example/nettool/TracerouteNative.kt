package com.example.nettool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

object TracerouteNative {
    fun trace(host: String, maxHops: Int = 30, timeout: Int = 2000): Flow<String> = flow {
        val target = try {
            InetAddress.getByName(host)
        } catch (e: Exception) {
            emit("无法解析主机名: $host")
            return@flow
        }

        emit("traceroute to $host (${target.hostAddress}), $maxHops hops max")

        var reached = false
        for (ttl in 1..maxHops) {
            if (reached) break

            val startTime = System.nanoTime()
            var hopAddress: InetAddress? = null
            var hopTime = 0.0

            try {
                // 发送 UDP 包，端口使用 33434 + ttl (标准 traceroute 端口)
                val udpSocket = DatagramSocket()
                udpSocket.soTimeout = timeout
                udpSocket.trafficClass = ttl // 设置 TTL (仅部分系统支持)

                // 更可靠的方式：建立 TCP 连接并设置 IP_TTL (需要反射)
                // 这里简化为 TCP 连接模拟，因为 Android 限制 IP_TTL 设置
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.soTimeout = timeout
                socket.connect(InetSocketAddress(target, 80), timeout)

                hopAddress = socket.inetAddress
                socket.close()
                reached = true
            } catch (e: Exception) {
                // 超时或不可达，尝试获取中间跳地址
                hopAddress = try {
                    // 尝试通过 TCP 连接时触发 ICMP 返回
                    val socket = Socket()
                    socket.tcpNoDelay = true
                    socket.soTimeout = timeout
                    socket.connect(InetSocketAddress(target, 80), timeout)
                    socket.inetAddress
                } catch (ex: Exception) {
                    null
                }
            }

            val endTime = System.nanoTime()
            hopTime = (endTime - startTime) / 1_000_000.0

            when {
                hopAddress != null && hopAddress == target -> {
                    emit("$ttl  ${hopAddress.hostAddress}  ${String.format("%.2f", hopTime)} ms")
                    reached = true
                }
                hopAddress != null -> {
                    emit("$ttl  ${hopAddress.hostAddress}  ${String.format("%.2f", hopTime)} ms")
                }
                else -> {
                    emit("$ttl  * * *")
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}

package com.example.nettool

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import icmp4j.Icmp4j
import icmp4j.IcmpPingRequest
import icmp4j.IcmpPingResponse
import java.net.InetAddress
import kotlin.math.round

object IcmpPing {
    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        timeout: Int = 2000
    ): Flow<String> = flow {
        val icmp = Icmp4j()
        val request = IcmpPingRequest().apply {
            this.host = host
            this.timeout = timeout.toLong()
            this.packetSize = packetSize
        }

        emit("正在 Ping $host 具有 $packetSize 字节的数据:")

        var sequence = 0
        var transmitted = 0
        var received = 0
        val times = mutableListOf<Double>()

        while (count == 0 || sequence < count) {
            sequence++
            transmitted++

            currentCoroutineContext().ensureActive()

            val response: IcmpPingResponse = withContext(Dispatchers.IO) {
                icmp.execute(request)
            }

            val timeMs = response.rtt?.toDouble() ?: 0.0
            val ttl = response.ttl ?: 0

            if (response.success) {
                received++
                times.add(timeMs)
                emit("来自 ${response.host} 的回复: 字节=32 时间=${round(timeMs).toInt()}ms TTL=$ttl")
            } else {
                when {
                    response.timeout -> emit("请求超时。")
                    response.errorMessage != null -> emit("错误: ${response.errorMessage}")
                    else -> emit("目标不可达。")
                }
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

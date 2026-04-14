package com.example.nettool

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import com.potterhsu.pinger.Pinger
import kotlin.math.round

object IcmpPing {
    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        timeout: Int = 2000
    ): Flow<String> = callbackFlow {
        val pinger = Pinger()
        var sequence = 0
        var transmitted = 0
        var received = 0
        val times = mutableListOf<Double>()
        var startTime = 0L

        emit("正在 Ping $host 具有 32 字节的数据:")

        pinger.setOnPingListener(object : Pinger.OnPingListener {
            override fun onPingSuccess() {
                transmitted++
                val seq = ++sequence
                if (startTime > 0) {
                    val rtt = System.currentTimeMillis() - startTime
                    times.add(rtt.toDouble())
                    received++
                    trySend("来自 $host 的回复: 字节=32 时间=${rtt}ms TTL=?? (seq=$seq)")
                }
                startTime = 0

                // 检查是否达到指定次数
                if (count > 0 && transmitted >= count) {
                    finishPing()
                } else {
                    // 继续下一次 ping
                    startTime = System.currentTimeMillis()
                    pinger.ping(host, 1)
                }
            }

            override fun onPingFailure() {
                transmitted++
                sequence++
                trySend("请求超时。")
                startTime = 0

                if (count > 0 && transmitted >= count) {
                    finishPing()
                } else {
                    startTime = System.currentTimeMillis()
                    pinger.ping(host, 1)
                }
            }

            override fun onPingFinish() {
                // 由 finishPing 处理
            }

            private fun finishPing() {
                val loss = if (transmitted > 0) (transmitted - received) * 100.0 / transmitted else 0.0
                val min = times.minOrNull() ?: 0.0
                val max = times.maxOrNull() ?: 0.0
                val avg = times.average().takeUnless { it.isNaN() } ?: 0.0

                trySend("")
                trySend("--- $host ping 统计 ---")
                trySend("发送包数 = $transmitted，接收包数 = $received，丢失 = ${transmitted - received} (${round(loss)}% 丢失)")
                if (received > 0) {
                    trySend("最短 = ${round(min).toInt()}ms，最长 = ${round(max).toInt()}ms，平均 = ${round(avg).toInt()}ms")
                }
                close()
            }
        })

        // 开始第一次 ping
        startTime = System.currentTimeMillis()
        if (count > 0) {
            pinger.ping(host, 1)
        } else {
            // 无限 ping 模式：持续 ping 直到协程被取消
            pinger.pingUntilSucceeded(host, 0)
        }

        awaitClose {
            pinger.setOnPingListener(null)
        }
    }.flowOn(Dispatchers.IO)
}

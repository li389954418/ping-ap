package com.example.nettool

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
<<<<<<< HEAD
import icmp4j.Icmp4j
import icmp4j.IcmpPingRequest
import icmp4j.IcmpPingResponse
import java.net.InetAddress
import kotlin.math.round
=======
import java.io.BufferedReader
import java.io.InputStreamReader
>>>>>>> b2a7d519900564732ff668a4801582e1fa3450dc

object IcmpPing {
    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        timeout: Int = 2000
    ): Flow<String> = flow {
<<<<<<< HEAD
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
=======
        val command = buildList {
            add("ping")
            if (count > 0) {
                add("-c")
                add(count.toString())
            }
            add("-s")
            add(packetSize.toString())
            add("-W")
            add((timeout / 1000).toString())
            add(host)
        }.toTypedArray()

        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val job = currentCoroutineContext()[Job]
            job?.invokeOnCompletion {
                process.destroy()
            }

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line ?: "")
                currentCoroutineContext().ensureActive()
            }
            while (errorReader.readLine().also { line = it } != null) {
                emit("ERROR: $line")
            }

            val exitCode = process.waitFor()
            reader.close()
            errorReader.close()

            if (exitCode != 0 && count > 0) {
                emit("Ping 命令退出码: $exitCode")
            }
        } catch (e: CancellationException) {
            emit("\n--- Ping 已取消 ---")
            throw e
        } catch (e: Exception) {
            emit("ICMP Ping 失败: ${e.message}")
>>>>>>> b2a7d519900564732ff668a4801582e1fa3450dc
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
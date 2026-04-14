package com.example.nettool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object IcmpPing {

    fun ping(
        host: String,
        count: Int = 4,          // Ping次数，-1为无限
        packetSize: Int = 56,    // 数据包大小
        timeout: Int = 2000      // 单次等待响应的超时（毫秒），对应 -W 参数
    ): Flow<String> = channelFlow {
        
        // 1. 参数验证
        require(count > 0 || count == -1 || count == 0) { "数据包数量必须为正整数、-1或0" }
        
        val isInfinity = count == -1 || count == 0
        val safeCount = if (isInfinity) 0 else count.coerceAtLeast(1).coerceAtMost(999999)

        // 2. 构建命令
        val command = buildList {
            add("ping")
            // 如果不是无限Ping，添加次数参数
            if (!isInfinity) {
                add("-c")
                add(safeCount.toString())
            }
            // 数据包大小
            add("-s")
            add(packetSize.toString())
            // 单次超时时间 (-W)，单位转换为秒
            add("-W")
            add((timeout / 1000).coerceAtLeast(1).toString())
            add(host)
        }.toTypedArray()

        var process: Process? = null

        try {
            // 3. 启动进程
            process = Runtime.getRuntime().exec(command)

            // --- 【修改点】立即发送“开始提示” ---
            // 这样用户点击后能立刻看到反馈，知道程序正在运行
            send("正在 Ping $host [大小: ${packetSize}B, 超时: ${timeout}ms]...")

            // 4. 读取标准输出流 (实时返回 Ping 结果)
            val outputJob = launch(Dispatchers.IO) {
                try {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            trySend(line)
                        }
                    }
                } catch (e: Exception) {
                    // 忽略流中断
                }
            }

            // 5. 读取错误流 (捕获权限错误等)
            val errorJob = launch(Dispatchers.IO) {
                try {
                    process.errorStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            trySend("[Error] $line")
                        }
                    }
                } catch (e: Exception) {
                    // 忽略
                }
            }

            // 6. 监控进程存活状态
            // 只要进程还活着，就一直等（支持无限Ping）
            while (isActive && process?.isAlive == true) {
                delay(500) 
            }

            // 等待读取线程结束，确保所有输出都被发送
            outputJob.join()
            errorJob.join()

        } catch (e: Exception) {
            send("Ping 启动失败: ${e.message ?: "Unknown Error"}")
        } finally {
            // 7. 最终清理：确保进程被杀死
            process?.destroyForcibly()
        }

        awaitClose {
            process?.destroyForcibly()
        }
    }.flowOn(Dispatchers.IO)
}

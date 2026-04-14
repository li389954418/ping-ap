package com.example.nettool

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

object IcmpPing {

    // 修改 1: 将 Flow<String> 改为 Flow<String?>，或者我们在内部处理 null。
    // 这里我们选择在内部过滤掉 null，保持对外接口为 Flow<String>
    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        timeout: Int = 2000
    ): Flow<String> = channelFlow {
        val command = buildList {
            add("ping")
            add("-c")
            add(count.toString())
            add("-s")
            add(packetSize.toString())
            add("-W")
            add((timeout / 1000).coerceAtLeast(1).toString())
            add(host)
        }.toTypedArray()

        var process: Process? = null
        var outputJob: Job? = null
        var errorJob: Job? = null

        try {
            // 1. 启动进程
            process = Runtime.getRuntime().exec(command)

            // 2. 创建读取任务
            val inputStream = InputStreamReader(process.inputStream)
            val errorStream = InputStreamReader(process.errorStream)

            // 3. 读取标准输出
            outputJob = launch(Dispatchers.IO) {
                BufferedReader(inputStream).use { reader ->
                    // 修改 2: 正确处理 readLine() 的 null 返回值
                    while (isActive) {
                        val line = reader.readLine() ?: break // 如果为 null，跳出循环
                        send(line) // line 此时确定为非空
                    }
                }
            }

            // 4. 读取错误输出
            errorJob = launch(Dispatchers.IO) {
                BufferedReader(errorStream).use { reader ->
                    // 修改 2: 正确处理 readLine() 的 null 返回值
                    while (isActive) {
                        val line = reader.readLine() ?: break // 如果为 null，跳出循环
                        send(line) // line 此时确定为非空
                    }
                }
            }

            // 5. 等待进程结束或超时
            try {
                val exitCode = withTimeout(15_000) { // 增加一点超时时间
                    process.waitFor()
                }
                outputJob?.join()
                errorJob?.join()
                if (exitCode != 0) {
                    // send("Ping 结束，退出码: $exitCode") // 如果需要显示退出码可以打开
                }
            } catch (e: TimeoutCancellationException) {
                // 处理超时
                process.destroyForcibly()
                send("Ping 超时")
            } catch (e: Exception) {
                process.destroyForcibly()
                send("Ping 异常: ${e.message}")
            }

        } catch (e: Exception) {
            // 修改 3: send 的参数必须是非空，所以这里确保字符串非空
            send("Ping 启动失败: ${e.message ?: "Unknown Error"}")
        } finally {
            // 确保进程被销毁
            process?.destroyForcibly()
        }
        awaitClose {
            process?.destroyForcibly()
        }
    }.flowOn(Dispatchers.IO)
}

package com.example.nettool

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object IcmpPing {
    fun ping(
        host: String,
        count: Int = 4,
        packetSize: Int = 56,
        timeout: Int = 2000,
        interval: Int = 1000
    ): Flow<String> = channelFlow {
        val command = mutableListOf("ping")

        if (count > 0) {
            command.add("-c")
            command.add(count.toString())
        }

        command.add("-W")
        command.add((timeout / 1000).toString())

        command.add("-i")
        command.add((interval / 1000).toString())

        command.add("-s")
        command.add(packetSize.toString())

        command.add(host)

        var process: Process? = null
        var inputReader: BufferedReader? = null
        var errorReader: BufferedReader? = null

        val job = coroutineContext[Job]
        try {
            process = Runtime.getRuntime().exec(command.toTypedArray())
            inputReader = BufferedReader(InputStreamReader(process.inputStream))
            errorReader = BufferedReader(InputStreamReader(process.errorStream))

            job?.invokeOnCompletion {
                process?.destroy()
                process?.waitFor(500, TimeUnit.MILLISECONDS)
                if (process?.isAlive == true) {
                    process?.destroyForcibly()
                }
            }

            coroutineScope {
                val outputJob = launch(Dispatchers.IO) {
                    var line: String?
                    while (inputReader.readLine().also { line = it } != null) {
                        send(line!!)
                    }
                }
                val errorJob = launch(Dispatchers.IO) {
                    var line: String?
                    while (errorReader.readLine().also { line = it } != null) {
                        send(line!!)
                    }
                }
                outputJob.join()
                errorJob.join()
            }
        } catch (e: CancellationException) {
            send("\n--- Ping 已手动取消 ---")
            throw e
        } catch (e: Exception) {
            send("执行异常: ${e.message}")
        } finally {
            withContext(NonCancellable) {
                inputReader?.close()
                errorReader?.close()
                process?.destroyForcibly()
            }
        }
        awaitClose()
    }.flowOn(Dispatchers.IO)
}

package com.example.nettool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

object WebDavClient {
    suspend fun upload(config: WebDavConfig, remotePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${config.serverUrl.trimEnd('/')}/$remotePath")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "Basic ${Base64.getEncoder().encodeToString("${config.username}:${config.password}".toByteArray())}")
            connection.doOutput = true
            connection.outputStream.use { it.write(content.toByteArray()) }
            connection.responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun download(config: WebDavConfig, remotePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${config.serverUrl.trimEnd('/')}/$remotePath")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Basic ${Base64.getEncoder().encodeToString("${config.username}:${config.password}".toByteArray())}")
            connection.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun listFiles(config: WebDavConfig, remotePath: String = ""): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${config.serverUrl.trimEnd('/')}/$remotePath")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PROPFIND"
            connection.setRequestProperty("Authorization", "Basic ${Base64.getEncoder().encodeToString("${config.username}:${config.password}".toByteArray())}")
            connection.setRequestProperty("Depth", "1")
            connection.inputStream.bufferedReader().readText().let { response ->
                // 简单解析，实际应使用 XML 解析器
                Regex("<D:href>(.*?)</D:href>").findAll(response).map { it.groupValues[1] }.filter { it != "/$remotePath" }.toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

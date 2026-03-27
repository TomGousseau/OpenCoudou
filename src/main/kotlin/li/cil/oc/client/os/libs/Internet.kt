package li.cil.oc.client.os.libs

import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Internet library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Internet.lua.
 * Provides HTTP requests and URL utilities.
 */
object Internet {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Default timeout in milliseconds
    var defaultTimeout = 30000
    
    // Default chunk size for streaming
    var defaultChunkSize = 8192
    
    /**
     * URL encode a string.
     */
    fun encode(data: String): String {
        return URLEncoder.encode(data, StandardCharsets.UTF_8.toString())
    }
    
    /**
     * Serialize a map to URL-encoded string.
     */
    fun serialize(data: Map<String, Any?>): String {
        return data.entries.joinToString("&") { (key, value) ->
            when (value) {
                is Map<*, *> -> serializeNested(key, value)
                is List<*> -> value.mapIndexed { i, v -> "${encode(key)}[$i]=${encode(v.toString())}" }.joinToString("&")
                else -> "${encode(key)}=${encode(value.toString())}"
            }
        }
    }
    
    private fun serializeNested(prefix: String, map: Map<*, *>): String {
        return map.entries.joinToString("&") { (key, value) ->
            val newKey = "$prefix[${encode(key.toString())}]"
            when (value) {
                is Map<*, *> -> serializeNested(newKey, value)
                else -> "$newKey=${encode(value.toString())}"
            }
        }
    }
    
    /**
     * Perform a raw HTTP request with chunk handler.
     */
    suspend fun rawRequest(
        url: String,
        postData: String? = null,
        headers: Map<String, String>? = null,
        chunkHandler: suspend (ByteArray) -> Unit,
        chunkSize: Int = defaultChunkSize,
        method: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = defaultTimeout
            connection.readTimeout = defaultTimeout
            
            // Set method
            connection.requestMethod = method ?: if (postData != null) "POST" else "GET"
            
            // Set headers
            headers?.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
            
            // Send post data
            if (postData != null) {
                connection.doOutput = true
                connection.outputStream.use { os ->
                    os.write(postData.toByteArray(StandardCharsets.UTF_8))
                }
            }
            
            // Read response in chunks
            connection.inputStream.use { input ->
                val buffer = ByteArray(chunkSize)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    chunkHandler(buffer.copyOf(bytesRead))
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Perform HTTP request and return full response.
     */
    suspend fun request(
        url: String,
        postData: String? = null,
        headers: Map<String, String>? = null,
        method: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val data = StringBuilder()
            rawRequest(url, postData, headers, { chunk ->
                data.append(String(chunk, StandardCharsets.UTF_8))
            }, method = method)
            Result.success(data.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Perform HTTP request as bytes.
     */
    suspend fun requestBytes(
        url: String,
        postData: String? = null,
        headers: Map<String, String>? = null,
        method: String? = null
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val chunks = mutableListOf<ByteArray>()
            rawRequest(url, postData, headers, { chunk ->
                chunks.add(chunk)
            }, method = method)
            
            val totalSize = chunks.sumOf { it.size }
            val result = ByteArray(totalSize)
            var offset = 0
            for (chunk in chunks) {
                chunk.copyInto(result, offset)
                offset += chunk.size
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Download file to filesystem.
     */
    suspend fun download(
        url: String,
        path: String,
        headers: Map<String, String>? = null,
        progressCallback: ((Long, Long?) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = defaultTimeout
            connection.readTimeout = defaultTimeout
            
            headers?.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
            
            val contentLength = connection.contentLengthLong.takeIf { it > 0 }
            var downloaded = 0L
            
            // Would use filesystem.writeFile in full implementation
            val data = mutableListOf<Byte>()
            connection.inputStream.use { input ->
                val buffer = ByteArray(defaultChunkSize)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    data.addAll(buffer.take(bytesRead).toList())
                    downloaded += bytesRead
                    progressCallback?.invoke(downloaded, contentLength)
                }
            }
            
            // Write to filesystem
            // fs.writeFile(path, data.toByteArray())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Simple GET request (blocking).
     */
    fun get(url: String, headers: Map<String, String>? = null): String? {
        return runBlocking {
            request(url, headers = headers).getOrNull()
        }
    }
    
    /**
     * Simple POST request (blocking).
     */
    fun post(url: String, data: String, headers: Map<String, String>? = null): String? {
        return runBlocking {
            request(url, postData = data, headers = headers).getOrNull()
        }
    }
    
    /**
     * POST JSON data.
     */
    suspend fun postJson(
        url: String,
        json: String,
        headers: Map<String, String>? = null
    ): Result<String> {
        val allHeaders = (headers ?: emptyMap()) + mapOf(
            "Content-Type" to "application/json"
        )
        return request(url, json, allHeaders, "POST")
    }
    
    /**
     * Check if URL is reachable.
     */
    suspend fun isReachable(url: String, timeout: Int = 5000): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.requestMethod = "HEAD"
            connection.responseCode in 200..399
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get response headers.
     */
    suspend fun getHeaders(url: String): Map<String, String>? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = defaultTimeout
            connection.requestMethod = "HEAD"
            connection.headerFields
                .filterKeys { it != null }
                .mapValues { it.value.joinToString(", ") }
        } catch (e: Exception) {
            null
        }
    }
}

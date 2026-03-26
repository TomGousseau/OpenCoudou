package li.cil.oc.server.component

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ComponentVisibility
import li.cil.oc.Settings
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Internet card component — provides HTTP/TCP access to external networks.
 *
 * Security: Only allows connections to whitelisted hosts (configurable).
 * Callbacks signal events instead of blocking.
 */
class InternetCardComponent : ComponentBase("internet") {

    // Thread pool for async HTTP requests
    private val executor = Executors.newCachedThreadPool { r ->
        Thread(r, "OC-InternetCard").also { it.isDaemon = true }
    }

    // Active requests: requestId -> Future
    private val activeRequests = ConcurrentHashMap<String, Future<*>>()

    // Pending response chunks, keyed by request ID
    private val pendingChunks = ConcurrentHashMap<String, ArrayDeque<String?>>()

    // Active TCP connections (simple socket streams)
    private val tcpConnections = ConcurrentHashMap<String, TcpConnection>()

    override fun methods() = mapOf(
        "isHttpEnabled"    to ::isHttpEnabled,
        "isTcpEnabled"     to ::isTcpEnabled,
        "checkUrl"         to ::checkUrl,
        "request"          to ::request,
        "hasData"          to ::hasData,
        "read"             to ::readChunk,
        "close"            to ::close,
        "connect"          to ::connect,
        "write"            to ::write,
        "finishConnect"    to ::finishConnect,
    )

    // ------- Utility -------

    private fun isUrlAllowed(url: String): Boolean {
        if (!Settings.internetEnabled) return false
        val host = try {
            URI(url).host?.lowercase() ?: return false
        } catch (e: Exception) {
            return false
        }
        val whitelist = Settings.internetWhitelist
        if (whitelist.isEmpty()) return true
        return whitelist.any { pattern ->
            host == pattern || host.endsWith(".$pattern")
        }
    }

    // ------- HTTP Methods -------

    private fun isHttpEnabled(ctx: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(Settings.internetEnabled)
    }

    private fun isTcpEnabled(ctx: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(Settings.internetEnabled)
    }

    private fun checkUrl(ctx: Context, args: Array<Any?>): Array<Any?> {
        val url = args.getOrNull(0) as? String ?: return arrayOf(false, "no URL provided")
        return if (isUrlAllowed(url)) arrayOf(true) else arrayOf(false, "URL not allowed by server config")
    }

    /**
     * Start an HTTP request.
     * @param url  The URL to request
     * @param postData  (optional) POST body — if present, POST is used; otherwise GET
     * @param headers   (optional) table of extra headers
     * @param method    (optional) HTTP method override
     * @return requestId string, or (false, errorMsg)
     */
    private fun request(ctx: Context, args: Array<Any?>): Array<Any?> {
        val url = args.getOrNull(0) as? String
            ?: return arrayOf(false, "URL required")

        if (!isUrlAllowed(url)) {
            return arrayOf(false, "URL not allowed: $url")
        }

        val postBody = args.getOrNull(1) as? String
        @Suppress("UNCHECKED_CAST")
        val headersMap = args.getOrNull(2) as? Map<String, String> ?: emptyMap()
        val methodOverride = (args.getOrNull(3) as? String)?.uppercase()

        val reqId = UUID.randomUUID().toString()
        val queue = ArrayDeque<String?>()
        pendingChunks[reqId] = queue

        val computerAddress = node()?.address ?: "unknown"

        val future = executor.submit {
            try {
                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                connection.connectTimeout = Settings.internetTimeout
                connection.readTimeout = Settings.internetTimeout
                connection.instanceFollowRedirects = true
                connection.useCaches = false

                // Set method
                val method = methodOverride ?: if (postBody != null) "POST" else "GET"
                connection.requestMethod = method

                // Set headers
                connection.setRequestProperty("User-Agent", "OpenComputers/1.0 (+Minecraft)")
                for ((k, v) in headersMap) {
                    connection.setRequestProperty(k, v)
                }

                // Write POST body
                if (postBody != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Length", postBody.length.toString())
                    val os: OutputStream = connection.outputStream
                    os.write(postBody.toByteArray(Charsets.UTF_8))
                    os.flush()
                    os.close()
                }

                // Read response status
                val statusCode = connection.responseCode
                val statusMsg = connection.responseMessage ?: ""

                // First chunk: status line + headers
                val sb = StringBuilder()
                sb.append("HTTP/1.1 $statusCode $statusMsg\r\n")
                for ((k, vals) in connection.headerFields) {
                    if (k != null) {
                        for (v in vals) {
                            sb.append("$k: $v\r\n")
                        }
                    }
                }
                sb.append("\r\n")
                synchronized(queue) { queue.addLast(sb.toString()) }

                // Stream body
                val stream = if (statusCode >= 400) connection.errorStream else connection.inputStream
                if (stream != null) {
                    val buf = ByteArray(2048)
                    var n: Int
                    while (stream.read(buf).also { n = it } != -1) {
                        if (Thread.currentThread().isInterrupted) break
                        val chunk = String(buf, 0, n, Charsets.UTF_8)
                        synchronized(queue) { queue.addLast(chunk) }
                    }
                    stream.close()
                }

                // Signal end-of-response with null
                synchronized(queue) { queue.addLast(null) }

                // Fire signal to computer
                ctx.signal("internet_response", reqId, null)

            } catch (e: InterruptedException) {
                synchronized(pendingChunks[reqId]!!) { pendingChunks[reqId]!!.addLast(null) }
            } catch (e: Exception) {
                val errMsg = e.message ?: e.javaClass.simpleName
                synchronized(pendingChunks[reqId] ?: ArrayDeque()) {
                    pendingChunks[reqId]?.addLast(null)
                }
                ctx.signal("internet_response", reqId, errMsg)
            } finally {
                activeRequests.remove(reqId)
            }
        }

        activeRequests[reqId] = future
        return arrayOf(reqId)
    }

    /**
     * Check if a request has data available.
     */
    private fun hasData(ctx: Context, args: Array<Any?>): Array<Any?> {
        val reqId = args.getOrNull(0) as? String ?: return arrayOf(false)
        val queue = pendingChunks[reqId] ?: return arrayOf(false)
        synchronized(queue) { return arrayOf(queue.isNotEmpty()) }
    }

    /**
     * Read the next available chunk from a request.
     * Returns nil when the response ends; returns (false, error) on error.
     */
    private fun readChunk(ctx: Context, args: Array<Any?>): Array<Any?> {
        val reqId = args.getOrNull(0) as? String ?: return arrayOf(null, "invalid request ID")
        val queue = pendingChunks[reqId] ?: return arrayOf(null, "unknown request")
        val chunk = synchronized(queue) {
            if (queue.isEmpty()) return arrayOf()  // No data yet, yield
            queue.removeFirst()
        }
        if (chunk == null) {
            // End of response
            pendingChunks.remove(reqId)
            return arrayOf(null)
        }
        return arrayOf(chunk)
    }

    /**
     * Cancel a request.
     */
    private fun close(ctx: Context, args: Array<Any?>): Array<Any?> {
        val reqId = args.getOrNull(0) as? String ?: return arrayOf()
        activeRequests[reqId]?.cancel(true)
        activeRequests.remove(reqId)
        pendingChunks.remove(reqId)
        tcpConnections[reqId]?.close()
        tcpConnections.remove(reqId)
        return arrayOf()
    }

    // ------- TCP Socket Methods -------

    /**
     * Open a TCP connection to a host:port.
     * Returns a socket handle ID.
     */
    private fun connect(ctx: Context, args: Array<Any?>): Array<Any?> {
        val address = args.getOrNull(0) as? String ?: return arrayOf(false, "address required")
        val port = (args.getOrNull(1) as? Double)?.toInt() ?: 80

        // Validate host
        val host = address.split(":").firstOrNull() ?: address
        if (!isUrlAllowed("tcp://$host")) {
            return arrayOf(false, "connection not allowed")
        }

        val connId = UUID.randomUUID().toString()
        val conn = TcpConnection(host, port)

        val future = executor.submit {
            try {
                conn.connect(Settings.internetTimeout)
                ctx.signal("internet_connected", connId, true)
            } catch (e: Exception) {
                ctx.signal("internet_connected", connId, false, e.message ?: "connection failed")
            }
        }

        tcpConnections[connId] = conn
        activeRequests[connId] = future

        return arrayOf(connId)
    }

    /**
     * Check if TCP connection is established.
     */
    private fun finishConnect(ctx: Context, args: Array<Any?>): Array<Any?> {
        val connId = args.getOrNull(0) as? String ?: return arrayOf(false)
        val conn = tcpConnections[connId] ?: return arrayOf(false, "unknown connection")
        return arrayOf(conn.isConnected)
    }

    /**
     * Write data to a TCP connection.
     */
    private fun write(ctx: Context, args: Array<Any?>): Array<Any?> {
        val connId = args.getOrNull(0) as? String ?: return arrayOf(false, "connection ID required")
        val data = args.getOrNull(1) as? String ?: return arrayOf(false, "data required")
        val conn = tcpConnections[connId] ?: return arrayOf(false, "unknown connection")
        return try {
            conn.write(data)
            arrayOf(true)
        } catch (e: Exception) {
            arrayOf(false, e.message)
        }
    }

    fun onDisconnect() {
        // Cancel all pending requests when computer disconnects
        activeRequests.values.forEach { it.cancel(true) }
        activeRequests.clear()
        pendingChunks.clear()
        tcpConnections.values.forEach { it.close() }
        tcpConnections.clear()
        executor.shutdownNow()
    }
}

/**
 * Simple TCP connection wrapper.
 */
class TcpConnection(private val host: String, private val port: Int) {
    private var socket: java.net.Socket? = null
    private var outputStream: java.io.OutputStream? = null
    private var inputStream: java.io.InputStream? = null
    var isConnected = false
        private set

    fun connect(timeoutMs: Int) {
        val s = java.net.Socket()
        s.connect(java.net.InetSocketAddress(host, port), timeoutMs)
        s.soTimeout = timeoutMs
        socket = s
        outputStream = s.getOutputStream()
        inputStream = s.getInputStream()
        isConnected = true
    }

    fun write(data: String) {
        outputStream?.write(data.toByteArray(Charsets.UTF_8))
        outputStream?.flush()
    }

    fun read(maxBytes: Int = 2048): String? {
        val buf = ByteArray(maxBytes)
        val n = inputStream?.read(buf) ?: return null
        if (n == -1) return null
        return String(buf, 0, n, Charsets.UTF_8)
    }

    fun close() {
        isConnected = false
        try { socket?.close() } catch (_: Exception) {}
    }
}

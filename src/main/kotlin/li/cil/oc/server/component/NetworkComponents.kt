package li.cil.oc.server.component

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import java.io.*
import java.net.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.*
import kotlin.math.min

/**
 * Internet card component - provides HTTP and TCP networking.
 */
class InternetComponent : ComponentBase("internet") {
    
    private val openConnections = ConcurrentHashMap<Int, NetworkConnection>()
    private var nextConnectionId = 0
    private val maxConnections = 4
    private val executor = Executors.newCachedThreadPool()
    
    interface NetworkConnection : Closeable {
        fun read(count: Int): ByteArray?
        fun write(data: ByteArray): Boolean
        fun id(): Int
        fun finishConnect(): Boolean
    }
    
    @Callback(doc = "function(url: string[, postData: string[, headers: table]]): userdata -- Starts an HTTP request")
    fun request(context: Context, args: Array<Any?>): Array<Any?> {
        if (openConnections.size >= maxConnections) {
            return arrayOf(null, "too many connections")
        }
        
        val urlStr = args.getOrNull(0) as? String ?: return arrayOf(null, "invalid url")
        val postData = args.getOrNull(1) as? String
        @Suppress("UNCHECKED_CAST")
        val headers = args.getOrNull(2) as? Map<String, String> ?: emptyMap()
        
        try {
            val url = URL(urlStr)
            if (url.protocol != "http" && url.protocol != "https") {
                return arrayOf(null, "unsupported protocol")
            }
            
            val id = nextConnectionId++
            val connection = HttpConnection(id, url, postData, headers, executor)
            openConnections[id] = connection
            
            return arrayOf(HttpHandle(connection))
        } catch (e: MalformedURLException) {
            return arrayOf(null, "invalid url")
        }
    }
    
    @Callback(doc = "function(address: string, port: number): userdata -- Opens a TCP connection")
    fun connect(context: Context, args: Array<Any?>): Array<Any?> {
        if (openConnections.size >= maxConnections) {
            return arrayOf(null, "too many connections")
        }
        
        val address = args.getOrNull(0) as? String ?: return arrayOf(null, "invalid address")
        val port = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(null, "invalid port")
        
        if (port < 1 || port > 65535) {
            return arrayOf(null, "port out of range")
        }
        
        try {
            val id = nextConnectionId++
            val connection = TcpConnection(id, address, port, executor)
            openConnections[id] = connection
            
            return arrayOf(TcpHandle(connection))
        } catch (e: Exception) {
            return arrayOf(null, e.message ?: "connection failed")
        }
    }
    
    @Callback(doc = "function(): boolean -- Checks if TCP connections are available")
    fun isTcpEnabled(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): boolean -- Checks if HTTP requests are available")
    fun isHttpEnabled(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    fun closeConnection(id: Int) {
        openConnections.remove(id)?.close()
    }
    
    fun closeAll() {
        openConnections.values.forEach { it.close() }
        openConnections.clear()
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        // Connections are not persisted - they will be closed on save
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        closeAll()
    }
    
    private class HttpConnection(
        private val id: Int,
        private val url: URL,
        private val postData: String?,
        private val headers: Map<String, String>,
        private val executor: ExecutorService
    ) : NetworkConnection {
        
        private var future: Future<HttpResult>? = null
        private var result: HttpResult? = null
        private var position = 0
        
        init {
            future = executor.submit<HttpResult> {
                try {
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    
                    headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                    
                    if (postData != null) {
                        conn.requestMethod = "POST"
                        conn.doOutput = true
                        conn.outputStream.use { it.write(postData.toByteArray()) }
                    }
                    
                    val responseCode = conn.responseCode
                    val responseHeaders = mutableMapOf<String, String>()
                    conn.headerFields.forEach { (k, v) ->
                        if (k != null && v.isNotEmpty()) {
                            responseHeaders[k] = v.joinToString(", ")
                        }
                    }
                    
                    val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                    val data = stream?.readAllBytes() ?: ByteArray(0)
                    
                    HttpResult(responseCode, responseHeaders, data, null)
                } catch (e: Exception) {
                    HttpResult(-1, emptyMap(), ByteArray(0), e.message)
                }
            }
        }
        
        override fun id() = id
        
        override fun finishConnect(): Boolean {
            if (result != null) return true
            val f = future ?: return false
            if (!f.isDone) return false
            result = f.get()
            return true
        }
        
        override fun read(count: Int): ByteArray? {
            val r = result ?: return null
            if (r.error != null) return null
            if (position >= r.data.size) return null
            
            val toRead = min(count, r.data.size - position)
            val data = r.data.copyOfRange(position, position + toRead)
            position += toRead
            return data
        }
        
        override fun write(data: ByteArray): Boolean = false
        
        override fun close() {
            future?.cancel(true)
        }
        
        fun getResponseCode(): Int = result?.code ?: -1
        fun getResponseHeaders(): Map<String, String> = result?.headers ?: emptyMap()
        fun getError(): String? = result?.error
    }
    
    private data class HttpResult(
        val code: Int,
        val headers: Map<String, String>,
        val data: ByteArray,
        val error: String?
    )
    
    private class TcpConnection(
        private val id: Int,
        private val address: String,
        private val port: Int,
        private val executor: ExecutorService
    ) : NetworkConnection {
        
        private var socket: Socket? = null
        private var connectFuture: Future<Boolean>? = null
        private var connected = false
        
        init {
            connectFuture = executor.submit<Boolean> {
                try {
                    socket = Socket()
                    socket?.connect(InetSocketAddress(address, port), 5000)
                    socket?.soTimeout = 100
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        override fun id() = id
        
        override fun finishConnect(): Boolean {
            if (connected) return true
            val f = connectFuture ?: return false
            if (!f.isDone) return false
            connected = f.get()
            return connected
        }
        
        override fun read(count: Int): ByteArray? {
            if (!connected) return null
            val s = socket ?: return null
            
            try {
                val buffer = ByteArray(count)
                val read = s.getInputStream().read(buffer)
                if (read <= 0) return null
                return if (read == count) buffer else buffer.copyOf(read)
            } catch (e: SocketTimeoutException) {
                return ByteArray(0) // No data available
            } catch (e: Exception) {
                return null
            }
        }
        
        override fun write(data: ByteArray): Boolean {
            if (!connected) return false
            val s = socket ?: return false
            
            try {
                s.getOutputStream().write(data)
                return true
            } catch (e: Exception) {
                return false
            }
        }
        
        override fun close() {
            connectFuture?.cancel(true)
            try {
                socket?.close()
            } catch (e: Exception) {}
        }
    }
    
    class HttpHandle(private val conn: HttpConnection) {
        fun read(count: Int = Int.MAX_VALUE): ByteArray? = conn.read(count)
        fun finishConnect(): Boolean = conn.finishConnect()
        fun response(): Int = conn.getResponseCode()
        fun headers(): Map<String, String> = conn.getResponseHeaders()
        fun close() = conn.close()
    }
    
    class TcpHandle(private val conn: TcpConnection) {
        fun read(count: Int = Int.MAX_VALUE): ByteArray? = conn.read(count)
        fun write(data: ByteArray): Boolean = conn.write(data)
        fun finishConnect(): Boolean = conn.finishConnect()
        fun close() = conn.close()
    }
}

/**
 * Modem component - provides wireless and wired networking within the game.
 */
class ModemComponent(val isWireless: Boolean = false) : ComponentBase("modem") {
    
    private val openPorts = mutableSetOf<Int>()
    private var strength = if (isWireless) 16 else 0
    private var wakeMessage: String? = null
    private var wakeFuzzy = false
    
    @Callback(doc = "function(port: number): boolean -- Opens a port for listening")
    fun open(context: Context, args: Array<Any?>): Array<Any?> {
        val port = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid port")
        if (port < 1 || port > 65535) return arrayOf(null, "port out of range")
        if (openPorts.size >= 128) return arrayOf(null, "too many open ports")
        
        return arrayOf(openPorts.add(port))
    }
    
    @Callback(doc = "function(port: number): boolean -- Closes a port")
    fun close(context: Context, args: Array<Any?>): Array<Any?> {
        val port = (args.getOrNull(0) as? Number)?.toInt()
        
        if (port == null) {
            // Close all ports
            openPorts.clear()
            return arrayOf(true)
        }
        
        return arrayOf(openPorts.remove(port))
    }
    
    @Callback(doc = "function(port: number): boolean -- Checks if a port is open")
    fun isOpen(context: Context, args: Array<Any?>): Array<Any?> {
        val port = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(false)
        return arrayOf(port in openPorts)
    }
    
    @Callback(doc = "function(address: string, port: number, ...): boolean -- Sends a message to an address")
    fun send(context: Context, args: Array<Any?>): Array<Any?> {
        val address = args.getOrNull(0) as? String ?: return arrayOf(null, "invalid address")
        val port = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(null, "invalid port")
        
        // Would send actual network message
        return arrayOf(true)
    }
    
    @Callback(doc = "function(port: number, ...): boolean -- Broadcasts a message")
    fun broadcast(context: Context, args: Array<Any?>): Array<Any?> {
        val port = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid port")
        
        // Would broadcast actual network message
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): number -- Gets the signal strength")
    fun getStrength(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(strength)
    }
    
    @Callback(doc = "function(strength: number): number -- Sets the signal strength (wireless only)")
    fun setStrength(context: Context, args: Array<Any?>): Array<Any?> {
        if (!isWireless) return arrayOf(null, "not a wireless modem")
        
        val old = strength
        strength = ((args.getOrNull(0) as? Number)?.toInt() ?: 16).coerceIn(1, 400)
        return arrayOf(old)
    }
    
    @Callback(doc = "function(): boolean -- Checks if this is a wireless modem")
    fun isWireless(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(isWireless)
    }
    
    @Callback(doc = "function(): number -- Gets the maximum packet size")
    fun maxPacketSize(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(8192)
    }
    
    @Callback(doc = "function(): string -- Gets the wake message")
    fun getWakeMessage(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(wakeMessage)
    }
    
    @Callback(doc = "function(message: string[, fuzzy: boolean]): string -- Sets the wake message")
    fun setWakeMessage(context: Context, args: Array<Any?>): Array<Any?> {
        val old = wakeMessage
        wakeMessage = args.getOrNull(0) as? String
        wakeFuzzy = args.getOrNull(1) as? Boolean ?: false
        return arrayOf(old)
    }
    
    fun receiveMessage(sender: String, port: Int, distance: Double, vararg data: Any) {
        if (port !in openPorts) return
        queueSignal("modem_message", address, sender, port, distance, *data)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putBoolean("wireless", isWireless)
        tag.putInt("strength", strength)
        wakeMessage?.let { tag.putString("wakeMessage", it) }
        tag.putBoolean("wakeFuzzy", wakeFuzzy)
        
        val ports = ListTag()
        openPorts.forEach { ports.add(StringTag.valueOf(it.toString())) }
        tag.put("ports", ports)
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        strength = tag.getInt("strength")
        wakeMessage = if (tag.contains("wakeMessage")) tag.getString("wakeMessage") else null
        wakeFuzzy = tag.getBoolean("wakeFuzzy")
        
        openPorts.clear()
        val ports = tag.getList("ports", Tag.TAG_STRING.toInt())
        for (i in 0 until ports.size) {
            openPorts.add(ports.getString(i).toIntOrNull() ?: continue)
        }
    }
}

/**
 * Linked card component - instant communication over any distance.
 */
class LinkedComponent : ComponentBase("tunnel") {
    
    private var linkedChannel: String? = null
    
    @Callback(doc = "function(...): boolean -- Sends a message through the tunnel")
    fun send(context: Context, args: Array<Any?>): Array<Any?> {
        // Would send through linked channel
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): number -- Gets the maximum packet size")
    fun maxPacketSize(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(8192)
    }
    
    @Callback(doc = "function(): string -- Gets the channel ID")
    fun getChannel(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(linkedChannel ?: "")
    }
    
    @Callback(doc = "function(): string -- Gets the wake message")
    fun getWakeMessage(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(null)
    }
    
    @Callback(doc = "function(message: string): string -- Sets the wake message")
    fun setWakeMessage(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(null)
    }
    
    fun receiveMessage(vararg data: Any) {
        queueSignal("modem_message", address, "", 0, 0.0, *data)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        linkedChannel?.let { tag.putString("channel", it) }
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        linkedChannel = if (tag.contains("channel")) tag.getString("channel") else null
    }
}

/**
 * Network card component - wired network communication.
 */
class NetworkCardComponent : ComponentBase("modem") {
    
    private val openPorts = mutableSetOf<Int>()
    
    @Callback(doc = "function(port: number): boolean -- Opens a port")
    fun open(context: Context, args: Array<Any?>): Array<Any?> {
        val port = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid port")
        if (port < 1 || port > 65535) return arrayOf(null, "port out of range")
        return arrayOf(openPorts.add(port))
    }
    
    @Callback(doc = "function(port: number): boolean -- Closes a port")
    fun close(context: Context, args: Array<Any?>): Array<Any?> {
        val port = (args.getOrNull(0) as? Number)?.toInt()
        if (port == null) {
            openPorts.clear()
            return arrayOf(true)
        }
        return arrayOf(openPorts.remove(port))
    }
    
    @Callback(doc = "function(port: number): boolean -- Checks if a port is open")
    fun isOpen(context: Context, args: Array<Any?>): Array<Any?> {
        val port = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(false)
        return arrayOf(port in openPorts)
    }
    
    @Callback(doc = "function(address: string, port: number, ...): boolean -- Sends a message")
    fun send(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(port: number, ...): boolean -- Broadcasts a message")
    fun broadcast(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): boolean -- Checks if this is wireless")
    fun isWireless(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(false)
    }
    
    @Callback(doc = "function(): number -- Gets the max packet size")
    fun maxPacketSize(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(8192)
    }
}

/**
 * Access point component - bridges wired and wireless networks.
 */
class AccessPointComponent : ComponentBase("access_point") {
    
    private var strength = 16
    private var bridgeMode = true
    
    @Callback(doc = "function(): number -- Gets the signal strength")
    fun getStrength(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(strength)
    }
    
    @Callback(doc = "function(strength: number): number -- Sets the signal strength")
    fun setStrength(context: Context, args: Array<Any?>): Array<Any?> {
        val old = strength
        strength = ((args.getOrNull(0) as? Number)?.toInt() ?: 16).coerceIn(1, 400)
        return arrayOf(old)
    }
    
    @Callback(doc = "function(): boolean -- Checks if bridging is enabled")
    fun isRepeater(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(bridgeMode)
    }
    
    @Callback(doc = "function(enabled: boolean): boolean -- Sets bridging mode")
    fun setRepeater(context: Context, args: Array<Any?>): Array<Any?> {
        val old = bridgeMode
        bridgeMode = args.getOrNull(0) as? Boolean ?: true
        return arrayOf(old)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putInt("strength", strength)
        tag.putBoolean("bridge", bridgeMode)
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        strength = tag.getInt("strength")
        bridgeMode = tag.getBoolean("bridge")
    }
}

/**
 * Relay component - extends wired network range.
 */
class RelayComponent : ComponentBase("relay") {
    
    @Callback(doc = "function(): boolean -- Checks if the relay is active")
    fun isActive(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
}

/**
 * Net splitter component - controllable network connections.
 */
class NetSplitterComponent : ComponentBase("net_splitter") {
    
    private val sides = BooleanArray(6) { true }
    
    @Callback(doc = "function(side: number): boolean -- Gets whether a side is connected")
    fun getSideOpen(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid side")
        if (side !in 0..5) return arrayOf(null, "invalid side")
        return arrayOf(sides[side])
    }
    
    @Callback(doc = "function(side: number, open: boolean): boolean -- Sets whether a side is connected") 
    fun setSideOpen(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid side")
        if (side !in 0..5) return arrayOf(null, "invalid side")
        val open = args.getOrNull(1) as? Boolean ?: true
        
        val old = sides[side]
        sides[side] = open
        return arrayOf(old)
    }
    
    @Callback(doc = "function(): table -- Gets the state of all sides")
    fun getSides(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(sides.toList())
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        for (i in sides.indices) {
            tag.putBoolean("side$i", sides[i])
        }
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        for (i in sides.indices) {
            sides[i] = tag.getBoolean("side$i")
        }
    }
}

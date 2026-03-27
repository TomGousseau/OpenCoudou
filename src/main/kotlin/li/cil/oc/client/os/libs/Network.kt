package li.cil.oc.client.os.libs

import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

/**
 * Network library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Network.lua.
 * Provides modem/network card communication.
 */
object Network {
    
    // Modem settings
    var modemPort = 1488
    var modemPacketReserve = 128
    var modemTimeout = 2.0 // seconds
    var modemSignalStrength = 512
    
    // Internet settings
    var internetDelay = 0.05
    var internetTimeout = 1.0
    
    // Network name
    var networkName = "Computer"
    var networkEnabled = true
    
    // Proxy filesystem (for remote mounting)
    var proxySpaceUsed = 0L
    var proxySpaceTotal = 1024 * 1024 * 1024L // 1GB
    
    // Modem proxy reference
    private var modemProxy: Component.ComponentProxy? = null
    private var internetProxy: Component.ComponentProxy? = null
    
    // File handles for network filesystem
    private val filesystemHandles = ConcurrentHashMap<Int, NetworkFileHandle>()
    private var nextHandle = 1
    
    // Connected peers
    private val connectedPeers = ConcurrentHashMap<String, PeerInfo>()
    
    // Message listeners
    private val messageListeners = mutableListOf<(NetworkMessage) -> Unit>()
    
    /**
     * Peer information.
     */
    data class PeerInfo(
        val address: String,
        val name: String,
        val lastSeen: Long = java.lang.System.currentTimeMillis()
    )
    
    /**
     * Network message.
     */
    data class NetworkMessage(
        val sender: String,
        val port: Int,
        val distance: Double,
        val data: List<Any?>
    )
    
    /**
     * Network file handle for remote filesystems.
     */
    data class NetworkFileHandle(
        val id: Int,
        val address: String,
        val path: String,
        var position: Long = 0
    )
    
    // ==================== Component Management ====================
    
    /**
     * Update component references.
     */
    fun updateComponents() {
        modemProxy = Component.get(Component.Types.MODEM)
        internetProxy = Component.get(Component.Types.INTERNET)
        
        // Open modem port if available
        modemProxy?.let {
            it.invoke("open", modemPort)
        }
    }
    
    /**
     * Check if modem is available.
     */
    fun hasModem(): Boolean = modemProxy != null
    
    /**
     * Check if internet is available.
     */
    fun hasInternet(): Boolean = internetProxy != null
    
    // ==================== Modem Operations ====================
    
    /**
     * Open a port.
     */
    fun open(port: Int = modemPort): Boolean {
        return modemProxy?.invoke("open", port) as? Boolean ?: false
    }
    
    /**
     * Close a port.
     */
    fun close(port: Int = modemPort): Boolean {
        return modemProxy?.invoke("close", port) as? Boolean ?: false
    }
    
    /**
     * Check if port is open.
     */
    fun isOpen(port: Int = modemPort): Boolean {
        return modemProxy?.invoke("isOpen", port) as? Boolean ?: false
    }
    
    /**
     * Send a message to specific address.
     */
    fun send(address: String, port: Int, vararg data: Any?): Boolean {
        return modemProxy?.invoke("send", address, port, *data) as? Boolean ?: false
    }
    
    /**
     * Broadcast a message to all.
     */
    fun broadcast(port: Int, vararg data: Any?): Boolean {
        return modemProxy?.invoke("broadcast", port, *data) as? Boolean ?: false
    }
    
    /**
     * Get signal strength.
     */
    fun getStrength(): Int {
        return modemProxy?.invoke("getStrength") as? Int ?: modemSignalStrength
    }
    
    /**
     * Set signal strength.
     */
    fun setStrength(strength: Int): Int {
        val old = getStrength()
        modemProxy?.invoke("setStrength", strength)
        modemSignalStrength = strength
        return old
    }
    
    /**
     * Check if wireless.
     */
    fun isWireless(): Boolean {
        return modemProxy?.invoke("isWireless") as? Boolean ?: false
    }
    
    // ==================== Message Handling ====================
    
    /**
     * Register message listener.
     */
    fun onMessage(listener: (NetworkMessage) -> Unit) {
        messageListeners.add(listener)
    }
    
    /**
     * Remove message listener.
     */
    fun removeListener(listener: (NetworkMessage) -> Unit) {
        messageListeners.remove(listener)
    }
    
    /**
     * Process incoming message (called by event system).
     */
    fun handleModemMessage(sender: String, port: Int, distance: Double, vararg data: Any?) {
        val message = NetworkMessage(sender, port, distance, data.toList())
        messageListeners.forEach { it(message) }
    }
    
    // ==================== Peer Discovery ====================
    
    /**
     * Discover peers on network.
     */
    fun discover(): List<PeerInfo> {
        broadcast(modemPort, "discover", networkName)
        return connectedPeers.values.toList()
    }
    
    /**
     * Register a peer.
     */
    fun registerPeer(address: String, name: String) {
        connectedPeers[address] = PeerInfo(address, name)
    }
    
    /**
     * Unregister a peer.
     */
    fun unregisterPeer(address: String) {
        connectedPeers.remove(address)
    }
    
    /**
     * Get connected peers.
     */
    fun getPeers(): List<PeerInfo> = connectedPeers.values.toList()
    
    // ==================== FTP Mounting ====================
    
    /**
     * Mount a remote FTP server.
     */
    suspend fun mountFTP(
        address: String,
        port: Int = 21,
        username: String = "anonymous",
        password: String = "guest@",
        mountPath: String = "/mnt/ftp/"
    ): Result<FTPMount> {
        if (!hasInternet()) {
            return Result.failure(Exception("Internet component not available"))
        }
        
        return try {
            val client = FTP.connect(address, port, username, password).getOrThrow()
            val mount = FTPMount(address, port, username, mountPath, client)
            Result.success(mount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * FTP mount information.
     */
    data class FTPMount(
        val address: String,
        val port: Int,
        val username: String,
        val mountPath: String,
        val client: FTP
    )
    
    // ==================== Network Filesystem Proxy ====================
    
    /**
     * Create a network filesystem proxy.
     */
    fun createProxy(peerAddress: String): NetworkProxy {
        return NetworkProxy(peerAddress)
    }
    
    /**
     * Proxy for remote filesystem access.
     */
    class NetworkProxy(val peerAddress: String) {
        
        fun exists(path: String): Boolean {
            send(peerAddress, modemPort, "fs", "exists", path)
            // Would wait for response in full implementation
            return false
        }
        
        fun isDirectory(path: String): Boolean {
            send(peerAddress, modemPort, "fs", "isDirectory", path)
            return false
        }
        
        fun list(path: String): List<String>? {
            send(peerAddress, modemPort, "fs", "list", path)
            return null
        }
        
        fun read(path: String): ByteArray? {
            send(peerAddress, modemPort, "fs", "read", path)
            return null
        }
        
        fun write(path: String, data: ByteArray): Boolean {
            send(peerAddress, modemPort, "fs", "write", path, java.util.Base64.getEncoder().encodeToString(data))
            return false
        }
        
        fun delete(path: String): Boolean {
            send(peerAddress, modemPort, "fs", "delete", path)
            return false
        }
        
        fun makeDirectory(path: String): Boolean {
            send(peerAddress, modemPort, "fs", "mkdir", path)
            return false
        }
    }
    
    // ==================== Protocol Helpers ====================
    
    /**
     * Generate unique message ID.
     */
    fun generateMessageId(): String = UUID.randomUUID().toString().take(8)
    
    /**
     * Pack data for transmission.
     */
    fun pack(vararg data: Any?): String {
        return data.joinToString("\u0000") { 
            when (it) {
                null -> "\u0001NULL"
                is Boolean -> "\u0002${if (it) "1" else "0"}"
                is Number -> "\u0003$it"
                is ByteArray -> "\u0004${java.util.Base64.getEncoder().encodeToString(it)}"
                else -> "\u0005$it"
            }
        }
    }
    
    /**
     * Unpack received data.
     */
    fun unpack(packed: String): List<Any?> {
        return packed.split("\u0000").map { value ->
            when {
                value.startsWith("\u0001") -> null
                value.startsWith("\u0002") -> value.drop(1) == "1"
                value.startsWith("\u0003") -> value.drop(1).toDoubleOrNull() ?: 0.0
                value.startsWith("\u0004") -> java.util.Base64.getDecoder().decode(value.drop(1))
                value.startsWith("\u0005") -> value.drop(1)
                else -> value
            }
        }
    }
}

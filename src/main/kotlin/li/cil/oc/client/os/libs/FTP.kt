package li.cil.oc.client.os.libs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket
import java.net.InetSocketAddress

/**
 * FTP client library for SkibidiOS2.
 * Compatible with SkibidiLuaOS FTP.lua.
 * Supports basic FTP operations over plain connections.
 */
class FTP private constructor(
    private val host: String,
    private val port: Int
) {
    
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private var dataSocket: Socket? = null
    
    private var loggedIn = false
    private var passiveMode = true
    
    companion object {
        const val DEFAULT_PORT = 21
        const val DEFAULT_TIMEOUT = 30000 // 30 seconds
        
        /**
         * Create a new FTP connection.
         */
        suspend fun connect(
            host: String,
            port: Int = DEFAULT_PORT,
            username: String = "anonymous",
            password: String = "guest@"
        ): Result<FTP> = withContext(Dispatchers.IO) {
            try {
                val ftp = FTP(host, port)
                ftp.doConnect(username, password)
                Result.success(ftp)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private suspend fun doConnect(username: String, password: String) = withContext(Dispatchers.IO) {
        // Connect to server
        socket = Socket().apply {
            soTimeout = DEFAULT_TIMEOUT
            connect(InetSocketAddress(host, port), DEFAULT_TIMEOUT)
        }
        
        reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
        writer = PrintWriter(OutputStreamWriter(socket!!.getOutputStream()), true)
        
        // Read welcome message
        val welcome = readResponse()
        if (!welcome.isPositive()) {
            throw IOException("Connection failed: $welcome")
        }
        
        // Login
        sendCommand("USER $username")
        val userResponse = readResponse()
        
        if (userResponse.code == 331) {
            // Password required
            sendCommand("PASS $password")
            val passResponse = readResponse()
            if (!passResponse.isPositive()) {
                throw IOException("Login failed: $passResponse")
            }
        } else if (!userResponse.isPositive()) {
            throw IOException("Login failed: $userResponse")
        }
        
        loggedIn = true
        
        // Set binary mode by default
        setBinaryMode()
    }
    
    /**
     * FTP response data class.
     */
    data class Response(
        val code: Int,
        val message: String
    ) {
        fun isPositive() = code in 100..399
        fun isComplete() = code in 200..299
        fun isIntermediate() = code in 300..399
        fun isNegative() = code >= 400
        
        override fun toString() = "$code $message"
    }
    
    /**
     * File info data class.
     */
    data class FileInfo(
        val name: String,
        val size: Long,
        val isDirectory: Boolean,
        val modified: String?,
        val permissions: String?
    )
    
    private fun sendCommand(command: String) {
        writer?.println(command)
    }
    
    private fun readResponse(): Response {
        val lines = mutableListOf<String>()
        var code = 0
        
        while (true) {
            val line = reader?.readLine() ?: throw IOException("Connection closed")
            
            if (lines.isEmpty() && line.length >= 3) {
                code = line.substring(0, 3).toIntOrNull() ?: 0
            }
            
            lines.add(line)
            
            // Check if this is the final line (format: "XXX message" not "XXX-message")
            if (line.length >= 4 && line[3] == ' ' && line.startsWith(code.toString())) {
                break
            }
        }
        
        return Response(code, lines.joinToString("\n"))
    }
    
    /**
     * Set binary transfer mode.
     */
    suspend fun setBinaryMode(): Boolean = withContext(Dispatchers.IO) {
        sendCommand("TYPE I")
        readResponse().isPositive()
    }
    
    /**
     * Set ASCII transfer mode.
     */
    suspend fun setAsciiMode(): Boolean = withContext(Dispatchers.IO) {
        sendCommand("TYPE A")
        readResponse().isPositive()
    }
    
    /**
     * Enable passive mode (default).
     */
    fun setPassiveMode(passive: Boolean) {
        passiveMode = passive
    }
    
    /**
     * Get current working directory.
     */
    suspend fun pwd(): String? = withContext(Dispatchers.IO) {
        sendCommand("PWD")
        val response = readResponse()
        if (response.isPositive()) {
            // Parse directory from response like: 257 "/path" is current directory
            val match = Regex("\"([^\"]+)\"").find(response.message)
            match?.groupValues?.get(1)
        } else null
    }
    
    /**
     * Change directory.
     */
    suspend fun cd(path: String): Boolean = withContext(Dispatchers.IO) {
        sendCommand("CWD $path")
        readResponse().isPositive()
    }
    
    /**
     * Change to parent directory.
     */
    suspend fun cdup(): Boolean = withContext(Dispatchers.IO) {
        sendCommand("CDUP")
        readResponse().isPositive()
    }
    
    /**
     * List directory contents.
     */
    suspend fun list(path: String = ""): List<FileInfo> = withContext(Dispatchers.IO) {
        val dataStream = openDataConnection()
        
        sendCommand(if (path.isEmpty()) "LIST" else "LIST $path")
        val cmdResponse = readResponse()
        
        if (!cmdResponse.isPositive() && cmdResponse.code != 150 && cmdResponse.code != 125) {
            dataStream.close()
            throw IOException("LIST failed: $cmdResponse")
        }
        
        val files = mutableListOf<FileInfo>()
        val dataReader = BufferedReader(InputStreamReader(dataStream.getInputStream()))
        
        dataReader.useLines { lines ->
            lines.forEach { line ->
                parseListLine(line)?.let { files.add(it) }
            }
        }
        
        dataStream.close()
        
        // Read completion response
        readResponse()
        
        files
    }
    
    /**
     * List file names only.
     */
    suspend fun nlst(path: String = ""): List<String> = withContext(Dispatchers.IO) {
        val dataStream = openDataConnection()
        
        sendCommand(if (path.isEmpty()) "NLST" else "NLST $path")
        val cmdResponse = readResponse()
        
        if (!cmdResponse.isPositive() && cmdResponse.code != 150 && cmdResponse.code != 125) {
            dataStream.close()
            throw IOException("NLST failed: $cmdResponse")
        }
        
        val names = mutableListOf<String>()
        val dataReader = BufferedReader(InputStreamReader(dataStream.getInputStream()))
        
        dataReader.useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    names.add(line.trim())
                }
            }
        }
        
        dataStream.close()
        readResponse()
        
        names
    }
    
    private fun parseListLine(line: String): FileInfo? {
        // Parse Unix-style listing: drwxr-xr-x 2 user group 4096 Jan 1 12:00 filename
        val parts = line.split(Regex("\\s+"), limit = 9)
        if (parts.size < 9) return null
        
        val permissions = parts[0]
        val isDir = permissions.startsWith("d")
        val size = parts[4].toLongOrNull() ?: 0
        val modified = "${parts[5]} ${parts[6]} ${parts[7]}"
        val name = parts[8]
        
        return FileInfo(name, size, isDir, modified, permissions)
    }
    
    /**
     * Download a file.
     */
    suspend fun get(remotePath: String): ByteArray = withContext(Dispatchers.IO) {
        val dataStream = openDataConnection()
        
        sendCommand("RETR $remotePath")
        val cmdResponse = readResponse()
        
        if (!cmdResponse.isPositive() && cmdResponse.code != 150 && cmdResponse.code != 125) {
            dataStream.close()
            throw IOException("RETR failed: $cmdResponse")
        }
        
        val data = dataStream.getInputStream().readBytes()
        dataStream.close()
        
        readResponse()
        
        data
    }
    
    /**
     * Download a file as string.
     */
    suspend fun getString(remotePath: String, charset: String = "UTF-8"): String {
        return String(get(remotePath), charset(charset))
    }
    
    /**
     * Upload a file.
     */
    suspend fun put(remotePath: String, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val dataStream = openDataConnection()
        
        sendCommand("STOR $remotePath")
        val cmdResponse = readResponse()
        
        if (!cmdResponse.isPositive() && cmdResponse.code != 150 && cmdResponse.code != 125) {
            dataStream.close()
            throw IOException("STOR failed: $cmdResponse")
        }
        
        dataStream.getOutputStream().write(data)
        dataStream.close()
        
        readResponse().isPositive()
    }
    
    /**
     * Upload a string as a file.
     */
    suspend fun putString(remotePath: String, content: String, charset: String = "UTF-8"): Boolean {
        return put(remotePath, content.toByteArray(charset(charset)))
    }
    
    /**
     * Append to a file.
     */
    suspend fun append(remotePath: String, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val dataStream = openDataConnection()
        
        sendCommand("APPE $remotePath")
        val cmdResponse = readResponse()
        
        if (!cmdResponse.isPositive() && cmdResponse.code != 150 && cmdResponse.code != 125) {
            dataStream.close()
            throw IOException("APPE failed: $cmdResponse")
        }
        
        dataStream.getOutputStream().write(data)
        dataStream.close()
        
        readResponse().isPositive()
    }
    
    /**
     * Delete a file.
     */
    suspend fun delete(remotePath: String): Boolean = withContext(Dispatchers.IO) {
        sendCommand("DELE $remotePath")
        readResponse().isPositive()
    }
    
    /**
     * Rename a file.
     */
    suspend fun rename(from: String, to: String): Boolean = withContext(Dispatchers.IO) {
        sendCommand("RNFR $from")
        val fromResponse = readResponse()
        if (!fromResponse.isIntermediate()) return false
        
        sendCommand("RNTO $to")
        readResponse().isPositive()
    }
    
    /**
     * Create a directory.
     */
    suspend fun mkdir(path: String): Boolean = withContext(Dispatchers.IO) {
        sendCommand("MKD $path")
        readResponse().isPositive()
    }
    
    /**
     * Remove a directory.
     */
    suspend fun rmdir(path: String): Boolean = withContext(Dispatchers.IO) {
        sendCommand("RMD $path")
        readResponse().isPositive()
    }
    
    /**
     * Get file size.
     */
    suspend fun size(remotePath: String): Long? = withContext(Dispatchers.IO) {
        sendCommand("SIZE $remotePath")
        val response = readResponse()
        if (response.isPositive()) {
            response.message.split(" ").lastOrNull()?.toLongOrNull()
        } else null
    }
    
    /**
     * Get file modification time.
     */
    suspend fun modificationTime(remotePath: String): String? = withContext(Dispatchers.IO) {
        sendCommand("MDTM $remotePath")
        val response = readResponse()
        if (response.isPositive()) {
            response.message.split(" ").lastOrNull()
        } else null
    }
    
    /**
     * Send a NOOP command to keep connection alive.
     */
    suspend fun noop(): Boolean = withContext(Dispatchers.IO) {
        sendCommand("NOOP")
        readResponse().isPositive()
    }
    
    /**
     * Get server system type.
     */
    suspend fun system(): String? = withContext(Dispatchers.IO) {
        sendCommand("SYST")
        val response = readResponse()
        if (response.isPositive()) response.message else null
    }
    
    /**
     * Send custom command.
     */
    suspend fun sendCustom(command: String): Response = withContext(Dispatchers.IO) {
        sendCommand(command)
        readResponse()
    }
    
    private fun openDataConnection(): Socket = if (passiveMode) {
        openPassiveConnection()
    } else {
        throw UnsupportedOperationException("Active mode not supported")
    }
    
    private fun openPassiveConnection(): Socket {
        sendCommand("PASV")
        val response = readResponse()
        
        if (!response.isPositive()) {
            throw IOException("PASV failed: $response")
        }
        
        // Parse response: "227 Entering Passive Mode (h1,h2,h3,h4,p1,p2)"
        val match = Regex("\\((\\d+,\\d+,\\d+,\\d+,\\d+,\\d+)\\)").find(response.message)
            ?: throw IOException("Cannot parse PASV response: $response")
        
        val parts = match.groupValues[1].split(",").map { it.toInt() }
        val dataHost = "${parts[0]}.${parts[1]}.${parts[2]}.${parts[3]}"
        val dataPort = parts[4] * 256 + parts[5]
        
        return Socket().apply {
            soTimeout = DEFAULT_TIMEOUT
            connect(InetSocketAddress(dataHost, dataPort), DEFAULT_TIMEOUT)
        }
    }
    
    /**
     * Close the FTP connection.
     */
    suspend fun close() = withContext(Dispatchers.IO) {
        try {
            if (loggedIn) {
                sendCommand("QUIT")
                readResponse()
            }
        } catch (_: Exception) {
            // Ignore errors during close
        } finally {
            reader?.close()
            writer?.close()
            socket?.close()
            dataSocket?.close()
            
            reader = null
            writer = null
            socket = null
            dataSocket = null
            loggedIn = false
        }
    }
    
    /**
     * Check if connected and logged in.
     */
    fun isConnected(): Boolean {
        return socket?.isConnected == true && loggedIn
    }
}

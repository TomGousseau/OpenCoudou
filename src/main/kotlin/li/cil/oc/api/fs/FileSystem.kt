package li.cil.oc.api.fs

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.CallbackResult
import li.cil.oc.api.network.AbstractManagedEnvironment
import li.cil.oc.api.network.Network
import li.cil.oc.api.network.Reachability
import li.cil.oc.api.network.Visibility
import net.minecraft.nbt.CompoundTag
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.*

/**
 * FileSystem provides a virtual file system abstraction.
 * 
 * File systems can be backed by:
 * - Real directories on disk (for persistent storage)
 * - RAM (for temporary filesystems)
 * - Read-only archives (for boot ROMs)
 * 
 * @see Handle
 * @see Mode
 */
interface FileSystem {
    /**
     * The label of this filesystem (display name).
     */
    var label: String?
    
    /**
     * Whether this filesystem is read-only.
     */
    val isReadOnly: Boolean
    
    /**
     * Total space in bytes.
     */
    val totalSpace: Long
    
    /**
     * Used space in bytes.
     */
    val usedSpace: Long
    
    /**
     * Available space in bytes.
     */
    val freeSpace: Long
        get() = totalSpace - usedSpace
    
    /**
     * Checks if a path exists.
     */
    fun exists(path: String): Boolean
    
    /**
     * Checks if a path is a directory.
     */
    fun isDirectory(path: String): Boolean
    
    /**
     * Gets the size of a file in bytes.
     */
    fun size(path: String): Long
    
    /**
     * Gets the last modified time of a file.
     */
    fun lastModified(path: String): Long
    
    /**
     * Lists entries in a directory.
     */
    fun list(path: String): List<String>
    
    /**
     * Creates a directory and all parent directories.
     */
    fun makeDirectory(path: String): Boolean
    
    /**
     * Deletes a file or empty directory.
     */
    fun delete(path: String): Boolean
    
    /**
     * Renames/moves a file or directory.
     */
    fun rename(from: String, to: String): Boolean
    
    /**
     * Opens a file with the given mode.
     */
    fun open(path: String, mode: Mode): Handle
    
    /**
     * Closes all open handles.
     */
    fun close()
}

/**
 * File open modes.
 */
enum class Mode {
    /** Open for reading. File must exist. */
    READ,
    
    /** Open for writing. Creates or truncates file. */
    WRITE,
    
    /** Open for appending. Creates if needed. */
    APPEND,
    
    /** Open for reading and writing. File must exist. */
    READ_WRITE
}

/**
 * A handle to an open file.
 */
interface Handle : Closeable {
    /**
     * The filesystem this handle belongs to.
     */
    val fileSystem: FileSystem
    
    /**
     * The unique ID of this handle.
     */
    val id: Int
    
    /**
     * The current position in the file.
     */
    var position: Long
    
    /**
     * The length of the file.
     */
    val length: Long
    
    /**
     * Reads up to the specified number of bytes.
     * @return The bytes read, or null if at end of file
     */
    fun read(count: Int): ByteArray?
    
    /**
     * Writes bytes to the file.
     * @return The number of bytes written
     */
    fun write(data: ByteArray): Int
    
    /**
     * Seeks to a position in the file.
     * @param whence "set", "cur", or "end"
     * @param offset The offset from the whence position
     * @return The new position
     */
    fun seek(whence: String, offset: Long): Long
}

/**
 * A filesystem backed by a directory on disk.
 */
class DirectoryFileSystem(
    private val root: Path,
    private val capacity: Long,
    override val isReadOnly: Boolean = false
) : FileSystem {
    
    override var label: String? = null
    
    override val totalSpace: Long = capacity
    
    override val usedSpace: Long
        get() = calculateUsedSpace(root)
    
    private val handles = mutableMapOf<Int, HandleImpl>()
    private var nextHandleId = 1
    
    init {
        if (!root.exists()) {
            root.createDirectories()
        }
    }
    
    private fun resolve(path: String): Path {
        val resolved = root.resolve(path.removePrefix("/")).normalize()
        // Security check - ensure we don't escape the root
        require(resolved.startsWith(root)) { "Path escapes filesystem root" }
        return resolved
    }
    
    override fun exists(path: String): Boolean = resolve(path).exists()
    
    override fun isDirectory(path: String): Boolean = resolve(path).isDirectory()
    
    override fun size(path: String): Long {
        val p = resolve(path)
        return if (p.isRegularFile()) p.fileSize() else 0
    }
    
    override fun lastModified(path: String): Long = 
        resolve(path).getLastModifiedTime().toMillis()
    
    override fun list(path: String): List<String> {
        val dir = resolve(path)
        if (!dir.isDirectory()) return emptyList()
        return dir.listDirectoryEntries().map { 
            if (it.isDirectory()) "${it.name}/" else it.name
        }
    }
    
    override fun makeDirectory(path: String): Boolean {
        if (isReadOnly) return false
        return try {
            resolve(path).createDirectories()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun delete(path: String): Boolean {
        if (isReadOnly) return false
        return try {
            resolve(path).deleteIfExists()
        } catch (e: Exception) {
            false
        }
    }
    
    override fun rename(from: String, to: String): Boolean {
        if (isReadOnly) return false
        return try {
            resolve(from).moveTo(resolve(to))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun open(path: String, mode: Mode): Handle {
        val resolved = resolve(path)
        val options = when (mode) {
            Mode.READ -> setOf(StandardOpenOption.READ)
            Mode.WRITE -> setOf(
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            Mode.APPEND -> setOf(
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )
            Mode.READ_WRITE -> setOf(
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
            )
        }
        
        if (mode != Mode.READ && isReadOnly) {
            throw IOException("Filesystem is read-only")
        }
        
        val channel = Files.newByteChannel(resolved, options)
        val handle = HandleImpl(this, nextHandleId++, channel, mode)
        handles[handle.id] = handle
        return handle
    }
    
    override fun close() {
        handles.values.toList().forEach { it.close() }
        handles.clear()
    }
    
    internal fun removeHandle(id: Int) {
        handles.remove(id)
    }
    
    private fun calculateUsedSpace(path: Path): Long {
        return if (path.isDirectory()) {
            path.listDirectoryEntries().sumOf { calculateUsedSpace(it) }
        } else {
            path.fileSize()
        }
    }
    
    private class HandleImpl(
        override val fileSystem: DirectoryFileSystem,
        override val id: Int,
        private val channel: java.nio.channels.SeekableByteChannel,
        private val mode: Mode
    ) : Handle {
        
        override var position: Long
            get() = channel.position()
            set(value) { channel.position(value) }
        
        override val length: Long
            get() = channel.size()
        
        override fun read(count: Int): ByteArray? {
            if (mode == Mode.WRITE || mode == Mode.APPEND) {
                throw IOException("Handle not opened for reading")
            }
            val buffer = java.nio.ByteBuffer.allocate(count)
            val read = channel.read(buffer)
            return if (read == -1) null else buffer.array().copyOf(read)
        }
        
        override fun write(data: ByteArray): Int {
            if (mode == Mode.READ) {
                throw IOException("Handle not opened for writing")
            }
            return channel.write(java.nio.ByteBuffer.wrap(data))
        }
        
        override fun seek(whence: String, offset: Long): Long {
            position = when (whence.lowercase()) {
                "set" -> offset
                "cur" -> position + offset
                "end" -> length + offset
                else -> throw IllegalArgumentException("Invalid whence: $whence")
            }
            return position
        }
        
        override fun close() {
            channel.close()
            (fileSystem as DirectoryFileSystem).removeHandle(id)
        }
    }
}

/**
 * A RAM-based filesystem for temporary storage.
 */
class RamFileSystem(override val totalSpace: Long) : FileSystem {
    override var label: String? = null
    override val isReadOnly: Boolean = false
    
    private val files = mutableMapOf<String, ByteArray>()
    private val directories = mutableSetOf<String>("/")
    private val handles = mutableMapOf<Int, RamHandle>()
    private var nextHandleId = 1
    
    override val usedSpace: Long
        get() = files.values.sumOf { it.size.toLong() }
    
    private fun normalize(path: String): String {
        return "/" + path.removePrefix("/").removeSuffix("/")
    }
    
    override fun exists(path: String): Boolean {
        val normalized = normalize(path)
        return directories.contains(normalized) || files.containsKey(normalized)
    }
    
    override fun isDirectory(path: String): Boolean = directories.contains(normalize(path))
    
    override fun size(path: String): Long = files[normalize(path)]?.size?.toLong() ?: 0
    
    override fun lastModified(path: String): Long = System.currentTimeMillis()
    
    override fun list(path: String): List<String> {
        val normalized = normalize(path)
        if (!directories.contains(normalized)) return emptyList()
        
        val prefix = if (normalized == "/") "" else "$normalized/"
        val entries = mutableSetOf<String>()
        
        for (file in files.keys) {
            if (file.startsWith(prefix)) {
                val relative = file.removePrefix(prefix)
                val firstPart = relative.split("/").first()
                entries.add(firstPart)
            }
        }
        for (dir in directories) {
            if (dir.startsWith(prefix) && dir != normalized) {
                val relative = dir.removePrefix(prefix)
                val firstPart = relative.split("/").first()
                entries.add("$firstPart/")
            }
        }
        return entries.toList()
    }
    
    override fun makeDirectory(path: String): Boolean {
        val normalized = normalize(path)
        if (directories.contains(normalized)) return true
        if (files.containsKey(normalized)) return false
        
        // Create parent directories
        var current = ""
        for (part in normalized.split("/").filter { it.isNotEmpty() }) {
            current += "/$part"
            directories.add(current)
        }
        return true
    }
    
    override fun delete(path: String): Boolean {
        val normalized = normalize(path)
        return files.remove(normalized) != null || directories.remove(normalized)
    }
    
    override fun rename(from: String, to: String): Boolean {
        val fromNorm = normalize(from)
        val toNorm = normalize(to)
        
        return if (files.containsKey(fromNorm)) {
            files[toNorm] = files.remove(fromNorm)!!
            true
        } else {
            false
        }
    }
    
    override fun open(path: String, mode: Mode): Handle {
        val normalized = normalize(path)
        
        when (mode) {
            Mode.READ -> {
                if (!files.containsKey(normalized)) {
                    throw FileNotFoundException(path)
                }
            }
            Mode.WRITE -> {
                files[normalized] = ByteArray(0)
                // Ensure parent directory exists
                makeDirectory(path.substringBeforeLast("/", "/"))
            }
            Mode.APPEND -> {
                if (!files.containsKey(normalized)) {
                    files[normalized] = ByteArray(0)
                    makeDirectory(path.substringBeforeLast("/", "/"))
                }
            }
            Mode.READ_WRITE -> {
                if (!files.containsKey(normalized)) {
                    throw FileNotFoundException(path)
                }
            }
        }
        
        val handle = RamHandle(this, nextHandleId++, normalized, mode)
        handles[handle.id] = handle
        return handle
    }
    
    override fun close() {
        handles.values.toList().forEach { it.close() }
        handles.clear()
    }
    
    internal fun getData(path: String): ByteArray = files[path] ?: ByteArray(0)
    internal fun setData(path: String, data: ByteArray) { files[path] = data }
    internal fun removeHandle(id: Int) { handles.remove(id) }
    
    private class RamHandle(
        override val fileSystem: RamFileSystem,
        override val id: Int,
        private val path: String,
        private val mode: Mode
    ) : Handle {
        
        override var position: Long = if (mode == Mode.APPEND) {
            fileSystem.getData(path).size.toLong()
        } else 0
        
        override val length: Long
            get() = fileSystem.getData(path).size.toLong()
        
        override fun read(count: Int): ByteArray? {
            if (mode == Mode.WRITE || mode == Mode.APPEND) {
                throw IOException("Handle not opened for reading")
            }
            val data = fileSystem.getData(path)
            if (position >= data.size) return null
            val end = minOf(position.toInt() + count, data.size)
            val result = data.copyOfRange(position.toInt(), end)
            position = end.toLong()
            return result
        }
        
        override fun write(data: ByteArray): Int {
            if (mode == Mode.READ) {
                throw IOException("Handle not opened for writing")
            }
            val currentData = fileSystem.getData(path)
            val newData = if (mode == Mode.APPEND || position >= currentData.size) {
                currentData + data
            } else {
                val before = currentData.copyOfRange(0, position.toInt())
                val after = if (position.toInt() + data.size < currentData.size) {
                    currentData.copyOfRange(position.toInt() + data.size, currentData.size)
                } else ByteArray(0)
                before + data + after
            }
            fileSystem.setData(path, newData)
            position += data.size
            return data.size
        }
        
        override fun seek(whence: String, offset: Long): Long {
            position = when (whence.lowercase()) {
                "set" -> offset
                "cur" -> position + offset
                "end" -> length + offset
                else -> throw IllegalArgumentException("Invalid whence: $whence")
            }.coerceIn(0, length)
            return position
        }
        
        override fun close() {
            fileSystem.removeHandle(id)
        }
    }
}

/**
 * FileSystem component that can be accessed from Lua.
 */
class FileSystemComponent(
    private val fs: FileSystem,
    private val soundEnabled: Boolean = true
) : AbstractManagedEnvironment() {
    
    private val openHandles = mutableMapOf<Int, Handle>()
    private var nextHandle = 1
    
    init {
        createNode(
            Network.newNode(this)
                .withComponent("filesystem", Visibility.NEIGHBORS)
                .withReachability(Reachability.NETWORK)
        )
    }
    
    @Callback(doc = "function():string -- Returns the filesystem label")
    fun getLabel(context: Context, args: Arguments): Array<Any?> {
        return CallbackResult.success(fs.label)
    }
    
    @Callback(doc = "function(label:string):string -- Sets the filesystem label")
    fun setLabel(context: Context, args: Arguments): Array<Any?> {
        if (fs.isReadOnly) return CallbackResult.error("filesystem is read-only")
        fs.label = args.optString(0, "")
        return CallbackResult.success(fs.label)
    }
    
    @Callback(doc = "function():boolean -- Returns whether the filesystem is read-only")
    fun isReadOnly(context: Context, args: Arguments): Array<Any?> {
        return CallbackResult.bool(fs.isReadOnly)
    }
    
    @Callback(doc = "function():number -- Returns the total space in bytes")
    fun spaceTotal(context: Context, args: Arguments): Array<Any?> {
        return CallbackResult.success(fs.totalSpace)
    }
    
    @Callback(doc = "function():number -- Returns the used space in bytes")
    fun spaceUsed(context: Context, args: Arguments): Array<Any?> {
        return CallbackResult.success(fs.usedSpace)
    }
    
    @Callback(doc = "function(path:string):boolean -- Checks if a path exists")
    fun exists(context: Context, args: Arguments): Array<Any?> {
        return CallbackResult.bool(fs.exists(args.checkString(0)))
    }
    
    @Callback(doc = "function(path:string):boolean -- Checks if a path is a directory")
    fun isDirectory(context: Context, args: Arguments): Array<Any?> {
        return CallbackResult.bool(fs.isDirectory(args.checkString(0)))
    }
    
    @Callback(doc = "function(path:string):number -- Returns the file size")
    fun size(context: Context, args: Arguments): Array<Any?> {
        return CallbackResult.success(fs.size(args.checkString(0)))
    }
    
    @Callback(doc = "function(path:string):number -- Returns the last modified time")
    fun lastModified(context: Context, args: Arguments): Array<Any?> {
        return CallbackResult.success(fs.lastModified(args.checkString(0)))
    }
    
    @Callback(doc = "function(path:string):table -- Lists directory contents")
    fun list(context: Context, args: Arguments): Array<Any?> {
        val path = args.checkString(0)
        if (!fs.isDirectory(path)) return CallbackResult.error("not a directory")
        return CallbackResult.list(fs.list(path))
    }
    
    @Callback(doc = "function(path:string):boolean -- Creates a directory")
    fun makeDirectory(context: Context, args: Arguments): Array<Any?> {
        if (fs.isReadOnly) return CallbackResult.error("filesystem is read-only")
        return CallbackResult.bool(fs.makeDirectory(args.checkString(0)))
    }
    
    @Callback(doc = "function(path:string):boolean -- Deletes a file or directory")
    fun remove(context: Context, args: Arguments): Array<Any?> {
        if (fs.isReadOnly) return CallbackResult.error("filesystem is read-only")
        return CallbackResult.bool(fs.delete(args.checkString(0)))
    }
    
    @Callback(doc = "function(from:string, to:string):boolean -- Renames a file")
    fun rename(context: Context, args: Arguments): Array<Any?> {
        if (fs.isReadOnly) return CallbackResult.error("filesystem is read-only")
        return CallbackResult.bool(fs.rename(args.checkString(0), args.checkString(1)))
    }
    
    @Callback(doc = "function(path:string[, mode:string]):number -- Opens a file")
    fun open(context: Context, args: Arguments): Array<Any?> {
        val path = args.checkString(0)
        val modeStr = args.optString(1, "r")
        val mode = when (modeStr) {
            "r", "rb" -> Mode.READ
            "w", "wb" -> Mode.WRITE
            "a", "ab" -> Mode.APPEND
            "r+", "r+b" -> Mode.READ_WRITE
            else -> return CallbackResult.error("invalid mode: $modeStr")
        }
        
        return try {
            val handle = fs.open(path, mode)
            val id = nextHandle++
            openHandles[id] = handle
            CallbackResult.success(id)
        } catch (e: FileNotFoundException) {
            CallbackResult.error("file not found")
        } catch (e: IOException) {
            CallbackResult.error(e.message ?: "io error")
        }
    }
    
    @Callback(doc = "function(handle:number[, count:number]):string -- Reads from a file")
    fun read(context: Context, args: Arguments): Array<Any?> {
        val handle = openHandles[args.checkInteger(0)]
            ?: return CallbackResult.error("bad file descriptor")
        val count = args.optInteger(1, 4096)
        
        return try {
            val data = handle.read(count)
            if (data == null) {
                CallbackResult.success(null)
            } else {
                CallbackResult.success(String(data, Charsets.UTF_8))
            }
        } catch (e: IOException) {
            CallbackResult.error(e.message ?: "io error")
        }
    }
    
    @Callback(doc = "function(handle:number, data:string):boolean -- Writes to a file")
    fun write(context: Context, args: Arguments): Array<Any?> {
        val handle = openHandles[args.checkInteger(0)]
            ?: return CallbackResult.error("bad file descriptor")
        val data = args.checkString(1)
        
        return try {
            handle.write(data.toByteArray(Charsets.UTF_8))
            CallbackResult.bool(true)
        } catch (e: IOException) {
            CallbackResult.error(e.message ?: "io error")
        }
    }
    
    @Callback(doc = "function(handle:number, whence:string, offset:number):number -- Seeks in a file")
    fun seek(context: Context, args: Arguments): Array<Any?> {
        val handle = openHandles[args.checkInteger(0)]
            ?: return CallbackResult.error("bad file descriptor")
        val whence = args.checkString(1)
        val offset = args.optLong(2, 0)
        
        return try {
            CallbackResult.success(handle.seek(whence, offset))
        } catch (e: Exception) {
            CallbackResult.error(e.message ?: "seek error")
        }
    }
    
    @Callback(doc = "function(handle:number) -- Closes a file handle")
    fun close(context: Context, args: Arguments): Array<Any?> {
        val id = args.checkInteger(0)
        val handle = openHandles.remove(id) ?: return CallbackResult.success()
        handle.close()
        return CallbackResult.success()
    }
    
    override fun loadData(tag: CompoundTag) {
        super.loadData(tag)
        fs.label = if (tag.contains("label")) tag.getString("label") else null
    }
    
    override fun saveData(tag: CompoundTag) {
        super.saveData(tag)
        fs.label?.let { tag.putString("label", it) }
    }
}

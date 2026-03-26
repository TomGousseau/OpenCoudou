package li.cil.oc.common.filesystem

import li.cil.oc.api.machine.MachineHost
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.*

/**
 * Virtual file system implementation for OpenComputers
 * Supports multiple mount points, permissions, and persistence
 */
class FileSystem private constructor(
    private val label: String,
    private val capacity: Long,
    private val readonly: Boolean = false
) {
    companion object {
        private val fileSystems = ConcurrentHashMap<UUID, FileSystem>()
        
        fun create(label: String, capacity: Long, readonly: Boolean = false): FileSystem {
            val fs = FileSystem(label, capacity, readonly)
            fileSystems[fs.id] = fs
            return fs
        }
        
        fun get(id: UUID): FileSystem? = fileSystems[id]
        
        fun fromNbt(tag: CompoundTag): FileSystem? {
            val id = tag.getUUID("id")
            return fileSystems.getOrPut(id) {
                FileSystem(
                    tag.getString("label"),
                    tag.getLong("capacity"),
                    tag.getBoolean("readonly")
                ).apply {
                    this.loadNbt(tag)
                }
            }
        }
        
        const val BLOCK_SIZE = 512
        const val MAX_OPEN_HANDLES = 16
        const val MAX_PATH_LENGTH = 1024
        const val MAX_FILE_NAME_LENGTH = 255
    }
    
    val id: UUID = UUID.randomUUID()
    private val root: VirtualDirectory = VirtualDirectory("", null)
    private val openHandles = ConcurrentHashMap<Int, FileHandle>()
    private var nextHandle = 1
    private var usedSpace: Long = 0
    
    // File system operations
    
    fun exists(path: String): Boolean {
        val normalized = normalizePath(path)
        return root.resolve(normalized) != null
    }
    
    fun isDirectory(path: String): Boolean {
        val normalized = normalizePath(path)
        return root.resolve(normalized) is VirtualDirectory
    }
    
    fun isFile(path: String): Boolean {
        val normalized = normalizePath(path)
        return root.resolve(normalized) is VirtualFile
    }
    
    fun size(path: String): Long {
        val normalized = normalizePath(path)
        return when (val node = root.resolve(normalized)) {
            is VirtualFile -> node.size
            is VirtualDirectory -> 0
            else -> 0
        }
    }
    
    fun lastModified(path: String): Long {
        val normalized = normalizePath(path)
        return root.resolve(normalized)?.lastModified ?: 0
    }
    
    fun list(path: String): List<String> {
        val normalized = normalizePath(path)
        val dir = root.resolve(normalized) as? VirtualDirectory ?: return emptyList()
        return dir.children.keys.map { name ->
            if (dir.children[name] is VirtualDirectory) "$name/" else name
        }.sorted()
    }
    
    fun makeDirectory(path: String): Boolean {
        if (readonly) return false
        val normalized = normalizePath(path)
        if (normalized.isEmpty()) return false
        
        val parts = normalized.split("/").filter { it.isNotEmpty() }
        var current: VirtualDirectory = root
        
        for (part in parts) {
            if (part.length > MAX_FILE_NAME_LENGTH) return false
            
            val existing = current.children[part]
            current = when (existing) {
                is VirtualDirectory -> existing
                null -> {
                    val newDir = VirtualDirectory(part, current)
                    current.children[part] = newDir
                    newDir
                }
                else -> return false // File exists with same name
            }
        }
        return true
    }
    
    fun delete(path: String): Boolean {
        if (readonly) return false
        val normalized = normalizePath(path)
        if (normalized.isEmpty()) return false
        
        val parts = normalized.split("/").filter { it.isNotEmpty() }
        val parentPath = parts.dropLast(1).joinToString("/")
        val name = parts.last()
        
        val parent = if (parentPath.isEmpty()) root 
                     else root.resolve(parentPath) as? VirtualDirectory ?: return false
        
        val node = parent.children[name] ?: return false
        
        // Check if directory is empty
        if (node is VirtualDirectory && node.children.isNotEmpty()) {
            return false
        }
        
        // Release space if file
        if (node is VirtualFile) {
            usedSpace -= node.size
        }
        
        parent.children.remove(name)
        return true
    }
    
    fun rename(from: String, to: String): Boolean {
        if (readonly) return false
        val fromNormalized = normalizePath(from)
        val toNormalized = normalizePath(to)
        
        if (fromNormalized.isEmpty() || toNormalized.isEmpty()) return false
        if (exists(toNormalized)) return false
        
        val fromParts = fromNormalized.split("/").filter { it.isNotEmpty() }
        val toParts = toNormalized.split("/").filter { it.isNotEmpty() }
        
        val fromParentPath = fromParts.dropLast(1).joinToString("/")
        val fromName = fromParts.last()
        
        val toParentPath = toParts.dropLast(1).joinToString("/")
        val toName = toParts.last()
        
        if (toName.length > MAX_FILE_NAME_LENGTH) return false
        
        val fromParent = if (fromParentPath.isEmpty()) root 
                         else root.resolve(fromParentPath) as? VirtualDirectory ?: return false
        
        // Ensure destination directory exists
        if (toParentPath.isNotEmpty() && !makeDirectory(toParentPath)) {
            if (!isDirectory(toParentPath)) return false
        }
        
        val toParent = if (toParentPath.isEmpty()) root 
                       else root.resolve(toParentPath) as? VirtualDirectory ?: return false
        
        val node = fromParent.children.remove(fromName) ?: return false
        node.name = toName
        node.parent = toParent
        toParent.children[toName] = node
        
        return true
    }
    
    // File handle operations
    
    fun open(path: String, mode: OpenMode): Int {
        if (openHandles.size >= MAX_OPEN_HANDLES) return -1
        
        val normalized = normalizePath(path)
        if (normalized.isEmpty()) return -1
        if (normalized.length > MAX_PATH_LENGTH) return -1
        
        val parts = normalized.split("/").filter { it.isNotEmpty() }
        val parentPath = parts.dropLast(1).joinToString("/")
        val name = parts.last()
        
        if (name.length > MAX_FILE_NAME_LENGTH) return -1
        
        when (mode) {
            OpenMode.READ -> {
                val file = root.resolve(normalized) as? VirtualFile ?: return -1
                val handle = nextHandle++
                openHandles[handle] = FileHandle(file, mode, 0)
                return handle
            }
            OpenMode.WRITE, OpenMode.APPEND -> {
                if (readonly) return -1
                
                // Ensure parent directory exists
                if (parentPath.isNotEmpty() && !makeDirectory(parentPath)) {
                    if (!isDirectory(parentPath)) return -1
                }
                
                val parent = if (parentPath.isEmpty()) root 
                             else root.resolve(parentPath) as? VirtualDirectory ?: return -1
                
                val file = parent.children.getOrPut(name) {
                    VirtualFile(name, parent, ByteArray(0))
                } as? VirtualFile ?: return -1
                
                if (mode == OpenMode.WRITE) {
                    usedSpace -= file.size
                    file.data = ByteArray(0)
                }
                
                val handle = nextHandle++
                val position = if (mode == OpenMode.APPEND) file.size else 0
                openHandles[handle] = FileHandle(file, mode, position)
                return handle
            }
        }
    }
    
    fun close(handle: Int): Boolean {
        return openHandles.remove(handle) != null
    }
    
    fun read(handle: Int, count: Int): ByteArray? {
        val fh = openHandles[handle] ?: return null
        if (fh.mode != OpenMode.READ) return null
        
        val available = (fh.file.size - fh.position).toInt().coerceAtLeast(0)
        val toRead = minOf(count, available)
        
        if (toRead <= 0) return ByteArray(0)
        
        val result = fh.file.data.copyOfRange(fh.position.toInt(), fh.position.toInt() + toRead)
        fh.position += toRead
        return result
    }
    
    fun write(handle: Int, data: ByteArray): Boolean {
        val fh = openHandles[handle] ?: return false
        if (fh.mode == OpenMode.READ) return false
        if (readonly) return false
        
        val newSize = maxOf(fh.file.size, fh.position + data.size)
        val spaceNeeded = newSize - fh.file.size
        
        if (usedSpace + spaceNeeded > capacity) return false
        
        // Expand file if necessary
        if (newSize > fh.file.data.size) {
            val newData = ByteArray(newSize.toInt())
            fh.file.data.copyInto(newData)
            fh.file.data = newData
        }
        
        // Write data
        data.copyInto(fh.file.data, fh.position.toInt())
        fh.position += data.size
        
        usedSpace += spaceNeeded
        fh.file.lastModified = System.currentTimeMillis()
        
        return true
    }
    
    fun seek(handle: Int, whence: SeekMode, offset: Long): Long {
        val fh = openHandles[handle] ?: return -1
        
        val newPosition = when (whence) {
            SeekMode.SET -> offset
            SeekMode.CUR -> fh.position + offset
            SeekMode.END -> fh.file.size + offset
        }
        
        if (newPosition < 0) return -1
        fh.position = newPosition
        return newPosition
    }
    
    // Utility functions
    
    fun getLabel(): String = label
    fun getCapacity(): Long = capacity
    fun getUsedSpace(): Long = usedSpace
    fun getFreeSpace(): Long = capacity - usedSpace
    fun isReadOnly(): Boolean = readonly
    
    fun spaceTotal(): Long = capacity
    fun spaceUsed(): Long = usedSpace
    
    private fun normalizePath(path: String): String {
        val parts = path.split("/", "\\").filter { it.isNotEmpty() && it != "." }
        val stack = mutableListOf<String>()
        
        for (part in parts) {
            when (part) {
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.add(part)
            }
        }
        
        return stack.joinToString("/")
    }
    
    // NBT persistence
    
    fun saveNbt(): CompoundTag {
        val tag = CompoundTag()
        tag.putUUID("id", id)
        tag.putString("label", label)
        tag.putLong("capacity", capacity)
        tag.putBoolean("readonly", readonly)
        tag.putLong("usedSpace", usedSpace)
        tag.put("root", root.saveNbt())
        return tag
    }
    
    private fun loadNbt(tag: CompoundTag) {
        usedSpace = tag.getLong("usedSpace")
        if (tag.contains("root")) {
            root.loadNbt(tag.getCompound("root"))
        }
    }
    
    // Inner classes
    
    private abstract class VirtualNode(
        var name: String,
        var parent: VirtualDirectory?
    ) {
        var lastModified: Long = System.currentTimeMillis()
        
        abstract fun saveNbt(): CompoundTag
        abstract fun loadNbt(tag: CompoundTag)
    }
    
    private class VirtualDirectory(
        name: String,
        parent: VirtualDirectory?
    ) : VirtualNode(name, parent) {
        val children = mutableMapOf<String, VirtualNode>()
        
        fun resolve(path: String): VirtualNode? {
            if (path.isEmpty()) return this
            
            val parts = path.split("/").filter { it.isNotEmpty() }
            var current: VirtualNode = this
            
            for (part in parts) {
                val dir = current as? VirtualDirectory ?: return null
                current = dir.children[part] ?: return null
            }
            
            return current
        }
        
        override fun saveNbt(): CompoundTag {
            val tag = CompoundTag()
            tag.putString("name", name)
            tag.putLong("lastModified", lastModified)
            
            val childrenList = ListTag()
            for ((childName, child) in children) {
                val childTag = child.saveNbt()
                childTag.putBoolean("isDirectory", child is VirtualDirectory)
                childrenList.add(childTag)
            }
            tag.put("children", childrenList)
            
            return tag
        }
        
        override fun loadNbt(tag: CompoundTag) {
            lastModified = tag.getLong("lastModified")
            children.clear()
            
            val childrenList = tag.getList("children", Tag.TAG_COMPOUND.toInt())
            for (i in 0 until childrenList.size) {
                val childTag = childrenList.getCompound(i)
                val isDirectory = childTag.getBoolean("isDirectory")
                val childName = childTag.getString("name")
                
                val child = if (isDirectory) {
                    VirtualDirectory(childName, this)
                } else {
                    VirtualFile(childName, this, ByteArray(0))
                }
                child.loadNbt(childTag)
                children[childName] = child
            }
        }
    }
    
    private class VirtualFile(
        name: String,
        parent: VirtualDirectory?,
        var data: ByteArray
    ) : VirtualNode(name, parent) {
        val size: Long get() = data.size.toLong()
        
        override fun saveNbt(): CompoundTag {
            val tag = CompoundTag()
            tag.putString("name", name)
            tag.putLong("lastModified", lastModified)
            tag.putByteArray("data", data)
            return tag
        }
        
        override fun loadNbt(tag: CompoundTag) {
            lastModified = tag.getLong("lastModified")
            data = tag.getByteArray("data")
        }
    }
    
    private data class FileHandle(
        val file: VirtualFile,
        val mode: OpenMode,
        var position: Long
    )
    
    enum class OpenMode {
        READ, WRITE, APPEND
    }
    
    enum class SeekMode {
        SET, CUR, END
    }
}

/**
 * Manages file system mounts for a machine
 */
class MountManager(private val host: MachineHost) {
    private val mounts = mutableMapOf<String, Mount>()
    private var nextMountId = 1
    
    fun mount(fs: FileSystem, path: String, readOnly: Boolean = false): Boolean {
        val normalized = normalizeMountPath(path)
        if (mounts.containsKey(normalized)) return false
        
        mounts[normalized] = Mount(fs, normalized, readOnly || fs.isReadOnly())
        return true
    }
    
    fun unmount(path: String): Boolean {
        val normalized = normalizeMountPath(path)
        return mounts.remove(normalized) != null
    }
    
    fun getMounts(): Map<String, Mount> = mounts.toMap()
    
    fun resolve(path: String): Pair<FileSystem, String>? {
        val normalized = normalizePath(path)
        
        // Find longest matching mount
        var bestMount: Mount? = null
        var bestLength = -1
        
        for ((mountPath, mount) in mounts) {
            if (normalized.startsWith(mountPath) || normalized == mountPath.trimEnd('/')) {
                if (mountPath.length > bestLength) {
                    bestMount = mount
                    bestLength = mountPath.length
                }
            }
        }
        
        if (bestMount == null) return null
        
        val relativePath = if (normalized.length > bestLength) {
            normalized.substring(bestLength).trimStart('/')
        } else {
            ""
        }
        
        return bestMount.fs to relativePath
    }
    
    private fun normalizeMountPath(path: String): String {
        return "/" + path.trim('/') + "/"
    }
    
    private fun normalizePath(path: String): String {
        return "/" + path.trim('/') 
    }
    
    fun saveNbt(): CompoundTag {
        val tag = CompoundTag()
        val mountsList = ListTag()
        
        for ((path, mount) in mounts) {
            val mountTag = CompoundTag()
            mountTag.putString("path", path)
            mountTag.putBoolean("readOnly", mount.readOnly)
            mountTag.put("fs", mount.fs.saveNbt())
            mountsList.add(mountTag)
        }
        
        tag.put("mounts", mountsList)
        return tag
    }
    
    fun loadNbt(tag: CompoundTag) {
        mounts.clear()
        val mountsList = tag.getList("mounts", Tag.TAG_COMPOUND.toInt())
        
        for (i in 0 until mountsList.size) {
            val mountTag = mountsList.getCompound(i)
            val path = mountTag.getString("path")
            val readOnly = mountTag.getBoolean("readOnly")
            val fs = FileSystem.fromNbt(mountTag.getCompound("fs"))
            
            if (fs != null) {
                mounts[path] = Mount(fs, path, readOnly)
            }
        }
    }
    
    data class Mount(
        val fs: FileSystem,
        val path: String,
        val readOnly: Boolean
    )
}

/**
 * Temporary file system backed by real disk storage
 */
class TmpFileSystem(
    private val basePath: Path,
    capacity: Long
) {
    private var usedSpace: Long = 0
    private val maxCapacity = capacity
    
    init {
        if (!basePath.exists()) {
            Files.createDirectories(basePath)
        }
    }
    
    fun exists(path: String): Boolean {
        val resolved = resolvePath(path) ?: return false
        return resolved.exists()
    }
    
    fun isDirectory(path: String): Boolean {
        val resolved = resolvePath(path) ?: return false
        return resolved.isDirectory()
    }
    
    fun list(path: String): List<String> {
        val resolved = resolvePath(path) ?: return emptyList()
        if (!resolved.isDirectory()) return emptyList()
        
        return resolved.listDirectoryEntries().map { p ->
            if (p.isDirectory()) "${p.name}/" else p.name
        }.sorted()
    }
    
    fun makeDirectory(path: String): Boolean {
        val resolved = resolvePath(path) ?: return false
        return try {
            Files.createDirectories(resolved)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun delete(path: String): Boolean {
        val resolved = resolvePath(path) ?: return false
        return try {
            if (resolved.isDirectory() && resolved.listDirectoryEntries().isNotEmpty()) {
                false
            } else {
                val size = if (resolved.isRegularFile()) resolved.fileSize() else 0
                Files.deleteIfExists(resolved)
                usedSpace -= size
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun read(path: String): ByteArray? {
        val resolved = resolvePath(path) ?: return null
        return try {
            Files.readAllBytes(resolved)
        } catch (e: Exception) {
            null
        }
    }
    
    fun write(path: String, data: ByteArray): Boolean {
        val resolved = resolvePath(path) ?: return false
        
        val existingSize = if (resolved.exists()) resolved.fileSize() else 0
        val newSize = data.size.toLong()
        val delta = newSize - existingSize
        
        if (usedSpace + delta > maxCapacity) return false
        
        return try {
            Files.createDirectories(resolved.parent)
            Files.write(resolved, data)
            usedSpace += delta
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun clear() {
        try {
            basePath.toFile().deleteRecursively()
            Files.createDirectories(basePath)
            usedSpace = 0
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    private fun resolvePath(path: String): Path? {
        val normalized = path.replace("\\", "/")
            .split("/")
            .filter { it.isNotEmpty() && it != "." && it != ".." }
            .joinToString("/")
        
        if (normalized.isEmpty()) return basePath
        
        val resolved = basePath.resolve(normalized)
        
        // Security check - ensure path doesn't escape base
        return if (resolved.normalize().startsWith(basePath.normalize())) {
            resolved
        } else {
            null
        }
    }
    
    fun getUsedSpace(): Long = usedSpace
    fun getFreeSpace(): Long = maxCapacity - usedSpace
    fun getCapacity(): Long = maxCapacity
}

/**
 * Read-only file system for bundled Lua libraries
 */
class ResourceFileSystem(
    private val resourcePath: String,
    private val classLoader: ClassLoader = ResourceFileSystem::class.java.classLoader
) {
    private val cachedEntries = mutableMapOf<String, CachedEntry>()
    private var initialized = false
    
    private fun initialize() {
        if (initialized) return
        initialized = true
        
        // Scan resource directory
        try {
            val url = classLoader.getResource(resourcePath)
            if (url != null) {
                scanDirectory("", resourcePath)
            }
        } catch (e: Exception) {
            // Resource scanning failed
        }
    }
    
    private fun scanDirectory(virtualPath: String, resourceDir: String) {
        try {
            val url = classLoader.getResource(resourceDir) ?: return
            val uri = url.toURI()
            
            if (uri.scheme == "jar") {
                // Handle JAR resources
                val jarPath = uri.toString().substringBefore("!").removePrefix("jar:")
                java.util.jar.JarFile(File(java.net.URI(jarPath))).use { jar ->
                    val entries = jar.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name.startsWith(resourceDir)) {
                            val relative = entry.name.removePrefix(resourceDir).trimStart('/')
                            if (relative.isNotEmpty()) {
                                val vPath = if (virtualPath.isEmpty()) relative else "$virtualPath/$relative"
                                cachedEntries[vPath] = CachedEntry(
                                    isDirectory = entry.isDirectory,
                                    size = entry.size
                                )
                            }
                        }
                    }
                }
            } else {
                // Handle file system resources
                val dir = File(uri)
                if (dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        val vPath = if (virtualPath.isEmpty()) file.name else "$virtualPath/${file.name}"
                        cachedEntries[vPath] = CachedEntry(
                            isDirectory = file.isDirectory,
                            size = file.length()
                        )
                        if (file.isDirectory) {
                            scanDirectory(vPath, "$resourceDir/${file.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Scanning failed
        }
    }
    
    fun exists(path: String): Boolean {
        initialize()
        val normalized = normalizePath(path)
        return cachedEntries.containsKey(normalized) || normalized.isEmpty()
    }
    
    fun isDirectory(path: String): Boolean {
        initialize()
        val normalized = normalizePath(path)
        return normalized.isEmpty() || cachedEntries[normalized]?.isDirectory == true
    }
    
    fun list(path: String): List<String> {
        initialize()
        val normalized = normalizePath(path)
        val prefix = if (normalized.isEmpty()) "" else "$normalized/"
        
        return cachedEntries.keys
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix).split("/").first() }
            .distinct()
            .map { name ->
                if (cachedEntries["$prefix$name"]?.isDirectory == true) "$name/" else name
            }
            .sorted()
    }
    
    fun read(path: String): ByteArray? {
        val normalized = normalizePath(path)
        if (normalized.isEmpty()) return null
        
        val fullPath = "$resourcePath/$normalized"
        return try {
            classLoader.getResourceAsStream(fullPath)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }
    
    fun size(path: String): Long {
        initialize()
        return cachedEntries[normalizePath(path)]?.size ?: 0
    }
    
    private fun normalizePath(path: String): String {
        return path.replace("\\", "/")
            .split("/")
            .filter { it.isNotEmpty() && it != "." && it != ".." }
            .joinToString("/")
    }
    
    private data class CachedEntry(
        val isDirectory: Boolean,
        val size: Long
    )
}

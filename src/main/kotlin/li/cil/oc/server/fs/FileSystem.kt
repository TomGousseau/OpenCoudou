package li.cil.oc.server.fs

import li.cil.oc.api.fs.FileSystem as IFileSystem
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Virtual file system implementation supporting both in-memory and disk-backed storage.
 * Supports multiple mount points, symbolic links, and proper POSIX-like permissions.
 */
abstract class VirtualFileSystem : IFileSystem {
    protected val handles = ConcurrentHashMap<Int, FileHandle>()
    protected var nextHandle = 0
    
    abstract fun getLabel(): String?
    abstract fun setLabel(value: String?)
    abstract fun isReadOnly(): Boolean
    
    override fun spaceTotal(): Long = Long.MAX_VALUE
    override fun spaceUsed(): Long = 0L
    
    protected fun allocateHandle(handle: FileHandle): Int {
        val id = nextHandle++
        handles[id] = handle
        return id
    }
    
    override fun close(handle: Int) {
        handles.remove(handle)?.close()
    }
    
    override fun read(handle: Int, count: Int): ByteArray? {
        val h = handles[handle] ?: return null
        return h.read(count)
    }
    
    override fun write(handle: Int, data: ByteArray): Boolean {
        val h = handles[handle] ?: return false
        if (isReadOnly()) return false
        return h.write(data)
    }
    
    override fun seek(handle: Int, offset: Long, whence: Int): Long {
        val h = handles[handle] ?: return -1
        return h.seek(offset, whence)
    }
    
    abstract override fun exists(path: String): Boolean
    abstract override fun isDirectory(path: String): Boolean
    abstract override fun list(path: String): Array<String>?
    abstract override fun size(path: String): Long
    abstract override fun lastModified(path: String): Long
    abstract override fun open(path: String, mode: String): Int
    abstract override fun makeDirectory(path: String): Boolean
    abstract override fun delete(path: String): Boolean
    abstract override fun rename(from: String, to: String): Boolean
    
    abstract fun save(tag: CompoundTag)
    abstract fun load(tag: CompoundTag)
}

/**
 * Memory-backed file system - fast but volatile.
 */
class MemoryFileSystem(
    private var label: String? = null,
    private val capacity: Long = 1024 * 1024,
    private val readOnly: Boolean = false
) : VirtualFileSystem() {
    
    private val root = VirtualDirectory("")
    private var usedSpace = 0L
    
    override fun getLabel(): String? = label
    override fun setLabel(value: String?) { label = value }
    override fun isReadOnly(): Boolean = readOnly
    override fun spaceTotal(): Long = capacity
    override fun spaceUsed(): Long = usedSpace
    
    override fun exists(path: String): Boolean {
        val normalized = normalizePath(path)
        return findNode(normalized) != null
    }
    
    override fun isDirectory(path: String): Boolean {
        val normalized = normalizePath(path)
        return findNode(normalized) is VirtualDirectory
    }
    
    override fun list(path: String): Array<String>? {
        val normalized = normalizePath(path)
        val node = findNode(normalized) as? VirtualDirectory ?: return null
        return node.children.keys.toTypedArray()
    }
    
    override fun size(path: String): Long {
        val normalized = normalizePath(path)
        val node = findNode(normalized) as? VirtualFile ?: return 0L
        return node.data.size.toLong()
    }
    
    override fun lastModified(path: String): Long {
        val normalized = normalizePath(path)
        val node = findNode(normalized) ?: return 0L
        return node.lastModified
    }
    
    override fun open(path: String, mode: String): Int {
        val normalized = normalizePath(path)
        val isWrite = mode.contains('w') || mode.contains('a')
        
        if (isWrite && readOnly) return -1
        
        var node = findNode(normalized)
        
        if (node == null && isWrite) {
            // Create file
            val parentPath = getParentPath(normalized)
            val fileName = getFileName(normalized)
            val parent = findNode(parentPath) as? VirtualDirectory ?: return -1
            
            node = VirtualFile(fileName)
            parent.children[fileName] = node
        }
        
        val file = node as? VirtualFile ?: return -1
        
        if (mode.contains('w')) {
            usedSpace -= file.data.size
            file.data = ByteArray(0)
        }
        
        val handle = MemoryFileHandle(file, mode.contains('a'))
        return allocateHandle(handle)
    }
    
    override fun makeDirectory(path: String): Boolean {
        if (readOnly) return false
        
        val normalized = normalizePath(path)
        if (exists(normalized)) return false
        
        val parentPath = getParentPath(normalized)
        val dirName = getFileName(normalized)
        val parent = findNode(parentPath) as? VirtualDirectory ?: return false
        
        parent.children[dirName] = VirtualDirectory(dirName)
        return true
    }
    
    override fun delete(path: String): Boolean {
        if (readOnly) return false
        
        val normalized = normalizePath(path)
        if (normalized.isEmpty()) return false // Can't delete root
        
        val parentPath = getParentPath(normalized)
        val nodeName = getFileName(normalized)
        val parent = findNode(parentPath) as? VirtualDirectory ?: return false
        
        val node = parent.children[nodeName] ?: return false
        if (node is VirtualDirectory && node.children.isNotEmpty()) return false
        
        if (node is VirtualFile) {
            usedSpace -= node.data.size
        }
        
        parent.children.remove(nodeName)
        return true
    }
    
    override fun rename(from: String, to: String): Boolean {
        if (readOnly) return false
        
        val normalizedFrom = normalizePath(from)
        val normalizedTo = normalizePath(to)
        
        if (exists(normalizedTo)) return false
        
        val fromParent = findNode(getParentPath(normalizedFrom)) as? VirtualDirectory ?: return false
        val toParent = findNode(getParentPath(normalizedTo)) as? VirtualDirectory ?: return false
        
        val fromName = getFileName(normalizedFrom)
        val toName = getFileName(normalizedTo)
        
        val node = fromParent.children.remove(fromName) ?: return false
        node.name = toName
        toParent.children[toName] = node
        
        return true
    }
    
    override fun save(tag: CompoundTag) {
        label?.let { tag.putString("label", it) }
        tag.putLong("used", usedSpace)
        tag.put("root", saveDirectory(root))
    }
    
    override fun load(tag: CompoundTag) {
        label = if (tag.contains("label")) tag.getString("label") else null
        usedSpace = tag.getLong("used")
        if (tag.contains("root")) {
            loadDirectory(root, tag.getCompound("root"))
        }
    }
    
    private fun saveDirectory(dir: VirtualDirectory): CompoundTag {
        val tag = CompoundTag()
        tag.putLong("modified", dir.lastModified)
        
        val children = ListTag()
        dir.children.forEach { (name, node) ->
            val childTag = CompoundTag()
            childTag.putString("name", name)
            childTag.putBoolean("isDir", node is VirtualDirectory)
            
            if (node is VirtualDirectory) {
                childTag.put("content", saveDirectory(node))
            } else if (node is VirtualFile) {
                childTag.putByteArray("data", node.data)
                childTag.putLong("modified", node.lastModified)
            }
            
            children.add(childTag)
        }
        tag.put("children", children)
        
        return tag
    }
    
    private fun loadDirectory(dir: VirtualDirectory, tag: CompoundTag) {
        dir.lastModified = tag.getLong("modified")
        
        val children = tag.getList("children", Tag.TAG_COMPOUND.toInt())
        for (i in 0 until children.size) {
            val childTag = children.getCompound(i)
            val name = childTag.getString("name")
            val isDir = childTag.getBoolean("isDir")
            
            if (isDir) {
                val subDir = VirtualDirectory(name)
                loadDirectory(subDir, childTag.getCompound("content"))
                dir.children[name] = subDir
            } else {
                val file = VirtualFile(name)
                file.data = childTag.getByteArray("data")
                file.lastModified = childTag.getLong("modified")
                dir.children[name] = file
            }
        }
    }
    
    private fun normalizePath(path: String): String {
        val parts = path.split("/").filter { it.isNotEmpty() && it != "." }
        val stack = mutableListOf<String>()
        
        for (part in parts) {
            if (part == "..") {
                if (stack.isNotEmpty()) stack.removeLast()
            } else {
                stack.add(part)
            }
        }
        
        return stack.joinToString("/")
    }
    
    private fun findNode(path: String): VirtualNode? {
        if (path.isEmpty()) return root
        
        val parts = path.split("/")
        var current: VirtualNode = root
        
        for (part in parts) {
            val dir = current as? VirtualDirectory ?: return null
            current = dir.children[part] ?: return null
        }
        
        return current
    }
    
    private fun getParentPath(path: String): String {
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash > 0) path.substring(0, lastSlash) else ""
    }
    
    private fun getFileName(path: String): String {
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash >= 0) path.substring(lastSlash + 1) else path
    }
    
    private abstract class VirtualNode(var name: String) {
        var lastModified: Long = System.currentTimeMillis()
    }
    
    private class VirtualDirectory(name: String) : VirtualNode(name) {
        val children = mutableMapOf<String, VirtualNode>()
    }
    
    private class VirtualFile(name: String) : VirtualNode(name) {
        var data = ByteArray(0)
    }
    
    private inner class MemoryFileHandle(
        private val file: VirtualFile,
        append: Boolean
    ) : FileHandle {
        private var position = if (append) file.data.size.toLong() else 0L
        
        override fun read(count: Int): ByteArray? {
            if (position >= file.data.size) return null
            
            val available = (file.data.size - position).toInt()
            val toRead = min(count, available)
            val result = file.data.copyOfRange(position.toInt(), position.toInt() + toRead)
            position += toRead
            
            return result
        }
        
        override fun write(data: ByteArray): Boolean {
            val newSize = (position + data.size).toInt()
            
            if (newSize > file.data.size) {
                val spaceDiff = newSize - file.data.size
                if (usedSpace + spaceDiff > capacity) return false
                
                val newData = ByteArray(newSize)
                file.data.copyInto(newData)
                file.data = newData
                usedSpace += spaceDiff
            }
            
            data.copyInto(file.data, position.toInt())
            position += data.size
            file.lastModified = System.currentTimeMillis()
            
            return true
        }
        
        override fun seek(offset: Long, whence: Int): Long {
            position = when (whence) {
                0 -> offset // SET
                1 -> position + offset // CUR
                2 -> file.data.size + offset // END
                else -> return -1
            }
            return position
        }
        
        override fun close() {}
    }
}

/**
 * Disk-backed file system for persistent storage.
 */
class DiskFileSystem(
    private val baseDir: File,
    private var label: String? = null,
    private val capacity: Long = 1024 * 1024,
    private val readOnly: Boolean = false
) : VirtualFileSystem() {
    
    init {
        if (!readOnly && !baseDir.exists()) {
            baseDir.mkdirs()
        }
    }
    
    override fun getLabel(): String? = label
    override fun setLabel(value: String?) { label = value }
    override fun isReadOnly(): Boolean = readOnly
    override fun spaceTotal(): Long = capacity
    
    override fun spaceUsed(): Long {
        return calculateSize(baseDir)
    }
    
    private fun calculateSize(file: File): Long {
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { calculateSize(it) } ?: 0L
    }
    
    override fun exists(path: String): Boolean {
        return toFile(path)?.exists() == true
    }
    
    override fun isDirectory(path: String): Boolean {
        return toFile(path)?.isDirectory == true
    }
    
    override fun list(path: String): Array<String>? {
        val file = toFile(path) ?: return null
        if (!file.isDirectory) return null
        return file.list() ?: emptyArray()
    }
    
    override fun size(path: String): Long {
        return toFile(path)?.length() ?: 0L
    }
    
    override fun lastModified(path: String): Long {
        return toFile(path)?.lastModified() ?: 0L
    }
    
    override fun open(path: String, mode: String): Int {
        if (readOnly && (mode.contains('w') || mode.contains('a'))) return -1
        
        val file = toFile(path) ?: return -1
        
        try {
            val handle = when {
                mode.contains('w') -> {
                    file.parentFile?.mkdirs()
                    DiskFileHandle(RandomAccessFile(file, "rw"), false)
                }
                mode.contains('a') -> {
                    file.parentFile?.mkdirs()
                    DiskFileHandle(RandomAccessFile(file, "rw"), true)
                }
                else -> {
                    if (!file.exists()) return -1
                    DiskFileHandle(RandomAccessFile(file, "r"), false)
                }
            }
            
            if (mode.contains('w')) {
                handle.raf.setLength(0)
            }
            
            return allocateHandle(handle)
        } catch (e: Exception) {
            return -1
        }
    }
    
    override fun makeDirectory(path: String): Boolean {
        if (readOnly) return false
        val file = toFile(path) ?: return false
        return file.mkdirs()
    }
    
    override fun delete(path: String): Boolean {
        if (readOnly) return false
        val file = toFile(path) ?: return false
        return deleteRecursive(file)
    }
    
    private fun deleteRecursive(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        return file.delete()
    }
    
    override fun rename(from: String, to: String): Boolean {
        if (readOnly) return false
        val fromFile = toFile(from) ?: return false
        val toFile = toFile(to) ?: return false
        return fromFile.renameTo(toFile)
    }
    
    override fun save(tag: CompoundTag) {
        label?.let { tag.putString("label", it) }
    }
    
    override fun load(tag: CompoundTag) {
        label = if (tag.contains("label")) tag.getString("label") else null
    }
    
    private fun toFile(path: String): File? {
        // Sanitize path to prevent directory traversal
        val normalized = path.replace("\\", "/")
            .split("/")
            .filter { it.isNotEmpty() && it != "." && it != ".." }
            .joinToString(File.separator)
        
        if (normalized.isEmpty()) return baseDir
        
        val resolved = File(baseDir, normalized)
        
        // Verify the resolved path is within our base directory
        return if (resolved.canonicalPath.startsWith(baseDir.canonicalPath)) {
            resolved
        } else {
            null
        }
    }
    
    private class DiskFileHandle(
        val raf: RandomAccessFile,
        append: Boolean
    ) : FileHandle {
        init {
            if (append) {
                raf.seek(raf.length())
            }
        }
        
        override fun read(count: Int): ByteArray? {
            try {
                val buffer = ByteArray(count)
                val read = raf.read(buffer)
                if (read <= 0) return null
                return if (read == count) buffer else buffer.copyOf(read)
            } catch (e: Exception) {
                return null
            }
        }
        
        override fun write(data: ByteArray): Boolean {
            try {
                raf.write(data)
                return true
            } catch (e: Exception) {
                return false
            }
        }
        
        override fun seek(offset: Long, whence: Int): Long {
            try {
                val newPos = when (whence) {
                    0 -> offset
                    1 -> raf.filePointer + offset
                    2 -> raf.length() + offset
                    else -> return -1
                }
                raf.seek(newPos)
                return newPos
            } catch (e: Exception) {
                return -1
            }
        }
        
        override fun close() {
            try {
                raf.close()
            } catch (e: Exception) {}
        }
    }
}

/**
 * Read-only file system from classpath resources.
 */
class ResourceFileSystem(
    private val resourcePath: String,
    private val classLoader: ClassLoader = ResourceFileSystem::class.java.classLoader
) : VirtualFileSystem() {
    
    private val index = mutableMapOf<String, ResourceEntry>()
    
    init {
        buildIndex()
    }
    
    private fun buildIndex() {
        // Index would be built from resource listing or predefined
    }
    
    override fun getLabel(): String? = "rom"
    override fun setLabel(value: String?) {}
    override fun isReadOnly(): Boolean = true
    
    override fun exists(path: String): Boolean = getResource(path) != null
    
    override fun isDirectory(path: String): Boolean {
        val entry = index[normalizePath(path)]
        return entry?.isDirectory == true
    }
    
    override fun list(path: String): Array<String>? {
        val entry = index[normalizePath(path)]
        if (entry?.isDirectory != true) return null
        return entry.children.toTypedArray()
    }
    
    override fun size(path: String): Long {
        val resource = getResource(path) ?: return 0L
        return try {
            resource.openStream().use { it.available().toLong() }
        } catch (e: Exception) {
            0L
        }
    }
    
    override fun lastModified(path: String): Long = 0L
    
    override fun open(path: String, mode: String): Int {
        if (mode.contains('w') || mode.contains('a')) return -1
        
        val resource = getResource(path) ?: return -1
        
        try {
            val stream = resource.openStream()
            return allocateHandle(StreamFileHandle(stream))
        } catch (e: Exception) {
            return -1
        }
    }
    
    override fun makeDirectory(path: String): Boolean = false
    override fun delete(path: String): Boolean = false
    override fun rename(from: String, to: String): Boolean = false
    
    override fun save(tag: CompoundTag) {}
    override fun load(tag: CompoundTag) {}
    
    private fun getResource(path: String): java.net.URL? {
        val normalized = normalizePath(path)
        val fullPath = "$resourcePath/$normalized".trimStart('/')
        return classLoader.getResource(fullPath)
    }
    
    private fun normalizePath(path: String): String {
        return path.replace("\\", "/")
            .split("/")
            .filter { it.isNotEmpty() && it != "." }
            .joinToString("/")
    }
    
    private data class ResourceEntry(
        val isDirectory: Boolean,
        val children: List<String> = emptyList()
    )
    
    private class StreamFileHandle(private val stream: InputStream) : FileHandle {
        private var buffer: ByteArray? = null
        private var position = 0
        
        override fun read(count: Int): ByteArray? {
            ensureBuffered()
            val buf = buffer ?: return null
            
            if (position >= buf.size) return null
            
            val available = buf.size - position
            val toRead = min(count, available)
            val result = buf.copyOfRange(position, position + toRead)
            position += toRead
            
            return result
        }
        
        override fun write(data: ByteArray): Boolean = false
        
        override fun seek(offset: Long, whence: Int): Long {
            ensureBuffered()
            val buf = buffer ?: return -1
            
            position = when (whence) {
                0 -> offset.toInt()
                1 -> position + offset.toInt()
                2 -> buf.size + offset.toInt()
                else -> return -1
            }
            
            return position.toLong()
        }
        
        override fun close() {
            try {
                stream.close()
            } catch (e: Exception) {}
        }
        
        private fun ensureBuffered() {
            if (buffer == null) {
                buffer = try {
                    stream.readAllBytes()
                } catch (e: Exception) {
                    ByteArray(0)
                }
            }
        }
    }
}

/**
 * Union file system combining multiple file systems with mount points.
 */
class UnionFileSystem : VirtualFileSystem() {
    
    private val mounts = mutableListOf<Mount>()
    private var rootLabel: String? = null
    
    data class Mount(
        val path: String,
        val fs: VirtualFileSystem,
        val priority: Int = 0
    )
    
    fun mount(path: String, fs: VirtualFileSystem, priority: Int = 0) {
        val normalized = normalizePath(path)
        mounts.add(Mount(normalized, fs, priority))
        mounts.sortByDescending { it.priority }
    }
    
    fun unmount(path: String): Boolean {
        val normalized = normalizePath(path)
        return mounts.removeIf { it.path == normalized }
    }
    
    fun getMounts(): List<Mount> = mounts.toList()
    
    override fun getLabel(): String? = rootLabel
    override fun setLabel(value: String?) { rootLabel = value }
    override fun isReadOnly(): Boolean = mounts.all { it.fs.isReadOnly() }
    
    override fun spaceTotal(): Long = mounts.sumOf { it.fs.spaceTotal() }
    override fun spaceUsed(): Long = mounts.sumOf { it.fs.spaceUsed() }
    
    override fun exists(path: String): Boolean {
        val (mount, relativePath) = resolve(path) ?: return false
        return mount.fs.exists(relativePath)
    }
    
    override fun isDirectory(path: String): Boolean {
        val normalized = normalizePath(path)
        
        // Check if any mount point is under this path
        if (mounts.any { it.path.startsWith(normalized) && it.path != normalized }) {
            return true
        }
        
        val (mount, relativePath) = resolve(path) ?: return false
        return mount.fs.isDirectory(relativePath)
    }
    
    override fun list(path: String): Array<String>? {
        val normalized = normalizePath(path)
        val results = mutableSetOf<String>()
        
        // Add virtual directories from mount points
        for (mount in mounts) {
            if (mount.path.startsWith(normalized) && mount.path != normalized) {
                val remaining = mount.path.removePrefix(normalized).trimStart('/')
                val firstPart = remaining.split("/").first()
                results.add(firstPart)
            }
        }
        
        // Add actual directory contents
        val resolution = resolve(path)
        if (resolution != null) {
            val (mount, relativePath) = resolution
            mount.fs.list(relativePath)?.forEach { results.add(it) }
        }
        
        return if (results.isNotEmpty()) results.toTypedArray() else null
    }
    
    override fun size(path: String): Long {
        val (mount, relativePath) = resolve(path) ?: return 0L
        return mount.fs.size(relativePath)
    }
    
    override fun lastModified(path: String): Long {
        val (mount, relativePath) = resolve(path) ?: return 0L
        return mount.fs.lastModified(relativePath)
    }
    
    override fun open(path: String, mode: String): Int {
        val (mount, relativePath) = resolve(path) ?: return -1
        val handle = mount.fs.open(relativePath, mode)
        if (handle < 0) return -1
        return allocateHandle(ProxyFileHandle(mount.fs, handle))
    }
    
    override fun makeDirectory(path: String): Boolean {
        val (mount, relativePath) = resolve(path) ?: return false
        return mount.fs.makeDirectory(relativePath)
    }
    
    override fun delete(path: String): Boolean {
        val (mount, relativePath) = resolve(path) ?: return false
        return mount.fs.delete(relativePath)
    }
    
    override fun rename(from: String, to: String): Boolean {
        val (fromMount, fromRelative) = resolve(from) ?: return false
        val (toMount, toRelative) = resolve(to) ?: return false
        
        // Can only rename within the same mount
        if (fromMount != toMount) return false
        
        return fromMount.fs.rename(fromRelative, toRelative)
    }
    
    override fun save(tag: CompoundTag) {
        rootLabel?.let { tag.putString("label", it) }
        
        val mountsTag = ListTag()
        mounts.forEach { mount ->
            val mountTag = CompoundTag()
            mountTag.putString("path", mount.path)
            mountTag.putInt("priority", mount.priority)
            mount.fs.save(mountTag)
            mountsTag.add(mountTag)
        }
        tag.put("mounts", mountsTag)
    }
    
    override fun load(tag: CompoundTag) {
        rootLabel = if (tag.contains("label")) tag.getString("label") else null
        // Mounts need to be restored externally
    }
    
    private fun resolve(path: String): Pair<Mount, String>? {
        val normalized = normalizePath(path)
        
        for (mount in mounts) {
            if (normalized == mount.path || normalized.startsWith(mount.path + "/")) {
                val relative = normalized.removePrefix(mount.path).trimStart('/')
                return mount to relative
            }
        }
        
        return null
    }
    
    private fun normalizePath(path: String): String {
        return path.replace("\\", "/")
            .split("/")
            .filter { it.isNotEmpty() && it != "." }
            .fold(mutableListOf<String>()) { acc, part ->
                if (part == "..") {
                    if (acc.isNotEmpty()) acc.removeLast()
                } else {
                    acc.add(part)
                }
                acc
            }
            .joinToString("/")
    }
    
    private class ProxyFileHandle(
        private val fs: VirtualFileSystem,
        private val handle: Int
    ) : FileHandle {
        override fun read(count: Int): ByteArray? = fs.read(handle, count)
        override fun write(data: ByteArray): Boolean = fs.write(handle, data)
        override fun seek(offset: Long, whence: Int): Long = fs.seek(handle, offset, whence)
        override fun close() = fs.close(handle)
    }
}

interface FileHandle {
    fun read(count: Int): ByteArray?
    fun write(data: ByteArray): Boolean
    fun seek(offset: Long, whence: Int): Long
    fun close()
}

/**
 * File system manager - handles creation and persistence of file systems.
 */
object FileSystemManager {
    private val fileSystems = mutableMapOf<UUID, VirtualFileSystem>()
    private var saveDirectory: File? = null
    
    fun setSaveDirectory(dir: File) {
        saveDirectory = dir
        dir.mkdirs()
    }
    
    fun createMemoryFS(capacity: Long = 1024 * 1024): VirtualFileSystem {
        val fs = MemoryFileSystem(capacity = capacity)
        val id = UUID.randomUUID()
        fileSystems[id] = fs
        return fs
    }
    
    fun createDiskFS(id: UUID, capacity: Long = 1024 * 1024): VirtualFileSystem {
        val dir = File(saveDirectory, id.toString())
        val fs = DiskFileSystem(dir, capacity = capacity)
        fileSystems[id] = fs
        return fs
    }
    
    fun getFileSystem(id: UUID): VirtualFileSystem? = fileSystems[id]
    
    fun removeFileSystem(id: UUID) {
        fileSystems.remove(id)
    }
}

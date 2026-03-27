package li.cil.oc.client.os.filesystem

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance virtual filesystem for SkibidiOS2.
 * Features:
 * - In-memory caching for fast access
 * - Hierarchical directory structure
 * - File permissions and metadata
 * - Mount points for component drives
 */

sealed class FSNode {
    abstract val name: String
    abstract var parent: DirectoryNode?
    abstract var permissions: Int
    abstract var created: Long
    abstract var modified: Long
    
    fun getPath(): String {
        val parts = mutableListOf<String>()
        var current: FSNode? = this
        while (current != null && current.name.isNotEmpty()) {
            parts.add(0, current.name)
            current = current.parent
        }
        return "/" + parts.joinToString("/")
    }
}

class FileNode(
    override val name: String,
    override var parent: DirectoryNode? = null,
    override var permissions: Int = 0x1FF, // rwxrwxrwx
    override var created: Long = System.currentTimeMillis(),
    override var modified: Long = System.currentTimeMillis()
) : FSNode() {
    private var data: ByteArray = ByteArray(0)
    
    val size: Long get() = data.size.toLong()
    
    fun read(): ByteArray = data.clone()
    
    fun readString(): String = String(data, Charsets.UTF_8)
    
    fun write(content: ByteArray) {
        data = content.clone()
        modified = System.currentTimeMillis()
    }
    
    fun writeString(content: String) {
        write(content.toByteArray(Charsets.UTF_8))
    }
    
    fun append(content: ByteArray) {
        data = data + content
        modified = System.currentTimeMillis()
    }
    
    fun openInputStream(): ByteArrayInputStream = ByteArrayInputStream(data)
    
    fun openOutputStream(): ByteArrayOutputStream = ByteArrayOutputStream().also { 
        it.write(data) 
    }
}

class DirectoryNode(
    override val name: String,
    override var parent: DirectoryNode? = null,
    override var permissions: Int = 0x1FF,
    override var created: Long = System.currentTimeMillis(),
    override var modified: Long = System.currentTimeMillis()
) : FSNode() {
    private val children = ConcurrentHashMap<String, FSNode>()
    
    fun list(): List<String> = children.keys.toList()
    
    fun listNodes(): List<FSNode> = children.values.toList()
    
    fun getChild(name: String): FSNode? = children[name]
    
    fun addChild(node: FSNode): Boolean {
        if (children.containsKey(node.name)) return false
        node.parent = this
        children[node.name] = node
        modified = System.currentTimeMillis()
        return true
    }
    
    fun removeChild(name: String): FSNode? {
        modified = System.currentTimeMillis()
        return children.remove(name)
    }
    
    fun hasChild(name: String): Boolean = children.containsKey(name)
}

data class MountPoint(
    val path: String,
    val label: String,
    val address: String,
    val readonly: Boolean = false,
    val spaceTotal: Long = 0,
    val spaceUsed: Long = 0
)

class VirtualFileSystem {
    private val root = DirectoryNode("")
    private val mounts = ConcurrentHashMap<String, MountPoint>()
    private val handles = ConcurrentHashMap<Int, FileHandle>()
    private var nextHandle = 1
    
    init {
        // Create standard directories
        mkdir("/bin")
        mkdir("/etc")
        mkdir("/home")
        mkdir("/home/user")
        mkdir("/lib")
        mkdir("/mnt")
        mkdir("/tmp")
        mkdir("/usr")
        mkdir("/usr/share")
        mkdir("/var")
        mkdir("/var/log")
    }
    
    // ==================== Path Resolution ====================
    
    private fun normalizePath(path: String): String {
        val parts = path.split("/").filter { it.isNotEmpty() && it != "." }
        val result = mutableListOf<String>()
        for (part in parts) {
            when (part) {
                ".." -> if (result.isNotEmpty()) result.removeLast()
                else -> result.add(part)
            }
        }
        return "/" + result.joinToString("/")
    }
    
    private fun resolvePath(path: String): FSNode? {
        val normalized = normalizePath(path)
        if (normalized == "/") return root
        
        val parts = normalized.substring(1).split("/")
        var current: FSNode = root
        
        for (part in parts) {
            if (current !is DirectoryNode) return null
            current = current.getChild(part) ?: return null
        }
        
        return current
    }
    
    private fun resolveParent(path: String): Pair<DirectoryNode, String>? {
        val normalized = normalizePath(path)
        val lastSlash = normalized.lastIndexOf('/')
        val parentPath = if (lastSlash <= 0) "/" else normalized.substring(0, lastSlash)
        val name = normalized.substring(lastSlash + 1)
        
        val parent = resolvePath(parentPath) as? DirectoryNode ?: return null
        return Pair(parent, name)
    }
    
    // ==================== File Operations ====================
    
    fun exists(path: String): Boolean = resolvePath(path) != null
    
    fun isFile(path: String): Boolean = resolvePath(path) is FileNode
    
    fun isDirectory(path: String): Boolean = resolvePath(path) is DirectoryNode
    
    fun size(path: String): Long = (resolvePath(path) as? FileNode)?.size ?: 0
    
    fun list(path: String): List<String>? {
        val node = resolvePath(path) as? DirectoryNode ?: return null
        return node.list()
    }
    
    fun listDetailed(path: String): List<FileInfo>? {
        val node = resolvePath(path) as? DirectoryNode ?: return null
        return node.listNodes().map { child ->
            FileInfo(
                name = child.name,
                path = child.getPath(),
                isDirectory = child is DirectoryNode,
                size = if (child is FileNode) child.size else 0,
                permissions = child.permissions,
                created = child.created,
                modified = child.modified
            )
        }
    }
    
    fun mkdir(path: String): Boolean {
        val normalized = normalizePath(path)
        if (normalized == "/") return true
        
        val parts = normalized.substring(1).split("/")
        var current: DirectoryNode = root
        
        for (part in parts) {
            val existing = current.getChild(part)
            current = when (existing) {
                is DirectoryNode -> existing
                null -> {
                    val newDir = DirectoryNode(part)
                    current.addChild(newDir)
                    newDir
                }
                else -> return false // File exists with that name
            }
        }
        return true
    }
    
    fun mkdirs(path: String): Boolean = mkdir(path) // mkdir already creates parents
    
    fun touch(path: String): Boolean {
        if (exists(path)) {
            val node = resolvePath(path)
            node?.modified = System.currentTimeMillis()
            return true
        }
        
        val (parent, name) = resolveParent(path) ?: return false
        return parent.addChild(FileNode(name))
    }
    
    fun delete(path: String, recursive: Boolean = false): Boolean {
        val normalized = normalizePath(path)
        if (normalized == "/") return false
        
        val (parent, name) = resolveParent(path) ?: return false
        val node = parent.getChild(name) ?: return false
        
        if (node is DirectoryNode && node.listNodes().isNotEmpty() && !recursive) {
            return false
        }
        
        return parent.removeChild(name) != null
    }
    
    fun rename(from: String, to: String): Boolean {
        val fromNode = resolvePath(from) ?: return false
        val (toParent, toName) = resolveParent(to) ?: return false
        
        if (toParent.hasChild(toName)) return false
        
        val oldParent = fromNode.parent ?: return false
        oldParent.removeChild(fromNode.name)
        
        // Create new node with new name
        val newNode = when (fromNode) {
            is FileNode -> FileNode(toName, toParent, fromNode.permissions, fromNode.created).also {
                it.write(fromNode.read())
            }
            is DirectoryNode -> DirectoryNode(toName, toParent, fromNode.permissions, fromNode.created).also { dir ->
                fromNode.listNodes().forEach { child ->
                    dir.addChild(child)
                }
            }
        }
        
        return toParent.addChild(newNode)
    }
    
    fun copy(from: String, to: String, recursive: Boolean = false): Boolean {
        val fromNode = resolvePath(from) ?: return false
        val (toParent, toName) = resolveParent(to) ?: return false
        
        if (toParent.hasChild(toName)) return false
        
        return when (fromNode) {
            is FileNode -> {
                val newFile = FileNode(toName)
                newFile.write(fromNode.read())
                toParent.addChild(newFile)
            }
            is DirectoryNode -> {
                if (!recursive) return false
                val newDir = DirectoryNode(toName)
                toParent.addChild(newDir)
                copyDirectoryContents(fromNode, newDir)
            }
        }
    }
    
    private fun copyDirectoryContents(from: DirectoryNode, to: DirectoryNode): Boolean {
        for (child in from.listNodes()) {
            when (child) {
                is FileNode -> {
                    val newFile = FileNode(child.name)
                    newFile.write(child.read())
                    to.addChild(newFile)
                }
                is DirectoryNode -> {
                    val newDir = DirectoryNode(child.name)
                    to.addChild(newDir)
                    copyDirectoryContents(child, newDir)
                }
            }
        }
        return true
    }
    
    // ==================== Read/Write Operations ====================
    
    fun readFile(path: String): ByteArray? {
        return (resolvePath(path) as? FileNode)?.read()
    }
    
    fun readText(path: String): String? {
        return (resolvePath(path) as? FileNode)?.readString()
    }
    
    fun writeFile(path: String, data: ByteArray): Boolean {
        val file = resolvePath(path) as? FileNode
        if (file != null) {
            file.write(data)
            return true
        }
        
        // Create new file
        val (parent, name) = resolveParent(path) ?: return false
        val newFile = FileNode(name)
        newFile.write(data)
        return parent.addChild(newFile)
    }
    
    fun writeText(path: String, content: String): Boolean {
        return writeFile(path, content.toByteArray(Charsets.UTF_8))
    }
    
    fun appendFile(path: String, data: ByteArray): Boolean {
        val file = resolvePath(path) as? FileNode
        if (file != null) {
            file.append(data)
            return true
        }
        return writeFile(path, data)
    }
    
    fun appendText(path: String, content: String): Boolean {
        return appendFile(path, content.toByteArray(Charsets.UTF_8))
    }
    
    // ==================== File Handle Operations ====================
    
    fun open(path: String, mode: String = "r"): Int? {
        val normalized = normalizePath(path)
        
        when {
            "r" in mode -> {
                val file = resolvePath(normalized) as? FileNode ?: return null
                val handle = FileHandle(nextHandle++, normalized, file, mode)
                handles[handle.id] = handle
                return handle.id
            }
            "w" in mode -> {
                val file = resolvePath(normalized) as? FileNode
                    ?: run {
                        val (parent, name) = resolveParent(normalized) ?: return null
                        val newFile = FileNode(name)
                        parent.addChild(newFile)
                        newFile
                    }
                if ("w" in mode && "a" !in mode) file.write(ByteArray(0))
                val handle = FileHandle(nextHandle++, normalized, file, mode)
                handles[handle.id] = handle
                return handle.id
            }
            else -> return null
        }
    }
    
    fun read(handle: Int, count: Int = -1): ByteArray? {
        val h = handles[handle] ?: return null
        return h.read(count)
    }
    
    fun write(handle: Int, data: ByteArray): Boolean {
        val h = handles[handle] ?: return false
        return h.write(data)
    }
    
    fun seek(handle: Int, whence: String, offset: Int): Long? {
        val h = handles[handle] ?: return null
        return h.seek(whence, offset)
    }
    
    fun close(handle: Int): Boolean {
        handles.remove(handle)?.close()
        return true
    }
    
    // ==================== Mount Operations ====================
    
    fun mount(address: String, path: String, label: String, readonly: Boolean = false): Boolean {
        val normalized = normalizePath(path)
        if (!mkdir(normalized)) return false
        mounts[normalized] = MountPoint(normalized, label, address, readonly)
        return true
    }
    
    fun unmount(path: String): Boolean {
        val normalized = normalizePath(path)
        return mounts.remove(normalized) != null
    }
    
    fun getMounts(): List<MountPoint> = mounts.values.toList()
    
    fun getMount(path: String): MountPoint? {
        val normalized = normalizePath(path)
        return mounts.entries
            .filter { normalized.startsWith(it.key) }
            .maxByOrNull { it.key.length }
            ?.value
    }
    
    // ==================== Stats ====================
    
    fun stat(path: String): FileInfo? {
        val node = resolvePath(path) ?: return null
        return FileInfo(
            name = node.name,
            path = node.getPath(),
            isDirectory = node is DirectoryNode,
            size = if (node is FileNode) node.size else 0,
            permissions = node.permissions,
            created = node.created,
            modified = node.modified
        )
    }
    
    fun spaceTotal(): Long = 1024 * 1024 * 1024 // 1GB virtual space
    
    fun spaceUsed(): Long {
        fun countSize(node: FSNode): Long = when (node) {
            is FileNode -> node.size
            is DirectoryNode -> node.listNodes().sumOf { countSize(it) }
        }
        return countSize(root)
    }
    
    fun spaceFree(): Long = spaceTotal() - spaceUsed()
}

data class FileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: Int,
    val created: Long,
    val modified: Long
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")
    
    val icon: String
        get() = when {
            isDirectory -> "📁"
            extension in listOf("lua", "kt", "java", "py", "js") -> "📜"
            extension in listOf("txt", "md", "cfg", "conf") -> "📄"
            extension in listOf("png", "jpg", "pic") -> "🖼"
            extension in listOf("wav", "mp3", "ogg") -> "🎵"
            else -> "📄"
        }
}

class FileHandle(
    val id: Int,
    val path: String,
    private val file: FileNode,
    private val mode: String
) {
    private var position: Long = if ("a" in mode) file.size else 0
    private var buffer = file.read()
    private var modified = false
    
    fun read(count: Int): ByteArray? {
        if ("r" !in mode && "+" !in mode) return null
        
        val available = buffer.size - position.toInt()
        if (available <= 0) return ByteArray(0)
        
        val toRead = if (count < 0) available else minOf(count, available)
        val result = buffer.copyOfRange(position.toInt(), position.toInt() + toRead)
        position += toRead
        return result
    }
    
    fun write(data: ByteArray): Boolean {
        if ("w" !in mode && "a" !in mode && "+" !in mode) return false
        
        val before = buffer.copyOfRange(0, position.toInt())
        val after = if (position.toInt() + data.size < buffer.size) {
            buffer.copyOfRange(position.toInt() + data.size, buffer.size)
        } else {
            ByteArray(0)
        }
        
        buffer = before + data + after
        position += data.size
        modified = true
        return true
    }
    
    fun seek(whence: String, offset: Int): Long {
        position = when (whence) {
            "set" -> offset.toLong()
            "cur" -> position + offset
            "end" -> buffer.size + offset
            else -> position
        }.coerceIn(0, buffer.size.toLong())
        return position
    }
    
    fun close() {
        if (modified) {
            file.write(buffer)
        }
    }
}

/**
 * Filesystem persistence - Save/Load to NBT or JSON format.
 * This allows the filesystem to persist across computer restarts.
 */
object FileSystemPersistence {
    
    /**
     * Serialize the entire filesystem to a JSON string.
     */
    fun serialize(fs: VirtualFileSystem): String {
        val sb = StringBuilder()
        sb.append("{\"version\":1,\"files\":{")
        
        val files = mutableListOf<String>()
        serializeDirectory(fs, "/", files)
        
        sb.append(files.joinToString(","))
        sb.append("}}")
        return sb.toString()
    }
    
    private fun serializeDirectory(fs: VirtualFileSystem, path: String, files: MutableList<String>) {
        val entries = fs.list(path) ?: return
        
        for (entry in entries) {
            val fullPath = if (path == "/") "/$entry" else "$path/$entry"
            
            if (fs.isDirectory(fullPath)) {
                // Add directory marker
                files.add("\"${escapeJson(fullPath)}\":{\"type\":\"dir\"}")
                serializeDirectory(fs, fullPath, files)
            } else {
                // Add file with content (base64 encoded for binary safety)
                val content = fs.readFile(fullPath) ?: ByteArray(0)
                val base64 = java.util.Base64.getEncoder().encodeToString(content)
                val stat = fs.stat(fullPath)
                files.add("\"${escapeJson(fullPath)}\":{\"type\":\"file\",\"data\":\"$base64\",\"modified\":${stat?.modified ?: 0}}")
            }
        }
    }
    
    /**
     * Deserialize JSON back to filesystem.
     */
    fun deserialize(fs: VirtualFileSystem, json: String) {
        try {
            // Simple JSON parser for our format
            val filesStart = json.indexOf("\"files\":{") + 9
            val filesEnd = json.lastIndexOf("}}")
            if (filesStart < 9 || filesEnd < 0) return
            
            val filesJson = json.substring(filesStart, filesEnd)
            
            // Parse each entry
            var pos = 0
            while (pos < filesJson.length) {
                // Find path
                val pathStart = filesJson.indexOf('"', pos)
                if (pathStart < 0) break
                val pathEnd = filesJson.indexOf('"', pathStart + 1)
                if (pathEnd < 0) break
                
                val path = unescapeJson(filesJson.substring(pathStart + 1, pathEnd))
                
                // Find type
                val typeStart = filesJson.indexOf("\"type\":\"", pathEnd) + 8
                val typeEnd = filesJson.indexOf('"', typeStart)
                val type = filesJson.substring(typeStart, typeEnd)
                
                if (type == "dir") {
                    fs.mkdir(path)
                    pos = filesJson.indexOf('}', typeEnd) + 1
                } else {
                    // Extract data
                    val dataStart = filesJson.indexOf("\"data\":\"", typeEnd) + 8
                    val dataEnd = filesJson.indexOf('"', dataStart)
                    val base64 = filesJson.substring(dataStart, dataEnd)
                    val content = java.util.Base64.getDecoder().decode(base64)
                    
                    fs.writeFile(path, content)
                    pos = filesJson.indexOf('}', dataEnd) + 1
                }
                
                // Skip comma
                if (pos < filesJson.length && filesJson[pos] == ',') pos++
            }
        } catch (e: Exception) {
            // Failed to deserialize - start fresh
            println("Failed to deserialize filesystem: ${e.message}")
        }
    }
    
    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
    }
    
    private fun unescapeJson(s: String): String {
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
    }
}

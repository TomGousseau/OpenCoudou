package li.cil.oc.client.os.libs

/**
 * Path manipulation utilities for SkibidiOS2.
 * Compatible with SkibidiLuaOS Paths.lua
 */
object Paths {
    
    /**
     * Normalize a path (resolve . and .., remove trailing slash).
     */
    fun normalize(path: String): String {
        val parts = path.split("/").filter { it.isNotEmpty() && it != "." }
        val result = mutableListOf<String>()
        
        for (part in parts) {
            when (part) {
                ".." -> if (result.isNotEmpty() && result.last() != "..") result.removeLast()
                        else if (!path.startsWith("/")) result.add("..")
                else -> result.add(part)
            }
        }
        
        val normalized = result.joinToString("/")
        return if (path.startsWith("/")) "/$normalized" else normalized.ifEmpty { "." }
    }
    
    /**
     * Join multiple path segments.
     */
    fun join(vararg parts: String): String {
        val combined = parts.filter { it.isNotEmpty() }.joinToString("/")
        return normalize(combined)
    }
    
    /**
     * Get parent directory of a path.
     */
    fun parent(path: String): String {
        val normalized = normalize(path)
        val lastSlash = normalized.lastIndexOf('/')
        return when {
            lastSlash < 0 -> "."
            lastSlash == 0 -> "/"
            else -> normalized.substring(0, lastSlash)
        }
    }
    
    /**
     * Get filename from path (last component).
     */
    fun name(path: String): String {
        val normalized = normalize(path)
        val lastSlash = normalized.lastIndexOf('/')
        return if (lastSlash < 0) normalized else normalized.substring(lastSlash + 1)
    }
    
    /**
     * Get filename without extension.
     */
    fun baseName(path: String): String {
        val fileName = name(path)
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
    }
    
    /**
     * Get file extension (without dot).
     */
    fun extension(path: String): String {
        val fileName = name(path)
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) fileName.substring(dotIndex + 1) else ""
    }
    
    /**
     * Check if path is absolute.
     */
    fun isAbsolute(path: String): Boolean {
        return path.startsWith("/")
    }
    
    /**
     * Convert relative path to absolute using base.
     */
    fun toAbsolute(path: String, base: String = "/"): String {
        return if (isAbsolute(path)) normalize(path)
               else normalize(join(base, path))
    }
    
    /**
     * Convert absolute path to relative from base.
     */
    fun toRelative(path: String, base: String): String {
        val absPath = normalize(path).removePrefix("/").split("/")
        val absBase = normalize(base).removePrefix("/").split("/")
        
        var common = 0
        while (common < minOf(absPath.size, absBase.size) && 
               absPath[common] == absBase[common]) {
            common++
        }
        
        val ups = (absBase.size - common)
        val result = mutableListOf<String>()
        repeat(ups) { result.add("..") }
        result.addAll(absPath.subList(common, absPath.size))
        
        return result.joinToString("/").ifEmpty { "." }
    }
    
    /**
     * Split path into components.
     */
    fun split(path: String): List<String> {
        return normalize(path).removePrefix("/").split("/").filter { it.isNotEmpty() }
    }
    
    /**
     * Check if child path is inside parent path.
     */
    fun isInside(parent: String, child: String): Boolean {
        val normalParent = normalize(parent)
        val normalChild = normalize(child)
        return normalChild.startsWith(normalParent) && 
               (normalChild.length == normalParent.length || 
                normalChild[normalParent.length] == '/')
    }
    
    /**
     * Check if path matches a glob pattern.
     * Supports * (any chars) and ? (single char).
     */
    fun matches(path: String, pattern: String): Boolean {
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return Regex(regex).matches(path)
    }
    
    /**
     * Get the depth of a path (number of components).
     */
    fun depth(path: String): Int {
        return split(path).size
    }
    
    /**
     * Check if path has given extension.
     */
    fun hasExtension(path: String, ext: String): Boolean {
        return extension(path).equals(ext, ignoreCase = true)
    }
    
    /**
     * Change extension of a path.
     */
    fun changeExtension(path: String, newExt: String): String {
        val parentDir = parent(path)
        val base = baseName(path)
        val ext = if (newExt.startsWith(".")) newExt else ".$newExt"
        return join(parentDir, base + ext)
    }
    
    /**
     * Remove extension from path.
     */
    fun removeExtension(path: String): String {
        val parentDir = parent(path)
        val base = baseName(path)
        return join(parentDir, base)
    }
    
    /**
     * Check if path is hidden (starts with dot).
     */
    fun isHidden(path: String): Boolean {
        return name(path).startsWith(".")
    }
    
    /**
     * Common path operations for file types.
     */
    object Extensions {
        val TEXT = setOf("txt", "md", "log", "cfg", "ini", "conf", "json", "xml", "yaml", "yml")
        val CODE = setOf("lua", "kt", "java", "py", "js", "ts", "c", "cpp", "h", "rs")
        val IMAGE = setOf("png", "jpg", "jpeg", "gif", "bmp", "ico", "pic")
        val ARCHIVE = setOf("zip", "tar", "gz", "rar", "7z")
        val EXECUTABLE = setOf("lua", "sh", "bat", "exe")
        
        fun isText(path: String): Boolean = TEXT.contains(extension(path).lowercase())
        fun isCode(path: String): Boolean = CODE.contains(extension(path).lowercase())
        fun isImage(path: String): Boolean = IMAGE.contains(extension(path).lowercase())
        fun isArchive(path: String): Boolean = ARCHIVE.contains(extension(path).lowercase())
        fun isExecutable(path: String): Boolean = EXECUTABLE.contains(extension(path).lowercase())
    }
}

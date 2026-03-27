package li.cil.oc.client.os.libs

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.*

/**
 * Compression/decompression library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Compressor.lua
 */
object Compressor {
    
    /**
     * Compress data using DEFLATE algorithm.
     */
    fun compress(data: ByteArray, level: Int = Deflater.DEFAULT_COMPRESSION): ByteArray {
        val deflater = Deflater(level)
        deflater.setInput(data)
        deflater.finish()
        
        val output = ByteArrayOutputStream(data.size)
        val buffer = ByteArray(1024)
        
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            output.write(buffer, 0, count)
        }
        
        deflater.end()
        return output.toByteArray()
    }
    
    fun compress(data: String, level: Int = Deflater.DEFAULT_COMPRESSION): ByteArray {
        return compress(data.toByteArray(Charsets.UTF_8), level)
    }
    
    /**
     * Decompress DEFLATE-compressed data.
     */
    fun decompress(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        
        val output = ByteArrayOutputStream(data.size * 2)
        val buffer = ByteArray(1024)
        
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            output.write(buffer, 0, count)
        }
        
        inflater.end()
        return output.toByteArray()
    }
    
    fun decompressString(data: ByteArray): String {
        return String(decompress(data), Charsets.UTF_8)
    }
    
    /**
     * Compress using GZIP format.
     */
    fun gzipCompress(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(data) }
        return output.toByteArray()
    }
    
    fun gzipCompress(data: String): ByteArray {
        return gzipCompress(data.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * Decompress GZIP data.
     */
    fun gzipDecompress(data: ByteArray): ByteArray {
        val input = ByteArrayInputStream(data)
        val output = ByteArrayOutputStream()
        GZIPInputStream(input).use { gzip ->
            val buffer = ByteArray(1024)
            var len: Int
            while (gzip.read(buffer).also { len = it } != -1) {
                output.write(buffer, 0, len)
            }
        }
        return output.toByteArray()
    }
    
    fun gzipDecompressString(data: ByteArray): String {
        return String(gzipDecompress(data), Charsets.UTF_8)
    }
    
    /**
     * Compress using ZIP format (single file).
     */
    fun zipCompress(data: ByteArray, filename: String = "data"): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            val entry = ZipEntry(filename)
            zip.putNextEntry(entry)
            zip.write(data)
            zip.closeEntry()
        }
        return output.toByteArray()
    }
    
    /**
     * Create a ZIP archive from multiple files.
     */
    fun zipCreate(files: Map<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            for ((name, content) in files) {
                val entry = ZipEntry(name)
                zip.putNextEntry(entry)
                zip.write(content)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
    
    /**
     * Extract files from ZIP archive.
     */
    fun zipExtract(data: ByteArray): Map<String, ByteArray> {
        val files = mutableMapOf<String, ByteArray>()
        val input = ByteArrayInputStream(data)
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    files[entry.name] = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
        return files
    }
    
    /**
     * List files in ZIP archive.
     */
    fun zipList(data: ByteArray): List<ZipFileInfo> {
        val files = mutableListOf<ZipFileInfo>()
        val input = ByteArrayInputStream(data)
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                files.add(ZipFileInfo(
                    name = entry.name,
                    size = entry.size,
                    compressedSize = entry.compressedSize,
                    isDirectory = entry.isDirectory,
                    time = entry.time
                ))
                entry = zip.nextEntry
            }
        }
        return files
    }
    
    data class ZipFileInfo(
        val name: String,
        val size: Long,
        val compressedSize: Long,
        val isDirectory: Boolean,
        val time: Long
    )
    
    /**
     * Compression levels.
     */
    object Level {
        const val NONE = Deflater.NO_COMPRESSION
        const val FASTEST = Deflater.BEST_SPEED
        const val DEFAULT = Deflater.DEFAULT_COMPRESSION
        const val BEST = Deflater.BEST_COMPRESSION
    }
}

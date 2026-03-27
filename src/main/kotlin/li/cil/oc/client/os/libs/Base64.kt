package li.cil.oc.client.os.libs

import java.util.Base64 as JavaBase64

/**
 * Base64 encoding/decoding library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Base64.lua
 */
object Base64 {
    private val encoder = JavaBase64.getEncoder()
    private val decoder = JavaBase64.getDecoder()
    private val urlEncoder = JavaBase64.getUrlEncoder()
    private val urlDecoder = JavaBase64.getUrlDecoder()
    private val mimeEncoder = JavaBase64.getMimeEncoder()
    private val mimeDecoder = JavaBase64.getMimeDecoder()
    
    /**
     * Encode a string to Base64.
     */
    fun encode(data: String): String {
        return encoder.encodeToString(data.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * Encode bytes to Base64.
     */
    fun encode(data: ByteArray): String {
        return encoder.encodeToString(data)
    }
    
    /**
     * Decode a Base64 string to string.
     */
    fun decode(data: String): String {
        return String(decoder.decode(data), Charsets.UTF_8)
    }
    
    /**
     * Decode a Base64 string to bytes.
     */
    fun decodeBytes(data: String): ByteArray {
        return decoder.decode(data)
    }
    
    /**
     * URL-safe Base64 encoding.
     */
    fun encodeUrl(data: String): String {
        return urlEncoder.encodeToString(data.toByteArray(Charsets.UTF_8))
    }
    
    fun encodeUrl(data: ByteArray): String {
        return urlEncoder.encodeToString(data)
    }
    
    /**
     * URL-safe Base64 decoding.
     */
    fun decodeUrl(data: String): String {
        return String(urlDecoder.decode(data), Charsets.UTF_8)
    }
    
    fun decodeUrlBytes(data: String): ByteArray {
        return urlDecoder.decode(data)
    }
    
    /**
     * MIME Base64 encoding (with line breaks).
     */
    fun encodeMime(data: String): String {
        return mimeEncoder.encodeToString(data.toByteArray(Charsets.UTF_8))
    }
    
    fun encodeMime(data: ByteArray): String {
        return mimeEncoder.encodeToString(data)
    }
    
    /**
     * MIME Base64 decoding.
     */
    fun decodeMime(data: String): String {
        return String(mimeDecoder.decode(data), Charsets.UTF_8)
    }
    
    /**
     * Check if a string is valid Base64.
     */
    fun isValid(data: String): Boolean {
        return try {
            decoder.decode(data)
            true
        } catch (e: Exception) {
            false
        }
    }
}

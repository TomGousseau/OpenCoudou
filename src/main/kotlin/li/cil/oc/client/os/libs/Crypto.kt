package li.cil.oc.client.os.libs

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * SHA-256 and other cryptographic hash functions for SkibidiOS2.
 * Compatible with SkibidiLuaOS SHA-256.lua
 */
object SHA256 {
    
    /**
     * Compute SHA-256 hash of a string.
     */
    fun hash(data: String): String {
        return hash(data.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * Compute SHA-256 hash of bytes.
     */
    fun hash(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Compute SHA-256 hash and return raw bytes.
     */
    fun hashBytes(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }
    
    /**
     * Compute HMAC-SHA256.
     */
    fun hmac(key: String, data: String): String {
        return hmac(key.toByteArray(Charsets.UTF_8), data.toByteArray(Charsets.UTF_8))
    }
    
    fun hmac(key: ByteArray, data: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val result = mac.doFinal(data)
        return result.joinToString("") { "%02x".format(it) }
    }
    
    fun hmacBytes(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}

/**
 * MD5 hash function (for legacy compatibility).
 */
object MD5 {
    fun hash(data: String): String {
        return hash(data.toByteArray(Charsets.UTF_8))
    }
    
    fun hash(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * SHA-1 hash function. 
 */
object SHA1 {
    fun hash(data: String): String {
        return hash(data.toByteArray(Charsets.UTF_8))
    }
    
    fun hash(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * SHA-512 hash function.
 */
object SHA512 {
    fun hash(data: String): String {
        return hash(data.toByteArray(Charsets.UTF_8))
    }
    
    fun hash(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    fun hmac(key: String, data: String): String {
        return hmac(key.toByteArray(Charsets.UTF_8), data.toByteArray(Charsets.UTF_8))
    }
    
    fun hmac(key: ByteArray, data: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(key, "HmacSHA512"))
        val result = mac.doFinal(data)
        return result.joinToString("") { "%02x".format(it) }
    }
}

/**
 * CRC32 checksum.
 */
object CRC32 {
    fun checksum(data: String): Long {
        return checksum(data.toByteArray(Charsets.UTF_8))
    }
    
    fun checksum(data: ByteArray): Long {
        val crc = java.util.zip.CRC32()
        crc.update(data)
        return crc.value
    }
    
    fun checksumHex(data: ByteArray): String {
        return "%08x".format(checksum(data))
    }
}

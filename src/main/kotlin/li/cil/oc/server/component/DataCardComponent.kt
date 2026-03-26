package li.cil.oc.server.component

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor
import kotlin.math.*

/**
 * Data card component — provides cryptographic hashing, encoding/decoding,
 * and compression utilities for Lua programs.
 *
 * Tier 1: CRC32, deflate/inflate, base64 encode/decode
 * Tier 2: + MD5, SHA256, HMAC-SHA256
 * Tier 3: + AES-128/256 encryption, secure random, diffie-hellman
 */
class DataCardComponent(val tier: Int = 1) : ComponentBase("data") {

    companion object {
        private val BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        private val BASE64_DECODE = IntArray(256) { -1 }.also { arr ->
            BASE64_CHARS.forEachIndexed { i, c -> arr[c.code] = i }
            arr['='.code] = 0
        }

        /** Maximum data length to process in one call (prevents DoS) */
        const val MAX_DATA_LEN = 1_048_576 // 1 MB
    }

    override fun methods(): Map<String, Any> {
        val m = mutableMapOf<String, Any>(
            "crc32"         to ::crc32,
            "deflate"       to ::deflate,
            "inflate"       to ::inflate,
            "encode64"      to ::encode64,
            "decode64"      to ::decode64,
            "toHex"         to ::toHex,
            "fromHex"       to ::fromHex
        )
        if (tier >= 2) {
            m["md5"]        = ::md5
            m["sha256"]     = ::sha256
            m["hmac"]       = ::hmac
        }
        if (tier >= 3) {
            m["encrypt"]    = ::encrypt
            m["decrypt"]    = ::decrypt
            m["random"]     = ::random
            m["generateKeyPair"] = ::generateKeyPair
        }
        return m
    }

    // -----------------------------------------------------------------------
    // Tier 1 — Basic encoding + compression
    // -----------------------------------------------------------------------

    @Callback(doc = "function(data:string):number -- Compute CRC-32 checksum of data.")
    fun crc32(context: Context, args: Array<Any?>): Array<Any?> {
        val data = getBytes(args, 0) ?: return arrayOf(null, "data required")
        val crc  = CRC32()
        crc.update(data)
        return arrayOf(crc.value)
    }

    @Callback(doc = "function(data:string):string -- Deflate-compress data.")
    fun deflate(context: Context, args: Array<Any?>): Array<Any?> {
        val data  = getBytes(args, 0) ?: return arrayOf(null, "data required")
        val level = (args.getOrNull(1) as? Number)?.toInt()?.coerceIn(0, 9) ?: Deflater.BEST_SPEED

        val deflater = Deflater(level)
        deflater.setInput(data)
        deflater.finish()

        val buf   = ByteArray(data.size + 64)
        val len   = deflater.deflate(buf)
        deflater.end()

        return arrayOf(String(buf, 0, len, Charsets.ISO_8859_1))
    }

    @Callback(doc = "function(data:string):string -- Inflate (decompress) deflated data.")
    fun inflate(context: Context, args: Array<Any?>): Array<Any?> {
        val data     = getBytes(args, 0) ?: return arrayOf(null, "data required")
        val inflater = Inflater()
        inflater.setInput(data)

        val chunks = mutableListOf<ByteArray>()
        val buf    = ByteArray(4096)
        var total  = 0

        try {
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n == 0) break
                chunks.add(buf.copyOf(n))
                total += n
                if (total > MAX_DATA_LEN) return arrayOf(null, "decompressed data too large")
            }
        } catch (e: Exception) {
            return arrayOf(null, "inflate error: ${e.message}")
        } finally {
            inflater.end()
        }

        val result = ByteArray(total)
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return arrayOf(String(result, Charsets.ISO_8859_1))
    }

    @Callback(doc = "function(data:string):string -- Base64-encode data.")
    fun encode64(context: Context, args: Array<Any?>): Array<Any?> {
        val data = getBytes(args, 0) ?: return arrayOf(null, "data required")
        return arrayOf(java.util.Base64.getEncoder().encodeToString(data))
    }

    @Callback(doc = "function(data:string):string -- Decode base64 string.")
    fun decode64(context: Context, args: Array<Any?>): Array<Any?> {
        val str = args.getOrNull(0) as? String ?: return arrayOf(null, "string required")
        return try {
            val bytes = java.util.Base64.getDecoder().decode(str)
            arrayOf(String(bytes, Charsets.ISO_8859_1))
        } catch (e: Exception) {
            arrayOf(null, "invalid base64: ${e.message}")
        }
    }

    @Callback(doc = "function(data:string):string -- Convert binary string to lowercase hex.")
    fun toHex(context: Context, args: Array<Any?>): Array<Any?> {
        val data = getBytes(args, 0) ?: return arrayOf(null, "data required")
        return arrayOf(data.joinToString("") { "%02x".format(it) })
    }

    @Callback(doc = "function(hex:string):string -- Convert hex string to binary string.")
    fun fromHex(context: Context, args: Array<Any?>): Array<Any?> {
        val hex = args.getOrNull(0) as? String ?: return arrayOf(null, "string required")
        val clean = hex.replace("\\s".toRegex(), "")
        if (clean.length % 2 != 0) return arrayOf(null, "odd-length hex string")
        return try {
            val bytes = ByteArray(clean.length / 2) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            arrayOf(String(bytes, Charsets.ISO_8859_1))
        } catch (e: NumberFormatException) {
            arrayOf(null, "invalid hex character")
        }
    }

    // -----------------------------------------------------------------------
    // Tier 2 — Hashing
    // -----------------------------------------------------------------------

    @Callback(doc = "function(data:string):string -- Compute MD5 hash (returns binary string).")
    fun md5(context: Context, args: Array<Any?>): Array<Any?> {
        val data = getBytes(args, 0) ?: return arrayOf(null, "data required")
        val digest = MessageDigest.getInstance("MD5").digest(data)
        return arrayOf(String(digest, Charsets.ISO_8859_1))
    }

    @Callback(doc = "function(data:string):string -- Compute SHA-256 hash (binary string).")
    fun sha256(context: Context, args: Array<Any?>): Array<Any?> {
        val data = getBytes(args, 0) ?: return arrayOf(null, "data required")
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return arrayOf(String(digest, Charsets.ISO_8859_1))
    }

    @Callback(doc = "function(data:string, key:string[, algo:string]):string -- HMAC (default SHA-256).")
    fun hmac(context: Context, args: Array<Any?>): Array<Any?> {
        val data = getBytes(args, 0) ?: return arrayOf(null, "data required")
        val key  = getBytes(args, 1) ?: return arrayOf(null, "key required")
        val algo = (args.getOrNull(2) as? String)?.uppercase() ?: "SHA256"

        val javaAlgo = when (algo) {
            "SHA256", "SHA-256" -> "HmacSHA256"
            "SHA512", "SHA-512" -> "HmacSHA512"
            "MD5"               -> "HmacMD5"
            else                -> return arrayOf(null, "unsupported algorithm: $algo")
        }

        val mac     = javax.crypto.Mac.getInstance(javaAlgo)
        val keySpec = SecretKeySpec(key, javaAlgo)
        mac.init(keySpec)
        val result  = mac.doFinal(data)
        return arrayOf(String(result, Charsets.ISO_8859_1))
    }

    // -----------------------------------------------------------------------
    // Tier 3 — Encryption + random
    // -----------------------------------------------------------------------

    @Callback(doc = "function(data:string, key:string, iv:string):string -- AES-CBC encrypt.")
    fun encrypt(context: Context, args: Array<Any?>): Array<Any?> {
        val data = getBytes(args, 0) ?: return arrayOf(null, "data required")
        val key  = getBytes(args, 1) ?: return arrayOf(null, "key required")
        val iv   = getBytes(args, 2) ?: return arrayOf(null, "iv required")

        if (key.size != 16 && key.size != 32) return arrayOf(null, "key must be 16 or 32 bytes")
        if (iv.size  != 16) return arrayOf(null, "iv must be 16 bytes")

        return try {
            val keyBits = key.size * 8
            val cipher  = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val enc = cipher.doFinal(data)
            arrayOf(String(enc, Charsets.ISO_8859_1))
        } catch (e: Exception) {
            arrayOf(null, "encrypt error: ${e.message}")
        }
    }

    @Callback(doc = "function(data:string, key:string, iv:string):string -- AES-CBC decrypt.")
    fun decrypt(context: Context, args: Array<Any?>): Array<Any?> {
        val data = getBytes(args, 0) ?: return arrayOf(null, "data required")
        val key  = getBytes(args, 1) ?: return arrayOf(null, "key required")
        val iv   = getBytes(args, 2) ?: return arrayOf(null, "iv required")

        if (key.size != 16 && key.size != 32) return arrayOf(null, "key must be 16 or 32 bytes")
        if (iv.size  != 16) return arrayOf(null, "iv must be 16 bytes")

        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val dec = cipher.doFinal(data)
            arrayOf(String(dec, Charsets.ISO_8859_1))
        } catch (e: Exception) {
            arrayOf(null, "decrypt error: ${e.message}")
        }
    }

    @Callback(doc = "function(n:number):string -- Generate n cryptographically random bytes.")
    fun random(context: Context, args: Array<Any?>): Array<Any?> {
        val n = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(1, 256) ?: 16
        val bytes = ByteArray(n)
        java.security.SecureRandom().nextBytes(bytes)
        return arrayOf(String(bytes, Charsets.ISO_8859_1))
    }

    @Callback(doc = "function():string, string -- Generate a Diffie-Hellman keypair (publicKey, privateKey) as hex strings.")
    fun generateKeyPair(context: Context, args: Array<Any?>): Array<Any?> {
        // Use ECDH with P-256 curve
        return try {
            val kpg      = java.security.KeyPairGenerator.getInstance("EC")
            kpg.initialize(256)
            val pair     = kpg.generateKeyPair()
            val pubHex   = pair.public.encoded.joinToString("") { "%02x".format(it) }
            val privHex  = pair.private.encoded.joinToString("") { "%02x".format(it) }
            arrayOf(pubHex, privHex)
        } catch (e: Exception) {
            arrayOf(null, "keygen error: ${e.message}")
        }
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    private fun getBytes(args: Array<Any?>, index: Int): ByteArray? {
        val v = args.getOrNull(index) ?: return null
        return when (v) {
            is String    -> v.toByteArray(Charsets.ISO_8859_1)
            is ByteArray -> v
            else         -> null
        }
    }
}

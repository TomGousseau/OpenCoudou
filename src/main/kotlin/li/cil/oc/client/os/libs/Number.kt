package li.cil.oc.client.os.libs

import kotlin.math.*

/**
 * Number formatting and utilities for SkibidiOS2.
 * Compatible with SkibidiLuaOS Number.lua
 */
object Number {
    
    /**
     * Round to specified decimal places.
     */
    fun round(value: Double, decimals: Int = 0): Double {
        val factor = 10.0.pow(decimals)
        return kotlin.math.round(value * factor) / factor
    }
    
    /**
     * Format number with thousands separator.
     */
    fun formatWithCommas(value: Long): String {
        return "%,d".format(value)
    }
    
    fun formatWithCommas(value: Double, decimals: Int = 2): String {
        return "%,.${decimals}f".format(value)
    }
    
    /**
     * Format bytes to human-readable size.
     */
    fun formatBytes(bytes: Long, decimals: Int = 2): String {
        if (bytes == 0L) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")
        val k = 1024.0
        val i = floor(ln(bytes.toDouble()) / ln(k)).toInt()
        
        return "%.${decimals}f ${units[i]}".format(bytes / k.pow(i))
    }
    
    /**
     * Format number with SI prefix (k, M, G, etc.)
     */
    fun formatSI(value: Double, decimals: Int = 2): String {
        if (value == 0.0) return "0"
        
        val prefixes = arrayOf("", "k", "M", "G", "T", "P", "E")
        val negativePrefixes = arrayOf("", "m", "μ", "n", "p", "f", "a")
        
        return if (abs(value) >= 1) {
            val i = minOf(floor(log10(abs(value)) / 3).toInt(), prefixes.size - 1)
            "%.${decimals}f${prefixes[i]}".format(value / 10.0.pow(i * 3))
        } else {
            val i = minOf(ceil(-log10(abs(value)) / 3).toInt(), negativePrefixes.size - 1)
            "%.${decimals}f${negativePrefixes[i]}".format(value * 10.0.pow(i * 3))
        }
    }
    
    /**
     * Format percentage.
     */
    fun formatPercent(value: Double, decimals: Int = 1): String {
        return "%.${decimals}f%%".format(value * 100)
    }
    
    /**
     * Format time duration in seconds to human-readable format.
     */
    fun formatDuration(seconds: Long): String {
        if (seconds < 60) return "${seconds}s"
        if (seconds < 3600) return "${seconds / 60}m ${seconds % 60}s"
        if (seconds < 86400) return "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        return "${seconds / 86400}d ${(seconds % 86400) / 3600}h"
    }
    
    /**
     * Format time duration with full words.
     */
    fun formatDurationFull(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days day${if (days != 1L) "s" else ""}")
        if (hours > 0) parts.add("$hours hour${if (hours != 1L) "s" else ""}")
        if (minutes > 0) parts.add("$minutes minute${if (minutes != 1L) "s" else ""}")
        if (secs > 0 || parts.isEmpty()) parts.add("$secs second${if (secs != 1L) "s" else ""}")
        
        return parts.joinToString(", ")
    }
    
    /**
     * Parse string to number, return null if invalid.
     */
    fun parseOrNull(value: String): Double? {
        return value.trim().toDoubleOrNull()
    }
    
    /**
     * Parse string to integer, return null if invalid.
     */
    fun parseIntOrNull(value: String): Int? {
        return value.trim().toIntOrNull()
    }
    
    /**
     * Clamp value between min and max.
     */
    fun clamp(value: Double, min: Double, max: Double): Double {
        return value.coerceIn(min, max)
    }
    
    fun clamp(value: Int, min: Int, max: Int): Int {
        return value.coerceIn(min, max)
    }
    
    /**
     * Linear interpolation between two values.
     */
    fun lerp(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * t
    }
    
    /**
     * Inverse linear interpolation.
     */
    fun inverseLerp(a: Double, b: Double, value: Double): Double {
        return if (a == b) 0.0 else (value - a) / (b - a)
    }
    
    /**
     * Map value from one range to another.
     */
    fun map(value: Double, fromMin: Double, fromMax: Double, toMin: Double, toMax: Double): Double {
        return lerp(toMin, toMax, inverseLerp(fromMin, fromMax, value))
    }
    
    /**
     * Check if number is in range (inclusive).
     */
    fun inRange(value: Double, min: Double, max: Double): Boolean {
        return value in min..max
    }
    
    /**
     * Check if number is approximately equal to another.
     */
    fun approxEqual(a: Double, b: Double, epsilon: Double = 1e-10): Boolean {
        return abs(a - b) < epsilon
    }
    
    /**
     * Sign of number (-1, 0, or 1).
     */
    fun sign(value: Double): Int {
        return when {
            value > 0 -> 1
            value < 0 -> -1
            else -> 0
        }
    }
    
    /**
     * Check if integer is even.
     */
    fun isEven(value: Int): Boolean = value and 1 == 0
    
    /**
     * Check if integer is odd.
     */
    fun isOdd(value: Int): Boolean = value and 1 == 1
    
    /**
     * Check if integer is prime.
     */
    fun isPrime(value: Int): Boolean {
        if (value < 2) return false
        if (value == 2) return true
        if (value % 2 == 0) return false
        for (i in 3..sqrt(value.toDouble()).toInt() step 2) {
            if (value % i == 0) return false
        }
        return true
    }
    
    /**
     * Greatest common divisor.
     */
    fun gcd(a: Int, b: Int): Int {
        return if (b == 0) abs(a) else gcd(b, a % b)
    }
    
    /**
     * Least common multiple.
     */
    fun lcm(a: Int, b: Int): Int {
        return abs(a * b) / gcd(a, b)
    }
    
    /**
     * Factorial (n!).
     */
    fun factorial(n: Int): Long {
        require(n >= 0) { "Factorial not defined for negative numbers" }
        var result = 1L
        for (i in 2..n) {
            result *= i
        }
        return result
    }
    
    /**
     * Fibonacci number at index.
     */
    fun fibonacci(n: Int): Long {
        require(n >= 0) { "Fibonacci index must be non-negative" }
        if (n <= 1) return n.toLong()
        var a = 0L
        var b = 1L
        repeat(n - 1) {
            val temp = a + b
            a = b
            b = temp
        }
        return b
    }
    
    /**
     * Convert degrees to radians.
     */
    fun toRadians(degrees: Double): Double = Math.toRadians(degrees)
    
    /**
     * Convert radians to degrees.
     */
    fun toDegrees(radians: Double): Double = Math.toDegrees(radians)
    
    /**
     * Convert to hexadecimal string.
     */
    fun toHex(value: Int): String = value.toString(16).uppercase()
    
    /**
     * Convert to binary string.
     */
    fun toBinary(value: Int): String = value.toString(2)
    
    /**
     * Convert to octal string.
     */
    fun toOctal(value: Int): String = value.toString(8)
    
    /**
     * Parse hexadecimal string.
     */
    fun fromHex(hex: String): Int = hex.removePrefix("0x").removePrefix("0X").toInt(16)
    
    /**
     * Parse binary string.
     */
    fun fromBinary(binary: String): Int = binary.removePrefix("0b").removePrefix("0B").toInt(2)
}

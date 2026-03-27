package li.cil.oc.client.os.libs

/**
 * Bitwise operations library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Bit32.lua and Lua's bit32 library.
 */
object Bit32 {
    
    private const val MASK_32 = 0xFFFFFFFFL
    
    /**
     * Bitwise AND of all arguments.
     */
    fun band(vararg values: Int): Int {
        if (values.isEmpty()) return -1
        var result = values[0]
        for (i in 1 until values.size) {
            result = result and values[i]
        }
        return result
    }
    
    /**
     * Bitwise OR of all arguments.
     */
    fun bor(vararg values: Int): Int {
        if (values.isEmpty()) return 0
        var result = values[0]
        for (i in 1 until values.size) {
            result = result or values[i]
        }
        return result
    }
    
    /**
     * Bitwise XOR of all arguments.
     */
    fun bxor(vararg values: Int): Int {
        if (values.isEmpty()) return 0
        var result = values[0]
        for (i in 1 until values.size) {
            result = result xor values[i]
        }
        return result
    }
    
    /**
     * Bitwise NOT (one's complement).
     */
    fun bnot(value: Int): Int {
        return value.inv()
    }
    
    /**
     * Test if specific bits are set.
     */
    fun btest(vararg values: Int): Boolean {
        return band(*values) != 0
    }
    
    /**
     * Left shift.
     */
    fun lshift(value: Int, shift: Int): Int {
        if (shift < 0) return rshift(value, -shift)
        if (shift >= 32) return 0
        return ((value.toLong() and MASK_32) shl shift).toInt()
    }
    
    /**
     * Logical right shift (fills with zeros).
     */
    fun rshift(value: Int, shift: Int): Int {
        if (shift < 0) return lshift(value, -shift)
        if (shift >= 32) return 0
        return ((value.toLong() and MASK_32) shr shift).toInt()
    }
    
    /**
     * Arithmetic right shift (preserves sign).
     */
    fun arshift(value: Int, shift: Int): Int {
        if (shift < 0) return lshift(value, -shift)
        if (shift >= 32) return if (value < 0) -1 else 0
        return value shr shift
    }
    
    /**
     * Left rotate.
     */
    fun lrotate(value: Int, shift: Int): Int {
        val s = shift and 31
        if (s == 0) return value
        return (value shl s) or (value ushr (32 - s))
    }
    
    /**
     * Right rotate.
     */
    fun rrotate(value: Int, shift: Int): Int {
        val s = shift and 31
        if (s == 0) return value
        return (value ushr s) or (value shl (32 - s))
    }
    
    /**
     * Extract a field of bits.
     * @param field Starting bit (0-31)
     * @param width Width of field (default 1)
     */
    fun extract(value: Int, field: Int, width: Int = 1): Int {
        require(field in 0..31) { "field must be 0-31" }
        require(width >= 1) { "width must be at least 1" }
        require(field + width <= 32) { "field + width must not exceed 32" }
        
        val mask = (1 shl width) - 1
        return (value ushr field) and mask
    }
    
    /**
     * Replace a field of bits.
     */
    fun replace(value: Int, replacement: Int, field: Int, width: Int = 1): Int {
        require(field in 0..31) { "field must be 0-31" }
        require(width >= 1) { "width must be at least 1" }
        require(field + width <= 32) { "field + width must not exceed 32" }
        
        val mask = (1 shl width) - 1
        val clearMask = (mask shl field).inv()
        return (value and clearMask) or ((replacement and mask) shl field)
    }
    
    /**
     * Count number of set bits (population count).
     */
    fun popcount(value: Int): Int {
        return Integer.bitCount(value)
    }
    
    /**
     * Count leading zeros.
     */
    fun clz(value: Int): Int {
        return Integer.numberOfLeadingZeros(value)
    }
    
    /**
     * Count trailing zeros.
     */
    fun ctz(value: Int): Int {
        return Integer.numberOfTrailingZeros(value)
    }
    
    /**
     * Get the highest set bit position (0-31), or -1 if value is 0.
     */
    fun highestBit(value: Int): Int {
        if (value == 0) return -1
        return 31 - Integer.numberOfLeadingZeros(value)
    }
    
    /**
     * Get the lowest set bit position (0-31), or -1 if value is 0.
     */
    fun lowestBit(value: Int): Int {
        if (value == 0) return -1
        return Integer.numberOfTrailingZeros(value)
    }
    
    /**
     * Check if a specific bit is set.
     */
    fun isSet(value: Int, bit: Int): Boolean {
        require(bit in 0..31) { "bit must be 0-31" }
        return (value and (1 shl bit)) != 0
    }
    
    /**
     * Set a specific bit.
     */
    fun setBit(value: Int, bit: Int): Int {
        require(bit in 0..31) { "bit must be 0-31" }
        return value or (1 shl bit)
    }
    
    /**
     * Clear a specific bit.
     */
    fun clearBit(value: Int, bit: Int): Int {
        require(bit in 0..31) { "bit must be 0-31" }
        return value and (1 shl bit).inv()
    }
    
    /**
     * Toggle a specific bit.
     */
    fun toggleBit(value: Int, bit: Int): Int {
        require(bit in 0..31) { "bit must be 0-31" }
        return value xor (1 shl bit)
    }
    
    /**
     * Reverse the bits.
     */
    fun reverse(value: Int): Int {
        return Integer.reverse(value)
    }
    
    /**
     * Reverse the bytes.
     */
    fun byteSwap(value: Int): Int {
        return Integer.reverseBytes(value)
    }
    
    /**
     * Convert to unsigned long.
     */
    fun toUnsigned(value: Int): Long {
        return value.toLong() and MASK_32
    }
    
    /**
     * Create a mask with n lowest bits set.
     */
    fun mask(n: Int): Int {
        require(n in 0..32) { "n must be 0-32" }
        return if (n == 32) -1 else (1 shl n) - 1
    }
    
    /**
     * Format as binary string.
     */
    fun toBinary(value: Int, width: Int = 32): String {
        return value.toUInt().toString(2).padStart(width, '0')
    }
    
    /**
     * Format as hexadecimal string.
     */
    fun toHex(value: Int, width: Int = 8): String {
        return value.toUInt().toString(16).padStart(width, '0').uppercase()
    }
    
    /**
     * Parse binary string.
     */
    fun fromBinary(binary: String): Int {
        return binary.replace("_", "").toInt(2)
    }
    
    /**
     * Parse hexadecimal string.
     */
    fun fromHex(hex: String): Int {
        return hex.removePrefix("0x").removePrefix("0X").replace("_", "").toInt(16)
    }
}

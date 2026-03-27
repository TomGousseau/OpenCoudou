package li.cil.oc.client.os.libs

import kotlin.math.*

/**
 * Color manipulation library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Color.lua
 */
object Color {
    
    // Standard OC colors (0-15 palette)
    val WHITE = 0xFFFFFF
    val ORANGE = 0xFFCC33
    val MAGENTA = 0xCC66CC
    val LIGHT_BLUE = 0x6699FF
    val YELLOW = 0xFFFF33
    val LIME = 0x33FF33
    val PINK = 0xFF6699
    val GRAY = 0x666666
    val LIGHT_GRAY = 0x999999
    val CYAN = 0x33CCCC
    val PURPLE = 0x9933CC
    val BLUE = 0x3366CC
    val BROWN = 0x663300
    val GREEN = 0x336600
    val RED = 0xFF3333
    val BLACK = 0x000000
    
    // Named color palette
    private val namedColors = mapOf(
        "white" to WHITE,
        "orange" to ORANGE,
        "magenta" to MAGENTA,
        "lightblue" to LIGHT_BLUE,
        "yellow" to YELLOW,
        "lime" to LIME,
        "pink" to PINK,
        "gray" to GRAY,
        "grey" to GRAY,
        "lightgray" to LIGHT_GRAY,
        "lightgrey" to LIGHT_GRAY,
        "cyan" to CYAN,
        "purple" to PURPLE,
        "blue" to BLUE,
        "brown" to BROWN,
        "green" to GREEN,
        "red" to RED,
        "black" to BLACK
    )
    
    /**
     * Get RGB components from packed color.
     */
    fun toRGB(color: Int): Triple<Int, Int, Int> {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return Triple(r, g, b)
    }
    
    /**
     * Create packed color from RGB components.
     */
    fun fromRGB(r: Int, g: Int, b: Int): Int {
        return ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    }
    
    /**
     * Create color from RGB floats (0.0-1.0).
     */
    fun fromRGBf(r: Float, g: Float, b: Float): Int {
        return fromRGB(
            (r.coerceIn(0f, 1f) * 255).toInt(),
            (g.coerceIn(0f, 1f) * 255).toInt(),
            (b.coerceIn(0f, 1f) * 255).toInt()
        )
    }
    
    /**
     * Convert to HSV (Hue, Saturation, Value).
     */
    fun toHSV(color: Int): Triple<Float, Float, Float> {
        val (r, g, b) = toRGB(color)
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min
        
        val h = when {
            delta == 0f -> 0f
            max == rf -> ((gf - bf) / delta) % 6
            max == gf -> (bf - rf) / delta + 2
            else -> (rf - gf) / delta + 4
        } * 60
        
        val s = if (max == 0f) 0f else delta / max
        val v = max
        
        return Triple(if (h < 0) h + 360 else h, s, v)
    }
    
    /**
     * Create color from HSV.
     */
    fun fromHSV(h: Float, s: Float, v: Float): Int {
        val c = v * s
        val x = c * (1 - abs((h / 60) % 2 - 1))
        val m = v - c
        
        val (rf, gf, bf) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        
        return fromRGB(
            ((rf + m) * 255).toInt(),
            ((gf + m) * 255).toInt(),
            ((bf + m) * 255).toInt()
        )
    }
    
    /**
     * Convert to HSL (Hue, Saturation, Lightness).
     */
    fun toHSL(color: Int): Triple<Float, Float, Float> {
        val (r, g, b) = toRGB(color)
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val l = (max + min) / 2
        
        if (max == min) {
            return Triple(0f, 0f, l)
        }
        
        val delta = max - min
        val s = if (l > 0.5f) delta / (2 - max - min) else delta / (max + min)
        
        val h = when {
            max == rf -> ((gf - bf) / delta + (if (gf < bf) 6 else 0))
            max == gf -> (bf - rf) / delta + 2
            else -> (rf - gf) / delta + 4
        } * 60
        
        return Triple(h, s, l)
    }
    
    /**
     * Create color from HSL.
     */
    fun fromHSL(h: Float, s: Float, l: Float): Int {
        if (s == 0f) {
            val gray = (l * 255).toInt()
            return fromRGB(gray, gray, gray)
        }
        
        val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
        val p = 2 * l - q
        
        fun hue2rgb(p: Float, q: Float, t: Float): Float {
            var tt = t
            if (tt < 0) tt += 1f
            if (tt > 1) tt -= 1f
            return when {
                tt < 1f/6 -> p + (q - p) * 6 * tt
                tt < 1f/2 -> q
                tt < 2f/3 -> p + (q - p) * (2f/3 - tt) * 6
                else -> p
            }
        }
        
        val r = hue2rgb(p, q, h/360 + 1f/3)
        val g = hue2rgb(p, q, h/360)
        val b = hue2rgb(p, q, h/360 - 1f/3)
        
        return fromRGB((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }
    
    /**
     * Mix two colors.
     */
    fun mix(color1: Int, color2: Int, ratio: Float = 0.5f): Int {
        val (r1, g1, b1) = toRGB(color1)
        val (r2, g2, b2) = toRGB(color2)
        val t = ratio.coerceIn(0f, 1f)
        
        return fromRGB(
            (r1 + (r2 - r1) * t).toInt(),
            (g1 + (g2 - g1) * t).toInt(),
            (b1 + (b2 - b1) * t).toInt()
        )
    }
    
    /**
     * Lighten a color.
     */
    fun lighten(color: Int, amount: Float): Int {
        return mix(color, WHITE, amount)
    }
    
    /**
     * Darken a color.
     */
    fun darken(color: Int, amount: Float): Int {
        return mix(color, BLACK, amount)
    }
    
    /**
     * Get complementary color.
     */
    fun complement(color: Int): Int {
        val (h, s, v) = toHSV(color)
        return fromHSV((h + 180) % 360, s, v)
    }
    
    /**
     * Adjust brightness.
     */
    fun brightness(color: Int, factor: Float): Int {
        val (r, g, b) = toRGB(color)
        return fromRGB(
            (r * factor).toInt().coerceIn(0, 255),
            (g * factor).toInt().coerceIn(0, 255),
            (b * factor).toInt().coerceIn(0, 255)
        )
    }
    
    /**
     * Adjust saturation.
     */
    fun saturate(color: Int, amount: Float): Int {
        val (h, s, v) = toHSV(color)
        return fromHSV(h, (s + amount).coerceIn(0f, 1f), v)
    }
    
    /**
     * Desaturate (make more gray).
     */
    fun desaturate(color: Int, amount: Float): Int {
        return saturate(color, -amount)
    }
    
    /**
     * Invert color.
     */
    fun invert(color: Int): Int {
        val (r, g, b) = toRGB(color)
        return fromRGB(255 - r, 255 - g, 255 - b)
    }
    
    /**
     * Convert to grayscale.
     */
    fun grayscale(color: Int): Int {
        val (r, g, b) = toRGB(color)
        val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        return fromRGB(gray, gray, gray)
    }
    
    /**
     * Get luminance (perceived brightness 0-1).
     */
    fun luminance(color: Int): Float {
        val (r, g, b) = toRGB(color)
        return (0.299f * r + 0.587f * g + 0.114f * b) / 255f
    }
    
    /**
     * Check if color is dark (for text contrast).
     */
    fun isDark(color: Int): Boolean = luminance(color) < 0.5f
    
    /**
     * Check if color is light.
     */
    fun isLight(color: Int): Boolean = luminance(color) >= 0.5f
    
    /**
     * Get contrasting text color (black or white).
     */
    fun contrastText(backgroundColor: Int): Int {
        return if (isDark(backgroundColor)) WHITE else BLACK
    }
    
    /**
     * Calculate contrast ratio between two colors.
     */
    fun contrastRatio(color1: Int, color2: Int): Float {
        val l1 = luminance(color1) + 0.05f
        val l2 = luminance(color2) + 0.05f
        return maxOf(l1, l2) / minOf(l1, l2)
    }
    
    /**
     * Format as hex string.
     */
    fun toHex(color: Int): String {
        return "#%06X".format(color and 0xFFFFFF)
    }
    
    /**
     * Parse hex string.
     */
    fun fromHex(hex: String): Int {
        val clean = hex.removePrefix("#").removePrefix("0x")
        return clean.toInt(16)
    }
    
    /**
     * Get color by name.
     */
    fun byName(name: String): Int? {
        return namedColors[name.lowercase()]
    }
    
    /**
     * Convert OC palette index (0-15) to RGB.
     */
    fun fromPalette(index: Int): Int {
        val palette = arrayOf(
            WHITE, ORANGE, MAGENTA, LIGHT_BLUE,
            YELLOW, LIME, PINK, GRAY,
            LIGHT_GRAY, CYAN, PURPLE, BLUE,
            BROWN, GREEN, RED, BLACK
        )
        return palette[index.coerceIn(0, 15)]
    }
    
    /**
     * Find closest palette color.
     */
    fun toPalette(color: Int): Int {
        val (r, g, b) = toRGB(color)
        var closest = 0
        var minDist = Int.MAX_VALUE
        
        for (i in 0..15) {
            val (pr, pg, pb) = toRGB(fromPalette(i))
            val dist = (r - pr) * (r - pr) + (g - pg) * (g - pg) + (b - pb) * (b - pb)
            if (dist < minDist) {
                minDist = dist
                closest = i
            }
        }
        
        return closest
    }
}

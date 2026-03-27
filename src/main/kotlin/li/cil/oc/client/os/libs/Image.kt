package li.cil.oc.client.os.libs

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Image library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Image.lua.
 * Handles OpenComputers .pic format and pixel manipulation.
 */
object Image {
    
    /**
     * Pixel data: foreground color, background color, and character.
     */
    data class Pixel(
        var foreground: Int = 0xFFFFFF,
        var background: Int = 0x000000,
        var char: Char = ' ',
        var alpha: Float = 1.0f
    ) {
        fun copy() = Pixel(foreground, background, char, alpha)
    }
    
    /**
     * Image data class.
     */
    class ImageData(
        val width: Int,
        val height: Int
    ) {
        private val pixels: Array<Array<Pixel>> = Array(height) { Array(width) { Pixel() } }
        
        /**
         * Get pixel at coordinates.
         */
        fun get(x: Int, y: Int): Pixel? {
            if (x < 0 || x >= width || y < 0 || y >= height) return null
            return pixels[y][x]
        }
        
        /**
         * Set pixel at coordinates.
         */
        fun set(x: Int, y: Int, pixel: Pixel) {
            if (x < 0 || x >= width || y < 0 || y >= height) return
            pixels[y][x] = pixel.copy()
        }
        
        /**
         * Fill entire image with a color/char.
         */
        fun fill(foreground: Int = 0xFFFFFF, background: Int = 0x000000, char: Char = ' ') {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y][x].foreground = foreground
                    pixels[y][x].background = background
                    pixels[y][x].char = char
                }
            }
        }
        
        /**
         * Fill a rectangle.
         */
        fun fillRect(
            x1: Int, y1: Int, x2: Int, y2: Int,
            foreground: Int = 0xFFFFFF, background: Int = 0x000000, char: Char = ' '
        ) {
            for (y in maxOf(0, y1)..minOf(height - 1, y2)) {
                for (x in maxOf(0, x1)..minOf(width - 1, x2)) {
                    pixels[y][x].foreground = foreground
                    pixels[y][x].background = background
                    pixels[y][x].char = char
                }
            }
        }
        
        /**
         * Draw a line.
         */
        fun drawLine(
            x1: Int, y1: Int, x2: Int, y2: Int,
            foreground: Int = 0xFFFFFF, background: Int = 0x000000, char: Char = '#'
        ) {
            val dx = kotlin.math.abs(x2 - x1)
            val dy = kotlin.math.abs(y2 - y1)
            val sx = if (x1 < x2) 1 else -1
            val sy = if (y1 < y2) 1 else -1
            var err = dx - dy
            
            var x = x1
            var y = y1
            
            while (true) {
                if (x in 0 until width && y in 0 until height) {
                    pixels[y][x].foreground = foreground
                    pixels[y][x].background = background
                    pixels[y][x].char = char
                }
                
                if (x == x2 && y == y2) break
                
                val e2 = 2 * err
                if (e2 > -dy) {
                    err -= dy
                    x += sx
                }
                if (e2 < dx) {
                    err += dx
                    y += sy
                }
            }
        }
        
        /**
         * Draw text.
         */
        fun drawText(
            x: Int, y: Int, text: String,
            foreground: Int = 0xFFFFFF, background: Int? = null
        ) {
            if (y < 0 || y >= height) return
            
            for ((i, char) in text.withIndex()) {
                val px = x + i
                if (px >= 0 && px < width) {
                    pixels[y][px].foreground = foreground
                    if (background != null) {
                        pixels[y][px].background = background
                    }
                    pixels[y][px].char = char
                }
            }
        }
        
        /**
         * Copy a region from another image.
         */
        fun blit(
            source: ImageData,
            destX: Int, destY: Int,
            srcX: Int = 0, srcY: Int = 0,
            copyWidth: Int = source.width, copyHeight: Int = source.height
        ) {
            for (sy in 0 until copyHeight) {
                for (sx in 0 until copyWidth) {
                    val sourcePixel = source.get(srcX + sx, srcY + sy) ?: continue
                    val dx = destX + sx
                    val dy = destY + sy
                    if (dx in 0 until width && dy in 0 until height) {
                        pixels[dy][dx] = sourcePixel.copy()
                    }
                }
            }
        }
        
        /**
         * Create a copy of this image.
         */
        fun copy(): ImageData {
            val newImage = ImageData(width, height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    newImage.pixels[y][x] = pixels[y][x].copy()
                }
            }
            return newImage
        }
        
        /**
         * Extract a sub-region.
         */
        fun subImage(x: Int, y: Int, w: Int, h: Int): ImageData {
            val sub = ImageData(w, h)
            for (sy in 0 until h) {
                for (sx in 0 until w) {
                    get(x + sx, y + sy)?.let { sub.set(sx, sy, it) }
                }
            }
            return sub
        }
        
        /**
         * Scale the image.
         */
        fun scale(newWidth: Int, newHeight: Int): ImageData {
            val scaled = ImageData(newWidth, newHeight)
            val xRatio = width.toFloat() / newWidth
            val yRatio = height.toFloat() / newHeight
            
            for (y in 0 until newHeight) {
                for (x in 0 until newWidth) {
                    val srcX = (x * xRatio).toInt()
                    val srcY = (y * yRatio).toInt()
                    get(srcX, srcY)?.let { scaled.set(x, y, it) }
                }
            }
            return scaled
        }
        
        /**
         * Flip horizontally.
         */
        fun flipH(): ImageData {
            val flipped = ImageData(width, height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    get(width - 1 - x, y)?.let { flipped.set(x, y, it) }
                }
            }
            return flipped
        }
        
        /**
         * Flip vertically.
         */
        fun flipV(): ImageData {
            val flipped = ImageData(width, height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    get(x, height - 1 - y)?.let { flipped.set(x, y, it) }
                }
            }
            return flipped
        }
        
        /**
         * Apply a color transformation to all pixels.
         */
        fun transform(block: (Pixel) -> Pixel) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y][x] = block(pixels[y][x])
                }
            }
        }
    }
    
    // OC .pic format constants
    private const val OCIF_SIGNATURE = "OCIF"
    private const val OCIF_VERSION: Byte = 6
    
    /**
     * Create a new empty image.
     */
    fun create(width: Int, height: Int): ImageData {
        return ImageData(width, height)
    }
    
    /**
     * Load image from OC .pic format bytes.
     */
    fun load(data: ByteArray): ImageData? {
        return try {
            val input = DataInputStream(ByteArrayInputStream(data))
            
            // Read signature
            val sig = ByteArray(4)
            input.readFully(sig)
            if (String(sig) != OCIF_SIGNATURE) {
                return null
            }
            
            // Read version
            val version = input.readByte()
            if (version > OCIF_VERSION) {
                return null
            }
            
            // Read dimensions
            val width = input.readUnsignedByte()
            val height = input.readUnsignedByte()
            
            // Read compressed data
            val compressedSize = input.readInt()
            val compressed = ByteArray(compressedSize)
            input.readFully(compressed)
            
            // Decompress
            val inflater = Inflater()
            inflater.setInput(compressed)
            val decompressed = ByteArray(width * height * 8) // Estimate
            val decompressedSize = inflater.inflate(decompressed)
            inflater.end()
            
            // Parse pixel data
            val image = ImageData(width, height)
            val pixelInput = DataInputStream(ByteArrayInputStream(decompressed, 0, decompressedSize))
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val fg = pixelInput.readInt() and 0xFFFFFF
                    val bg = pixelInput.readInt() and 0xFFFFFF
                    val charCode = pixelInput.readUnsignedShort()
                    
                    image.set(x, y, Pixel(fg, bg, charCode.toChar()))
                }
            }
            
            image
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Save image to OC .pic format bytes.
     */
    fun save(image: ImageData): ByteArray {
        // Prepare pixel data
        val pixelOutput = ByteArrayOutputStream()
        val pixelData = DataOutputStream(pixelOutput)
        
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.get(x, y) ?: Pixel()
                pixelData.writeInt(pixel.foreground)
                pixelData.writeInt(pixel.background)
                pixelData.writeShort(pixel.char.code)
            }
        }
        
        val uncompressed = pixelOutput.toByteArray()
        
        // Compress
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(uncompressed)
        deflater.finish()
        
        val compressedBuffer = ByteArray(uncompressed.size + 100)
        val compressedSize = deflater.deflate(compressedBuffer)
        deflater.end()
        
        // Write file
        val output = ByteArrayOutputStream()
        val dataOutput = DataOutputStream(output)
        
        dataOutput.writeBytes(OCIF_SIGNATURE)
        dataOutput.writeByte(OCIF_VERSION.toInt())
        dataOutput.writeByte(image.width)
        dataOutput.writeByte(image.height)
        dataOutput.writeInt(compressedSize)
        dataOutput.write(compressedBuffer, 0, compressedSize)
        
        return output.toByteArray()
    }
    
    /**
     * Convert image to ANSI escape codes for terminal rendering.
     */
    fun toAnsi(image: ImageData): String {
        val sb = StringBuilder()
        var lastFg = -1
        var lastBg = -1
        
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.get(x, y) ?: Pixel()
                
                // Set colors if changed
                if (pixel.foreground != lastFg) {
                    sb.append("\u001B[38;2;${(pixel.foreground shr 16) and 0xFF};${(pixel.foreground shr 8) and 0xFF};${pixel.foreground and 0xFF}m")
                    lastFg = pixel.foreground
                }
                if (pixel.background != lastBg) {
                    sb.append("\u001B[48;2;${(pixel.background shr 16) and 0xFF};${(pixel.background shr 8) and 0xFF};${pixel.background and 0xFF}m")
                    lastBg = pixel.background
                }
                
                sb.append(pixel.char)
            }
            sb.append("\u001B[0m\n")
            lastFg = -1
            lastBg = -1
        }
        
        return sb.toString()
    }
    
    /**
     * Parse simple PPM (P3/P6) image format.
     */
    fun loadPPM(data: ByteArray): ImageData? {
        return try {
            val text = String(data)
            val lines = text.lines().filter { !it.startsWith("#") && it.isNotBlank() }
            
            val header = lines[0].trim()
            if (header != "P3") return null // Only ASCII PPM supported
            
            val dimensions = lines[1].trim().split(Regex("\\s+"))
            val width = dimensions[0].toInt()
            val height = dimensions[1].toInt()
            val maxVal = lines[2].trim().toInt()
            
            val pixels = lines.drop(3).joinToString(" ").trim()
                .split(Regex("\\s+")).map { (it.toInt() * 255 / maxVal) }
            
            val image = ImageData(width, height)
            var i = 0
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val r = pixels[i++]
                    val g = pixels[i++]
                    val b = pixels[i++]
                    val color = (r shl 16) or (g shl 8) or b
                    
                    // Use block character with color
                    image.set(x, y, Pixel(color, color, '█'))
                }
            }
            
            image
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Color quantization - reduce colors in image.
     */
    fun quantize(image: ImageData, maxColors: Int): ImageData {
        // Collect all unique colors
        val colors = mutableSetOf<Int>()
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                image.get(x, y)?.let {
                    colors.add(it.foreground)
                    colors.add(it.background)
                }
            }
        }
        
        // Simple quantization by reducing color depth
        val shiftBits = maxOf(0, 8 - (kotlin.math.log2(maxColors.toDouble()) / 3).toInt())
        
        val result = image.copy()
        result.transform { pixel ->
            Pixel(
                quantizeColor(pixel.foreground, shiftBits),
                quantizeColor(pixel.background, shiftBits),
                pixel.char,
                pixel.alpha
            )
        }
        
        return result
    }
    
    private fun quantizeColor(color: Int, shiftBits: Int): Int {
        val r = ((color shr 16) and 0xFF) shr shiftBits shl shiftBits
        val g = ((color shr 8) and 0xFF) shr shiftBits shl shiftBits
        val b = (color and 0xFF) shr shiftBits shl shiftBits
        return (r shl 16) or (g shl 8) or b
    }
    
    /**
     * Apply grayscale filter.
     */
    fun grayscale(image: ImageData): ImageData {
        val result = image.copy()
        result.transform { pixel ->
            val fgGray = Color.grayscale(pixel.foreground)
            val bgGray = Color.grayscale(pixel.background)
            Pixel(fgGray, bgGray, pixel.char, pixel.alpha)
        }
        return result
    }
    
    /**
     * Invert colors.
     */
    fun invert(image: ImageData): ImageData {
        val result = image.copy()
        result.transform { pixel ->
            Pixel(
                0xFFFFFF - pixel.foreground,
                0xFFFFFF - pixel.background,
                pixel.char,
                pixel.alpha
            )
        }
        return result
    }
}

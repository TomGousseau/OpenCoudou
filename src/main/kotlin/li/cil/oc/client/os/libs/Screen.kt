package li.cil.oc.client.os.libs

/**
 * Screen/GPU library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Screen.lua.
 * Provides double-buffered rendering and GPU abstraction.
 */
object Screen {
    
    // Screen dimensions
    var width: Int = 160
        private set
    var height: Int = 50
        private set
    
    // Color depth
    var depth: Int = 8 // 8 = 256 colors (tier 3)
        private set
    
    // Current colors
    private var currentBackground: Int = 0x000000
    private var currentForeground: Int = 0xFFFFFF
    
    // Double buffer
    private var currentFrame: ScreenBuffer = ScreenBuffer(width, height)
    private var newFrame: ScreenBuffer = ScreenBuffer(width, height)
    
    // Draw limits (clipping)
    private var drawLimitX1: Int = 1
    private var drawLimitY1: Int = 1
    private var drawLimitX2: Int = width
    private var drawLimitY2: Int = height
    
    // GPU address
    private var gpuAddress: String? = null
    
    /**
     * Pixel data for buffering.
     */
    data class Pixel(
        var background: Int = 0x000000,
        var foreground: Int = 0xFFFFFF,
        var char: Char = ' '
    )
    
    /**
     * Screen buffer.
     */
    class ScreenBuffer(val width: Int, val height: Int) {
        val backgrounds = IntArray(width * height) { 0x000000 }
        val foregrounds = IntArray(width * height) { 0xFFFFFF }
        val chars = CharArray(width * height) { ' ' }
        
        fun getIndex(x: Int, y: Int): Int = (y - 1) * width + (x - 1)
        
        fun get(x: Int, y: Int): Pixel? {
            if (x < 1 || x > width || y < 1 || y > height) return null
            val i = getIndex(x, y)
            return Pixel(backgrounds[i], foregrounds[i], chars[i])
        }
        
        fun set(x: Int, y: Int, bg: Int, fg: Int, char: Char) {
            if (x < 1 || x > width || y < 1 || y > height) return
            val i = getIndex(x, y)
            backgrounds[i] = bg
            foregrounds[i] = fg
            chars[i] = char
        }
        
        fun clear() {
            backgrounds.fill(0x000000)
            foregrounds.fill(0xFFFFFF)
            chars.fill(' ')
        }
        
        fun copyFrom(other: ScreenBuffer) {
            other.backgrounds.copyInto(backgrounds)
            other.foregrounds.copyInto(foregrounds)
            other.chars.copyInto(chars)
        }
    }
    
    // ==================== Initialization ====================
    
    /**
     * Initialize screen with dimensions.
     */
    fun flush(newWidth: Int? = null, newHeight: Int? = null) {
        if (newWidth != null && newHeight != null) {
            width = newWidth
            height = newHeight
        }
        
        currentFrame = ScreenBuffer(width, height)
        newFrame = ScreenBuffer(width, height)
        resetDrawLimit()
    }
    
    /**
     * Bind to a GPU.
     */
    fun bind(address: String, reset: Boolean = true): Boolean {
        gpuAddress = address
        if (reset) {
            flush()
        }
        return true
    }
    
    /**
     * Get current resolution.
     */
    fun getResolution(): Pair<Int, Int> = width to height
    
    /**
     * Set resolution.
     */
    fun setResolution(w: Int, h: Int): Boolean {
        width = w
        height = h
        flush(w, h)
        return true
    }
    
    /**
     * Get max resolution.
     */
    fun maxResolution(): Pair<Int, Int> = 160 to 50 // Tier 3
    
    /**
     * Get color depth.
     */
    fun getDepth(): Int = depth
    
    /**
     * Set color depth.
     */
    fun setDepth(d: Int): Boolean {
        if (d !in listOf(1, 4, 8)) return false
        depth = d
        return true
    }
    
    // ==================== Drawing Limits ====================
    
    /**
     * Set draw limits (clipping region).
     */
    fun setDrawLimit(x1: Int, y1: Int, x2: Int, y2: Int) {
        drawLimitX1 = x1
        drawLimitY1 = y1
        drawLimitX2 = x2
        drawLimitY2 = y2
    }
    
    /**
     * Reset draw limits to full screen.
     */
    fun resetDrawLimit() {
        drawLimitX1 = 1
        drawLimitY1 = 1
        drawLimitX2 = width
        drawLimitY2 = height
    }
    
    /**
     * Get current draw limits.
     */
    fun getDrawLimit(): List<Int> = listOf(drawLimitX1, drawLimitY1, drawLimitX2, drawLimitY2)
    
    // ==================== Colors ====================
    
    /**
     * Set background color.
     */
    fun setBackground(color: Int): Int {
        val old = currentBackground
        currentBackground = color
        return old
    }
    
    /**
     * Get background color.
     */
    fun getBackground(): Int = currentBackground
    
    /**
     * Set foreground color.
     */
    fun setForeground(color: Int): Int {
        val old = currentForeground
        currentForeground = color
        return old
    }
    
    /**
     * Get foreground color.
     */
    fun getForeground(): Int = currentForeground
    
    // ==================== Drawing ====================
    
    /**
     * Set a character at position.
     */
    fun set(x: Int, y: Int, text: String, vertical: Boolean = false) {
        if (vertical) {
            for ((i, char) in text.withIndex()) {
                val py = y + i
                if (py in drawLimitY1..drawLimitY2 && x in drawLimitX1..drawLimitX2) {
                    newFrame.set(x, py, currentBackground, currentForeground, char)
                }
            }
        } else {
            for ((i, char) in text.withIndex()) {
                val px = x + i
                if (px in drawLimitX1..drawLimitX2 && y in drawLimitY1..drawLimitY2) {
                    newFrame.set(px, y, currentBackground, currentForeground, char)
                }
            }
        }
    }
    
    /**
     * Get character and colors at position.
     */
    fun get(x: Int, y: Int): Triple<String, Int, Int>? {
        val pixel = newFrame.get(x, y) ?: return null
        return Triple(pixel.char.toString(), pixel.foreground, pixel.background)
    }
    
    /**
     * Fill a rectangle.
     */
    fun fill(x: Int, y: Int, w: Int, h: Int, char: Char) {
        val c = char
        for (py in y until y + h) {
            for (px in x until x + w) {
                if (px in drawLimitX1..drawLimitX2 && py in drawLimitY1..drawLimitY2) {
                    newFrame.set(px, py, currentBackground, currentForeground, c)
                }
            }
        }
    }
    
    /**
     * Copy a region.
     */
    fun copy(x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) {
        // Copy to temp buffer first to handle overlapping regions
        val temp = Array(h) { dy ->
            Array(w) { dx ->
                newFrame.get(x + dx, y + dy)
            }
        }
        
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                val pixel = temp[dy][dx] ?: continue
                val px = x + tx + dx
                val py = y + ty + dy
                if (px in drawLimitX1..drawLimitX2 && py in drawLimitY1..drawLimitY2) {
                    newFrame.set(px, py, pixel.background, pixel.foreground, pixel.char)
                }
            }
        }
    }
    
    // ==================== High-Level Drawing ====================
    
    /**
     * Draw a filled rectangle.
     */
    fun drawRectangle(x: Int, y: Int, w: Int, h: Int, background: Int, foreground: Int = currentForeground, char: Char = ' ') {
        val oldBg = setBackground(background)
        val oldFg = setForeground(foreground)
        fill(x, y, w, h, char)
        setBackground(oldBg)
        setForeground(oldFg)
    }
    
    /**
     * Draw text.
     */
    fun drawText(x: Int, y: Int, foreground: Int, text: String) {
        val oldFg = setForeground(foreground)
        set(x, y, text)
        setForeground(oldFg)
    }
    
    /**
     * Draw centered text.
     */
    fun drawCenteredText(y: Int, foreground: Int, text: String) {
        val x = (width - text.length) / 2 + 1
        drawText(x, y, foreground, text)
    }
    
    /**
     * Draw a frame/border.
     */
    fun drawFrame(x: Int, y: Int, w: Int, h: Int, single: Boolean = true) {
        val chars = if (single) {
            listOf('┌', '┐', '└', '┘', '─', '│')
        } else {
            listOf('╔', '╗', '╚', '╝', '═', '║')
        }
        
        set(x, y, chars[0].toString())
        set(x + w - 1, y, chars[1].toString())
        set(x, y + h - 1, chars[2].toString())
        set(x + w - 1, y + h - 1, chars[3].toString())
        
        for (i in 1 until w - 1) {
            set(x + i, y, chars[4].toString())
            set(x + i, y + h - 1, chars[4].toString())
        }
        
        for (i in 1 until h - 1) {
            set(x, y + i, chars[5].toString())
            set(x + w - 1, y + i, chars[5].toString())
        }
    }
    
    // ==================== Buffer Management ====================
    
    /**
     * Update screen - copy new frame to current and render.
     * Returns list of changes for optimized GPU calls.
     */
    fun update(force: Boolean = false): List<DrawCommand> {
        val commands = mutableListOf<DrawCommand>()
        
        for (y in 1..height) {
            var x = 1
            while (x <= width) {
                val oldPixel = currentFrame.get(x, y)
                val newPixel = newFrame.get(x, y)
                
                if (force || oldPixel != newPixel) {
                    // Batch consecutive characters with same colors
                    val bg = newPixel?.background ?: 0
                    val fg = newPixel?.foreground ?: 0xFFFFFF
                    val sb = StringBuilder()
                    sb.append(newPixel?.char ?: ' ')
                    
                    var endX = x + 1
                    while (endX <= width) {
                        val nextOld = currentFrame.get(endX, y)
                        val nextNew = newFrame.get(endX, y)
                        if ((force || nextOld != nextNew) && 
                            nextNew?.background == bg && nextNew.foreground == fg) {
                            sb.append(nextNew.char)
                            endX++
                        } else {
                            break
                        }
                    }
                    
                    commands.add(DrawCommand(x, y, bg, fg, sb.toString()))
                    x = endX
                } else {
                    x++
                }
            }
        }
        
        // Swap buffers
        currentFrame.copyFrom(newFrame)
        
        return commands
    }
    
    /**
     * Clear the new frame buffer.
     */
    fun clear() {
        newFrame.clear()
    }
    
    /**
     * Get raw buffer access for advanced operations.
     */
    fun getCurrentFrame(): ScreenBuffer = currentFrame
    fun getNewFrame(): ScreenBuffer = newFrame
    
    /**
     * Draw command for GPU.
     */
    data class DrawCommand(
        val x: Int,
        val y: Int,
        val background: Int,
        val foreground: Int,
        val text: String
    )
}

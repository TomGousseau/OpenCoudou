package li.cil.oc.server.component

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * GPU Component - handles text rendering and graphics operations.
 * Supports multiple tiers with different capabilities:
 * - Tier 1: 50x16, 1-bit color
 * - Tier 2: 80x25, 4-bit color  
 * - Tier 3: 160x50, 8-bit color
 */
class GPUComponent(private val tier: Int = 1) : ComponentBase("gpu") {
    
    private var boundScreen: ScreenBuffer? = null
    private var screenAddress: String? = null
    
    // Current drawing state
    private var foreground = 0xFFFFFF
    private var background = 0x000000
    private var foregroundPalette = -1
    private var backgroundPalette = -1
    
    // Tier-based limits
    val maxWidth: Int get() = when (tier) {
        1 -> 50
        2 -> 80
        else -> 160
    }
    
    val maxHeight: Int get() = when (tier) {
        1 -> 16
        2 -> 25
        else -> 50
    }
    
    val maxColorDepth: ColorDepth get() = when (tier) {
        1 -> ColorDepth.OneBit
        2 -> ColorDepth.FourBit
        else -> ColorDepth.EightBit
    }
    
    fun bind(screen: ScreenBuffer, address: String): Boolean {
        // Check if screen tier is compatible
        if (screen.tier > tier) return false
        boundScreen = screen
        screenAddress = address
        return true
    }
    
    fun unbind() {
        boundScreen = null
        screenAddress = null
    }
    
    fun getScreen(): ScreenBuffer? = boundScreen
    
    @Callback(doc = "function(address: string): boolean -- Binds the GPU to a screen")
    fun bind(context: Context, args: Array<Any?>): Array<Any?> {
        val address = args.getOrNull(0) as? String ?: return arrayOf(null, "invalid address")
        
        // In real implementation, would look up screen by address
        // For now, return success if already bound
        return if (screenAddress == address) {
            arrayOf(true)
        } else {
            arrayOf(null, "no such screen")
        }
    }
    
    @Callback(doc = "function(): string -- Gets the address of the bound screen")
    fun getScreen(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(screenAddress)
    }
    
    @Callback(doc = "function(): number, number -- Gets the current resolution")
    fun getResolution(context: Context, args: Array<Any?>): Array<Any?> {
        val screen = boundScreen ?: return arrayOf(null, "no screen")
        return arrayOf(screen.width, screen.height)
    }
    
    @Callback(doc = "function(width: number, height: number): boolean -- Sets the resolution")
    fun setResolution(context: Context, args: Array<Any?>): Array<Any?> {
        val screen = boundScreen ?: return arrayOf(null, "no screen")
        val width = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid width")
        val height = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(null, "invalid height")
        
        if (width < 1 || width > maxWidth || height < 1 || height > maxHeight) {
            return arrayOf(null, "unsupported resolution")
        }
        
        screen.setResolution(width, height)
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): number, number -- Gets the maximum resolution")
    fun maxResolution(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(maxWidth, maxHeight)
    }
    
    @Callback(doc = "function(): number -- Gets the current color depth")
    fun getDepth(context: Context, args: Array<Any?>): Array<Any?> {
        val screen = boundScreen ?: return arrayOf(null, "no screen")
        return arrayOf(screen.colorDepth.bits)
    }
    
    @Callback(doc = "function(depth: number): boolean -- Sets the color depth")
    fun setDepth(context: Context, args: Array<Any?>): Array<Any?> {
        val screen = boundScreen ?: return arrayOf(null, "no screen")
        val depth = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid depth")
        
        val colorDepth = ColorDepth.fromBits(depth)
        if (colorDepth == null || colorDepth.bits > maxColorDepth.bits) {
            return arrayOf(null, "unsupported depth")
        }
        
        screen.colorDepth = colorDepth
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): number -- Gets the maximum color depth")
    fun maxDepth(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(maxColorDepth.bits)
    }
    
    @Callback(doc = "function(): number, boolean -- Gets the foreground color")
    fun getForeground(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(foreground, foregroundPalette >= 0)
    }
    
    @Callback(doc = "function(color: number[, isPalette: boolean]): number -- Sets the foreground color")
    fun setForeground(context: Context, args: Array<Any?>): Array<Any?> {
        val color = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid color")
        val isPalette = args.getOrNull(1) as? Boolean ?: false
        
        val oldFg = foreground
        if (isPalette) {
            foregroundPalette = color
            foreground = boundScreen?.getPaletteColor(color) ?: color
        } else {
            foregroundPalette = -1
            foreground = color and 0xFFFFFF
        }
        
        return arrayOf(oldFg)
    }
    
    @Callback(doc = "function(): number, boolean -- Gets the background color")
    fun getBackground(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(background, backgroundPalette >= 0)
    }
    
    @Callback(doc = "function(color: number[, isPalette: boolean]): number -- Sets the background color")
    fun setBackground(context: Context, args: Array<Any?>): Array<Any?> {
        val color = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid color")
        val isPalette = args.getOrNull(1) as? Boolean ?: false
        
        val oldBg = background
        if (isPalette) {
            backgroundPalette = color
            background = boundScreen?.getPaletteColor(color) ?: color
        } else {
            backgroundPalette = -1
            background = color and 0xFFFFFF
        }
        
        return arrayOf(oldBg)
    }
    
    @Callback(doc = "function(index: number): number -- Gets a palette color")
    fun getPaletteColor(context: Context, args: Array<Any?>): Array<Any?> {
        val screen = boundScreen ?: return arrayOf(null, "no screen")
        val index = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid index")
        return arrayOf(screen.getPaletteColor(index))
    }
    
    @Callback(doc = "function(index: number, color: number): number -- Sets a palette color")
    fun setPaletteColor(context: Context, args: Array<Any?>): Array<Any?> {
        val screen = boundScreen ?: return arrayOf(null, "no screen")
        val index = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid index")
        val color = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(null, "invalid color")
        
        val old = screen.getPaletteColor(index)
        screen.setPaletteColor(index, color)
        return arrayOf(old)
    }
    
    @Callback(doc = "function(x: number, y: number): string, number, number -- Gets character at position")
    fun get(context: Context, args: Array<Any?>): Array<Any?> {
        val screen = boundScreen ?: return arrayOf(null, "no screen")
        val x = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid x")
        val y = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(null, "invalid y")
        
        if (x < 1 || x > screen.width || y < 1 || y > screen.height) {
            return arrayOf(null, "index out of bounds")
        }
        
        val cell = screen.get(x - 1, y - 1)
        return arrayOf(cell.char.toString(), cell.foreground, cell.background)
    }
    
    @Callback(doc = "function(x: number, y: number, value: string[, vertical: boolean]): boolean -- Sets characters")
    fun set(context: Context, args: Array<Any?>): Array<Any?> {
        val screen = boundScreen ?: return arrayOf(null, "no screen")
        val x = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid x")
        val y = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(null, "invalid y")
        val value = args.getOrNull(2) as? String ?: return arrayOf(null, "invalid value")
        val vertical = args.getOrNull(3) as? Boolean ?: false
        
        screen.set(x - 1, y - 1, value, foreground, background, vertical)
        return arrayOf(true)
    }
    
    @Callback(doc = "function(x: number, y: number, width: number, height: number, char: string): boolean -- Fills area")
    fun fill(context: Context, args: Array<Any?>): Array<Any?> {
        val screen = boundScreen ?: return arrayOf(null, "no screen")
        val x = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid x")
        val y = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(null, "invalid y")
        val width = (args.getOrNull(2) as? Number)?.toInt() ?: return arrayOf(null, "invalid width")
        val height = (args.getOrNull(3) as? Number)?.toInt() ?: return arrayOf(null, "invalid height")
        val char = (args.getOrNull(4) as? String)?.firstOrNull() ?: ' '
        
        screen.fill(x - 1, y - 1, width, height, char, foreground, background)
        return arrayOf(true)
    }
    
    @Callback(doc = "function(x: number, y: number, width: number, height: number, tx: number, ty: number): boolean -- Copies area")
    fun copy(context: Context, args: Array<Any?>): Array<Any?> {
        val screen = boundScreen ?: return arrayOf(null, "no screen")
        val x = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid x")
        val y = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(null, "invalid y")
        val width = (args.getOrNull(2) as? Number)?.toInt() ?: return arrayOf(null, "invalid width")
        val height = (args.getOrNull(3) as? Number)?.toInt() ?: return arrayOf(null, "invalid height")
        val tx = (args.getOrNull(4) as? Number)?.toInt() ?: return arrayOf(null, "invalid tx")
        val ty = (args.getOrNull(5) as? Number)?.toInt() ?: return arrayOf(null, "invalid ty")
        
        screen.copy(x - 1, y - 1, width, height, tx, ty)
        return arrayOf(true)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putInt("tier", tier)
        tag.putInt("fg", foreground)
        tag.putInt("bg", background)
        tag.putInt("fgPal", foregroundPalette)
        tag.putInt("bgPal", backgroundPalette)
        screenAddress?.let { tag.putString("screen", it) }
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        foreground = tag.getInt("fg")
        background = tag.getInt("bg")
        foregroundPalette = tag.getInt("fgPal")
        backgroundPalette = tag.getInt("bgPal")
        screenAddress = if (tag.contains("screen")) tag.getString("screen") else null
    }
}

/**
 * Screen buffer - stores the actual screen contents.
 */
class ScreenBuffer(val tier: Int = 1) {
    
    var width: Int = when (tier) {
        1 -> 50
        2 -> 80
        else -> 160
    }
        private set
        
    var height: Int = when (tier) {
        1 -> 16
        2 -> 25
        else -> 50
    }
        private set
    
    var colorDepth: ColorDepth = when (tier) {
        1 -> ColorDepth.OneBit
        2 -> ColorDepth.FourBit
        else -> ColorDepth.EightBit
    }
    
    private var buffer = Array(height) { Array(width) { Cell() } }
    private val palette = IntArray(16) { DEFAULT_PALETTE[it] }
    
    // Dirty tracking for efficient rendering
    private var dirty = true
    private val dirtyRows = BitSet(height)
    
    data class Cell(
        var char: Char = ' ',
        var foreground: Int = 0xFFFFFF,
        var background: Int = 0x000000
    )
    
    fun setResolution(newWidth: Int, newHeight: Int) {
        if (newWidth == width && newHeight == height) return
        
        val oldBuffer = buffer
        buffer = Array(newHeight) { y ->
            Array(newWidth) { x ->
                if (y < height && x < width) {
                    oldBuffer[y][x].copy()
                } else {
                    Cell()
                }
            }
        }
        
        width = newWidth
        height = newHeight
        markAllDirty()
    }
    
    fun get(x: Int, y: Int): Cell {
        return if (x in 0 until width && y in 0 until height) {
            buffer[y][x]
        } else {
            Cell()
        }
    }
    
    fun set(x: Int, y: Int, text: String, fg: Int, bg: Int, vertical: Boolean) {
        if (vertical) {
            for ((i, char) in text.withIndex()) {
                val cy = y + i
                if (cy in 0 until height && x in 0 until width) {
                    buffer[cy][x].apply {
                        this.char = char
                        this.foreground = fg
                        this.background = bg
                    }
                    dirtyRows.set(cy)
                }
            }
        } else {
            for ((i, char) in text.withIndex()) {
                val cx = x + i
                if (y in 0 until height && cx in 0 until width) {
                    buffer[y][cx].apply {
                        this.char = char
                        this.foreground = fg
                        this.background = bg
                    }
                }
            }
            if (y in 0 until height) dirtyRows.set(y)
        }
        dirty = true
    }
    
    fun fill(x: Int, y: Int, w: Int, h: Int, char: Char, fg: Int, bg: Int) {
        val x1 = max(0, x)
        val y1 = max(0, y)
        val x2 = min(width, x + w)
        val y2 = min(height, y + h)
        
        for (cy in y1 until y2) {
            for (cx in x1 until x2) {
                buffer[cy][cx].apply {
                    this.char = char
                    this.foreground = fg
                    this.background = bg
                }
            }
            dirtyRows.set(cy)
        }
        dirty = true
    }
    
    fun copy(x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) {
        if (tx == 0 && ty == 0) return
        
        // Create temporary copy to avoid overwriting source
        val temp = Array(h) { dy ->
            Array(w) { dx ->
                val sx = x + dx
                val sy = y + dy
                if (sx in 0 until width && sy in 0 until height) {
                    buffer[sy][sx].copy()
                } else {
                    Cell()
                }
            }
        }
        
        // Write to destination
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                val destX = x + dx + tx
                val destY = y + dy + ty
                if (destX in 0 until width && destY in 0 until height) {
                    buffer[destY][destX] = temp[dy][dx]
                    dirtyRows.set(destY)
                }
            }
        }
        dirty = true
    }
    
    fun clear() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                buffer[y][x] = Cell()
            }
        }
        markAllDirty()
    }
    
    fun getPaletteColor(index: Int): Int {
        return if (index in palette.indices) palette[index] else 0
    }
    
    fun setPaletteColor(index: Int, color: Int) {
        if (index in palette.indices) {
            palette[index] = color and 0xFFFFFF
        }
    }
    
    fun isDirty(): Boolean = dirty
    
    fun clearDirty() {
        dirty = false
        dirtyRows.clear()
    }
    
    fun getDirtyRows(): BitSet = dirtyRows.clone() as BitSet
    
    private fun markAllDirty() {
        dirty = true
        dirtyRows.set(0, height)
    }
    
    fun save(tag: CompoundTag) {
        tag.putInt("width", width)
        tag.putInt("height", height)
        tag.putInt("depth", colorDepth.bits)
        tag.putIntArray("palette", palette)
        
        val rows = ListTag()
        for (y in 0 until height) {
            val row = CompoundTag()
            val chars = StringBuilder()
            val fgColors = IntArray(width)
            val bgColors = IntArray(width)
            
            for (x in 0 until width) {
                chars.append(buffer[y][x].char)
                fgColors[x] = buffer[y][x].foreground
                bgColors[x] = buffer[y][x].background
            }
            
            row.putString("text", chars.toString())
            row.putIntArray("fg", fgColors)
            row.putIntArray("bg", bgColors)
            rows.add(row)
        }
        tag.put("rows", rows)
    }
    
    fun load(tag: CompoundTag) {
        width = tag.getInt("width")
        height = tag.getInt("height")
        colorDepth = ColorDepth.fromBits(tag.getInt("depth")) ?: ColorDepth.OneBit
        
        val savedPalette = tag.getIntArray("palette")
        for (i in savedPalette.indices) {
            if (i < palette.size) palette[i] = savedPalette[i]
        }
        
        buffer = Array(height) { Array(width) { Cell() } }
        
        val rows = tag.getList("rows", Tag.TAG_COMPOUND.toInt())
        for (y in 0 until min(rows.size, height)) {
            val row = rows.getCompound(y)
            val text = row.getString("text")
            val fgColors = row.getIntArray("fg")
            val bgColors = row.getIntArray("bg")
            
            for (x in 0 until min(text.length, width)) {
                buffer[y][x].apply {
                    char = text[x]
                    foreground = if (x < fgColors.size) fgColors[x] else 0xFFFFFF
                    background = if (x < bgColors.size) bgColors[x] else 0x000000
                }
            }
        }
        
        markAllDirty()
    }
    
    companion object {
        val DEFAULT_PALETTE = intArrayOf(
            0x000000, 0x990000, 0x009900, 0x999900,
            0x000099, 0x990099, 0x009999, 0xCCCCCC,
            0x333333, 0xFF0000, 0x00FF00, 0xFFFF00,
            0x0000FF, 0xFF00FF, 0x00FFFF, 0xFFFFFF
        )
    }
}

enum class ColorDepth(val bits: Int, val colors: Int) {
    OneBit(1, 2),
    FourBit(4, 16),
    EightBit(8, 256);
    
    companion object {
        fun fromBits(bits: Int): ColorDepth? = entries.find { it.bits == bits }
    }
}

/**
 * Keyboard component - handles keyboard input events.
 */
class KeyboardComponent : ComponentBase("keyboard") {
    
    private val pressedKeys = mutableSetOf<Int>()
    
    fun keyDown(keyCode: Int, char: Char) {
        pressedKeys.add(keyCode)
        queueSignal("key_down", address, char.code, keyCode)
    }
    
    fun keyUp(keyCode: Int, char: Char) {
        pressedKeys.remove(keyCode)
        queueSignal("key_up", address, char.code, keyCode)
    }
    
    fun isKeyDown(keyCode: Int): Boolean = keyCode in pressedKeys
    
    @Callback(doc = "function(code: number): boolean -- Checks if a key is pressed")
    fun isKeyDown(context: Context, args: Array<Any?>): Array<Any?> {
        val code = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(false)
        return arrayOf(isKeyDown(code))
    }
    
    @Callback(doc = "function(): boolean -- Checks if any control key is pressed")
    fun isControl(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(isKeyDown(LWJGL_LCONTROL) || isKeyDown(LWJGL_RCONTROL))
    }
    
    @Callback(doc = "function(): boolean -- Checks if any shift key is pressed")
    fun isShift(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(isKeyDown(LWJGL_LSHIFT) || isKeyDown(LWJGL_RSHIFT))
    }
    
    @Callback(doc = "function(): boolean -- Checks if any alt key is pressed")
    fun isAlt(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(isKeyDown(LWJGL_LALT) || isKeyDown(LWJGL_RALT))
    }
    
    companion object {
        const val LWJGL_LCONTROL = 29
        const val LWJGL_RCONTROL = 157
        const val LWJGL_LSHIFT = 42
        const val LWJGL_RSHIFT = 54
        const val LWJGL_LALT = 56
        const val LWJGL_RALT = 184
    }
}

/**
 * Screen component - handles touch/click events and viewport.
 */
class ScreenComponent(val tier: Int = 1) : ComponentBase("screen") {
    
    val buffer = ScreenBuffer(tier)
    private var boundGPU: GPUComponent? = null
    private var isPrecise = false
    private var touchModeInverted = false
    
    fun bindGPU(gpu: GPUComponent) {
        boundGPU = gpu
        gpu.bind(buffer, address)
    }
    
    fun unbindGPU() {
        boundGPU?.unbind()
        boundGPU = null
    }
    
    fun touch(x: Double, y: Double, button: Int) {
        val (charX, charY) = if (isPrecise) {
            (x + 1) to (y + 1)
        } else {
            (x.toInt() + 1) to (y.toInt() + 1)
        }
        
        queueSignal(if (touchModeInverted) "drop" else "touch", address, charX, charY, button)
    }
    
    fun drag(x: Double, y: Double, button: Int) {
        val (charX, charY) = if (isPrecise) {
            (x + 1) to (y + 1)
        } else {
            (x.toInt() + 1) to (y.toInt() + 1)
        }
        
        queueSignal("drag", address, charX, charY, button)
    }
    
    fun drop(x: Double, y: Double, button: Int) {
        val (charX, charY) = if (isPrecise) {
            (x + 1) to (y + 1)
        } else {
            (x.toInt() + 1) to (y.toInt() + 1)
        }
        
        queueSignal(if (touchModeInverted) "touch" else "drop", address, charX, charY, button)
    }
    
    fun scroll(x: Double, y: Double, delta: Int) {
        queueSignal("scroll", address, x.toInt() + 1, y.toInt() + 1, delta)
    }
    
    @Callback(doc = "function(): boolean -- Returns whether the screen is on")
    fun isOn(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): boolean -- Turns the screen on")
    fun turnOn(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): boolean -- Turns the screen off")
    fun turnOff(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): number -- Returns the aspect ratio")
    fun getAspectRatio(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(1.0, 1.0)
    }
    
    @Callback(doc = "function(): table -- Returns the keyboards connected to this screen")
    fun getKeyboards(context: Context, args: Array<Any?>): Array<Any?> {
        // Would return connected keyboard addresses
        return arrayOf(emptyList<String>())
    }
    
    @Callback(doc = "function(precise: boolean): boolean -- Sets precise mode")
    fun setPrecise(context: Context, args: Array<Any?>): Array<Any?> {
        val old = isPrecise
        isPrecise = args.getOrNull(0) as? Boolean ?: false
        return arrayOf(old)
    }
    
    @Callback(doc = "function(): boolean -- Returns whether precise mode is active")
    fun isPrecise(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(isPrecise)
    }
    
    @Callback(doc = "function(inverted: boolean): boolean -- Sets touch mode inversion")
    fun setTouchModeInverted(context: Context, args: Array<Any?>): Array<Any?> {
        val old = touchModeInverted
        touchModeInverted = args.getOrNull(0) as? Boolean ?: false
        return arrayOf(old)
    }
    
    @Callback(doc = "function(): boolean -- Returns whether touch mode is inverted")
    fun isTouchModeInverted(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(touchModeInverted)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putInt("tier", tier)
        tag.putBoolean("precise", isPrecise)
        tag.putBoolean("inverted", touchModeInverted)
        
        val bufferTag = CompoundTag()
        buffer.save(bufferTag)
        tag.put("buffer", bufferTag)
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        isPrecise = tag.getBoolean("precise")
        touchModeInverted = tag.getBoolean("inverted")
        
        if (tag.contains("buffer")) {
            buffer.load(tag.getCompound("buffer"))
        }
    }
}

/**
 * Hologram component - 3D volumetric display.
 */
class HologramComponent(val tier: Int = 1) : ComponentBase("hologram") {
    
    val width = 48
    val height = 32
    val depth = 48
    
    private val voxels = Array(height) { Array(width) { IntArray(depth) } }
    private val palette = IntArray(if (tier >= 2) 3 else 1) { 0xFFFFFF }
    private var scale = 1.0f
    private var translation = floatArrayOf(0f, 0f, 0f)
    private var rotation = floatArrayOf(0f, 0f, 0f)
    private var dirty = true
    
    fun set(x: Int, y: Int, z: Int, value: Int) {
        if (x in 0 until width && y in 0 until height && z in 0 until depth) {
            voxels[y][x][z] = value
            dirty = true
        }
    }
    
    fun get(x: Int, y: Int, z: Int): Int {
        return if (x in 0 until width && y in 0 until height && z in 0 until depth) {
            voxels[y][x][z]
        } else {
            0
        }
    }
    
    fun fill(x: Int, y: Int, z: Int, w: Int, h: Int, d: Int, value: Int) {
        for (py in y until min(y + h, height)) {
            for (px in x until min(x + w, width)) {
                for (pz in z until min(z + d, depth)) {
                    if (px >= 0 && py >= 0 && pz >= 0) {
                        voxels[py][px][pz] = value
                    }
                }
            }
        }
        dirty = true
    }
    
    fun clear() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                for (z in 0 until depth) {
                    voxels[y][x][z] = 0
                }
            }
        }
        dirty = true
    }
    
    @Callback(doc = "function(x: number, y: number, z: number, value: number): void -- Sets a voxel")
    fun set(context: Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf()
        val y = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf()
        val z = (args.getOrNull(2) as? Number)?.toInt() ?: return arrayOf()
        val value = (args.getOrNull(3) as? Number)?.toInt() ?: return arrayOf()
        
        set(x - 1, y - 1, z - 1, value)
        return arrayOf()
    }
    
    @Callback(doc = "function(x: number, y: number, z: number): number -- Gets a voxel")
    fun get(context: Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        val y = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(0)
        val z = (args.getOrNull(2) as? Number)?.toInt() ?: return arrayOf(0)
        
        return arrayOf(get(x - 1, y - 1, z - 1))
    }
    
    @Callback(doc = "function(x: number, y: number, z: number, w: number, h: number, d: number, value: number): void -- Fills a region")
    fun fill(context: Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf()
        val y = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf()
        val z = (args.getOrNull(2) as? Number)?.toInt() ?: return arrayOf()
        val w = (args.getOrNull(3) as? Number)?.toInt() ?: return arrayOf()
        val h = (args.getOrNull(4) as? Number)?.toInt() ?: return arrayOf()
        val d = (args.getOrNull(5) as? Number)?.toInt() ?: return arrayOf()
        val value = (args.getOrNull(6) as? Number)?.toInt() ?: return arrayOf()
        
        fill(x - 1, y - 1, z - 1, w, h, d, value)
        return arrayOf()
    }
    
    @Callback(doc = "function(): void -- Clears the hologram")
    fun clear(context: Context, args: Array<Any?>): Array<Any?> {
        clear()
        return arrayOf()
    }
    
    @Callback(doc = "function(index: number, color: number): number -- Sets a palette color")
    fun setPaletteColor(context: Context, args: Array<Any?>): Array<Any?> {
        val index = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        val color = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(0)
        
        if (index in palette.indices) {
            val old = palette[index]
            palette[index] = color and 0xFFFFFF
            dirty = true
            return arrayOf(old)
        }
        return arrayOf(0)
    }
    
    @Callback(doc = "function(index: number): number -- Gets a palette color")
    fun getPaletteColor(context: Context, args: Array<Any?>): Array<Any?> {
        val index = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        return arrayOf(if (index in palette.indices) palette[index] else 0)
    }
    
    @Callback(doc = "function(): number, number, number -- Gets the dimensions")
    fun getDimensions(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(width, height, depth)
    }
    
    @Callback(doc = "function(scale: number): number -- Sets the scale")
    fun setScale(context: Context, args: Array<Any?>): Array<Any?> {
        val old = scale
        scale = ((args.getOrNull(0) as? Number)?.toFloat() ?: 1f).coerceIn(0.33f, 3f)
        dirty = true
        return arrayOf(old)
    }
    
    @Callback(doc = "function(): number -- Gets the scale")
    fun getScale(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(scale)
    }
    
    @Callback(doc = "function(x: number, y: number, z: number): void -- Sets translation offset")
    fun setTranslation(context: Context, args: Array<Any?>): Array<Any?> {
        translation[0] = (args.getOrNull(0) as? Number)?.toFloat() ?: 0f
        translation[1] = (args.getOrNull(1) as? Number)?.toFloat() ?: 0f
        translation[2] = (args.getOrNull(2) as? Number)?.toFloat() ?: 0f
        dirty = true
        return arrayOf()
    }
    
    @Callback(doc = "function(angle: number, x: number, y: number, z: number): void -- Sets rotation")
    fun setRotation(context: Context, args: Array<Any?>): Array<Any?> {
        val angle = (args.getOrNull(0) as? Number)?.toFloat() ?: 0f
        val x = (args.getOrNull(1) as? Number)?.toFloat() ?: 0f
        val y = (args.getOrNull(2) as? Number)?.toFloat() ?: 0f
        val z = (args.getOrNull(3) as? Number)?.toFloat() ?: 0f
        
        // Simplified - just store angle about Y axis
        rotation[1] = angle
        dirty = true
        return arrayOf()
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putInt("tier", tier)
        tag.putFloat("scale", scale)
        tag.putIntArray("palette", palette)
        
        // Compress voxel data
        val data = mutableListOf<Int>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                for (z in 0 until depth) {
                    if (voxels[y][x][z] != 0) {
                        data.add((y shl 20) or (x shl 10) or z)
                        data.add(voxels[y][x][z])
                    }
                }
            }
        }
        tag.putIntArray("voxels", data.toIntArray())
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        scale = tag.getFloat("scale")
        
        val savedPalette = tag.getIntArray("palette")
        for (i in savedPalette.indices) {
            if (i < palette.size) palette[i] = savedPalette[i]
        }
        
        clear()
        val data = tag.getIntArray("voxels")
        for (i in data.indices step 2) {
            if (i + 1 < data.size) {
                val pos = data[i]
                val value = data[i + 1]
                val y = (pos shr 20) and 0x3FF
                val x = (pos shr 10) and 0x3FF
                val z = pos and 0x3FF
                if (y < height && x < width && z < depth) {
                    voxels[y][x][z] = value
                }
            }
        }
        dirty = true
    }
}

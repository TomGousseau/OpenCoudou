package li.cil.oc.server.component

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ComponentVisibility
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.NodeBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks

/**
 * Base class for all Lua-callable components.
 */
abstract class ComponentBase(
    val componentType: String,
    visibility: ComponentVisibility = ComponentVisibility.NEIGHBORS
) : Environment {
    
    protected var _node: Node? = null
    
    override fun node(): Node? = _node
    
    fun createNode(host: Any, energy: Double = 0.0): Node {
        val builder = NodeBuilder.create()
            .withHost(host)
            .withComponent(componentType, visibility)
        
        if (energy > 0) {
            builder.withConnector(energy)
        }
        
        _node = builder.build()
        return _node!!
    }
    
    private val visibility = visibility
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    /**
     * Gets the methods available on this component.
     */
    abstract fun methods(): Map<String, (Context, Array<Any?>) -> Array<Any?>>
}

/**
 * GPU component - handles graphics rendering.
 */
class GPUComponent(
    val maxWidth: Int,
    val maxHeight: Int,
    val maxDepth: Int // Color depth (1, 4, or 8)
) : ComponentBase("gpu") {
    
    private var screenAddress: String? = null
    private var foreground = 0xFFFFFF
    private var background = 0x000000
    private var currentWidth = maxWidth.coerceAtMost(80)
    private var currentHeight = maxHeight.coerceAtMost(25)
    
    // Screen buffer
    private val buffer = Array(currentHeight) { CharArray(currentWidth) { ' ' } }
    private val fgColors = Array(currentHeight) { IntArray(currentWidth) { foreground } }
    private val bgColors = Array(currentHeight) { IntArray(currentWidth) { background } }
    
    override fun methods() = mapOf(
        "bind" to ::bind,
        "getScreen" to ::getScreen,
        "getBackground" to ::getBackground,
        "setBackground" to ::setBackground,
        "getForeground" to ::getForeground,
        "setForeground" to ::setForeground,
        "getPaletteColor" to ::getPaletteColor,
        "setPaletteColor" to ::setPaletteColor,
        "maxDepth" to ::maxDepth,
        "getDepth" to ::getDepth,
        "setDepth" to ::setDepth,
        "maxResolution" to ::maxResolution,
        "getResolution" to ::getResolution,
        "setResolution" to ::setResolution,
        "getViewport" to ::getViewport,
        "setViewport" to ::setViewport,
        "get" to ::get,
        "set" to ::set,
        "copy" to ::copy,
        "fill" to ::fill
    )
    
    @Callback(doc = "function(address:string):boolean -- Binds the GPU to the screen with the specified address.")
    fun bind(context: Context, args: Array<Any?>): Array<Any?> {
        val address = args.getOrNull(0) as? String ?: return arrayOf(null, "invalid address")
        screenAddress = address
        return arrayOf(true)
    }
    
    @Callback(doc = "function():string -- Get the address of the bound screen.")
    fun getScreen(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(screenAddress)
    }
    
    @Callback(doc = "function():number, boolean -- Get current background color.")
    fun getBackground(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(background, false)
    }
    
    @Callback(doc = "function(color:number[, isPaletteIndex:boolean]):number, boolean -- Set background color.")
    fun setBackground(context: Context, args: Array<Any?>): Array<Any?> {
        val old = background
        background = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        return arrayOf(old, false)
    }
    
    @Callback(doc = "function():number, boolean -- Get current foreground color.")
    fun getForeground(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(foreground, false)
    }
    
    @Callback(doc = "function(color:number[, isPaletteIndex:boolean]):number, boolean -- Set foreground color.")
    fun setForeground(context: Context, args: Array<Any?>): Array<Any?> {
        val old = foreground
        foreground = (args.getOrNull(0) as? Number)?.toInt() ?: 0xFFFFFF
        return arrayOf(old, false)
    }
    
    @Callback(doc = "function(index:number):number -- Get palette color.")
    fun getPaletteColor(context: Context, args: Array<Any?>): Array<Any?> {
        val index = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        return arrayOf(0xFFFFFF) // Simplified
    }
    
    @Callback(doc = "function(index:number, value:number):number -- Set palette color.")
    fun setPaletteColor(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(0xFFFFFF)
    }
    
    @Callback(doc = "function():number -- Get maximum color depth.")
    fun maxDepth(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(maxDepth)
    }
    
    @Callback(doc = "function():number -- Get current color depth.")
    fun getDepth(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(maxDepth)
    }
    
    @Callback(doc = "function(depth:number):boolean -- Set color depth.")
    fun setDepth(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function():number, number -- Get maximum resolution.")
    fun maxResolution(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(maxWidth, maxHeight)
    }
    
    @Callback(doc = "function():number, number -- Get current resolution.")
    fun getResolution(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(currentWidth, currentHeight)
    }
    
    @Callback(doc = "function(width:number, height:number):boolean -- Set resolution.")
    fun setResolution(context: Context, args: Array<Any?>): Array<Any?> {
        currentWidth = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(1, maxWidth) ?: currentWidth
        currentHeight = (args.getOrNull(1) as? Number)?.toInt()?.coerceIn(1, maxHeight) ?: currentHeight
        return arrayOf(true)
    }
    
    @Callback(doc = "function():number, number -- Get viewport.")
    fun getViewport(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(currentWidth, currentHeight)
    }
    
    @Callback(doc = "function(width:number, height:number):boolean -- Set viewport.")
    fun setViewport(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(x:number, y:number):string, number, number -- Get character at position.")
    fun get(context: Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
        val y = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        
        if (x !in 0 until currentWidth || y !in 0 until currentHeight) {
            return arrayOf(" ", foreground, background)
        }
        
        return arrayOf(buffer[y][x].toString(), fgColors[y][x], bgColors[y][x])
    }
    
    @Callback(doc = "function(x:number, y:number, value:string[, vertical:boolean]):boolean -- Set text at position.")
    fun set(context: Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
        val y = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        val value = args.getOrNull(2) as? String ?: ""
        val vertical = args.getOrNull(3) as? Boolean ?: false
        
        for ((i, char) in value.withIndex()) {
            val cx = if (vertical) x else x + i
            val cy = if (vertical) y + i else y
            
            if (cx in 0 until currentWidth && cy in 0 until currentHeight) {
                buffer[cy][cx] = char
                fgColors[cy][cx] = foreground
                bgColors[cy][cx] = background
            }
        }
        
        return arrayOf(true)
    }
    
    @Callback(doc = "function(x:number, y:number, width:number, height:number, tx:number, ty:number):boolean -- Copy region.")
    fun copy(context: Context, args: Array<Any?>): Array<Any?> {
        // Copy implementation
        return arrayOf(true)
    }
    
    @Callback(doc = "function(x:number, y:number, width:number, height:number, char:string):boolean -- Fill region.")
    fun fill(context: Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
        val y = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        val w = (args.getOrNull(2) as? Number)?.toInt() ?: 1
        val h = (args.getOrNull(3) as? Number)?.toInt() ?: 1
        val charStr = args.getOrNull(4) as? String ?: " "
        val char = charStr.firstOrNull() ?: ' '
        
        for (cy in y until (y + h).coerceAtMost(currentHeight)) {
            for (cx in x until (x + w).coerceAtMost(currentWidth)) {
                if (cx >= 0 && cy >= 0) {
                    buffer[cy][cx] = char
                    fgColors[cy][cx] = foreground
                    bgColors[cy][cx] = background
                }
            }
        }
        
        return arrayOf(true)
    }
}

/**
 * Screen component - the display buffer.
 */
class ScreenComponent(
    val tier: Int
) : ComponentBase("screen") {
    
    val maxWidth = when (tier) {
        1 -> 50
        2 -> 80
        else -> 160
    }
    
    val maxHeight = when (tier) {
        1 -> 16
        2 -> 25
        else -> 50
    }
    
    var width = maxWidth.coerceAtMost(50)
    var height = maxHeight.coerceAtMost(16)
    var isPrecise = false
    var touchModeInverted = false
    
    // Text buffer
    val textBuffer = Array(maxHeight) { CharArray(maxWidth) { ' ' } }
    val foregroundColors = Array(maxHeight) { IntArray(maxWidth) { 0xFFFFFF } }
    val backgroundColors = Array(maxHeight) { IntArray(maxWidth) { 0x000000 } }
    
    override fun methods() = mapOf(
        "isOn" to ::isOn,
        "turnOn" to ::turnOn,
        "turnOff" to ::turnOff,
        "getAspectRatio" to ::getAspectRatio,
        "getKeyboards" to ::getKeyboards,
        "setPrecise" to ::setPrecise,
        "isPrecise" to ::isPrecise,
        "setTouchModeInverted" to ::setTouchModeInverted,
        "isTouchModeInverted" to ::isTouchModeInverted
    )
    
    private var isOn = true
    
    @Callback
    fun isOn(context: Context, args: Array<Any?>): Array<Any?> = arrayOf(isOn)
    
    @Callback
    fun turnOn(context: Context, args: Array<Any?>): Array<Any?> {
        isOn = true
        return arrayOf(true)
    }
    
    @Callback
    fun turnOff(context: Context, args: Array<Any?>): Array<Any?> {
        isOn = false
        return arrayOf(true)
    }
    
    @Callback
    fun getAspectRatio(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(1, 1)
    }
    
    @Callback
    fun getKeyboards(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(emptyArray<String>())
    }
    
    @Callback
    fun setPrecise(context: Context, args: Array<Any?>): Array<Any?> {
        isPrecise = args.getOrNull(0) as? Boolean ?: false
        return arrayOf(true)
    }
    
    @Callback
    fun isPrecise(context: Context, args: Array<Any?>): Array<Any?> = arrayOf(isPrecise)
    
    @Callback
    fun setTouchModeInverted(context: Context, args: Array<Any?>): Array<Any?> {
        touchModeInverted = args.getOrNull(0) as? Boolean ?: false
        return arrayOf(true)
    }
    
    @Callback
    fun isTouchModeInverted(context: Context, args: Array<Any?>): Array<Any?> = arrayOf(touchModeInverted)
    
    fun getChar(x: Int, y: Int): Char = textBuffer.getOrNull(y)?.getOrNull(x) ?: ' '
    fun getForegroundColor(x: Int, y: Int): Int = foregroundColors.getOrNull(y)?.getOrNull(x) ?: 0xFFFFFF
    fun getBackgroundColor(x: Int, y: Int): Int = backgroundColors.getOrNull(y)?.getOrNull(x) ?: 0x000000
}

/**
 * Filesystem component - provides file access.
 */
class FilesystemComponent(
    val label: String,
    val capacity: Long,
    val isReadOnly: Boolean = false
) : ComponentBase("filesystem") {
    
    private val files = mutableMapOf<String, ByteArray>()
    private var usedSpace = 0L
    
    override fun methods() = mapOf(
        "getLabel" to ::getLabel,
        "setLabel" to ::setLabel,
        "isReadOnly" to ::isReadOnly,
        "spaceTotal" to ::spaceTotal,
        "spaceUsed" to ::spaceUsed,
        "exists" to ::exists,
        "isDirectory" to ::isDirectory,
        "list" to ::list,
        "size" to ::size,
        "lastModified" to ::lastModified,
        "makeDirectory" to ::makeDirectory,
        "remove" to ::remove,
        "rename" to ::rename,
        "open" to ::open,
        "read" to ::read,
        "write" to ::write,
        "close" to ::close,
        "seek" to ::seek
    )
    
    @Callback
    fun getLabel(context: Context, args: Array<Any?>): Array<Any?> = arrayOf(label)
    
    @Callback
    fun setLabel(context: Context, args: Array<Any?>): Array<Any?> {
        // Label is read-only after creation
        return arrayOf(label)
    }
    
    @Callback
    fun isReadOnly(context: Context, args: Array<Any?>): Array<Any?> = arrayOf(isReadOnly)
    
    @Callback
    fun spaceTotal(context: Context, args: Array<Any?>): Array<Any?> = arrayOf(capacity)
    
    @Callback
    fun spaceUsed(context: Context, args: Array<Any?>): Array<Any?> = arrayOf(usedSpace)
    
    @Callback
    fun exists(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(false)
        return arrayOf(files.containsKey(path) || files.keys.any { it.startsWith("$path/") })
    }
    
    @Callback
    fun isDirectory(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(false)
        return arrayOf(files.keys.any { it.startsWith("$path/") && it != path })
    }
    
    @Callback
    fun list(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0)?.toString()?.trimEnd('/') ?: ""
        val prefix = if (path.isEmpty()) "" else "$path/"
        
        val entries = files.keys
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix).split('/').first() }
            .distinct()
            .sorted()
        
        return arrayOf(entries.toTypedArray())
    }
    
    @Callback
    fun size(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(0L)
        return arrayOf(files[path]?.size?.toLong() ?: 0L)
    }
    
    @Callback
    fun lastModified(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(System.currentTimeMillis())
    }
    
    @Callback
    fun makeDirectory(context: Context, args: Array<Any?>): Array<Any?> {
        if (isReadOnly) return arrayOf(false)
        return arrayOf(true)
    }
    
    @Callback
    fun remove(context: Context, args: Array<Any?>): Array<Any?> {
        if (isReadOnly) return arrayOf(false)
        val path = args.getOrNull(0) as? String ?: return arrayOf(false)
        val data = files.remove(path)
        if (data != null) {
            usedSpace -= data.size
        }
        return arrayOf(data != null)
    }
    
    @Callback
    fun rename(context: Context, args: Array<Any?>): Array<Any?> {
        if (isReadOnly) return arrayOf(false)
        val from = args.getOrNull(0) as? String ?: return arrayOf(false)
        val to = args.getOrNull(1) as? String ?: return arrayOf(false)
        val data = files.remove(from) ?: return arrayOf(false)
        files[to] = data
        return arrayOf(true)
    }
    
    // File handles
    private val handles = mutableMapOf<Int, FileHandle>()
    private var nextHandle = 1
    
    data class FileHandle(
        val path: String,
        var position: Long,
        val mode: String
    )
    
    @Callback
    fun open(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(null, "invalid path")
        val mode = args.getOrNull(1) as? String ?: "r"
        
        if (mode.contains('w') && isReadOnly) {
            return arrayOf(null, "filesystem is read-only")
        }
        
        if (mode.contains('w') || mode.contains('a')) {
            if (!files.containsKey(path)) {
                files[path] = ByteArray(0)
            }
        } else if (!files.containsKey(path)) {
            return arrayOf(null, "file not found")
        }
        
        val handle = nextHandle++
        handles[handle] = FileHandle(path, if (mode.contains('a')) files[path]?.size?.toLong() ?: 0 else 0, mode)
        return arrayOf(handle)
    }
    
    @Callback
    fun read(context: Context, args: Array<Any?>): Array<Any?> {
        val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid handle")
        val count = (args.getOrNull(1) as? Number)?.toInt() ?: 1
        
        val fh = handles[handle] ?: return arrayOf(null, "bad file descriptor")
        val data = files[fh.path] ?: return arrayOf(null, "file not found")
        
        if (fh.position >= data.size) return arrayOf(null)
        
        val end = (fh.position + count).coerceAtMost(data.size.toLong()).toInt()
        val result = data.sliceArray(fh.position.toInt() until end)
        fh.position = end.toLong()
        
        return arrayOf(String(result))
    }
    
    @Callback
    fun write(context: Context, args: Array<Any?>): Array<Any?> {
        if (isReadOnly) return arrayOf(false)
        
        val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid handle")
        val data = args.getOrNull(1)?.toString()?.toByteArray() ?: return arrayOf(null, "invalid data")
        
        val fh = handles[handle] ?: return arrayOf(null, "bad file descriptor")
        val current = files[fh.path] ?: ByteArray(0)
        
        val newData = ByteArray((fh.position + data.size).toInt())
        current.copyInto(newData)
        data.copyInto(newData, fh.position.toInt())
        
        usedSpace += data.size
        files[fh.path] = newData
        fh.position += data.size
        
        return arrayOf(true)
    }
    
    @Callback
    fun close(context: Context, args: Array<Any?>): Array<Any?> {
        val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf()
        handles.remove(handle)
        return arrayOf()
    }
    
    @Callback
    fun seek(context: Context, args: Array<Any?>): Array<Any?> {
        val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid handle")
        val whence = args.getOrNull(1) as? String ?: "cur"
        val offset = (args.getOrNull(2) as? Number)?.toLong() ?: 0
        
        val fh = handles[handle] ?: return arrayOf(null, "bad file descriptor")
        val size = files[fh.path]?.size ?: 0
        
        fh.position = when (whence) {
            "set" -> offset
            "cur" -> fh.position + offset
            "end" -> size + offset
            else -> fh.position
        }.coerceIn(0, size.toLong())
        
        return arrayOf(fh.position)
    }
}

/**
 * EEPROM component - small persistent storage and boot code.
 */
class EEPROMComponent(
    var codeSize: Int = 4096,
    var dataSize: Int = 256
) : ComponentBase("eeprom") {
    
    private var code = ByteArray(0)
    private var data = ByteArray(0)
    private var label = "EEPROM"
    private var isReadOnly = false
    
    override fun methods() = mapOf(
        "get" to ::get,
        "set" to ::set,
        "getLabel" to ::getLabel,
        "setLabel" to ::setLabel,
        "getSize" to ::getSize,
        "getDataSize" to ::getDataSize,
        "getData" to ::getData,
        "setData" to ::setData,
        "getChecksum" to ::getChecksum,
        "makeReadonly" to ::makeReadonly
    )
    
    @Callback(doc = "function():string -- Get the BIOS code.")
    fun get(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(String(code))
    }
    
    @Callback(doc = "function(data:string) -- Set the BIOS code.")
    fun set(context: Context, args: Array<Any?>): Array<Any?> {
        if (isReadOnly) return arrayOf(null, "EEPROM is read-only")
        val newCode = args.getOrNull(0)?.toString()?.toByteArray() ?: ByteArray(0)
        if (newCode.size > codeSize) return arrayOf(null, "code too large")
        code = newCode
        return arrayOf()
    }
    
    @Callback
    fun getLabel(context: Context, args: Array<Any?>): Array<Any?> = arrayOf(label)
    
    @Callback
    fun setLabel(context: Context, args: Array<Any?>): Array<Any?> {
        if (isReadOnly) return arrayOf(null, "EEPROM is read-only")
        label = args.getOrNull(0)?.toString()?.take(16) ?: label
        return arrayOf(label)
    }
    
    @Callback
    fun getSize(context: Context, args: Array<Any?>): Array<Any?> = arrayOf(codeSize)
    
    @Callback
    fun getDataSize(context: Context, args: Array<Any?>): Array<Any?> = arrayOf(dataSize)
    
    @Callback
    fun getData(context: Context, args: Array<Any?>): Array<Any?> = arrayOf(String(data))
    
    @Callback
    fun setData(context: Context, args: Array<Any?>): Array<Any?> {
        if (isReadOnly) return arrayOf(null, "EEPROM is read-only")
        val newData = args.getOrNull(0)?.toString()?.toByteArray() ?: ByteArray(0)
        if (newData.size > dataSize) return arrayOf(null, "data too large")
        data = newData
        return arrayOf()
    }
    
    @Callback
    fun getChecksum(context: Context, args: Array<Any?>): Array<Any?> {
        var checksum = 0
        for (b in code) checksum = (checksum * 31 + b) and 0xFFFF
        return arrayOf(checksum.toString(16))
    }
    
    @Callback
    fun makeReadonly(context: Context, args: Array<Any?>): Array<Any?> {
        val checksum = args.getOrNull(0) as? String
        if (checksum == getChecksum(context, args)[0]) {
            isReadOnly = true
            return arrayOf(true)
        }
        return arrayOf(false, "incorrect checksum")
    }
}

/**
 * Geolyzer component - scans terrain.
 */
class GeolyzerComponent(
    private val level: () -> Level?,
    private val position: () -> BlockPos
) : ComponentBase("geolyzer") {
    
    override fun methods() = mapOf(
        "scan" to ::scan,
        "analyze" to ::analyze,
        "store" to ::store,
        "detect" to ::detect,
        "canSeeSky" to ::canSeeSky,
        "isSunVisible" to ::isSunVisible
    )
    
    @Callback(doc = "function(x:number, z:number[, y:number[, w:number[, d:number[, h:number[, ignoreReplaceable:boolean[, options:table]]]]]]):table -- Scan blocks.")
    fun scan(context: Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val z = (args.getOrNull(1) as? Number)?.toInt() ?: 0
        val y = (args.getOrNull(2) as? Number)?.toInt() ?: -32
        val w = (args.getOrNull(3) as? Number)?.toInt() ?: 1
        val d = (args.getOrNull(4) as? Number)?.toInt() ?: 1
        val h = (args.getOrNull(5) as? Number)?.toInt() ?: 64
        
        val world = level() ?: return arrayOf(null, "no world")
        val pos = position()
        
        val results = mutableListOf<Double>()
        
        for (dy in 0 until h) {
            for (dz in 0 until d) {
                for (dx in 0 until w) {
                    val blockPos = pos.offset(x + dx, y + dy, z + dz)
                    val state = world.getBlockState(blockPos)
                    
                    // Return hardness as a proxy for block density
                    val hardness = state.getDestroySpeed(world, blockPos).toDouble()
                    results.add(if (state.isAir) 0.0 else hardness + 1.0)
                }
            }
        }
        
        return arrayOf(results.toTypedArray())
    }
    
    @Callback(doc = "function(side:number):table -- Get detailed info about adjacent block.")
    fun analyze(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val dir = Direction.entries.getOrElse(side) { Direction.DOWN }
        
        val world = level() ?: return arrayOf(null, "no world")
        val blockPos = position().relative(dir)
        val state = world.getBlockState(blockPos)
        
        val result = mutableMapOf<String, Any?>()
        result["name"] = BuiltInRegistries.BLOCK.getKey(state.block).toString()
        result["hardness"] = state.getDestroySpeed(world, blockPos)
        
        return arrayOf(result)
    }
    
    @Callback(doc = "function(side:number, dbAddress:string, dbSlot:number):boolean -- Store block info in database.")
    fun store(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(side:number):boolean, string -- Detect block type on side.")
    fun detect(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val dir = Direction.entries.getOrElse(side) { Direction.DOWN }
        
        val world = level() ?: return arrayOf(false, "no world")
        val blockPos = position().relative(dir)
        val state = world.getBlockState(blockPos)
        
        return when {
            state.isAir -> arrayOf(false, "air")
            state.liquid() -> arrayOf(true, "liquid")
            state.isSolidRender(world, blockPos) -> arrayOf(true, "solid")
            else -> arrayOf(true, "passable")
        }
    }
    
    @Callback(doc = "function():boolean -- Check if can see the sky.")
    fun canSeeSky(context: Context, args: Array<Any?>): Array<Any?> {
        val world = level() ?: return arrayOf(false)
        return arrayOf(world.canSeeSky(position()))
    }
    
    @Callback(doc = "function():boolean -- Check if sun is visible.")
    fun isSunVisible(context: Context, args: Array<Any?>): Array<Any?> {
        val world = level() ?: return arrayOf(false)
        return arrayOf(world.canSeeSky(position()) && world.isDay)
    }
}

/**
 * Redstone component - redstone I/O.
 */
class RedstoneComponent(
    private val level: () -> Level?,
    private val position: () -> BlockPos
) : ComponentBase("redstone") {
    
    private val bundledInput = Array(6) { IntArray(16) }
    private val bundledOutput = Array(6) { IntArray(16) }
    private val wakeThreshold = IntArray(6) { -1 }
    
    override fun methods() = mapOf(
        "getInput" to ::getInput,
        "getOutput" to ::getOutput,
        "setOutput" to ::setOutput,
        "getBundledInput" to ::getBundledInput,
        "getBundledOutput" to ::getBundledOutput,
        "setBundledOutput" to ::setBundledOutput,
        "getComparatorInput" to ::getComparatorInput,
        "getWakeThreshold" to ::getWakeThreshold,
        "setWakeThreshold" to ::setWakeThreshold
    )
    
    @Callback(doc = "function(side:number):number -- Get redstone input level on side.")
    fun getInput(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val dir = Direction.entries.getOrElse(side) { Direction.DOWN }
        
        val world = level() ?: return arrayOf(0)
        val pos = position()
        val signal = world.getSignal(pos.relative(dir), dir)
        
        return arrayOf(signal)
    }
    
    @Callback(doc = "function(side:number):number -- Get redstone output level on side.")
    fun getOutput(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        return arrayOf(bundledOutput.getOrElse(side) { IntArray(16) }.sum().coerceIn(0, 15))
    }
    
    @Callback(doc = "function(side:number, value:number):number -- Set redstone output level.")
    fun setOutput(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val value = (args.getOrNull(1) as? Number)?.toInt()?.coerceIn(0, 15) ?: 0
        
        // Set all bundled colors to this value (simplified)
        val old = bundledOutput.getOrElse(side) { IntArray(16) }.sum()
        if (side in bundledOutput.indices) {
            bundledOutput[side] = IntArray(16) { value }
        }
        
        return arrayOf(old)
    }
    
    @Callback(doc = "function(side:number):table -- Get bundled redstone input.")
    fun getBundledInput(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        return arrayOf(bundledInput.getOrElse(side) { IntArray(16) })
    }
    
    @Callback(doc = "function(side:number):table -- Get bundled redstone output.")
    fun getBundledOutput(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        return arrayOf(bundledOutput.getOrElse(side) { IntArray(16) })
    }
    
    @Callback(doc = "function(side:number, colors:table):table -- Set bundled redstone output.")
    fun setBundledOutput(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val colors = args.getOrNull(1)
        
        val old = bundledOutput.getOrElse(side) { IntArray(16) }.clone()
        
        // Parse colors table
        if (colors is Map<*, *>) {
            for ((key, value) in colors) {
                val index = (key as? Number)?.toInt()?.minus(1) ?: continue
                val level = (value as? Number)?.toInt()?.coerceIn(0, 255) ?: continue
                if (index in 0..15 && side in bundledOutput.indices) {
                    bundledOutput[side][index] = level
                }
            }
        }
        
        return arrayOf(old)
    }
    
    @Callback(doc = "function(side:number):number -- Get comparator input on side.")
    fun getComparatorInput(context: Context, args: Array<Any?>): Array<Any?> {
        // Comparator input is the same as regular input for simplicity
        return getInput(context, args)
    }
    
    @Callback(doc = "function(side:number):number -- Get wake threshold for side.")
    fun getWakeThreshold(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        return arrayOf(wakeThreshold.getOrElse(side) { -1 })
    }
    
    @Callback(doc = "function(side:number, threshold:number):number -- Set wake threshold.")
    fun setWakeThreshold(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val threshold = (args.getOrNull(1) as? Number)?.toInt() ?: -1
        
        val old = wakeThreshold.getOrElse(side) { -1 }
        if (side in wakeThreshold.indices) {
            wakeThreshold[side] = threshold
        }
        
        return arrayOf(old)
    }
}

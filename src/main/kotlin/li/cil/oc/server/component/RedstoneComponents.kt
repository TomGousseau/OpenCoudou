package li.cil.oc.server.component

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RedStoneWireBlock
import java.util.*
import kotlin.math.min

/**
 * Redstone component - handles redstone input/output.
 * Supports both vanilla redstone and bundled cables (when available).
 */
class RedstoneComponent(val tier: Int = 1) : ComponentBase("redstone") {
    
    private var level: Level? = null
    private var pos: BlockPos = BlockPos.ZERO
    
    // Output values per side (0=down, 1=up, 2=north, 3=south, 4=west, 5=east)
    private val outputs = IntArray(6)
    
    // Bundled output values per side (16 colors per side)
    private val bundledOutputs = Array(6) { IntArray(16) }
    
    // Wake threshold
    private var wakeThreshold = 0
    
    fun setContext(level: Level, pos: BlockPos) {
        this.level = level
        this.pos = pos
    }
    
    @Callback(doc = "function(side: number): number -- Gets the redstone input on a side")
    fun getInput(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        if (side !in 0..5) return arrayOf(0)
        
        val dir = Direction.from3DDataValue(side)
        val l = level ?: return arrayOf(0)
        val targetPos = pos.relative(dir)
        
        return arrayOf(l.getSignal(targetPos, dir))
    }
    
    @Callback(doc = "function(): table -- Gets all redstone inputs")
    fun getInputs(context: Context, args: Array<Any?>): Array<Any?> {
        val l = level ?: return arrayOf(IntArray(6))
        val inputs = IntArray(6)
        
        for (i in 0..5) {
            val dir = Direction.from3DDataValue(i)
            val targetPos = pos.relative(dir)
            inputs[i] = l.getSignal(targetPos, dir)
        }
        
        return arrayOf(inputs.toList())
    }
    
    @Callback(doc = "function(side: number): number -- Gets the redstone output on a side")
    fun getOutput(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        if (side !in 0..5) return arrayOf(0)
        return arrayOf(outputs[side])
    }
    
    @Callback(doc = "function(): table -- Gets all redstone outputs")
    fun getOutputs(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(outputs.toList())
    }
    
    @Callback(doc = "function(side: number, value: number): number -- Sets the redstone output on a side")
    fun setOutput(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        val value = (args.getOrNull(1) as? Number)?.toInt()?.coerceIn(0, 15) ?: 0
        
        if (side !in 0..5) return arrayOf(0)
        
        val old = outputs[side]
        outputs[side] = value
        updateNeighbor(side)
        
        return arrayOf(old)
    }
    
    @Callback(doc = "function(values: table): table -- Sets all redstone outputs")
    fun setOutputs(context: Context, args: Array<Any?>): Array<Any?> {
        @Suppress("UNCHECKED_CAST")
        val values = args.getOrNull(0) as? List<Number> ?: return arrayOf(outputs.toList())
        
        val old = outputs.copyOf()
        for (i in 0 until min(6, values.size)) {
            outputs[i] = values[i].toInt().coerceIn(0, 15)
        }
        
        for (i in 0..5) {
            if (outputs[i] != old[i]) {
                updateNeighbor(i)
            }
        }
        
        return arrayOf(old.toList())
    }
    
    // Bundled cable support (tier 2+)
    @Callback(doc = "function(side: number, color: number): number -- Gets bundled input for a color")
    fun getBundledInput(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "not supported")
        
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        val color = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(0)
        
        if (side !in 0..5 || color !in 0..15) return arrayOf(0)
        
        // Would read from bundled cable in actual implementation
        return arrayOf(0)
    }
    
    @Callback(doc = "function(side: number): table -- Gets all bundled inputs for a side")
    fun getBundledInputs(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "not supported")
        
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(IntArray(16))
        if (side !in 0..5) return arrayOf(IntArray(16))
        
        // Would read from bundled cable
        return arrayOf(IntArray(16).toList())
    }
    
    @Callback(doc = "function(side: number, color: number): number -- Gets bundled output for a color")
    fun getBundledOutput(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "not supported")
        
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        val color = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(0)
        
        if (side !in 0..5 || color !in 0..15) return arrayOf(0)
        
        return arrayOf(bundledOutputs[side][color])
    }
    
    @Callback(doc = "function(side: number, color: number, value: number): number -- Sets bundled output for a color")
    fun setBundledOutput(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "not supported")
        
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        val color = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(0)
        val value = (args.getOrNull(2) as? Number)?.toInt()?.coerceIn(0, 255) ?: 0
        
        if (side !in 0..5 || color !in 0..15) return arrayOf(0)
        
        val old = bundledOutputs[side][color]
        bundledOutputs[side][color] = value
        updateNeighbor(side)
        
        return arrayOf(old)
    }
    
    @Callback(doc = "function(side: number, values: table): table -- Sets all bundled outputs for a side")
    fun setBundledOutputs(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "not supported")
        
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(IntArray(16).toList())
        @Suppress("UNCHECKED_CAST")
        val values = args.getOrNull(1) as? List<Number> ?: return arrayOf(bundledOutputs[side].toList())
        
        if (side !in 0..5) return arrayOf(IntArray(16).toList())
        
        val old = bundledOutputs[side].copyOf()
        for (i in 0 until min(16, values.size)) {
            bundledOutputs[side][i] = values[i].toInt().coerceIn(0, 255)
        }
        
        updateNeighbor(side)
        return arrayOf(old.toList())
    }
    
    // Wireless redstone support (tier 3)
    @Callback(doc = "function(frequency: number): number -- Gets wireless redstone input")
    fun getWirelessInput(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 3) return arrayOf(null, "not supported")
        
        val frequency = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        // Would read from wireless redstone network
        return arrayOf(0)
    }
    
    @Callback(doc = "function(frequency: number, value: number): boolean -- Sets wireless redstone output")
    fun setWirelessOutput(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 3) return arrayOf(null, "not supported")
        
        val frequency = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(false)
        val value = (args.getOrNull(1) as? Number)?.toInt()?.coerceIn(0, 15) ?: 0
        // Would set wireless redstone output
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): number -- Gets the wake threshold")
    fun getWakeThreshold(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(wakeThreshold)
    }
    
    @Callback(doc = "function(threshold: number): number -- Sets the wake threshold")
    fun setWakeThreshold(context: Context, args: Array<Any?>): Array<Any?> {
        val old = wakeThreshold
        wakeThreshold = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 15) ?: 0
        return arrayOf(old)
    }
    
    // Comparator input (reads inventory/container levels)
    @Callback(doc = "function(side: number): number -- Gets the comparator input from a side")
    fun getComparatorInput(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        if (side !in 0..5) return arrayOf(0)
        
        val l = level ?: return arrayOf(0)
        val dir = Direction.from3DDataValue(side)
        val targetPos = pos.relative(dir)
        val state = l.getBlockState(targetPos)
        
        // Get comparator output from the block
        return arrayOf(state.getAnalogOutputSignal(l, targetPos))
    }
    
    private fun updateNeighbor(side: Int) {
        val l = level ?: return
        val dir = Direction.from3DDataValue(side)
        val targetPos = pos.relative(dir)
        
        l.neighborChanged(targetPos, l.getBlockState(pos).block, pos)
    }
    
    fun onNeighborChanged() {
        // Check if any input went above wake threshold
        val l = level ?: return
        
        for (i in 0..5) {
            val dir = Direction.from3DDataValue(i)
            val targetPos = pos.relative(dir)
            val signal = l.getSignal(targetPos, dir)
            
            if (signal >= wakeThreshold && wakeThreshold > 0) {
                queueSignal("redstone_changed", address, i, signal)
            }
        }
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putInt("tier", tier)
        tag.putIntArray("outputs", outputs)
        tag.putInt("wakeThreshold", wakeThreshold)
        
        if (tier >= 2) {
            for (i in 0..5) {
                tag.putIntArray("bundled$i", bundledOutputs[i])
            }
        }
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        
        val savedOutputs = tag.getIntArray("outputs")
        for (i in savedOutputs.indices) {
            if (i < outputs.size) outputs[i] = savedOutputs[i]
        }
        
        wakeThreshold = tag.getInt("wakeThreshold")
        
        if (tier >= 2) {
            for (i in 0..5) {
                val saved = tag.getIntArray("bundled$i")
                for (j in saved.indices) {
                    if (j < 16) bundledOutputs[i][j] = saved[j]
                }
            }
        }
    }
}

/**
 * Adapter component - allows interaction with adjacent blocks.
 */
class AdapterComponent : ComponentBase("adapter") {
    
    private var level: Level? = null
    private var pos: BlockPos = BlockPos.ZERO
    
    fun setContext(level: Level, pos: BlockPos) {
        this.level = level
        this.pos = pos
    }
    
    @Callback(doc = "function(): table -- Gets components connected through the adapter")
    fun getComponents(context: Context, args: Array<Any?>): Array<Any?> {
        // Would enumerate adapters for adjacent blocks
        return arrayOf(emptyMap<String, String>())
    }
    
    @Callback(doc = "function(side: number): table -- Gets the block info on a side")
    fun getBlockInfo(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null)
        if (side !in 0..5) return arrayOf(null)
        
        val l = level ?: return arrayOf(null)
        val dir = Direction.from3DDataValue(side)
        val targetPos = pos.relative(dir)
        val state = l.getBlockState(targetPos)
        
        return arrayOf(mapOf(
            "name" to state.block.descriptionId,
            "metadata" to 0
        ))
    }
}

/**
 * Printer component - 3D printing functionality.
 */
class PrinterComponent : ComponentBase("printer3d") {
    
    private var label: String = ""
    private var tooltip: String = ""
    private var lightLevel = 0
    private var redstoneLevel = 0
    private var buttonMode = false
    private var collidable = true
    
    private val shapes = mutableListOf<PrintShape>()
    private var printing = false
    private var progress = 0
    
    data class PrintShape(
        val minX: Int, val minY: Int, val minZ: Int,
        val maxX: Int, val maxY: Int, val maxZ: Int,
        val texture: String,
        val tint: Int = 0xFFFFFF,
        val state: Boolean = false
    )
    
    @Callback(doc = "function(label: string): void -- Sets the label")
    fun setLabel(context: Context, args: Array<Any?>): Array<Any?> {
        label = (args.getOrNull(0) as? String ?: "").take(24)
        return arrayOf()
    }
    
    @Callback(doc = "function(): string -- Gets the label")
    fun getLabel(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(label)
    }
    
    @Callback(doc = "function(tooltip: string): void -- Sets the tooltip")
    fun setTooltip(context: Context, args: Array<Any?>): Array<Any?> {
        tooltip = (args.getOrNull(0) as? String ?: "").take(128)
        return arrayOf()
    }
    
    @Callback(doc = "function(): string -- Gets the tooltip")
    fun getTooltip(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(tooltip)
    }
    
    @Callback(doc = "function(level: number): void -- Sets the light level")
    fun setLightLevel(context: Context, args: Array<Any?>): Array<Any?> {
        lightLevel = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 15) ?: 0
        return arrayOf()
    }
    
    @Callback(doc = "function(): number -- Gets the light level")
    fun getLightLevel(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(lightLevel)
    }
    
    @Callback(doc = "function(level: number): void -- Sets the redstone output level")
    fun setRedstoneEmitter(context: Context, args: Array<Any?>): Array<Any?> {
        redstoneLevel = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 15) ?: 0
        return arrayOf()
    }
    
    @Callback(doc = "function(): number -- Gets the redstone output level")
    fun getRedstoneEmitter(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(redstoneLevel)
    }
    
    @Callback(doc = "function(button: boolean): void -- Sets button mode")
    fun setButtonMode(context: Context, args: Array<Any?>): Array<Any?> {
        buttonMode = args.getOrNull(0) as? Boolean ?: false
        return arrayOf()
    }
    
    @Callback(doc = "function(): boolean -- Gets button mode")
    fun isButtonMode(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(buttonMode)
    }
    
    @Callback(doc = "function(collidable: boolean): void -- Sets whether the print is collidable")
    fun setCollidable(context: Context, args: Array<Any?>): Array<Any?> {
        collidable = args.getOrNull(0) as? Boolean ?: true
        return arrayOf()
    }
    
    @Callback(doc = "function(): boolean -- Gets whether the print is collidable")
    fun isCollidable(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(collidable)
    }
    
    @Callback(doc = "function(minX: number, minY: number, minZ: number, maxX: number, maxY: number, maxZ: number, texture: string[, state: boolean, tint: number]): boolean -- Adds a shape")
    fun addShape(context: Context, args: Array<Any?>): Array<Any?> {
        if (shapes.size >= 24) return arrayOf(false, "too many shapes")
        
        val minX = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(false, "invalid minX")
        val minY = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(false, "invalid minY")
        val minZ = (args.getOrNull(2) as? Number)?.toInt() ?: return arrayOf(false, "invalid minZ")
        val maxX = (args.getOrNull(3) as? Number)?.toInt() ?: return arrayOf(false, "invalid maxX")
        val maxY = (args.getOrNull(4) as? Number)?.toInt() ?: return arrayOf(false, "invalid maxY")
        val maxZ = (args.getOrNull(5) as? Number)?.toInt() ?: return arrayOf(false, "invalid maxZ")
        val texture = args.getOrNull(6) as? String ?: "minecraft:block/white_concrete"
        val state = args.getOrNull(7) as? Boolean ?: false
        val tint = (args.getOrNull(8) as? Number)?.toInt() ?: 0xFFFFFF
        
        // Validate bounds
        if (minX < 0 || maxX > 16 || minY < 0 || maxY > 16 || minZ < 0 || maxZ > 16) {
            return arrayOf(false, "shape out of bounds")
        }
        if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
            return arrayOf(false, "invalid shape dimensions")
        }
        
        shapes.add(PrintShape(minX, minY, minZ, maxX, maxY, maxZ, texture, tint, state))
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): number -- Gets the number of shapes")
    fun getShapeCount(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(shapes.size)
    }
    
    @Callback(doc = "function(): number -- Gets the maximum number of shapes")
    fun getMaxShapeCount(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(24)
    }
    
    @Callback(doc = "function(): void -- Resets all shapes")
    fun reset(context: Context, args: Array<Any?>): Array<Any?> {
        shapes.clear()
        label = ""
        tooltip = ""
        lightLevel = 0
        redstoneLevel = 0
        buttonMode = false
        collidable = true
        return arrayOf()
    }
    
    @Callback(doc = "function([count: number]): boolean -- Commits the print job")
    fun commit(context: Context, args: Array<Any?>): Array<Any?> {
        val count = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(1, 64) ?: 1
        
        if (shapes.isEmpty()) return arrayOf(false, "no shapes")
        if (printing) return arrayOf(false, "already printing")
        
        printing = true
        progress = 0
        
        // Would start actual printing process
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): boolean, number or string -- Gets the print status")
    fun status(context: Context, args: Array<Any?>): Array<Any?> {
        return if (printing) {
            arrayOf(false, progress)
        } else {
            arrayOf(true, "idle")
        }
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putString("label", label)
        tag.putString("tooltip", tooltip)
        tag.putInt("light", lightLevel)
        tag.putInt("redstone", redstoneLevel)
        tag.putBoolean("button", buttonMode)
        tag.putBoolean("collidable", collidable)
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        label = tag.getString("label")
        tooltip = tag.getString("tooltip")
        lightLevel = tag.getInt("light")
        redstoneLevel = tag.getInt("redstone")
        buttonMode = tag.getBoolean("button")
        collidable = tag.getBoolean("collidable")
    }
}

/**
 * Rack component - manages server rack contents.
 */
class RackComponent : ComponentBase("rack") {
    
    private var level: Level? = null
    private var pos: BlockPos = BlockPos.ZERO
    private val servers = arrayOfNulls<ServerInfo>(4)
    
    data class ServerInfo(
        val address: String,
        val name: String,
        var running: Boolean = false
    )
    
    fun setContext(level: Level, pos: BlockPos) {
        this.level = level
        this.pos = pos
    }
    
    @Callback(doc = "function(slot: number): table -- Gets info about a server in a slot")
    fun getServer(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return arrayOf(null)
        if (slot !in servers.indices) return arrayOf(null)
        
        val server = servers[slot] ?: return arrayOf(null)
        return arrayOf(mapOf(
            "address" to server.address,
            "name" to server.name,
            "running" to server.running
        ))
    }
    
    @Callback(doc = "function(slot: number): boolean -- Starts a server")
    fun start(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return arrayOf(false)
        if (slot !in servers.indices) return arrayOf(false)
        
        val server = servers[slot] ?: return arrayOf(false)
        server.running = true
        return arrayOf(true)
    }
    
    @Callback(doc = "function(slot: number): boolean -- Stops a server")
    fun stop(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return arrayOf(false)
        if (slot !in servers.indices) return arrayOf(false)
        
        val server = servers[slot] ?: return arrayOf(false)
        server.running = false
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): table -- Gets the addresses of all connected switches")
    fun getConnectedSwitches(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(emptyList<String>())
    }
}

/**
 * Data card component - provides data processing functions.
 */
class DataCardComponent(val tier: Int = 1) : ComponentBase("data") {
    
    @Callback(doc = "function(): number -- Gets the tier of the data card")
    fun getTier(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(tier)
    }
    
    @Callback(doc = "function(data: string): string -- Computes CRC32 checksum")
    fun crc32(context: Context, args: Array<Any?>): Array<Any?> {
        val data = args.getOrNull(0) as? String ?: return arrayOf("")
        val crc = java.util.zip.CRC32()
        crc.update(data.toByteArray())
        return arrayOf(crc.value.toString(16))
    }
    
    @Callback(doc = "function(data: string): string -- Computes MD5 hash")
    fun md5(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "tier too low")
        val data = args.getOrNull(0) as? String ?: return arrayOf("")
        
        val digest = java.security.MessageDigest.getInstance("MD5")
        val hash = digest.digest(data.toByteArray())
        return arrayOf(hash.joinToString("") { "%02x".format(it) })
    }
    
    @Callback(doc = "function(data: string): string -- Computes SHA256 hash")
    fun sha256(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "tier too low")
        val data = args.getOrNull(0) as? String ?: return arrayOf("")
        
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return arrayOf(hash.joinToString("") { "%02x".format(it) })
    }
    
    @Callback(doc = "function(data: string): string -- Encodes data as base64")
    fun encode64(context: Context, args: Array<Any?>): Array<Any?> {
        val data = args.getOrNull(0) as? String ?: return arrayOf("")
        return arrayOf(Base64.getEncoder().encodeToString(data.toByteArray()))
    }
    
    @Callback(doc = "function(data: string): string -- Decodes base64 data")
    fun decode64(context: Context, args: Array<Any?>): Array<Any?> {
        val data = args.getOrNull(0) as? String ?: return arrayOf("")
        return try {
            arrayOf(String(Base64.getDecoder().decode(data)))
        } catch (e: Exception) {
            arrayOf(null, "invalid base64")
        }
    }
    
    @Callback(doc = "function(data: string): string -- Deflates data using DEFLATE compression")
    fun deflate(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "tier too low")
        val data = args.getOrNull(0) as? String ?: return arrayOf("")
        
        val deflater = java.util.zip.Deflater()
        deflater.setInput(data.toByteArray())
        deflater.finish()
        
        val output = ByteArray(data.length + 100)
        val len = deflater.deflate(output)
        deflater.end()
        
        return arrayOf(String(output.copyOf(len), Charsets.ISO_8859_1))
    }
    
    @Callback(doc = "function(data: string): string -- Inflates DEFLATE compressed data")
    fun inflate(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "tier too low")
        val data = args.getOrNull(0) as? String ?: return arrayOf("")
        
        try {
            val inflater = java.util.zip.Inflater()
            inflater.setInput(data.toByteArray(Charsets.ISO_8859_1))
            
            val output = ByteArray(data.length * 10)
            val len = inflater.inflate(output)
            inflater.end()
            
            return arrayOf(String(output.copyOf(len)))
        } catch (e: Exception) {
            return arrayOf(null, "decompression failed")
        }
    }
    
    @Callback(doc = "function(data: string, key: string): string -- Encrypts data using AES")
    fun encrypt(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 3) return arrayOf(null, "tier too low")
        val data = args.getOrNull(0) as? String ?: return arrayOf("")
        val key = args.getOrNull(1) as? String ?: return arrayOf(null, "invalid key")
        
        try {
            val keyBytes = key.toByteArray().copyOf(16)
            val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
            val cipher = javax.crypto.Cipher.getInstance("AES")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
            
            val encrypted = cipher.doFinal(data.toByteArray())
            return arrayOf(Base64.getEncoder().encodeToString(encrypted))
        } catch (e: Exception) {
            return arrayOf(null, "encryption failed")
        }
    }
    
    @Callback(doc = "function(data: string, key: string): string -- Decrypts AES encrypted data")
    fun decrypt(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 3) return arrayOf(null, "tier too low")
        val data = args.getOrNull(0) as? String ?: return arrayOf("")
        val key = args.getOrNull(1) as? String ?: return arrayOf(null, "invalid key")
        
        try {
            val keyBytes = key.toByteArray().copyOf(16)
            val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
            val cipher = javax.crypto.Cipher.getInstance("AES")
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey)
            
            val decrypted = cipher.doFinal(Base64.getDecoder().decode(data))
            return arrayOf(String(decrypted))
        } catch (e: Exception) {
            return arrayOf(null, "decryption failed")
        }
    }
    
    @Callback(doc = "function(bits: number): string -- Generates random bytes")
    fun random(context: Context, args: Array<Any?>): Array<Any?> {
        val bits = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(8, 1024) ?: 256
        val bytes = ByteArray(bits / 8)
        java.security.SecureRandom().nextBytes(bytes)
        return arrayOf(Base64.getEncoder().encodeToString(bytes))
    }
    
    @Callback(doc = "function(): number -- Gets the maximum data size")
    fun getLimit(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(when (tier) {
            1 -> 8192
            2 -> 65536
            else -> 262144
        })
    }
}

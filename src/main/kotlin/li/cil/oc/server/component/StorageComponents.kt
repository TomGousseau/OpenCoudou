package li.cil.oc.server.component

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.server.fs.MemoryFileSystem
import li.cil.oc.server.fs.VirtualFileSystem
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.core.BlockPos
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import kotlin.math.*

/**
 * EEPROM component - small persistent storage for bootloaders.
 */
class EEPROMComponent : ComponentBase("eeprom") {
    
    private var code = ByteArray(0)
    private var data = ByteArray(0)
    private var label = "EEPROM"
    private var readonly = false
    
    val codeSize = 4096
    val dataSize = 256
    
    @Callback(doc = "function(): string -- Gets the EEPROM code")
    fun get(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(String(code, StandardCharsets.UTF_8))
    }
    
    @Callback(doc = "function(code: string): void -- Sets the EEPROM code")
    fun set(context: Context, args: Array<Any?>): Array<Any?> {
        if (readonly) return arrayOf(null, "storage is readonly")
        
        val newCode = (args.getOrNull(0) as? String ?: "").toByteArray(StandardCharsets.UTF_8)
        if (newCode.size > codeSize) {
            return arrayOf(null, "code too large")
        }
        
        code = newCode
        return arrayOf()
    }
    
    @Callback(doc = "function(): string -- Gets the EEPROM label")
    fun getLabel(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(label)
    }
    
    @Callback(doc = "function(label: string): string -- Sets the EEPROM label")
    fun setLabel(context: Context, args: Array<Any?>): Array<Any?> {
        val old = label
        label = (args.getOrNull(0) as? String ?: "").take(24)
        return arrayOf(old)
    }
    
    @Callback(doc = "function(): string -- Gets the EEPROM data")
    fun getData(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(String(data, StandardCharsets.UTF_8))
    }
    
    @Callback(doc = "function(data: string): void -- Sets the EEPROM data")
    fun setData(context: Context, args: Array<Any?>): Array<Any?> {
        if (readonly) return arrayOf(null, "storage is readonly")
        
        val newData = (args.getOrNull(0) as? String ?: "").toByteArray(StandardCharsets.UTF_8)
        if (newData.size > dataSize) {
            return arrayOf(null, "data too large")
        }
        
        data = newData
        return arrayOf()
    }
    
    @Callback(doc = "function(): number, number -- Gets the storage sizes")
    fun getSize(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(codeSize, dataSize)
    }
    
    @Callback(doc = "function(): string -- Gets the checksum")
    fun getChecksum(context: Context, args: Array<Any?>): Array<Any?> {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(code)
        val hash = digest.digest()
        return arrayOf(hash.take(4).joinToString("") { "%02x".format(it) })
    }
    
    @Callback(doc = "function(readonly: boolean): void -- Makes the EEPROM read-only")
    fun makeReadonly(context: Context, args: Array<Any?>): Array<Any?> {
        val newReadonly = args.getOrNull(0) as? Boolean ?: true
        if (newReadonly && !readonly) {
            readonly = true
        }
        return arrayOf()
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putByteArray("code", code)
        tag.putByteArray("data", data)
        tag.putString("label", label)
        tag.putBoolean("readonly", readonly)
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        code = tag.getByteArray("code")
        data = tag.getByteArray("data")
        label = tag.getString("label").ifEmpty { "EEPROM" }
        readonly = tag.getBoolean("readonly")
    }
}

/**
 * Disk drive component - provides access to floppy disks.
 */
class DiskDriveComponent : ComponentBase("disk_drive") {
    
    private var diskAddress: String? = null
    
    fun insertDisk(address: String) {
        if (diskAddress != null) {
            queueSignal("disk_eject", this.address)
        }
        diskAddress = address
        queueSignal("disk_insert", this.address)
    }
    
    fun ejectDisk(): String? {
        val old = diskAddress
        if (old != null) {
            diskAddress = null
            queueSignal("disk_eject", this.address)
        }
        return old
    }
    
    @Callback(doc = "function(): string -- Gets the address of the inserted disk")
    fun getDisk(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(diskAddress)
    }
    
    @Callback(doc = "function(): boolean -- Ejects the current disk")
    fun eject(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(ejectDisk() != null)
    }
    
    @Callback(doc = "function(): boolean -- Checks if a disk is present")
    fun isEmpty(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(diskAddress == null)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        diskAddress?.let { tag.putString("disk", it) }
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        diskAddress = if (tag.contains("disk")) tag.getString("disk") else null
    }
}

/**
 * Hard disk component - provides large persistent storage.
 */
class HardDiskComponent(val tier: Int = 1) : ComponentBase("filesystem") {
    
    val capacity: Long get() = when (tier) {
        1 -> 1024 * 1024        // 1 MB
        2 -> 2 * 1024 * 1024    // 2 MB
        3 -> 4 * 1024 * 1024    // 4 MB
        else -> 1024 * 1024
    }
    
    private var fs: VirtualFileSystem = MemoryFileSystem(capacity = capacity)
    private var label: String? = null
    
    fun getFileSystem(): VirtualFileSystem = fs
    
    @Callback(doc = "function(): string -- Gets the drive label")
    fun getLabel(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(label ?: fs.getLabel())
    }
    
    @Callback(doc = "function(label: string): string -- Sets the drive label")
    fun setLabel(context: Context, args: Array<Any?>): Array<Any?> {
        val old = label
        label = (args.getOrNull(0) as? String)?.take(16)
        fs.setLabel(label)
        return arrayOf(old)
    }
    
    @Callback(doc = "function(): boolean -- Checks if the filesystem is read-only")
    fun isReadOnly(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(fs.isReadOnly())
    }
    
    @Callback(doc = "function(): number -- Gets the total capacity")
    fun spaceTotal(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(fs.spaceTotal())
    }
    
    @Callback(doc = "function(): number -- Gets the used space")
    fun spaceUsed(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(fs.spaceUsed())
    }
    
    @Callback(doc = "function(path: string): boolean -- Checks if a path exists")
    fun exists(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(false)
        return arrayOf(fs.exists(path))
    }
    
    @Callback(doc = "function(path: string): boolean -- Checks if a path is a directory")
    fun isDirectory(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(false)
        return arrayOf(fs.isDirectory(path))
    }
    
    @Callback(doc = "function(path: string): table -- Lists directory contents")
    fun list(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(null, "invalid path")
        val list = fs.list(path) ?: return arrayOf(null, "no such directory")
        return arrayOf(list.toList())
    }
    
    @Callback(doc = "function(path: string): number -- Gets the file size")
    fun size(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(0)
        return arrayOf(fs.size(path))
    }
    
    @Callback(doc = "function(path: string): number -- Gets the last modified time")
    fun lastModified(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(0)
        return arrayOf(fs.lastModified(path))
    }
    
    @Callback(doc = "function(path: string[, mode: string]): number -- Opens a file")
    fun open(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(null, "invalid path")
        val mode = args.getOrNull(1) as? String ?: "r"
        
        val handle = fs.open(path, mode)
        if (handle < 0) return arrayOf(null, "no such file")
        return arrayOf(handle)
    }
    
    @Callback(doc = "function(handle: number, count: number): string -- Reads from a file")
    fun read(context: Context, args: Array<Any?>): Array<Any?> {
        val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid handle")
        val count = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(null, "invalid count")
        
        val data = fs.read(handle, count) ?: return arrayOf(null)
        return arrayOf(String(data, StandardCharsets.UTF_8))
    }
    
    @Callback(doc = "function(handle: number, data: string): boolean -- Writes to a file")
    fun write(context: Context, args: Array<Any?>): Array<Any?> {
        val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid handle")
        val data = (args.getOrNull(1) as? String ?: "").toByteArray(StandardCharsets.UTF_8)
        
        return arrayOf(fs.write(handle, data))
    }
    
    @Callback(doc = "function(handle: number, whence: string, offset: number): number -- Seeks in a file")
    fun seek(context: Context, args: Array<Any?>): Array<Any?> {
        val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(null, "invalid handle")
        val whence = when (args.getOrNull(1) as? String ?: "cur") {
            "set" -> 0
            "cur" -> 1
            "end" -> 2
            else -> return arrayOf(null, "invalid whence")
        }
        val offset = (args.getOrNull(2) as? Number)?.toLong() ?: 0
        
        return arrayOf(fs.seek(handle, offset, whence))
    }
    
    @Callback(doc = "function(handle: number): void -- Closes a file")
    fun close(context: Context, args: Array<Any?>): Array<Any?> {
        val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf()
        fs.close(handle)
        return arrayOf()
    }
    
    @Callback(doc = "function(path: string): boolean -- Creates a directory")
    fun makeDirectory(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(false)
        return arrayOf(fs.makeDirectory(path))
    }
    
    @Callback(doc = "function(path: string): boolean -- Removes a file or directory")
    fun remove(context: Context, args: Array<Any?>): Array<Any?> {
        val path = args.getOrNull(0) as? String ?: return arrayOf(false)
        return arrayOf(fs.delete(path))
    }
    
    @Callback(doc = "function(from: string, to: string): boolean -- Renames a file or directory")
    fun rename(context: Context, args: Array<Any?>): Array<Any?> {
        val from = args.getOrNull(0) as? String ?: return arrayOf(false)
        val to = args.getOrNull(1) as? String ?: return arrayOf(false)
        return arrayOf(fs.rename(from, to))
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putInt("tier", tier)
        label?.let { tag.putString("label", it) }
        
        val fsTag = CompoundTag()
        fs.save(fsTag)
        tag.put("fs", fsTag)
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        label = if (tag.contains("label")) tag.getString("label") else null
        
        if (tag.contains("fs")) {
            fs.load(tag.getCompound("fs"))
        }
    }
}

/**
 * Geolyzer component - analyzes terrain and blocks.
 */
class GeolyzerComponent : ComponentBase("geolyzer") {
    
    @Callback(doc = "function(x: number, z: number[, y: number, w: number, d: number, h: number, ignoreReplaceable: boolean, options: table]): table -- Scans an area")
    fun scan(context: Context, args: Array<Any?>): Array<Any?> {
        val x = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val z = (args.getOrNull(1) as? Number)?.toInt() ?: 0
        val y = (args.getOrNull(2) as? Number)?.toInt() ?: -32
        val w = (args.getOrNull(3) as? Number)?.toInt()?.coerceIn(1, 64) ?: 1
        val d = (args.getOrNull(4) as? Number)?.toInt()?.coerceIn(1, 64) ?: 1
        val h = (args.getOrNull(5) as? Number)?.toInt()?.coerceIn(1, 64) ?: 64
        
        // Would scan actual blocks in real implementation
        val result = mutableListOf<Double>()
        for (py in y until y + h) {
            for (pz in z until z + d) {
                for (px in x until x + w) {
                    // Return hardness values (0 for air, positive for blocks)
                    result.add(0.0)
                }
            }
        }
        
        return arrayOf(result)
    }
    
    @Callback(doc = "function(side: number[, options: table]): table -- Analyzes a block")
    fun analyze(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 3
        
        // Would analyze actual block in real implementation
        return arrayOf(mapOf(
            "name" to "minecraft:air",
            "hardness" to 0.0,
            "harvestLevel" to 0,
            "harvestTool" to ""
        ))
    }
    
    @Callback(doc = "function(x: number, y: number, z: number[, count: number]): table -- Stores scan results for later retrieval")
    fun store(context: Context, args: Array<Any?>): Array<Any?> {
        // Would store data to hardware
        return arrayOf(true)
    }
}

/**
 * Debug component - provides creative/debug functionality.
 */
class DebugComponent : ComponentBase("debug") {
    
    @Callback(doc = "function(): table -- Gets info about the connected player")
    fun getPlayer(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(null)
    }
    
    @Callback(doc = "function(): table -- Gets the world info")
    fun getWorld(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(mapOf(
            "name" to "world",
            "dimension" to "minecraft:overworld"
        ))
    }
    
    @Callback(doc = "function(x: number, y: number, z: number): table -- Gets block at position")
    fun getBlock(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(mapOf("name" to "minecraft:air"))
    }
    
    @Callback(doc = "function(x: number, y: number, z: number, name: string[, meta: number]): boolean -- Sets block at position")
    fun setBlock(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(command: string): number -- Runs a command")
    fun runCommand(context: Context, args: Array<Any?>): Array<Any?> {
        // Would execute command in actual implementation
        return arrayOf(1)
    }
    
    @Callback(doc = "function(): boolean -- Checks if debug mode is enabled")
    fun isDebugEnabled(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
}

/**
 * Chunkloader component - keeps chunks loaded.
 */
class ChunkloaderComponent : ComponentBase("chunkloader") {
    
    private var active = false
    
    @Callback(doc = "function(): boolean -- Checks if the chunkloader is active")
    fun isActive(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(active)
    }
    
    @Callback(doc = "function(active: boolean): boolean -- Sets whether the chunkloader is active")
    fun setActive(context: Context, args: Array<Any?>): Array<Any?> {
        val newActive = args.getOrNull(0) as? Boolean ?: true
        val old = active
        active = newActive
        return arrayOf(old)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putBoolean("active", active)
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        active = tag.getBoolean("active")
    }
}

/**
 * Sign component - reads and writes to signs.
 */
class SignComponent : ComponentBase("sign") {
    
    @Callback(doc = "function(): string -- Gets the text on the sign in front")
    fun getValue(context: Context, args: Array<Any?>): Array<Any?> {
        // Would read actual sign in real implementation
        return arrayOf("")
    }
    
    @Callback(doc = "function(value: string): string -- Sets the text on the sign in front")
    fun setValue(context: Context, args: Array<Any?>): Array<Any?> {
        val value = args.getOrNull(0) as? String ?: ""
        // Would write to actual sign
        return arrayOf(value)
    }
}

/**
 * Database component - stores and queries item/fluid templates.
 */
class DatabaseComponent(val tier: Int = 1) : ComponentBase("database") {
    
    val size: Int get() = when (tier) {
        1 -> 9
        2 -> 25
        else -> 81
    }
    
    private val entries = arrayOfNulls<ItemTemplate>(size)
    
    data class ItemTemplate(
        val name: String,
        val damage: Int = 0,
        val nbt: CompoundTag? = null
    )
    
    @Callback(doc = "function(slot: number): table -- Gets the entry at a slot")
    fun get(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return arrayOf(null)
        if (slot !in entries.indices) return arrayOf(null)
        
        val entry = entries[slot] ?: return arrayOf(null)
        return arrayOf(mapOf(
            "name" to entry.name,
            "damage" to entry.damage,
            "hasTag" to (entry.nbt != null)
        ))
    }
    
    @Callback(doc = "function(address: string, slot: number, dbSlot: number): boolean -- Copies from inventory to database")
    fun computeHash(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return arrayOf("")
        if (slot !in entries.indices) return arrayOf("")
        
        val entry = entries[slot] ?: return arrayOf("")
        val hash = entry.name.hashCode().toString(16)
        return arrayOf(hash)
    }
    
    @Callback(doc = "function(fromSlot: number, toSlot: number): boolean -- Copies an entry")
    fun copy(context: Context, args: Array<Any?>): Array<Any?> {
        val from = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return arrayOf(false)
        val to = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: return arrayOf(false)
        
        if (from !in entries.indices || to !in entries.indices) return arrayOf(false)
        
        entries[to] = entries[from]?.copy()
        return arrayOf(true)
    }
    
    @Callback(doc = "function(slot: number): boolean -- Clears an entry")
    fun clear(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return arrayOf(false)
        if (slot !in entries.indices) return arrayOf(false)
        
        entries[slot] = null
        return arrayOf(true)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putInt("tier", tier)
        // Save entries...
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        // Load entries...
    }
}

/**
 * Transposer component - moves items and fluids between inventories.
 */
class TransposerComponent : ComponentBase("transposer") {
    
    @Callback(doc = "function(sourceSide: number, sinkSide: number, count: number, sourceSlot: number, sinkSlot: number): number -- Transfers items")
    fun transferItem(context: Context, args: Array<Any?>): Array<Any?> {
        val sourceSide = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(0)
        val sinkSide = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(0)
        val count = (args.getOrNull(2) as? Number)?.toInt() ?: 64
        val sourceSlot = (args.getOrNull(3) as? Number)?.toInt()
        val sinkSlot = (args.getOrNull(4) as? Number)?.toInt()
        
        // Would transfer items in actual implementation
        return arrayOf(0)
    }
    
    @Callback(doc = "function(sourceSide: number, sinkSide: number, amount: number): boolean, number -- Transfers fluid")
    fun transferFluid(context: Context, args: Array<Any?>): Array<Any?> {
        val sourceSide = (args.getOrNull(0) as? Number)?.toInt() ?: return arrayOf(false, 0)
        val sinkSide = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(false, 0)
        val amount = (args.getOrNull(2) as? Number)?.toInt() ?: 1000
        
        // Would transfer fluid in actual implementation
        return arrayOf(false, 0)
    }
    
    @Callback(doc = "function(side: number): number -- Gets the inventory size on a side")
    fun getInventorySize(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(0)
    }
    
    @Callback(doc = "function(side: number, slot: number): table -- Gets info about an item")
    fun getSlotStackSize(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(0)
    }
    
    @Callback(doc = "function(side: number): number -- Gets the number of fluid tanks on a side")
    fun getTankCount(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(0)
    }
    
    @Callback(doc = "function(side: number, tank: number): table -- Gets info about a fluid tank")
    fun getFluidInTank(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(null)
    }
}

/**
 * World sensor component - detects world information.
 */
class WorldSensorComponent : ComponentBase("world_sensor") {
    
    private var level: Level? = null
    private var pos: BlockPos = BlockPos.ZERO
    
    fun setContext(level: Level, pos: BlockPos) {
        this.level = level
        this.pos = pos
    }
    
    @Callback(doc = "function(): number -- Gets the current world time")
    fun getWorldTime(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(level?.dayTime() ?: 0L)
    }
    
    @Callback(doc = "function(): boolean -- Checks if it's raining")
    fun isRaining(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(level?.isRaining == true)
    }
    
    @Callback(doc = "function(): boolean -- Checks if it's thundering")
    fun isThundering(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(level?.isThundering == true)
    }
    
    @Callback(doc = "function(): number -- Gets the light level")
    fun getLightLevel(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(level?.getBrightness(pos) ?: 0)
    }
    
    @Callback(doc = "function(): boolean -- Checks if the sky is visible")
    fun canSeeSky(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(level?.canSeeSky(pos) ?: false)
    }
    
    @Callback(doc = "function(): string -- Gets the biome name")
    fun getBiome(context: Context, args: Array<Any?>): Array<Any?> {
        val biomeHolder = level?.getBiome(pos)
        val biomeKey = biomeHolder?.unwrapKey()?.orElse(null)
        return arrayOf(biomeKey?.location()?.toString() ?: "unknown")
    }
    
    @Callback(doc = "function(): number -- Gets the temperature")
    fun getTemperature(context: Context, args: Array<Any?>): Array<Any?> {
        val biome = level?.getBiome(pos)?.value()
        return arrayOf(biome?.getBaseTemperature() ?: 0.5f)
    }
    
    @Callback(doc = "function(): number -- Gets the humidity")
    fun getHumidity(context: Context, args: Array<Any?>): Array<Any?> {
        // Humidity is no longer directly available in modern MC
        return arrayOf(0.5)
    }
}

/**
 * Motion sensor component - detects entity movement.
 */
class MotionSensorComponent : ComponentBase("motion_sensor") {
    
    private var sensitivity = 0.5
    
    @Callback(doc = "function(): number -- Gets the sensitivity")
    fun getSensitivity(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(sensitivity)
    }
    
    @Callback(doc = "function(value: number): number -- Sets the sensitivity")
    fun setSensitivity(context: Context, args: Array<Any?>): Array<Any?> {
        val old = sensitivity
        sensitivity = ((args.getOrNull(0) as? Number)?.toDouble() ?: 0.5).coerceIn(0.2, 1.0)
        return arrayOf(old)
    }
}

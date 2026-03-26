package li.cil.oc.server.component.upgrades

import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.*
import li.cil.oc.server.component.ManagedEnvironment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.security.MessageDigest
import java.util.*
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.math.*

/**
 * Data Card component - provides cryptographic and data manipulation functions
 */
class DataCardUpgrade(val tier: Int) : ManagedEnvironment {
    override val node: Node = ComponentNetwork.createNode(this, "data")
    
    private val supportedHashes = when (tier) {
        1 -> setOf("md5", "sha256")
        2 -> setOf("md5", "sha256", "sha512", "crc32")
        else -> setOf("md5", "sha256", "sha512", "sha3-256", "crc32")
    }
    
    private val hasCompression = tier >= 2
    private val hasEncryption = tier >= 3
    private val hasECDH = tier >= 3
    private val hasECDSA = tier >= 3
    
    @Callback(doc = "function(data:string, algorithm?:string):string -- Compute hash of data")
    fun hash(context: Context, args: Arguments): Array<Any?> {
        val data = args.checkByteArray(0)
        val algorithm = args.optString(1, "sha256")
        
        if (!supportedHashes.contains(algorithm.lowercase())) {
            return arrayOf(null, "unsupported algorithm")
        }
        
        return try {
            val hash = when (algorithm.lowercase()) {
                "crc32" -> {
                    val crc = java.util.zip.CRC32()
                    crc.update(data)
                    crc.value.toString(16).padStart(8, '0').toByteArray()
                }
                else -> {
                    val md = MessageDigest.getInstance(algorithm.uppercase().replace("-", ""))
                    md.digest(data)
                }
            }
            arrayOf(hash)
        } catch (e: Exception) {
            arrayOf(null, e.message)
        }
    }
    
    @Callback(doc = "function(data:string):string -- Encode data to Base64")
    fun encode64(context: Context, args: Arguments): Array<Any?> {
        val data = args.checkByteArray(0)
        return arrayOf(Base64.getEncoder().encodeToString(data))
    }
    
    @Callback(doc = "function(data:string):string -- Decode Base64 data")
    fun decode64(context: Context, args: Arguments): Array<Any?> {
        val data = args.checkString(0)
        return try {
            arrayOf(Base64.getDecoder().decode(data))
        } catch (e: Exception) {
            arrayOf(null, "invalid base64")
        }
    }
    
    @Callback(doc = "function(data:string):string -- Compress data using deflate")
    fun deflate(context: Context, args: Arguments): Array<Any?> {
        if (!hasCompression) return arrayOf(null, "not supported")
        
        val data = args.checkByteArray(0)
        val level = args.optInteger(1, Deflater.DEFAULT_COMPRESSION)
        
        return try {
            val deflater = Deflater(level.coerceIn(0, 9))
            deflater.setInput(data)
            deflater.finish()
            
            val buffer = ByteArray(data.size + 256)
            val compressedSize = deflater.deflate(buffer)
            deflater.end()
            
            arrayOf(buffer.copyOf(compressedSize))
        } catch (e: Exception) {
            arrayOf(null, e.message)
        }
    }
    
    @Callback(doc = "function(data:string):string -- Decompress deflated data")
    fun inflate(context: Context, args: Arguments): Array<Any?> {
        if (!hasCompression) return arrayOf(null, "not supported")
        
        val data = args.checkByteArray(0)
        
        return try {
            val inflater = Inflater()
            inflater.setInput(data)
            
            val chunks = mutableListOf<ByteArray>()
            val buffer = ByteArray(4096)
            
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count > 0) {
                    chunks.add(buffer.copyOf(count))
                }
            }
            inflater.end()
            
            val result = ByteArray(chunks.sumOf { it.size })
            var offset = 0
            for (chunk in chunks) {
                chunk.copyInto(result, offset)
                offset += chunk.size
            }
            
            arrayOf(result)
        } catch (e: Exception) {
            arrayOf(null, e.message)
        }
    }
    
    @Callback(doc = "function(count:number):string -- Generate random bytes")
    fun random(context: Context, args: Arguments): Array<Any?> {
        val count = args.checkInteger(0).coerceIn(1, 1024)
        val random = java.security.SecureRandom()
        val bytes = ByteArray(count)
        random.nextBytes(bytes)
        return arrayOf(bytes)
    }
    
    @Callback(doc = "function():table -- Get supported features")
    fun getFeatures(context: Context, args: Arguments): Array<Any?> {
        return arrayOf(mapOf(
            "hashes" to supportedHashes.toList(),
            "compression" to hasCompression,
            "encryption" to hasEncryption,
            "ecdh" to hasECDH,
            "ecdsa" to hasECDSA
        ))
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Navigation component - provides GPS-like positioning
 */
class NavigationUpgrade(
    private val level: () -> Level?,
    private val pos: () -> BlockPos,
    val tier: Int
) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "navigation")
    
    private val range: Int = when (tier) {
        1 -> 64
        2 -> 128
        else -> 256
    }
    
    @Callback(doc = "function():number,number,number -- Get current position")
    fun getPosition(context: Context, args: Arguments): Array<Any?> {
        val currentPos = pos()
        return arrayOf(currentPos.x.toDouble(), currentPos.y.toDouble(), currentPos.z.toDouble())
    }
    
    @Callback(doc = "function():number -- Get current facing direction")
    fun getFacing(context: Context, args: Arguments): Array<Any?> {
        return arrayOf(0)
    }
    
    @Callback(doc = "function():number -- Get range of this navigation upgrade")
    fun getRange(context: Context, args: Arguments): Array<Any?> {
        return arrayOf(range)
    }
    
    @Callback(doc = "function(x:number, y:number, z:number):number -- Get distance to target")
    fun getDistance(context: Context, args: Arguments): Array<Any?> {
        val tx = args.checkDouble(0)
        val ty = args.checkDouble(1)
        val tz = args.checkDouble(2)
        
        val currentPos = pos()
        val dx = tx - currentPos.x
        val dy = ty - currentPos.y
        val dz = tz - currentPos.z
        
        return arrayOf(sqrt(dx * dx + dy * dy + dz * dz))
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Geolyzer component - scans terrain
 */
class GeolyzerUpgrade(
    private val level: () -> Level?,
    private val pos: () -> BlockPos
) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "geolyzer")
    
    companion object {
        const val SCAN_RANGE = 32
        const val SCAN_HEIGHT = 64
    }
    
    @Callback(doc = "function(x:number, z:number, y?:number, w?:number, d?:number, h?:number):table -- Scan area")
    fun scan(context: Context, args: Arguments): Array<Any?> {
        val world = level() ?: return arrayOf(null, "no world")
        val basePos = pos()
        
        val offsetX = args.checkInteger(0).coerceIn(-SCAN_RANGE, SCAN_RANGE)
        val offsetZ = args.checkInteger(1).coerceIn(-SCAN_RANGE, SCAN_RANGE)
        val offsetY = args.optInteger(2, 0).coerceIn(-SCAN_RANGE, SCAN_RANGE)
        val width = args.optInteger(3, 1).coerceIn(1, 8)
        val depth = args.optInteger(4, 1).coerceIn(1, 8)
        val height = args.optInteger(5, 1).coerceIn(1, SCAN_HEIGHT)
        
        val result = mutableListOf<Double>()
        
        for (y in 0 until height) {
            for (z in 0 until depth) {
                for (x in 0 until width) {
                    val scanPos = basePos.offset(offsetX + x, offsetY + y, offsetZ + z)
                    val state = world.getBlockState(scanPos)
                    
                    val hardness = if (state.isAir) {
                        0.0
                    } else {
                        state.getDestroySpeed(world, scanPos).toDouble().coerceAtLeast(-1.0)
                    }
                    result.add(hardness)
                }
            }
        }
        
        return arrayOf(mapOf(
            "data" to result,
            "width" to width,
            "depth" to depth,
            "height" to height
        ))
    }
    
    @Callback(doc = "function(side:number):table -- Analyze block on specified side")
    fun analyze(context: Context, args: Arguments): Array<Any?> {
        val side = args.checkInteger(0)
        val world = level() ?: return arrayOf(null, "no world")
        val basePos = pos()
        
        val direction = Direction.from3DDataValue(side.coerceIn(0, 5))
        val targetPos = basePos.relative(direction)
        
        val state = world.getBlockState(targetPos)
        val block = state.block
        
        return arrayOf(mapOf(
            "name" to block.descriptionId,
            "hardness" to state.getDestroySpeed(world, targetPos).toDouble()
        ))
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Motion Sensor component - detects entity movement
 */
class MotionSensorUpgrade(
    private val level: () -> Level?,
    private val pos: () -> BlockPos
) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "motion_sensor")
    
    private var sensitivity: Double = 0.5
    private val trackedEntities = mutableMapOf<UUID, Vec3>()
    
    companion object {
        const val RANGE = 8.0
    }
    
    @Callback(doc = "function(sensitivity:number):number -- Set motion detection sensitivity")
    fun setSensitivity(context: Context, args: Arguments): Array<Any?> {
        sensitivity = args.checkDouble(0).coerceIn(0.1, 1.0)
        return arrayOf(sensitivity)
    }
    
    @Callback(doc = "function():number -- Get current sensitivity")
    fun getSensitivity(context: Context, args: Arguments): Array<Any?> {
        return arrayOf(sensitivity)
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {
        tag.putDouble("sensitivity", sensitivity)
    }
    
    override fun loadData(tag: CompoundTag) {
        sensitivity = tag.getDouble("sensitivity").takeIf { it > 0 } ?: 0.5
    }
}

/**
 * Tractor Beam component - picks up items
 */
class TractorBeamUpgrade(
    private val level: () -> Level?,
    private val pos: () -> BlockPos
) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "tractor_beam")
    
    companion object {
        const val RANGE = 3.0
        const val ENERGY_COST = 10.0
    }
    
    @Callback(doc = "function():boolean -- Activate tractor beam to pull nearby items")
    fun suck(context: Context, args: Arguments): Array<Any?> {
        val world = level() ?: return arrayOf(false, "no world")
        val basePos = pos()
        val center = Vec3.atCenterOf(basePos)
        
        val aabb = AABB.ofSize(center, RANGE * 2, RANGE * 2, RANGE * 2)
        val items = world.getEntitiesOfClass(ItemEntity::class.java, aabb)
        
        if (items.isEmpty()) {
            return arrayOf(false, "no items in range")
        }
        
        val closest = items.minByOrNull { it.distanceToSqr(center) } ?: return arrayOf(false)
        
        val direction = center.subtract(closest.position()).normalize().scale(0.5)
        closest.deltaMovement = closest.deltaMovement.add(direction)
        
        return arrayOf(true)
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Experience component - stores and manages experience
 */
class ExperienceUpgrade : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "experience")
    
    private var storedXP: Int = 0
    
    companion object {
        const val MAX_LEVEL = 30
        
        fun xpForLevel(level: Int): Int {
            return when {
                level <= 16 -> level * level + 6 * level
                level <= 31 -> (2.5 * level * level - 40.5 * level + 360).toInt()
                else -> (4.5 * level * level - 162.5 * level + 2220).toInt()
            }
        }
        
        fun levelForXP(xp: Int): Int {
            var level = 0
            while (xpForLevel(level + 1) <= xp && level < MAX_LEVEL) {
                level++
            }
            return level
        }
    }
    
    @Callback(doc = "function():number -- Get current stored XP")
    fun get(context: Context, args: Arguments): Array<Any?> {
        return arrayOf(storedXP)
    }
    
    @Callback(doc = "function():number -- Get current level from stored XP")
    fun level(context: Context, args: Arguments): Array<Any?> {
        return arrayOf(levelForXP(storedXP))
    }
    
    @Callback(doc = "function():number -- Get XP required for next level")
    fun xpForNextLevel(context: Context, args: Arguments): Array<Any?> {
        val currentLevel = levelForXP(storedXP)
        if (currentLevel >= MAX_LEVEL) return arrayOf(0)
        return arrayOf(xpForLevel(currentLevel + 1) - storedXP)
    }
    
    @Callback(doc = "function(amount:number):number -- Consume XP")
    fun consume(context: Context, args: Arguments): Array<Any?> {
        val amount = args.checkInteger(0).coerceAtLeast(0)
        val consumed = minOf(amount, storedXP)
        storedXP -= consumed
        return arrayOf(consumed)
    }
    
    fun addXP(amount: Int) {
        storedXP = (storedXP + amount).coerceIn(0, xpForLevel(MAX_LEVEL))
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {
        tag.putInt("storedXP", storedXP)
    }
    
    override fun loadData(tag: CompoundTag) {
        storedXP = tag.getInt("storedXP")
    }
}

/**
 * Inventory Controller component - interacts with inventories
 */
class InventoryControllerUpgrade(
    private val level: () -> Level?,
    private val pos: () -> BlockPos,
    private val robotInventory: () -> Container?
) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "inventory_controller")
    
    @Callback(doc = "function(side:number):number -- Get inventory size on side")
    fun getInventorySize(context: Context, args: Arguments): Array<Any?> {
        val side = args.checkInteger(0)
        val inventory = getInventory(side) ?: return arrayOf(0)
        return arrayOf(inventory.containerSize)
    }
    
    @Callback(doc = "function(side:number, slot:number):table -- Get stack info from inventory")
    fun getStackInSlot(context: Context, args: Arguments): Array<Any?> {
        val side = args.checkInteger(0)
        val slot = args.checkInteger(1) - 1 // Convert from 1-indexed
        
        val inventory = getInventory(side) ?: return arrayOf(null, "no inventory")
        if (slot < 0 || slot >= inventory.containerSize) return arrayOf(null, "slot out of range")
        
        val stack = inventory.getItem(slot)
        if (stack.isEmpty) return arrayOf(null)
        
        return arrayOf(stackToTable(stack))
    }
    
    @Callback(doc = "function(side:number):table -- Get all stacks from inventory")
    fun getAllStacks(context: Context, args: Arguments): Array<Any?> {
        val side = args.checkInteger(0)
        val inventory = getInventory(side) ?: return arrayOf(null, "no inventory")
        
        val stacks = mutableListOf<Map<String, Any?>?>()
        for (i in 0 until inventory.containerSize) {
            val stack = inventory.getItem(i)
            stacks.add(if (stack.isEmpty) null else stackToTable(stack))
        }
        
        return arrayOf(stacks)
    }
    
    @Callback(doc = "function(side:number, fromSlot:number, amount:number, toSlot?:number):number -- Transfer items")
    fun suckFromSlot(context: Context, args: Arguments): Array<Any?> {
        val side = args.checkInteger(0)
        val fromSlot = args.checkInteger(1) - 1
        val amount = args.checkInteger(2)
        val toSlot = args.optInteger(3, -1) - 1
        
        val sourceInventory = getInventory(side) ?: return arrayOf(0, "no inventory")
        val targetInventory = robotInventory() ?: return arrayOf(0, "no robot inventory")
        
        if (fromSlot < 0 || fromSlot >= sourceInventory.containerSize) {
            return arrayOf(0, "source slot out of range")
        }
        
        val sourceStack = sourceInventory.getItem(fromSlot)
        if (sourceStack.isEmpty) return arrayOf(0)
        
        val toTransfer = minOf(amount, sourceStack.count)
        val transferStack = sourceStack.copyWithCount(toTransfer)
        
        // Try to insert into target inventory
        var remaining = toTransfer
        if (toSlot >= 0 && toSlot < targetInventory.containerSize) {
            val targetStack = targetInventory.getItem(toSlot)
            if (targetStack.isEmpty) {
                targetInventory.setItem(toSlot, transferStack)
                remaining = 0
            } else if (ItemStack.isSameItemSameComponents(targetStack, transferStack)) {
                val canAdd = targetStack.maxStackSize - targetStack.count
                val added = minOf(canAdd, remaining)
                targetStack.grow(added)
                remaining -= added
            }
        } else {
            // Find any suitable slot
            for (i in 0 until targetInventory.containerSize) {
                if (remaining <= 0) break
                
                val targetStack = targetInventory.getItem(i)
                if (targetStack.isEmpty) {
                    targetInventory.setItem(i, transferStack.copyWithCount(remaining))
                    remaining = 0
                } else if (ItemStack.isSameItemSameComponents(targetStack, transferStack)) {
                    val canAdd = targetStack.maxStackSize - targetStack.count
                    val added = minOf(canAdd, remaining)
                    targetStack.grow(added)
                    remaining -= added
                }
            }
        }
        
        val transferred = toTransfer - remaining
        sourceStack.shrink(transferred)
        if (sourceStack.isEmpty) {
            sourceInventory.setItem(fromSlot, ItemStack.EMPTY)
        }
        
        return arrayOf(transferred)
    }
    
    @Callback(doc = "function(side:number, slot:number, amount:number, fromSlot?:number):number -- Drop items into inventory")
    fun dropIntoSlot(context: Context, args: Arguments): Array<Any?> {
        val side = args.checkInteger(0)
        val toSlot = args.checkInteger(1) - 1
        val amount = args.checkInteger(2)
        val fromSlot = args.optInteger(3, -1) - 1
        
        val targetInventory = getInventory(side) ?: return arrayOf(0, "no inventory")
        val sourceInventory = robotInventory() ?: return arrayOf(0, "no robot inventory")
        
        if (toSlot < 0 || toSlot >= targetInventory.containerSize) {
            return arrayOf(0, "target slot out of range")
        }
        
        // Get source stack
        val sourceSlot = if (fromSlot >= 0) fromSlot else {
            (0 until sourceInventory.containerSize).firstOrNull { 
                !sourceInventory.getItem(it).isEmpty 
            } ?: return arrayOf(0, "no items to drop")
        }
        
        if (sourceSlot >= sourceInventory.containerSize) {
            return arrayOf(0, "source slot out of range")
        }
        
        val sourceStack = sourceInventory.getItem(sourceSlot)
        if (sourceStack.isEmpty) return arrayOf(0)
        
        val toTransfer = minOf(amount, sourceStack.count)
        val targetStack = targetInventory.getItem(toSlot)
        
        var transferred = 0
        if (targetStack.isEmpty) {
            targetInventory.setItem(toSlot, sourceStack.copyWithCount(toTransfer))
            transferred = toTransfer
        } else if (ItemStack.isSameItemSameComponents(targetStack, sourceStack)) {
            val canAdd = targetStack.maxStackSize - targetStack.count
            transferred = minOf(canAdd, toTransfer)
            targetStack.grow(transferred)
        }
        
        sourceStack.shrink(transferred)
        if (sourceStack.isEmpty) {
            sourceInventory.setItem(sourceSlot, ItemStack.EMPTY)
        }
        
        return arrayOf(transferred)
    }
    
    private fun getInventory(side: Int): Container? {
        val world = level() ?: return null
        val basePos = pos()
        val direction = Direction.from3DDataValue(side.coerceIn(0, 5))
        val targetPos = basePos.relative(direction)
        
        return world.getBlockEntity(targetPos) as? Container
    }
    
    private fun stackToTable(stack: ItemStack): Map<String, Any?> {
        return mapOf(
            "name" to stack.item.descriptionId,
            "count" to stack.count,
            "damage" to stack.damageValue,
            "maxDamage" to stack.maxDamage,
            "maxStackSize" to stack.maxStackSize,
            "hasTag" to stack.hasTag()
        )
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Tank Controller component - interacts with fluid tanks
 */
class TankControllerUpgrade(
    private val level: () -> Level?,
    private val pos: () -> BlockPos
) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "tank_controller")
    
    @Callback(doc = "function(side:number):number -- Get number of tanks on side")
    fun getTankCount(context: Context, args: Arguments): Array<Any?> {
        // Would check for forge fluid capability
        return arrayOf(0)
    }
    
    @Callback(doc = "function(side:number, tank:number):table -- Get fluid info from tank")
    fun getFluidInTank(context: Context, args: Arguments): Array<Any?> {
        // Would use forge fluid capability
        return arrayOf(null, "no fluid handler")
    }
    
    @Callback(doc = "function(side:number,amount:number):boolean,number -- Transfer fluid from tank")
    fun drain(context: Context, args: Arguments): Array<Any?> {
        // Would transfer fluids
        return arrayOf(false, 0)
    }
    
    @Callback(doc = "function(side:number,amount:number):boolean,number -- Transfer fluid to tank")
    fun fill(context: Context, args: Arguments): Array<Any?> {
        // Would transfer fluids
        return arrayOf(false, 0)
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Generator component - generates power from fuel
 */
class GeneratorUpgrade(
    val tier: Int,
    private val energyBuffer: () -> EnergyBuffer?
) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "generator")
    
    private var fuelTicks: Int = 0
    private val efficiency: Double = when (tier) {
        1 -> 0.8
        2 -> 0.9
        else -> 1.0
    }
    
    interface EnergyBuffer {
        fun addEnergy(amount: Double): Double
        fun getEnergyStored(): Double
        fun getMaxEnergy(): Double
    }
    
    @Callback(doc = "function():number -- Get remaining fuel ticks")
    fun count(context: Context, args: Arguments): Array<Any?> {
        return arrayOf(fuelTicks)
    }
    
    @Callback(doc = "function(count:number):boolean -- Insert fuel items")
    fun insert(context: Context, args: Arguments): Array<Any?> {
        val count = args.checkInteger(0).coerceAtLeast(0)
        // Would check fuel value and add to buffer
        // Coal = 1600 ticks, etc
        return arrayOf(true)
    }
    
    @Callback(doc = "function():boolean -- Remove all fuel")
    fun remove(context: Context, args: Arguments): Array<Any?> {
        val removed = fuelTicks > 0
        fuelTicks = 0
        return arrayOf(removed)
    }
    
    fun tick() {
        if (fuelTicks > 0) {
            fuelTicks--
            val buffer = energyBuffer()
            if (buffer != null) {
                val generated = 10.0 * efficiency // Energy per tick
                buffer.addEnergy(generated)
            }
        }
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {
        tag.putInt("fuelTicks", fuelTicks)
    }
    
    override fun loadData(tag: CompoundTag) {
        fuelTicks = tag.getInt("fuelTicks")
    }
}

/**
 * Solar Generator component - generates power from sunlight
 */
class SolarGeneratorUpgrade(
    val tier: Int,
    private val level: () -> Level?,
    private val pos: () -> BlockPos,
    private val energyBuffer: () -> GeneratorUpgrade.EnergyBuffer?
) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "solar_generator")
    
    private val output: Double = when (tier) {
        1 -> 1.0
        2 -> 2.0
        else -> 4.0
    }
    
    @Callback(doc = "function():boolean -- Check if generator can see sky")
    fun canSeeSky(context: Context, args: Arguments): Array<Any?> {
        val world = level() ?: return arrayOf(false)
        return arrayOf(world.canSeeSky(pos().above()))
    }
    
    @Callback(doc = "function():boolean -- Check if it's day")
    fun isDay(context: Context, args: Arguments): Array<Any?> {
        val world = level() ?: return arrayOf(false)
        return arrayOf(world.isDay)
    }
    
    fun tick() {
        val world = level() ?: return
        val currentPos = pos()
        
        if (world.isDay && world.canSeeSky(currentPos.above()) && !world.isRaining) {
            val buffer = energyBuffer()
            buffer?.addEnergy(output)
        }
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Crafting component - provides crafting capability
 */
class CraftingUpgrade : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "crafting")
    
    @Callback(doc = "function():boolean -- Attempt to craft using items in robot inventory")
    fun craft(context: Context, args: Arguments): Array<Any?> {
        // Would attempt crafting using 3x3 grid in robot inventory
        return arrayOf(false, "not implemented")
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Piston component - pushes blocks
 */
class PistonUpgrade(
    private val level: () -> Level?,
    private val pos: () -> BlockPos,
    private val facing: () -> Direction
) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "piston")
    
    @Callback(doc = "function(side?:number):boolean -- Push block in front")
    fun push(context: Context, args: Arguments): Array<Any?> {
        val world = level() ?: return arrayOf(false, "no world")
        val basePos = pos()
        val direction = if (args.count() > 0) {
            Direction.from3DDataValue(args.checkInteger(0).coerceIn(0, 5))
        } else {
            facing()
        }
        
        val targetPos = basePos.relative(direction)
        val state = world.getBlockState(targetPos)
        
        if (state.isAir) return arrayOf(false, "no block")
        if (state.getDestroySpeed(world, targetPos) < 0) return arrayOf(false, "unbreakable")
        
        // Would need to implement actual block pushing logic
        return arrayOf(false, "not implemented")
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Angel upgrade - allows placing blocks in mid-air
 */
class AngelUpgrade : ManagedEnvironment {
    override val node: Node = ComponentNetwork.createNode(this, "angel")
    
    // No callbacks - this upgrade is passive
    // Just having it installed allows place() to work without adjacent blocks
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Hover upgrade - allows flying
 */
class HoverUpgrade(val tier: Int) : ManagedEnvironment {
    override val node: Node = ComponentNetwork.createNode(this, "hover")
    
    val maxHeight: Int = when (tier) {
        1 -> 8
        2 -> 16
        else -> 32
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Chunkloader upgrade - keeps chunks loaded
 */
class ChunkloaderUpgrade(
    private val level: () -> Level?,
    private val pos: () -> BlockPos
) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "chunkloader")
    
    private var active = false
    
    @Callback(doc = "function():boolean -- Check if chunkloader is active")
    fun isActive(context: Context, args: Arguments): Array<Any?> {
        return arrayOf(active)
    }
    
    @Callback(doc = "function(enable:boolean):boolean -- Enable/disable chunkloader")
    fun setActive(context: Context, args: Arguments): Array<Any?> {
        active = args.checkBoolean(0)
        // Would register/unregister with forge chunk loading system
        return arrayOf(active)
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {
        active = false
    }
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {
        tag.putBoolean("active", active)
    }
    
    override fun loadData(tag: CompoundTag) {
        active = tag.getBoolean("active")
    }
}

/**
 * Sign upgrade - reads and writes signs
 */
class SignUpgrade(
    private val level: () -> Level?,
    private val pos: () -> BlockPos
) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "sign")
    
    @Callback(doc = "function(side:number):table -- Get text from sign on specified side")
    fun getValue(context: Context, args: Arguments): Array<Any?> {
        val side = args.checkInteger(0)
        val world = level() ?: return arrayOf(null, "no world")
        val basePos = pos()
        
        val direction = Direction.from3DDataValue(side.coerceIn(0, 5))
        val targetPos = basePos.relative(direction)
        
        val blockEntity = world.getBlockEntity(targetPos)
        if (blockEntity is net.minecraft.world.level.block.entity.SignBlockEntity) {
            val lines = (0..3).map { i ->
                blockEntity.getFrontText().getMessage(i, false).string
            }
            return arrayOf(lines)
        }
        
        return arrayOf(null, "no sign found")
    }
    
    @Callback(doc = "function(side:number, lines:table):boolean -- Set text on sign")
    fun setValue(context: Context, args: Arguments): Array<Any?> {
        // Would need to implement sign writing
        return arrayOf(false, "not implemented")
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {}
    override fun loadData(tag: CompoundTag) {}
}

/**
 * Database upgrade - stores item definitions
 */
class DatabaseUpgrade(val tier: Int) : ManagedEnvironment {
    
    override val node: Node = ComponentNetwork.createNode(this, "database")
    
    private val entries: Int = when (tier) {
        1 -> 9
        2 -> 25
        else -> 81
    }
    
    private val database = arrayOfNulls<ItemStack>(entries)
    
    @Callback(doc = "function(slot:number):table -- Get item from database slot")
    fun get(context: Context, args: Arguments): Array<Any?> {
        val slot = args.checkInteger(0) - 1
        if (slot < 0 || slot >= entries) return arrayOf(null, "slot out of range")
        
        val stack = database[slot] ?: return arrayOf(null)
        return arrayOf(mapOf(
            "name" to stack.item.descriptionId,
            "damage" to stack.damageValue,
            "maxDamage" to stack.maxDamage
        ))
    }
    
    @Callback(doc = "function(slot:number):boolean -- Clear database slot")
    fun clear(context: Context, args: Arguments): Array<Any?> {
        val slot = args.checkInteger(0) - 1
        if (slot < 0 || slot >= entries) return arrayOf(false, "slot out of range")
        
        val wasSet = database[slot] != null
        database[slot] = null
        return arrayOf(wasSet)
    }
    
    @Callback(doc = "function(slot:number, dbAddress:string, dbSlot:number):boolean -- Copy entry from another database")
    fun copy(context: Context, args: Arguments): Array<Any?> {
        val toSlot = args.checkInteger(0) - 1
        // Would copy from another database component
        return arrayOf(false, "not implemented")
    }
    
    @Callback(doc = "function(slot:number):string -- Compute hash of entry")
    fun computeHash(context: Context, args: Arguments): Array<Any?> {
        val slot = args.checkInteger(0) - 1
        if (slot < 0 || slot >= entries) return arrayOf(null, "slot out of range")
        
        val stack = database[slot] ?: return arrayOf(null, "empty slot")
        val hash = stack.item.descriptionId.hashCode().toString(16)
        return arrayOf(hash)
    }
    
    @Callback(doc = "function():number -- Get database size")
    fun size(context: Context, args: Arguments): Array<Any?> {
        return arrayOf(entries)
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveData(tag: CompoundTag) {
        for (i in database.indices) {
            val stack = database[i]
            if (stack != null) {
                tag.put("slot$i", stack.save(tag.registryAccess()))
            }
        }
    }
    
    override fun loadData(tag: CompoundTag) {
        // Would need resource manager context
        // for (i in database.indices) {
        //     if (tag.contains("slot$i")) {
        //         database[i] = ItemStack.parse(...).orElse(null)
        //     }
        // }
    }
}

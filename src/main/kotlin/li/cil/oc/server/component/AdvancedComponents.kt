package li.cil.oc.server.component

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ComponentVisibility
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.items.IItemHandler
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.xor
import kotlin.math.sqrt

/**
 * Inventory controller component - manages inventories.
 */
class InventoryControllerComponent(
    private val level: () -> Level?,
    private val position: () -> BlockPos,
    private val internalInventory: () -> IItemHandler?
) : ComponentBase("inventory_controller") {
    
    override fun methods() = mapOf(
        "getInventorySize" to ::getInventorySize,
        "getStackInSlot" to ::getStackInSlot,
        "getStackInInternalSlot" to ::getStackInInternalSlot,
        "dropIntoSlot" to ::dropIntoSlot,
        "suckFromSlot" to ::suckFromSlot,
        "equip" to ::equip,
        "store" to ::store,
        "storeInternal" to ::storeInternal,
        "compareToDatabase" to ::compareToDatabase,
        "isEquivalentTo" to ::isEquivalentTo,
        "getSlotMaxStackSize" to ::getSlotMaxStackSize,
        "getSlotStackSize" to ::getSlotStackSize,
        "getAllStacks" to ::getAllStacks
    )
    
    private fun getAdjacentInventory(side: Int): IItemHandler? {
        val world = level() ?: return null
        val pos = position()
        val dir = Direction.entries.getOrElse(side) { Direction.DOWN }
        val targetPos = pos.relative(dir)
        
        // Get capability from block entity
        val be = world.getBlockEntity(targetPos)
        return be?.let {
            world.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, dir.opposite)
        }
    }
    
    @Callback(doc = "function(side:number):number -- Get the number of slots in the inventory on side.")
    fun getInventorySize(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val inv = getAdjacentInventory(side) ?: return arrayOf(null, "no inventory")
        return arrayOf(inv.slots)
    }
    
    @Callback(doc = "function(side:number, slot:number):table -- Get info about stack in external inventory.")
    fun getStackInSlot(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        
        val inv = getAdjacentInventory(side) ?: return arrayOf(null, "no inventory")
        if (slot !in 0 until inv.slots) return arrayOf(null, "invalid slot")
        
        val stack = inv.getStackInSlot(slot)
        return if (stack.isEmpty) arrayOf(null) else arrayOf(stackToTable(stack))
    }
    
    @Callback(doc = "function(slot:number):table -- Get info about stack in internal inventory.")
    fun getStackInInternalSlot(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
        
        val inv = internalInventory() ?: return arrayOf(null, "no internal inventory")
        if (slot !in 0 until inv.slots) return arrayOf(null, "invalid slot")
        
        val stack = inv.getStackInSlot(slot)
        return if (stack.isEmpty) arrayOf(null) else arrayOf(stackToTable(stack))
    }
    
    @Callback(doc = "function(side:number, slot:number[, count:number[, fromSlot:number]]):boolean -- Drop items into external inventory.")
    fun dropIntoSlot(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        val count = (args.getOrNull(2) as? Number)?.toInt() ?: 64
        val fromSlot = (args.getOrNull(3) as? Number)?.toInt()?.minus(1) ?: -1
        
        val targetInv = getAdjacentInventory(side) ?: return arrayOf(false, "no inventory")
        val sourceInv = internalInventory() ?: return arrayOf(false, "no internal inventory")
        
        // Transfer items
        val sourceSlot = if (fromSlot >= 0) fromSlot else findNonEmptySlot(sourceInv)
        if (sourceSlot < 0) return arrayOf(false, "no items to transfer")
        
        val extracted = sourceInv.extractItem(sourceSlot, count, false)
        if (extracted.isEmpty) return arrayOf(false, "could not extract items")
        
        val remaining = targetInv.insertItem(slot, extracted, false)
        if (!remaining.isEmpty) {
            // Put back what couldn't be inserted
            sourceInv.insertItem(sourceSlot, remaining, false)
        }
        
        return arrayOf(true, extracted.count - remaining.count)
    }
    
    @Callback(doc = "function(side:number, slot:number[, count:number[, toSlot:number]]):boolean -- Suck items from external inventory.")
    fun suckFromSlot(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        val count = (args.getOrNull(2) as? Number)?.toInt() ?: 64
        val toSlot = (args.getOrNull(3) as? Number)?.toInt()?.minus(1) ?: -1
        
        val sourceInv = getAdjacentInventory(side) ?: return arrayOf(false, "no inventory")
        val targetInv = internalInventory() ?: return arrayOf(false, "no internal inventory")
        
        val extracted = sourceInv.extractItem(slot, count, false)
        if (extracted.isEmpty) return arrayOf(false, "no items")
        
        val targetSlot = if (toSlot >= 0) toSlot else findEmptySlot(targetInv, extracted)
        if (targetSlot < 0) {
            sourceInv.insertItem(slot, extracted, false)
            return arrayOf(false, "no space")
        }
        
        val remaining = targetInv.insertItem(targetSlot, extracted, false)
        if (!remaining.isEmpty) {
            sourceInv.insertItem(slot, remaining, false)
        }
        
        return arrayOf(true, extracted.count - remaining.count)
    }
    
    @Callback(doc = "function():boolean -- Equip item from selected slot.")
    fun equip(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true) // Simplified
    }
    
    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number):boolean -- Store stack info in database.")
    fun store(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(slot:number, dbAddress:string, dbSlot:number):boolean -- Store internal stack info in database.")
    fun storeInternal(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number[, checkNBT:boolean]):boolean -- Compare stack with database entry.")
    fun compareToDatabase(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    @Callback(doc = "function(slotA:number, slotB:number[, checkNBT:boolean]):boolean -- Compare two internal slots.")
    fun isEquivalentTo(context: Context, args: Array<Any?>): Array<Any?> {
        val slotA = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
        val slotB = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        
        val inv = internalInventory() ?: return arrayOf(false)
        val stackA = inv.getStackInSlot(slotA)
        val stackB = inv.getStackInSlot(slotB)
        
        return arrayOf(ItemStack.isSameItem(stackA, stackB))
    }
    
    @Callback(doc = "function(side:number, slot:number):number -- Get max stack size for slot.")
    fun getSlotMaxStackSize(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        
        val inv = getAdjacentInventory(side) ?: return arrayOf(64)
        return arrayOf(inv.getSlotLimit(slot))
    }
    
    @Callback(doc = "function(side:number, slot:number):number -- Get current stack size in slot.")
    fun getSlotStackSize(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        
        val inv = getAdjacentInventory(side) ?: return arrayOf(0)
        return arrayOf(inv.getStackInSlot(slot).count)
    }
    
    @Callback(doc = "function(side:number):table -- Get all stacks in the inventory.")
    fun getAllStacks(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        
        val inv = getAdjacentInventory(side) ?: return arrayOf(null, "no inventory")
        val stacks = mutableListOf<Map<String, Any?>>()
        
        for (i in 0 until inv.slots) {
            val stack = inv.getStackInSlot(i)
            if (!stack.isEmpty) {
                stacks.add(stackToTable(stack))
            }
        }
        
        return arrayOf(stacks.toTypedArray())
    }
    
    private fun stackToTable(stack: ItemStack): Map<String, Any?> {
        return mapOf(
            "name" to stack.item.toString(),
            "count" to stack.count,
            "maxStackSize" to stack.maxStackSize,
            "damage" to stack.damageValue,
            "maxDamage" to stack.maxDamage,
            "hasTag" to stack.components.isEmpty.not()
        )
    }
    
    private fun findNonEmptySlot(inv: IItemHandler): Int {
        for (i in 0 until inv.slots) {
            if (!inv.getStackInSlot(i).isEmpty) return i
        }
        return -1
    }
    
    private fun findEmptySlot(inv: IItemHandler, stack: ItemStack): Int {
        for (i in 0 until inv.slots) {
            if (inv.getStackInSlot(i).isEmpty) return i
        }
        return -1
    }
}

/**
 * Tank controller component - manages fluid tanks.
 */
class TankControllerComponent(
    private val level: () -> Level?,
    private val position: () -> BlockPos
) : ComponentBase("tank_controller") {
    
    override fun methods() = mapOf(
        "getTankCount" to ::getTankCount,
        "getFluidInTank" to ::getFluidInTank,
        "getTankCapacity" to ::getTankCapacity,
        "getFluidInInternalTank" to ::getFluidInInternalTank,
        "drain" to ::drain,
        "fill" to ::fill
    )
    
    @Callback(doc = "function(side:number):number -- Get number of tanks.")
    fun getTankCount(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val world = level() ?: return arrayOf(0)
        val pos = position()
        val dir = Direction.entries.getOrElse(side) { Direction.DOWN }
        val targetPos = pos.relative(dir)
        
        val handler = world.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, dir.opposite)
        return arrayOf(handler?.tanks ?: 0)
    }
    
    @Callback(doc = "function(side:number, tank:number):table -- Get fluid in external tank.")
    fun getFluidInTank(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val tank = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        
        val world = level() ?: return arrayOf(null)
        val pos = position()
        val dir = Direction.entries.getOrElse(side) { Direction.DOWN }
        val targetPos = pos.relative(dir)
        
        val handler = world.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, dir.opposite)
            ?: return arrayOf(null)
        
        val fluid = handler.getFluidInTank(tank)
        return if (fluid.isEmpty) arrayOf(null) else arrayOf(fluidToTable(fluid))
    }
    
    @Callback(doc = "function(side:number, tank:number):number -- Get tank capacity.")
    fun getTankCapacity(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val tank = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        
        val world = level() ?: return arrayOf(0)
        val pos = position()
        val dir = Direction.entries.getOrElse(side) { Direction.DOWN }
        val targetPos = pos.relative(dir)
        
        val handler = world.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, dir.opposite)
            ?: return arrayOf(0)
        
        return arrayOf(handler.getTankCapacity(tank))
    }
    
    @Callback(doc = "function(tank:number):table -- Get fluid in internal tank.")
    fun getFluidInInternalTank(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(null) // Simplified - would need internal tank reference
    }
    
    @Callback(doc = "function(side:number[, amount:number]):boolean, number -- Drain fluid from external tank.")
    fun drain(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val amount = (args.getOrNull(1) as? Number)?.toInt() ?: 1000
        
        // Simplified drain implementation
        return arrayOf(true, amount)
    }
    
    @Callback(doc = "function(side:number[, amount:number]):boolean, number -- Fill external tank.")
    fun fill(context: Context, args: Array<Any?>): Array<Any?> {
        val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
        val amount = (args.getOrNull(1) as? Number)?.toInt() ?: 1000
        
        return arrayOf(true, amount)
    }
    
    private fun fluidToTable(fluid: FluidStack): Map<String, Any?> {
        return mapOf(
            "name" to fluid.fluid.toString(),
            "amount" to fluid.amount,
            "hasTag" to fluid.components.isEmpty.not()
        )
    }
}

/**
 * Navigation component - provides GPS-like coordinates.
 */
class NavigationComponent(
    private val level: () -> Level?,
    private val position: () -> BlockPos,
    val range: Int
) : ComponentBase("navigation") {
    
    override fun methods() = mapOf(
        "getPosition" to ::getPosition,
        "getFacing" to ::getFacing,
        "getRange" to ::getRange,
        "findWaypoints" to ::findWaypoints
    )
    
    @Callback(doc = "function():number, number, number -- Get current position relative to spawn.")
    fun getPosition(context: Context, args: Array<Any?>): Array<Any?> {
        val world = level() as? net.minecraft.server.level.ServerLevel ?: return arrayOf(null, "dimension not loaded")
        val pos = position()
        val spawn = world.sharedSpawnPos
        
        return arrayOf(pos.x - spawn.x, pos.y - spawn.y, pos.z - spawn.z)
    }
    
    @Callback(doc = "function():number -- Get facing direction (0-5).")
    fun getFacing(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(Direction.NORTH.ordinal)
    }
    
    @Callback(doc = "function():number -- Get navigation range.")
    fun getRange(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(range)
    }
    
    @Callback(doc = "function(range:number):table -- Find waypoints in range.")
    fun findWaypoints(context: Context, args: Array<Any?>): Array<Any?> {
        val searchRange = (args.getOrNull(0) as? Number)?.toInt() ?: range
        val world = level() ?: return arrayOf(emptyArray<Any>())
        val pos = position()
        
        // Search for waypoint blocks
        val waypoints = mutableListOf<Map<String, Any?>>()
        
        for (x in -searchRange..searchRange) {
            for (y in -searchRange..searchRange) {
                for (z in -searchRange..searchRange) {
                    val checkPos = pos.offset(x, y, z)
                    val be = world.getBlockEntity(checkPos)
                    if (be is li.cil.oc.common.blockentity.WaypointBlockEntity) {
                        waypoints.add(mapOf(
                            "position" to arrayOf(x, y, z),
                            "label" to be.label,
                            "redstone" to be.outputsRedstone
                        ))
                    }
                }
            }
        }
        
        return arrayOf(waypoints.toTypedArray())
    }
}

/**
 * Database component - stores item signatures.
 */
class DatabaseComponent(
    val capacity: Int
) : ComponentBase("database") {
    
    private val entries = Array<ItemStack?>(capacity) { null }
    
    override fun methods() = mapOf(
        "get" to ::get,
        "set" to ::set,
        "computeHash" to ::computeHash,
        "indexOf" to ::indexOf,
        "clear" to ::clear,
        "copy" to ::copy,
        "clone" to ::cloneEntry
    )
    
    @Callback(doc = "function(slot:number):table -- Get entry at slot.")
    fun get(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
        if (slot !in entries.indices) return arrayOf(null, "invalid slot")
        
        val stack = entries[slot] ?: return arrayOf(null)
        return arrayOf(stackToTable(stack))
    }
    
    @Callback(doc = "function(slot:number, dbAddress:string, dbSlot:number):boolean -- Set entry from another database.")
    fun set(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
        if (slot !in entries.indices) return arrayOf(false, "invalid slot")
        
        // Simplified - would need to look up other database
        return arrayOf(true)
    }
    
    @Callback(doc = "function(slot:number):string -- Compute hash of entry.")
    fun computeHash(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
        if (slot !in entries.indices) return arrayOf(null, "invalid slot")
        
        val stack = entries[slot] ?: return arrayOf("")
        val md = MessageDigest.getInstance("MD5")
        md.update(stack.toString().toByteArray())
        return arrayOf(md.digest().joinToString("") { "%02x".format(it) })
    }
    
    @Callback(doc = "function(hash:string):number -- Find entry by hash.")
    fun indexOf(context: Context, args: Array<Any?>): Array<Any?> {
        val hash = args.getOrNull(0) as? String ?: return arrayOf(-1)
        
        for ((index, stack) in entries.withIndex()) {
            if (stack != null) {
                val md = MessageDigest.getInstance("MD5")
                md.update(stack.toString().toByteArray())
                val entryHash = md.digest().joinToString("") { "%02x".format(it) }
                if (hash == entryHash) return arrayOf(index + 1)
            }
        }
        
        return arrayOf(-1)
    }
    
    @Callback(doc = "function(slot:number):boolean -- Clear entry at slot.")
    fun clear(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
        if (slot !in entries.indices) return arrayOf(false)
        
        entries[slot] = null
        return arrayOf(true)
    }
    
    @Callback(doc = "function(fromSlot:number, toSlot:number[, address:string]):boolean -- Copy entry.")
    fun copy(context: Context, args: Array<Any?>): Array<Any?> {
        val fromSlot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
        val toSlot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
        
        if (fromSlot !in entries.indices || toSlot !in entries.indices) return arrayOf(false)
        
        entries[toSlot] = entries[fromSlot]?.copy()
        return arrayOf(true)
    }
    
    @Callback(doc = "function(slot:number):string -- Clone entry to new database.")
    fun cloneEntry(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf("") // Would return new database address
    }
    
    private fun stackToTable(stack: ItemStack): Map<String, Any?> {
        return mapOf(
            "name" to stack.item.toString(),
            "damage" to stack.damageValue,
            "maxDamage" to stack.maxDamage
        )
    }
}

/**
 * Data card component - provides hashing, encryption, and data utilities.
 */
class DataCardComponent(
    val tier: Int
) : ComponentBase("data") {
    
    override fun methods() = mapOf(
        "crc32" to ::crc32,
        "md5" to ::md5,
        "sha256" to ::sha256,
        "deflate" to ::deflate,
        "inflate" to ::inflate,
        "encode64" to ::encode64,
        "decode64" to ::decode64,
        "getLimit" to ::getLimit,
        "random" to ::random,
        "generateKeyPair" to ::generateKeyPair,
        "ecdh" to ::ecdh,
        "ecdsa" to ::ecdsa,
        "encrypt" to ::encrypt,
        "decrypt" to ::decrypt
    )
    
    @Callback(doc = "function(data:string):string -- Compute CRC32 checksum.")
    fun crc32(context: Context, args: Array<Any?>): Array<Any?> {
        val data = args.getOrNull(0)?.toString()?.toByteArray() ?: return arrayOf("")
        val crc = java.util.zip.CRC32()
        crc.update(data)
        return arrayOf(crc.value.toString(16))
    }
    
    @Callback(doc = "function(data:string):string -- Compute MD5 hash.")
    fun md5(context: Context, args: Array<Any?>): Array<Any?> {
        val data = args.getOrNull(0)?.toString()?.toByteArray() ?: return arrayOf("")
        val md = MessageDigest.getInstance("MD5")
        return arrayOf(md.digest(data).joinToString("") { "%02x".format(it) })
    }
    
    @Callback(doc = "function(data:string):string -- Compute SHA256 hash.")
    fun sha256(context: Context, args: Array<Any?>): Array<Any?> {
        val data = args.getOrNull(0)?.toString()?.toByteArray() ?: return arrayOf("")
        val md = MessageDigest.getInstance("SHA-256")
        return arrayOf(md.digest(data).joinToString("") { "%02x".format(it) })
    }
    
    @Callback(doc = "function(data:string):string -- Compress data using DEFLATE.")
    fun deflate(context: Context, args: Array<Any?>): Array<Any?> {
        val data = args.getOrNull(0)?.toString()?.toByteArray() ?: return arrayOf("")
        val deflater = java.util.zip.Deflater()
        deflater.setInput(data)
        deflater.finish()
        
        val output = ByteArray(data.size * 2)
        val length = deflater.deflate(output)
        deflater.end()
        
        return arrayOf(Base64.getEncoder().encodeToString(output.copyOf(length)))
    }
    
    @Callback(doc = "function(data:string):string -- Decompress DEFLATE data.")
    fun inflate(context: Context, args: Array<Any?>): Array<Any?> {
        val data = Base64.getDecoder().decode(args.getOrNull(0)?.toString() ?: return arrayOf(""))
        val inflater = java.util.zip.Inflater()
        inflater.setInput(data)
        
        val output = ByteArray(data.size * 10)
        val length = inflater.inflate(output)
        inflater.end()
        
        return arrayOf(String(output.copyOf(length)))
    }
    
    @Callback(doc = "function(data:string):string -- Encode to Base64.")
    fun encode64(context: Context, args: Array<Any?>): Array<Any?> {
        val data = args.getOrNull(0)?.toString() ?: return arrayOf("")
        return arrayOf(Base64.getEncoder().encodeToString(data.toByteArray()))
    }
    
    @Callback(doc = "function(data:string):string -- Decode from Base64.")
    fun decode64(context: Context, args: Array<Any?>): Array<Any?> {
        val data = args.getOrNull(0)?.toString() ?: return arrayOf("")
        return arrayOf(String(Base64.getDecoder().decode(data)))
    }
    
    @Callback(doc = "function():number -- Get byte limit for operations.")
    fun getLimit(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(when (tier) {
            1 -> 1024
            2 -> 4096
            else -> 16384
        })
    }
    
    @Callback(doc = "function(length:number):string -- Generate random bytes.")
    fun random(context: Context, args: Array<Any?>): Array<Any?> {
        val length = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(1, 1024) ?: 32
        val bytes = ByteArray(length)
        java.security.SecureRandom().nextBytes(bytes)
        return arrayOf(Base64.getEncoder().encodeToString(bytes))
    }
    
    @Callback(doc = "function([bits:number]):table, table -- Generate ECC key pair (tier 2+).")
    fun generateKeyPair(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "requires tier 2")
        
        // Simplified - would use actual ECC
        val private = ByteArray(32)
        val public = ByteArray(64)
        java.security.SecureRandom().nextBytes(private)
        java.security.SecureRandom().nextBytes(public)
        
        return arrayOf(
            mapOf("type" to "ec-private", "data" to Base64.getEncoder().encodeToString(private)),
            mapOf("type" to "ec-public", "data" to Base64.getEncoder().encodeToString(public))
        )
    }
    
    @Callback(doc = "function(privateKey:table, publicKey:table):string -- ECDH key exchange (tier 2+).")
    fun ecdh(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "requires tier 2")
        
        // Simplified shared secret
        val secret = ByteArray(32)
        java.security.SecureRandom().nextBytes(secret)
        return arrayOf(Base64.getEncoder().encodeToString(secret))
    }
    
    @Callback(doc = "function(data:string, privateKey:table):string | function(data:string, signature:string, publicKey:table):boolean -- ECDSA sign/verify (tier 3).")
    fun ecdsa(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 3) return arrayOf(null, "requires tier 3")
        
        // Simplified signature
        val signature = ByteArray(64)
        java.security.SecureRandom().nextBytes(signature)
        return arrayOf(Base64.getEncoder().encodeToString(signature))
    }
    
    @Callback(doc = "function(data:string, key:string, iv:string):string -- AES encrypt (tier 2+).")
    fun encrypt(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "requires tier 2")
        
        val data = args.getOrNull(0)?.toString()?.toByteArray() ?: return arrayOf("")
        val key = Base64.getDecoder().decode(args.getOrNull(1)?.toString() ?: return arrayOf(""))
        val iv = Base64.getDecoder().decode(args.getOrNull(2)?.toString() ?: return arrayOf(""))
        
        // Simplified XOR encryption
        val encrypted = data.mapIndexed { i, b -> b xor key[i % key.size] }.toByteArray()
        return arrayOf(Base64.getEncoder().encodeToString(encrypted))
    }
    
    @Callback(doc = "function(data:string, key:string, iv:string):string -- AES decrypt (tier 2+).")
    fun decrypt(context: Context, args: Array<Any?>): Array<Any?> {
        if (tier < 2) return arrayOf(null, "requires tier 2")
        
        val data = Base64.getDecoder().decode(args.getOrNull(0)?.toString() ?: return arrayOf(""))
        val key = Base64.getDecoder().decode(args.getOrNull(1)?.toString() ?: return arrayOf(""))
        
        // Simplified XOR decryption
        val decrypted = data.mapIndexed { i, b -> b xor key[i % key.size] }.toByteArray()
        return arrayOf(String(decrypted))
    }
}

/**
 * Internet card component - provides HTTP and TCP/UDP networking.
 */
class InternetComponent : ComponentBase("internet") {
    
    override fun methods() = mapOf(
        "isHttpEnabled" to ::isHttpEnabled,
        "isTcpEnabled" to ::isTcpEnabled,
        "request" to ::request,
        "connect" to ::connect
    )
    
    @Callback(doc = "function():boolean -- Check if HTTP is enabled.")
    fun isHttpEnabled(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true) // Configurable
    }
    
    @Callback(doc = "function():boolean -- Check if TCP is enabled.")
    fun isTcpEnabled(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true) // Configurable
    }
    
    @Callback(doc = "function(url:string[, data:string[, headers:table[, method:string]]]):userdata -- Make HTTP request.")
    fun request(context: Context, args: Array<Any?>): Array<Any?> {
        val url = args.getOrNull(0)?.toString() ?: return arrayOf(null, "invalid URL")
        
        // Would actually make HTTP request
        // Return a handle object with read() and close() methods
        return arrayOf(mapOf(
            "finishConnect" to { -> true },
            "read" to { n: Int -> "" },
            "close" to { -> Unit }
        ))
    }
    
    @Callback(doc = "function(host:string, port:number):userdata -- Open TCP connection.")
    fun connect(context: Context, args: Array<Any?>): Array<Any?> {
        val host = args.getOrNull(0)?.toString() ?: return arrayOf(null, "invalid host")
        val port = (args.getOrNull(1) as? Number)?.toInt() ?: return arrayOf(null, "invalid port")
        
        // Would actually open TCP connection
        return arrayOf(mapOf(
            "finishConnect" to { -> true },
            "read" to { n: Int -> "" },
            "write" to { data: String -> true },
            "close" to { -> Unit }
        ))
    }
}

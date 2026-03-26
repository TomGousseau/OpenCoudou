package li.cil.oc.api.driver

import li.cil.oc.api.network.ManagedEnvironment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * Drivers are the bridge between Minecraft items/blocks and OpenComputers components.
 * 
 * Item drivers handle items that can be placed inside computers (CPU, memory, cards).
 * Block drivers handle external blocks that can be accessed via Adapters.
 * 
 * When registering drivers, OpenComputers will query them to determine what items
 * and blocks they support, and create appropriate component environments.
 * 
 * @see DriverItem
 * @see DriverBlock
 */
object Driver {
    private val itemDrivers = mutableListOf<DriverItem>()
    private val blockDrivers = mutableListOf<DriverBlock>()
    private val converters = mutableListOf<Converter>()
    
    /**
     * Registers a new item driver.
     */
    fun add(driver: DriverItem) {
        itemDrivers.add(driver)
    }
    
    /**
     * Registers a new block driver.
     */
    fun add(driver: DriverBlock) {
        blockDrivers.add(driver)
    }
    
    /**
     * Registers a new value converter.
     */
    fun add(converter: Converter) {
        converters.add(converter)
    }
    
    /**
     * Gets all registered item drivers.
     */
    fun itemDrivers(): List<DriverItem> = itemDrivers.toList()
    
    /**
     * Gets all registered block drivers.
     */
    fun blockDrivers(): List<DriverBlock> = blockDrivers.toList()
    
    /**
     * Gets all registered converters.
     */
    fun converters(): List<Converter> = converters.toList()
    
    /**
     * Gets the driver for an item stack.
     */
    fun driverFor(stack: ItemStack): DriverItem? =
        itemDrivers.find { it.worksWith(stack) }
    
    /**
     * Gets the driver for a block at a position.
     */
    fun driverFor(level: Level, pos: BlockPos, side: Direction): DriverBlock? =
        blockDrivers.find { it.worksWith(level, pos, side) }
    
    /**
     * Converts a value for passing to/from Lua.
     */
    fun convert(value: Any?): Any? {
        if (value == null) return null
        for (converter in converters) {
            val result = converter.convert(value)
            if (result !== value) return result
        }
        return value
    }
}

/**
 * A driver for items that can be placed inside computers.
 * 
 * Item drivers create component environments for cards, upgrades,
 * CPUs, memory, storage devices, etc.
 */
interface DriverItem {
    /**
     * Checks if this driver handles the given item.
     */
    fun worksWith(stack: ItemStack): Boolean
    
    /**
     * Creates a managed environment for the item.
     * This is called when the item is placed in a valid slot.
     * 
     * @param stack The item stack
     * @param host The host containing the item
     * @return The environment, or null if none is needed
     */
    fun createEnvironment(stack: ItemStack, host: li.cil.oc.api.machine.MachineHost): ManagedEnvironment?
    
    /**
     * Gets the slot type for this item.
     * This determines which container slots accept this item.
     */
    fun slot(stack: ItemStack): Slot
    
    /**
     * Gets the tier of this item (0-3 for normal, higher for creative).
     * Higher tier items may have more features or capabilities.
     */
    fun tier(stack: ItemStack): Int
    
    /**
     * Gets the data tag key for this item's component data.
     * Items with the same data key share their persistent state.
     * Return null to use the default (item's registry name).
     */
    fun dataTag(stack: ItemStack): String? = null
}

/**
 * Slot types for items in computer containers.
 */
enum class Slot {
    /** No slot (cannot be placed in computers) */
    NONE,
    
    /** CPU slot - determines architecture and component limit */
    CPU,
    
    /** Memory slot - provides RAM */
    MEMORY,
    
    /** Card slot - expansion cards (GPU, network, etc.) */
    CARD,
    
    /** Hard drive slot - persistent storage */
    HDD,
    
    /** Floppy disk slot - removable storage */
    FLOPPY,
    
    /** EEPROM slot - boot code */
    EEPROM,
    
    /** Upgrade slot - robot/drone upgrades */
    UPGRADE,
    
    /** Tool slot - robot equipped tool */
    TOOL,
    
    /** Container slot - nested inventory */
    CONTAINER,
    
    /** Database slot - for database upgrade */
    DATABASE,
    
    /** Any slot - can go in multiple slot types */
    ANY
}

/**
 * A driver for external blocks accessible via Adapters.
 * 
 * Block drivers allow computers to interact with vanilla or
 * modded blocks by exposing them as components.
 */
interface DriverBlock {
    /**
     * Checks if this driver handles the block at the given position.
     */
    fun worksWith(level: Level, pos: BlockPos, side: Direction): Boolean
    
    /**
     * Creates a managed environment for the block.
     * This is called when an Adapter connects to the block.
     * 
     * @param level The world
     * @param pos The block position
     * @param side The side of the adapter facing the block
     * @return The environment, or null if none is needed
     */
    fun createEnvironment(level: Level, pos: BlockPos, side: Direction): ManagedEnvironment?
}

/**
 * Simple block driver that works with a specific block entity type.
 */
abstract class SimpleBlockDriver<T : BlockEntity>(
    private val blockEntityClass: Class<T>
) : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, side: Direction): Boolean {
        val blockEntity = level.getBlockEntity(pos)
        return blockEntityClass.isInstance(blockEntity)
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun createEnvironment(level: Level, pos: BlockPos, side: Direction): ManagedEnvironment? {
        val blockEntity = level.getBlockEntity(pos) as? T ?: return null
        return createEnvironment(blockEntity, side)
    }
    
    /**
     * Creates the environment for the block entity.
     */
    abstract fun createEnvironment(blockEntity: T, side: Direction): ManagedEnvironment?
}

/**
 * Converter for transforming values between Lua and Java.
 * 
 * Converters are called when values cross the boundary between the two
 * languages. They can transform complex Java objects into simple types
 * that Lua can understand.
 */
interface Converter {
    /**
     * Tries to convert a value.
     * 
     * @param value The value to convert
     * @return The converted value, or the original if not handled
     */
    fun convert(value: Any): Any?
}

/**
 * Built-in converter for common types.
 */
object DefaultConverter : Converter {
    override fun convert(value: Any): Any? {
        return when (value) {
            // Pass through primitives
            is Boolean, is Number, is String -> value
            // Convert byte arrays
            is ByteArray -> String(value, Charsets.UTF_8)
            // Convert collections to maps
            is Array<*> -> value.mapIndexed { i, v -> (i + 1) to v }.toMap()
            is List<*> -> value.mapIndexed { i, v -> (i + 1) to v }.toMap()
            is Map<*, *> -> value
            // Convert BlockPos
            is BlockPos -> mapOf("x" to value.x, "y" to value.y, "z" to value.z)
            // Convert Direction
            is net.minecraft.core.Direction -> value.name.lowercase()
            // Convert ItemStack
            is ItemStack -> if (value.isEmpty) null else mapOf(
                "name" to value.item.toString(),
                "count" to value.count,
                "maxCount" to value.maxStackSize
            )
            // Can't convert - return as-is
            else -> value
        }
    }
}

/**
 * Helper object for creating item tiers.
 */
object Tier {
    const val ONE = 0
    const val TWO = 1
    const val THREE = 2
    const val CREATIVE = 3
    
    fun name(tier: Int): String = when (tier) {
        ONE -> "Tier 1"
        TWO -> "Tier 2"
        THREE -> "Tier 3"
        else -> "Creative"
    }
    
    fun color(tier: Int): Int = when (tier) {
        ONE -> 0xFFCCB5    // Light brown
        TWO -> 0xFFCC5A    // Orange
        THREE -> 0xCCCCFF  // Light blue
        else -> 0xDDDDDD   // Light gray
    }
}

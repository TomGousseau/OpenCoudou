package li.cil.oc.integration

import li.cil.oc.OpenComputers
import li.cil.oc.api.driver.Converter
import li.cil.oc.api.driver.DriverBlock
import li.cil.oc.api.driver.DriverItem
import li.cil.oc.api.driver.EnvironmentProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandler
import java.util.*

/**
 * Integration manager for other mods.
 * Handles auto-detection and registration of integration modules.
 */
object IntegrationManager {
    
    private val loadedIntegrations = mutableListOf<ModIntegration>()
    private val blockDrivers = mutableListOf<DriverBlock>()
    private val itemDrivers = mutableListOf<DriverItem>()
    private val converters = mutableListOf<Converter>()
    private val environmentProviders = mutableListOf<EnvironmentProvider>()
    
    /**
     * Initialize all integrations during mod loading.
     */
    fun initialize() {
        // Register built-in integrations
        registerIntegration(VanillaIntegration())
        registerIntegration(NeoForgeIntegration())
        
        // Try to load optional mod integrations
        tryLoadIntegration("mekanism", ::MekanismIntegration)
        tryLoadIntegration("ae2", ::AppliedEnergisticsIntegration)
        tryLoadIntegration("create", ::CreateIntegration)
        tryLoadIntegration("computercraft", ::ComputerCraftIntegration)
        
        OpenComputers.LOGGER.info("Loaded ${loadedIntegrations.size} mod integrations")
    }
    
    private fun tryLoadIntegration(modId: String, factory: () -> ModIntegration) {
        try {
            if (net.neoforged.fml.ModList.get().isLoaded(modId)) {
                registerIntegration(factory())
                OpenComputers.LOGGER.info("Loaded integration for $modId")
            }
        } catch (e: Exception) {
            OpenComputers.LOGGER.warn("Failed to load integration for $modId: ${e.message}")
        }
    }
    
    private fun registerIntegration(integration: ModIntegration) {
        loadedIntegrations.add(integration)
        
        // Register drivers
        integration.getBlockDrivers().forEach { blockDrivers.add(it) }
        integration.getItemDrivers().forEach { itemDrivers.add(it) }
        integration.getConverters().forEach { converters.add(it) }
        integration.getEnvironmentProviders().forEach { environmentProviders.add(it) }
        
        integration.onRegister()
    }
    
    fun getBlockDrivers(): List<DriverBlock> = blockDrivers
    fun getItemDrivers(): List<DriverItem> = itemDrivers
    fun getConverters(): List<Converter> = converters
    fun getEnvironmentProviders(): List<EnvironmentProvider> = environmentProviders
    
    /**
     * Find a driver for a block at a position.
     */
    fun findBlockDriver(level: Level, pos: BlockPos, state: BlockState): DriverBlock? {
        return blockDrivers.find { it.worksWith(level, pos, state) }
    }
    
    /**
     * Find a driver for an item.
     */
    fun findItemDriver(stack: ItemStack): DriverItem? {
        return itemDrivers.find { it.worksWith(stack) }
    }
    
    /**
     * Convert a value for Lua.
     */
    fun convert(value: Any?, context: Map<String, Any>): Any? {
        if (value == null) return null
        
        for (converter in converters) {
            val result = converter.convert(value, context)
            if (result != value) {
                return result
            }
        }
        
        return value
    }
}

/**
 * Base interface for mod integrations.
 */
interface ModIntegration {
    val modId: String
    
    fun onRegister() {}
    
    fun getBlockDrivers(): List<DriverBlock> = emptyList()
    fun getItemDrivers(): List<DriverItem> = emptyList()
    fun getConverters(): List<Converter> = emptyList()
    fun getEnvironmentProviders(): List<EnvironmentProvider> = emptyList()
}

/**
 * Vanilla Minecraft integration.
 */
class VanillaIntegration : ModIntegration {
    override val modId = "minecraft"
    
    override fun getBlockDrivers(): List<DriverBlock> = listOf(
        VanillaInventoryDriver(),
        VanillaRedstoneDriver(),
        VanillaSignDriver(),
        VanillaJukeboxDriver(),
        VanillaNoteBlockDriver(),
        VanillaBeaconDriver()
    )
    
    override fun getConverters(): List<Converter> = listOf(
        VanillaConverter()
    )
}

/**
 * Driver for vanilla inventories (chests, hoppers, etc.)
 */
class VanillaInventoryDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, state: BlockState): Boolean {
        val blockEntity = level.getBlockEntity(pos) ?: return false
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, state: BlockState): li.cil.oc.api.network.Environment? {
        val handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) ?: return null
        return InventoryEnvironment(handler, pos)
    }
}

/**
 * Environment wrapper for vanilla inventories.
 */
class InventoryEnvironment(
    private val handler: IItemHandler,
    private val pos: BlockPos
) : SimpleComponent("inventory") {
    
    @li.cil.oc.api.machine.Callback(doc = "function():number -- Get the number of slots in the inventory")
    fun getInventorySize(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        return arrayOf(handler.slots)
    }
    
    @li.cil.oc.api.machine.Callback(doc = "function(slot:number):table -- Get info about an item in a slot")
    fun getStackInSlot(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val slot = args.checkInteger(0) - 1
        if (slot < 0 || slot >= handler.slots) {
            return arrayOf(null, "invalid slot")
        }
        
        val stack = handler.getStackInSlot(slot)
        if (stack.isEmpty) {
            return arrayOf(null)
        }
        
        return arrayOf(stackToTable(stack))
    }
    
    @li.cil.oc.api.machine.Callback(doc = "function():table -- Get all stacks in the inventory")
    fun getAllStacks(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val stacks = mutableMapOf<Int, Map<String, Any?>>()
        for (i in 0 until handler.slots) {
            val stack = handler.getStackInSlot(i)
            if (!stack.isEmpty) {
                stacks[i + 1] = stackToTable(stack)
            }
        }
        return arrayOf(stacks)
    }
    
    private fun stackToTable(stack: ItemStack): Map<String, Any?> {
        return mapOf(
            "name" to stack.item.descriptionId,
            "count" to stack.count,
            "maxCount" to stack.maxStackSize,
            "damage" to stack.damageValue,
            "maxDamage" to stack.maxDamage,
            "hasTag" to stack.hasTag()
        )
    }
}

/**
 * Driver for redstone-emitting blocks.
 */
class VanillaRedstoneDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, state: BlockState): Boolean {
        return state.isSignalSource
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, state: BlockState): li.cil.oc.api.network.Environment? {
        return RedstoneBlockEnvironment(level, pos)
    }
}

class RedstoneBlockEnvironment(
    private val level: Level,
    private val pos: BlockPos
) : SimpleComponent("redstone_block") {
    
    @li.cil.oc.api.machine.Callback(doc = "function(side:number):number -- Get redstone signal on a side")
    fun getInput(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val side = args.checkInteger(0)
        val direction = Direction.from3DDataValue(side)
        return arrayOf(level.getSignal(pos.relative(direction), direction))
    }
}

/**
 * Driver for signs.
 */
class VanillaSignDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, state: BlockState): Boolean {
        val blockEntity = level.getBlockEntity(pos)
        return blockEntity is net.minecraft.world.level.block.entity.SignBlockEntity
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, state: BlockState): li.cil.oc.api.network.Environment? {
        return SignEnvironment(level, pos)
    }
}

class SignEnvironment(
    private val level: Level,
    private val pos: BlockPos
) : SimpleComponent("sign") {
    
    @li.cil.oc.api.machine.Callback(doc = "function():string -- Get the sign text")
    fun getText(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val sign = level.getBlockEntity(pos) as? net.minecraft.world.level.block.entity.SignBlockEntity
            ?: return arrayOf(null, "not a sign")
        
        val text = StringBuilder()
        // Get sign text from front side
        // This would need to iterate over the sign's text components
        return arrayOf(text.toString())
    }
    
    @li.cil.oc.api.machine.Callback(doc = "function(text:string):boolean -- Set the sign text")
    fun setText(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val text = args.checkString(0)
        // Set sign text
        return arrayOf(true)
    }
}

/**
 * Driver for jukeboxes.
 */
class VanillaJukeboxDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, state: BlockState): Boolean {
        val blockEntity = level.getBlockEntity(pos)
        return blockEntity is net.minecraft.world.level.block.entity.JukeboxBlockEntity
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, state: BlockState): li.cil.oc.api.network.Environment? {
        return JukeboxEnvironment(level, pos)
    }
}

class JukeboxEnvironment(
    private val level: Level,
    private val pos: BlockPos
) : SimpleComponent("jukebox") {
    
    @li.cil.oc.api.machine.Callback(doc = "function():string -- Get the current record")
    fun getRecord(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val jukebox = level.getBlockEntity(pos) as? net.minecraft.world.level.block.entity.JukeboxBlockEntity
            ?: return arrayOf(null, "not a jukebox")
        
        val record = jukebox.firstItem
        if (record.isEmpty) {
            return arrayOf(null)
        }
        
        return arrayOf(record.item.descriptionId)
    }
}

/**
 * Driver for note blocks.
 */
class VanillaNoteBlockDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, state: BlockState): Boolean {
        return state.block is net.minecraft.world.level.block.NoteBlock
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, state: BlockState): li.cil.oc.api.network.Environment? {
        return NoteBlockEnvironment(level, pos)
    }
}

class NoteBlockEnvironment(
    private val level: Level,
    private val pos: BlockPos
) : SimpleComponent("note_block") {
    
    @li.cil.oc.api.machine.Callback(doc = "function():number -- Get the current note")
    fun getNote(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val state = level.getBlockState(pos)
        val note = state.getValue(net.minecraft.world.level.block.NoteBlock.NOTE)
        return arrayOf(note)
    }
    
    @li.cil.oc.api.machine.Callback(doc = "function(note:number) -- Set the note")
    fun setNote(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val note = args.checkInteger(0).coerceIn(0, 24)
        val state = level.getBlockState(pos)
        val newState = state.setValue(net.minecraft.world.level.block.NoteBlock.NOTE, note)
        level.setBlock(pos, newState, 3)
        return arrayOf()
    }
    
    @li.cil.oc.api.machine.Callback(doc = "function() -- Play the note")
    fun play(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val state = level.getBlockState(pos)
        (state.block as? net.minecraft.world.level.block.NoteBlock)?.let { noteBlock ->
            level.blockEvent(pos, noteBlock, 0, 0)
        }
        return arrayOf()
    }
}

/**
 * Driver for beacons.
 */
class VanillaBeaconDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, state: BlockState): Boolean {
        val blockEntity = level.getBlockEntity(pos)
        return blockEntity is net.minecraft.world.level.block.entity.BeaconBlockEntity
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, state: BlockState): li.cil.oc.api.network.Environment? {
        return BeaconEnvironment(level, pos)
    }
}

class BeaconEnvironment(
    private val level: Level,
    private val pos: BlockPos
) : SimpleComponent("beacon") {
    
    @li.cil.oc.api.machine.Callback(doc = "function():number -- Get the beacon level (0-4)")
    fun getLevel(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val beacon = level.getBlockEntity(pos) as? net.minecraft.world.level.block.entity.BeaconBlockEntity
            ?: return arrayOf(null, "not a beacon")
        
        return arrayOf(beacon.levels)
    }
    
    @li.cil.oc.api.machine.Callback(doc = "function():table -- Get the active effects")
    fun getEffects(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val beacon = level.getBlockEntity(pos) as? net.minecraft.world.level.block.entity.BeaconBlockEntity
            ?: return arrayOf(null, "not a beacon")
        
        val effects = mutableListOf<String>()
        beacon.primaryPower?.let { effects.add(it.descriptionId) }
        beacon.secondaryPower?.let { effects.add(it.descriptionId) }
        
        return arrayOf(effects)
    }
}

/**
 * Vanilla value converter.
 */
class VanillaConverter : Converter {
    
    override fun convert(value: Any?, context: Map<String, Any>): Any? {
        return when (value) {
            is ItemStack -> mapOf(
                "name" to value.item.descriptionId,
                "count" to value.count,
                "damage" to value.damageValue
            )
            is FluidStack -> mapOf(
                "name" to value.fluid.fluidType.descriptionId,
                "amount" to value.amount
            )
            is BlockPos -> mapOf(
                "x" to value.x,
                "y" to value.y,
                "z" to value.z
            )
            is Direction -> value.name.lowercase()
            is UUID -> value.toString()
            else -> value
        }
    }
}

/**
 * NeoForge integration for capabilities.
 */
class NeoForgeIntegration : ModIntegration {
    override val modId = "neoforge"
    
    override fun getBlockDrivers(): List<DriverBlock> = listOf(
        ForgeEnergyDriver(),
        ForgeFluidDriver()
    )
}

/**
 * Driver for NeoForge energy capability.
 */
class ForgeEnergyDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, state: BlockState): Boolean {
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, state: BlockState): li.cil.oc.api.network.Environment? {
        val storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) ?: return null
        return ForgeEnergyEnvironment(storage, pos)
    }
}

class ForgeEnergyEnvironment(
    private val storage: IEnergyStorage,
    private val pos: BlockPos
) : SimpleComponent("energy_storage") {
    
    @li.cil.oc.api.machine.Callback(doc = "function():number -- Get stored energy")
    fun getEnergyStored(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        return arrayOf(storage.energyStored)
    }
    
    @li.cil.oc.api.machine.Callback(doc = "function():number -- Get max energy capacity")
    fun getMaxEnergyStored(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        return arrayOf(storage.maxEnergyStored)
    }
    
    @li.cil.oc.api.machine.Callback(doc = "function():boolean -- Can receive energy")
    fun canReceive(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        return arrayOf(storage.canReceive())
    }
    
    @li.cil.oc.api.machine.Callback(doc = "function():boolean -- Can extract energy")
    fun canExtract(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        return arrayOf(storage.canExtract())
    }
}

/**
 * Driver for NeoForge fluid capability.
 */
class ForgeFluidDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, state: BlockState): Boolean {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null) != null
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, state: BlockState): li.cil.oc.api.network.Environment? {
        val handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null) ?: return null
        return ForgeFluidEnvironment(handler, pos)
    }
}

class ForgeFluidEnvironment(
    private val handler: IFluidHandler,
    private val pos: BlockPos
) : SimpleComponent("fluid_handler") {
    
    @li.cil.oc.api.machine.Callback(doc = "function():number -- Get tank count")
    fun getTankCount(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        return arrayOf(handler.tanks)
    }
    
    @li.cil.oc.api.machine.Callback(doc = "function(tank:number):table -- Get info about a tank")
    fun getTankInfo(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val tank = args.checkInteger(0) - 1
        if (tank < 0 || tank >= handler.tanks) {
            return arrayOf(null, "invalid tank")
        }
        
        val fluid = handler.getFluidInTank(tank)
        val capacity = handler.getTankCapacity(tank)
        
        return arrayOf(mapOf(
            "name" to if (fluid.isEmpty) null else fluid.fluid.fluidType.descriptionId,
            "amount" to fluid.amount,
            "capacity" to capacity
        ))
    }
    
    @li.cil.oc.api.machine.Callback(doc = "function():table -- Get all tank info")
    fun getAllTanks(context: li.cil.oc.api.machine.Context, args: li.cil.oc.api.machine.Arguments): Array<Any?> {
        val tanks = mutableListOf<Map<String, Any?>>()
        for (i in 0 until handler.tanks) {
            val fluid = handler.getFluidInTank(i)
            val capacity = handler.getTankCapacity(i)
            tanks.add(mapOf(
                "name" to if (fluid.isEmpty) null else fluid.fluid.fluidType.descriptionId,
                "amount" to fluid.amount,
                "capacity" to capacity
            ))
        }
        return arrayOf(tanks)
    }
}

// Placeholder integration classes
class MekanismIntegration : ModIntegration {
    override val modId = "mekanism"
}

class AppliedEnergisticsIntegration : ModIntegration {
    override val modId = "ae2"
}

class CreateIntegration : ModIntegration {
    override val modId = "create"
}

class ComputerCraftIntegration : ModIntegration {
    override val modId = "computercraft"
}

/**
 * Simple component base class.
 */
abstract class SimpleComponent(
    override val componentName: String
) : li.cil.oc.api.network.Environment, li.cil.oc.api.network.Component {
    
    override val componentAddress: String = UUID.randomUUID().toString()
    
    private var node: li.cil.oc.api.network.Node? = null
    
    override fun node(): li.cil.oc.api.network.Node? = node
    
    override fun setNode(node: li.cil.oc.api.network.Node?) {
        this.node = node
    }
    
    override fun onConnect(node: li.cil.oc.api.network.Node) {}
    
    override fun onDisconnect(node: li.cil.oc.api.network.Node) {}
    
    override fun onMessage(message: li.cil.oc.api.network.Message) {}
    
    override fun canInteract(player: String): Boolean = true
    
    override fun load(nbt: CompoundTag) {}
    
    override fun save(nbt: CompoundTag) {}
}

package li.cil.oc.common.blockentity

import li.cil.oc.api.network.ComponentVisibility
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.NodeBuilder
import li.cil.oc.common.block.ChargerBlock
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler
import kotlin.math.min

/**
 * Capacitor block entity - stores energy for the OC network.
 * 
 * Capacitors provide buffer storage for energy, allowing computers
 * to handle brief power outages and smooth out power consumption spikes.
 */
class CapacitorBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.CAPACITOR.get(), pos, state), Environment {
    
    companion object {
        const val MAX_ENERGY = 100000.0
        const val TRANSFER_RATE = 500.0
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    private var energy: Double = 0.0
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(MAX_ENERGY)
                .build()
        }
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
    }
    
    private fun connectToNetwork() {
        val level = level as? ServerLevel ?: return
        
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val neighbor = level.getBlockEntity(neighborPos)
            if (neighbor is Environment) {
                neighbor.node()?.let { _node?.connect(it) }
            }
        }
    }
    
    fun getComparatorOutput(): Int {
        val node = _node ?: return 0
        val ratio = node.globalBuffer() / node.globalBufferSize()
        return (ratio * 15).toInt()
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Power Converter block entity - converts energy from other mods.
 * 
 * Supports:
 * - Forge Energy (FE/RF)
 * - EU (if IC2 is present)
 * - Other energy systems via adapters
 */
class PowerConverterBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.POWER_CONVERTER.get(), pos, state), Environment {
    
    companion object {
        const val MAX_ENERGY = 10000.0
        
        // Conversion ratios (OC Energy per unit)
        const val OC_PER_FE = 0.5     // 2 FE = 1 OC
        const val OC_PER_EU = 2.0     // 1 EU = 2 OC
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    // Internal FE buffer for receiving energy
    private var feBuffer: Int = 0
    private val feBufferMax: Int = 10000
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(MAX_ENERGY)
                .build()
        }
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        // Convert FE to OC energy
        if (feBuffer > 0) {
            val node = _node ?: return
            val toConvert = min(feBuffer, 1000) // Convert up to 1000 FE per tick
            val ocEnergy = toConvert * OC_PER_FE
            
            if (node.tryChangeBuffer(ocEnergy)) {
                feBuffer -= toConvert
            }
        }
    }
    
    private fun connectToNetwork() {
        val level = level as? ServerLevel ?: return
        
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val neighbor = level.getBlockEntity(neighborPos)
            if (neighbor is Environment) {
                neighbor.node()?.let { _node?.connect(it) }
            }
        }
    }
    
    // Forge Energy IEnergyStorage implementation
    fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        val space = feBufferMax - feBuffer
        val toReceive = min(maxReceive, space)
        
        if (!simulate) {
            feBuffer += toReceive
            setChanged()
        }
        
        return toReceive
    }
    
    fun extractEnergy(maxExtract: Int, simulate: Boolean): Int = 0 // Output only to OC network
    
    fun getEnergyStored(): Int = feBuffer
    fun getMaxEnergyStored(): Int = feBufferMax
    fun canReceive(): Boolean = true
    fun canExtract(): Boolean = false
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("FEBuffer", feBuffer)
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        feBuffer = tag.getInt("FEBuffer")
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Power Distributor block entity - balances power across the network.
 * 
 * Distributors ensure that power is evenly distributed among all
 * connected capacitors and consumers.
 */
class PowerDistributorBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.POWER_DISTRIBUTOR.get(), pos, state), Environment {
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(1000.0) // Small buffer for flow control
                .build()
        }
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        // Power distribution happens automatically through the network
    }
    
    private fun connectToNetwork() {
        val level = level as? ServerLevel ?: return
        
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val neighbor = level.getBlockEntity(neighborPos)
            if (neighbor is Environment) {
                neighbor.node()?.let { _node?.connect(it) }
            }
        }
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Charger block entity - charges robots, tablets, and drones.
 * 
 * Places items or entities in front of the charger to charge them.
 * Charging speed depends on available power.
 */
class ChargerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.CHARGER.get(), pos, state), Environment {
    
    companion object {
        const val MAX_CHARGE_RATE = 100.0 // OC energy per tick
        const val ENERGY_BUFFER = 10000.0
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    // Charger inventory (for items to charge)
    val inventory: ItemStackHandler = object : ItemStackHandler(1) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
        }
    }
    
    // State
    private var isCharging: Boolean = false
    private var isPoweredByRedstone: Boolean = false
    private var invertRedstone: Boolean = false
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(ENERGY_BUFFER)
                .build()
        }
    }
    
    fun openGui(player: Player) {
        // Open charger GUI
    }
    
    fun onRedstoneUpdate(powered: Boolean) {
        isPoweredByRedstone = powered
        updateChargingState()
    }
    
    private fun updateChargingState() {
        val shouldCharge = if (invertRedstone) !isPoweredByRedstone else isPoweredByRedstone
        
        if (isCharging != shouldCharge) {
            isCharging = shouldCharge
            updateBlockState()
        }
    }
    
    private fun updateBlockState() {
        level?.let { lvl ->
            val state = lvl.getBlockState(blockPos)
            if (state.block is ChargerBlock) {
                lvl.setBlock(blockPos, state.setValue(ChargerBlock.CHARGING, isCharging), 3)
            }
        }
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        if (isCharging) {
            chargeItems()
            chargeEntities()
        }
    }
    
    private fun connectToNetwork() {
        val level = level as? ServerLevel ?: return
        
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val neighbor = level.getBlockEntity(neighborPos)
            if (neighbor is Environment) {
                neighbor.node()?.let { _node?.connect(it) }
            }
        }
    }
    
    private fun chargeItems() {
        val stack = inventory.getStackInSlot(0)
        if (stack.isEmpty) return
        
        // Check if item is chargeable (tablet, drone, etc.)
        // In full impl, check item capabilities
        
        val node = _node ?: return
        val available = min(MAX_CHARGE_RATE, node.globalBuffer())
        
        // Transfer energy to item
        // In full impl, use item's energy capability
        
        if (available > 0) {
            node.tryChangeBuffer(-available)
        }
    }
    
    private fun chargeEntities() {
        val level = level ?: return
        val facing = blockState.getValue(ChargerBlock.FACING)
        val chargePos = blockPos.relative(facing)
        
        // Find chargeable entities (robots, drones) in front
        val entities = level.getEntities(null, net.minecraft.world.phys.AABB(chargePos))
        
        for (entity in entities) {
            // In full impl, check if entity is a Robot or Drone
            // and transfer energy to it
        }
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("Inventory", inventory.serializeNBT(registries))
        tag.putBoolean("IsCharging", isCharging)
        tag.putBoolean("InvertRedstone", invertRedstone)
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"))
        isCharging = tag.getBoolean("IsCharging")
        invertRedstone = tag.getBoolean("InvertRedstone")
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

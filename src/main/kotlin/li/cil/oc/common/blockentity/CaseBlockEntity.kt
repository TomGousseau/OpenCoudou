package li.cil.oc.common.blockentity

import li.cil.oc.api.machine.Machine
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.ComponentVisibility
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.NodeBuilder
import li.cil.oc.common.block.CaseBlock
import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.machine.MachineImpl
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.Containers
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler

/**
 * Computer Case block entity - the core of a computer system.
 * 
 * The case holds:
 * - CPU (determines max components and architecture)
 * - Memory (determines available RAM)
 * - Storage (HDD, SSD, EEPROM)
 * - Cards (GPU, network, redstone, etc.)
 * - Other upgrades
 * 
 * Components installed in the case determine the computer's capabilities.
 */
class CaseBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.CASE.get(), pos, state), MachineHost, Environment {
    
    // ========================================
    // Constants
    // ========================================
    
    companion object {
        const val SLOT_CPU = 0
        const val SLOT_RAM_START = 1
        const val SLOT_RAM_COUNT = 4
        const val SLOT_HDD_START = 5
        const val SLOT_HDD_COUNT = 2
        const val SLOT_CARD_START = 7
        const val SLOT_CARD_COUNT = 4
        const val SLOT_EEPROM = 11
        const val TOTAL_SLOTS = 12
        
        // Tier capacities
        val TIER_SLOTS = mapOf(
            1 to TOTAL_SLOTS,
            2 to TOTAL_SLOTS,
            3 to TOTAL_SLOTS,
            4 to TOTAL_SLOTS // Creative
        )
    }
    
    // ========================================
    // Inventory
    // ========================================
    
    val inventory: ItemStackHandler = object : ItemStackHandler(TOTAL_SLOTS) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
            onInventoryChanged(slot)
        }
        
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            return isValidForSlot(slot, stack)
        }
    }
    
    // ========================================
    // Machine State
    // ========================================
    
    private var _machine: MachineImpl? = null
    val machine: Machine?
        get() = _machine
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    private var isRunning = false
    private var tier: Int = 1
    
    // Power
    private var energy: Double = 0.0
    private var maxEnergy: Double = 10000.0
    
    // ========================================
    // Initialization
    // ========================================
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(maxEnergy)
                .withComponent("computer", ComponentVisibility.NETWORK)
                .build()
        }
    }
    
    // ========================================
    // MachineHost Implementation
    // ========================================
    
    override fun internalComponents(): Array<ItemStack> {
        return (0 until TOTAL_SLOTS)
            .map { inventory.getStackInSlot(it) }
            .filter { !it.isEmpty }
            .toTypedArray()
    }
    
    override fun componentSlot(address: String): Int {
        // Find slot by component address
        for (i in 0 until TOTAL_SLOTS) {
            val stack = inventory.getStackInSlot(i)
            // Check if this stack has the address
            // In the full impl, we'd check the item's stored address
        }
        return -1
    }
    
    override fun markChanged() {
        setChanged()
    }
    
    override fun onMachineConnect(node: Node) {
        // Machine connected to network
    }
    
    override fun onMachineDisconnect(node: Node) {
        // Machine disconnected from network
    }
    
    override fun world(): Level? = level
    
    override fun xPosition(): Double = blockPos.x.toDouble()
    override fun yPosition(): Double = blockPos.y.toDouble()
    override fun zPosition(): Double = blockPos.z.toDouble()
    
    // ========================================
    // Power Management
    // ========================================
    
    fun consumeEnergy(amount: Double): Boolean {
        val node = _node ?: return false
        return node.tryChangeBuffer(-amount)
    }
    
    fun getEnergyStored(): Double = _node?.localBufferSize()?.let { 
        _node?.globalBuffer() ?: 0.0 
    } ?: 0.0
    
    fun getMaxEnergyStored(): Double = _node?.localBufferSize() ?: maxEnergy
    
    // ========================================
    // Computer Control
    // ========================================
    
    fun start(): Boolean {
        if (level?.isClientSide == true) return false
        if (isRunning) return false
        
        // Check for required components
        if (!hasRequiredComponents()) return false
        
        // Create machine if needed
        if (_machine == null) {
            _machine = MachineImpl(this)
        }
        
        val started = _machine?.start() == true
        if (started) {
            isRunning = true
            updateRunningState()
        }
        
        return started
    }
    
    fun stop(): Boolean {
        if (level?.isClientSide == true) return false
        if (!isRunning) return false
        
        val stopped = _machine?.stop() == true
        if (stopped) {
            isRunning = false
            updateRunningState()
        }
        
        return stopped
    }
    
    fun toggle(): Boolean {
        return if (isRunning) stop() else start()
    }
    
    private fun hasRequiredComponents(): Boolean {
        // Need at least a CPU and RAM
        val hasCPU = !inventory.getStackInSlot(SLOT_CPU).isEmpty
        val hasRAM = (SLOT_RAM_START until SLOT_RAM_START + SLOT_RAM_COUNT)
            .any { !inventory.getStackInSlot(it).isEmpty }
        return hasCPU && hasRAM
    }
    
    private fun updateRunningState() {
        level?.let { lvl ->
            val state = lvl.getBlockState(blockPos)
            if (state.block is CaseBlock) {
                lvl.setBlock(blockPos, state.setValue(CaseBlock.RUNNING, isRunning), 3)
            }
        }
    }
    
    // ========================================
    // Tick
    // ========================================
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        // Initialize node if needed
        if (_node == null) {
            initializeOnLoad()
            // Connect to adjacent OC blocks
            connectToNetwork()
        }
        
        // Update machine
        if (isRunning) {
            _machine?.update()
            
            // Check if machine crashed or stopped
            if (_machine?.isRunning == false) {
                isRunning = false
                updateRunningState()
            }
        }
    }
    
    private fun connectToNetwork() {
        val level = level as? ServerLevel ?: return
        
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val neighbor = level.getBlockEntity(neighborPos)
            if (neighbor is Environment) {
                val neighborNode = neighbor.node()
                if (neighborNode != null && _node != null) {
                    _node?.connect(neighborNode)
                }
            }
        }
    }
    
    // ========================================
    // Environment Implementation
    // ========================================
    
    override fun onConnect(node: Node) {
        // Nothing special on connect
    }
    
    override fun onDisconnect(node: Node) {
        // Stop the machine if we lose all network connections
    }
    
    override fun onMessage(message: Message) {
        // Handle network messages
    }
    
    // ========================================
    // Inventory Validation
    // ========================================
    
    private fun isValidForSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return true
        
        // In full implementation, check item types
        // For now, accept everything
        return true
    }
    
    private fun onInventoryChanged(slot: Int) {
        // Rebuild component list when inventory changes
        _machine?.onHostInventoryChanged(slot)
    }
    
    // ========================================
    // GUI
    // ========================================
    
    fun openGui(player: Player) {
        // Open computer GUI
        // In full implementation, create a menu and open it
    }
    
    // ========================================
    // Drop Contents
    // ========================================
    
    fun dropContents(level: Level, pos: BlockPos) {
        // Stop machine first
        if (isRunning) stop()
        
        // Drop all items
        for (i in 0 until TOTAL_SLOTS) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                inventory.setStackInSlot(i, ItemStack.EMPTY)
            }
        }
    }
    
    // ========================================
    // Persistence
    // ========================================
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        
        tag.put("Inventory", inventory.serializeNBT(registries))
        tag.putBoolean("Running", isRunning)
        tag.putInt("Tier", tier)
        
        // Save node state
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
        
        // Save machine state
        _machine?.let { machine ->
            val machineTag = CompoundTag()
            machine.save(machineTag)
            tag.put("Machine", machineTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"))
        isRunning = tag.getBoolean("Running")
        tier = tag.getInt("Tier").coerceAtLeast(1)
        
        // Load node state
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
        
        // Load machine state
        if (tag.contains("Machine") && _node != null) {
            if (_machine == null) {
                _machine = MachineImpl(this)
            }
            _machine?.load(tag.getCompound("Machine"))
        }
    }
    
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        tag.putBoolean("Running", isRunning)
        return tag
    }
    
    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket {
        return ClientboundBlockEntityDataPacket.create(this)
    }
}

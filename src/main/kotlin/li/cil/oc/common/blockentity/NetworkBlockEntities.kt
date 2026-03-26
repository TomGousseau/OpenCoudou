package li.cil.oc.common.blockentity

import li.cil.oc.api.network.ComponentVisibility
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.NodeBuilder
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Cable block entity - connects OC components in a network.
 * 
 * Cables don't have much logic themselves, they just provide network connectivity.
 * They can be dyed different colors to create separate networks.
 */
class CableBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.CABLE.get(), pos, state), Environment {
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    // Cable color (for network separation)
    private var color: Int = -1 // -1 = no color (connects to anything)
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .build() // Cables are not components, just connectors
        }
    }
    
    fun setColor(newColor: Int) {
        if (color != newColor) {
            // Disconnect from network first
            _node?.network()?.let { network ->
                for (other in network.nodes()) {
                    if (other != _node) {
                        _node?.disconnect(other)
                    }
                }
            }
            
            color = newColor
            setChanged()
            
            // Reconnect with new color
            reconnectToNetwork()
        }
    }
    
    fun getColor(): Int = color
    
    fun canConnectTo(other: CableBlockEntity): Boolean {
        // Connect if either cable has no color, or colors match
        return color == -1 || other.color == -1 || color == other.color
    }
    
    private fun reconnectToNetwork() {
        val level = level as? ServerLevel ?: return
        
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val neighbor = level.getBlockEntity(neighborPos)
            
            if (neighbor is CableBlockEntity) {
                if (canConnectTo(neighbor)) {
                    neighbor.node()?.let { _node?.connect(it) }
                }
            } else if (neighbor is Environment) {
                neighbor.node()?.let { _node?.connect(it) }
            }
        }
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("Color", color)
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        color = tag.getInt("Color")
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Relay block entity - bridges separate networks.
 * 
 * Relays allow components on one network to see components on another,
 * optionally with filtering/access control.
 */
class RelayBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.RELAY.get(), pos, state), Environment {
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    // Relay statistics
    private var packetsRelayed: Long = 0
    private var lastSecondPackets: Int = 0
    private var tickCounter: Int = 0
    
    // Configuration
    private var maxPacketsPerSecond: Int = 20
    private var signalRange: Int = 16
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withComponent("relay", ComponentVisibility.NETWORK)
                .build()
        }
    }
    
    fun openGui(player: net.minecraft.world.entity.player.Player) {
        // Open relay configuration GUI
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        // Reset packet counter every second
        tickCounter++
        if (tickCounter >= 20) {
            tickCounter = 0
            lastSecondPackets = 0
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
    
    fun relayPacket(packet: Message): Boolean {
        if (lastSecondPackets >= maxPacketsPerSecond) {
            return false // Rate limited
        }
        
        lastSecondPackets++
        packetsRelayed++
        return true
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        // Relay messages to other connected networks
        if (message.name() == "network.message" && relayPacket(message)) {
            _node?.sendToReachable(message.name(), *message.data())
        }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putLong("PacketsRelayed", packetsRelayed)
        tag.putInt("MaxPacketsPerSecond", maxPacketsPerSecond)
        tag.putInt("SignalRange", signalRange)
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        packetsRelayed = tag.getLong("PacketsRelayed")
        maxPacketsPerSecond = tag.getInt("MaxPacketsPerSecond").coerceAtLeast(1)
        signalRange = tag.getInt("SignalRange").coerceAtLeast(1)
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Access Point block entity - wireless network bridge.
 */
class AccessPointBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.ACCESS_POINT.get(), pos, state), Environment {
    
    companion object {
        const val DEFAULT_SIGNAL_STRENGTH = 16
        const val MAX_SIGNAL_STRENGTH = 400
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    // Wireless configuration
    private var signalStrength: Int = DEFAULT_SIGNAL_STRENGTH
    private var isRepeater: Boolean = false
    
    // Power consumption scales with signal strength
    private val powerPerTick: Double
        get() = signalStrength * 0.1
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(1000.0)
                .withComponent("access_point", ComponentVisibility.NETWORK)
                .build()
        }
    }
    
    fun openGui(player: net.minecraft.world.entity.player.Player) {
        // Open configuration GUI
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        // Consume power
        _node?.tryChangeBuffer(-powerPerTick)
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
    
    fun getSignalStrength(): Int = signalStrength
    
    fun setSignalStrength(strength: Int) {
        signalStrength = strength.coerceIn(1, MAX_SIGNAL_STRENGTH)
        setChanged()
    }
    
    fun broadcast(port: Int, data: Array<Any>): Boolean {
        // Send wireless message to all access points in range
        val level = level as? ServerLevel ?: return false
        
        // In full impl, would search for other access points
        // and send message if in range
        return true
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        when (message.name()) {
            "modem.broadcast" -> {
                val args = message.data()
                if (args.size >= 2) {
                    val port = args[0] as? Int ?: return
                    val data = args.drop(1).toTypedArray()
                    broadcast(port, data)
                }
            }
        }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("SignalStrength", signalStrength)
        tag.putBoolean("IsRepeater", isRepeater)
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        signalStrength = tag.getInt("SignalStrength").coerceIn(1, MAX_SIGNAL_STRENGTH)
        isRepeater = tag.getBoolean("IsRepeater")
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Adapter block entity - exposes adjacent blocks as components.
 */
class AdapterBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.ADAPTER.get(), pos, state), Environment {
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    // Adapted block info per side
    private val adaptedBlocks = mutableMapOf<Direction, AdaptedBlock>()
    
    data class AdaptedBlock(
        val pos: BlockPos,
        val blockName: String,
        val componentName: String
    )
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withComponent("adapter", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    fun updateAdaptedBlocks() {
        val level = level ?: return
        
        adaptedBlocks.clear()
        
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val neighborState = level.getBlockState(neighborPos)
            
            if (!neighborState.isAir) {
                // Check if we have a driver for this block
                val blockName = neighborState.block.descriptionId
                
                // In full impl, would check DriverRegistry for a matching driver
                adaptedBlocks[dir] = AdaptedBlock(
                    neighborPos,
                    blockName,
                    "external_$blockName"
                )
            }
        }
        
        setChanged()
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
            updateAdaptedBlocks()
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
 * Transposer block entity - moves items and fluids.
 */
class TransposerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.TRANSPOSER.get(), pos, state), Environment {
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(1000.0)
                .withComponent("transposer", ComponentVisibility.NEIGHBORS)
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
    
    // Transposer API methods
    
    fun transferItem(sourceSide: Direction, sinkSide: Direction, count: Int, sourceSlot: Int, sinkSlot: Int): Int {
        val level = level ?: return 0
        
        val sourcePos = blockPos.relative(sourceSide)
        val sinkPos = blockPos.relative(sinkSide)
        
        // Get item handlers from source and sink
        // In full impl, use capability system
        
        return 0 // Return number of items transferred
    }
    
    fun transferFluid(sourceSide: Direction, sinkSide: Direction, amount: Int): Int {
        val level = level ?: return 0
        
        val sourcePos = blockPos.relative(sourceSide)
        val sinkPos = blockPos.relative(sinkSide)
        
        // Get fluid handlers from source and sink
        // In full impl, use capability system
        
        return 0 // Return amount of fluid transferred
    }
    
    fun getInventorySize(side: Direction): Int {
        val level = level ?: return 0
        val neighborPos = blockPos.relative(side)
        
        // Get container capability
        // In full impl, return actual inventory size
        
        return 0
    }
    
    fun getFluidTankCapacity(side: Direction): Int {
        val level = level ?: return 0
        val neighborPos = blockPos.relative(side)
        
        // Get fluid capability
        // In full impl, return actual tank capacity
        
        return 0
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        when (message.name()) {
            "transposer.transferItem" -> {
                val args = message.data()
                // Parse arguments and call transferItem
            }
            "transposer.transferFluid" -> {
                val args = message.data()
                // Parse arguments and call transferFluid
            }
        }
    }
    
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

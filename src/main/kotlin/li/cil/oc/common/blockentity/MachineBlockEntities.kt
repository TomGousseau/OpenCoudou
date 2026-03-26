package li.cil.oc.common.blockentity

import li.cil.oc.api.network.ComponentVisibility
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.NodeBuilder
import li.cil.oc.common.block.DiskDriveBlock
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Containers
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler

/**
 * Disk Drive block entity - provides access to floppy disks.
 */
class DiskDriveBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.DISK_DRIVE.get(), pos, state), Environment {
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    val inventory: ItemStackHandler = object : ItemStackHandler(1) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
            updateBlockState()
            onDiskChanged()
        }
        
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            // Only accept floppy disks
            // In full impl, check for floppy item
            return true
        }
    }
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withComponent("disk_drive", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    private fun updateBlockState() {
        level?.let { lvl ->
            val state = lvl.getBlockState(blockPos)
            if (state.block is DiskDriveBlock) {
                val hasDisk = !inventory.getStackInSlot(0).isEmpty
                lvl.setBlock(blockPos, state.setValue(DiskDriveBlock.HAS_DISK, hasDisk), 3)
            }
        }
    }
    
    private fun onDiskChanged() {
        val node = _node ?: return
        val stack = inventory.getStackInSlot(0)
        
        if (stack.isEmpty) {
            node.sendToReachable("computer.signal", "component_removed", node.address())
        } else {
            node.sendToReachable("computer.signal", "component_added", node.address())
        }
    }
    
    fun openGui(player: Player) {
        // Open disk drive GUI
    }
    
    fun dropContents(level: Level, pos: BlockPos) {
        val stack = inventory.getStackInSlot(0)
        if (!stack.isEmpty) {
            Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
            inventory.setStackInSlot(0, ItemStack.EMPTY)
        }
    }
    
    fun eject(): ItemStack {
        val stack = inventory.extractItem(0, 64, false)
        return stack
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        when (message.name()) {
            "disk_drive.eject" -> eject()
        }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("Inventory", inventory.serializeNBT(registries))
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"))
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * RAID block entity - combines multiple hard drives.
 */
class RaidBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.RAID.get(), pos, state), Environment {
    
    companion object {
        const val DISK_SLOTS = 3
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    val inventory: ItemStackHandler = object : ItemStackHandler(DISK_SLOTS) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
            rebuildRaid()
        }
        
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            // Only accept hard drives
            return true
        }
    }
    
    // RAID label and combined filesystem UUID
    private var label: String = ""
    private var raidUuid: String? = null
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withComponent("filesystem", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    private fun rebuildRaid() {
        // Recalculate total capacity from all HDDs
        var totalCapacity = 0L
        
        for (i in 0 until DISK_SLOTS) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                // In full impl, get HDD capacity from item
                totalCapacity += 1024 * 1024 // Placeholder: 1MB per disk
            }
        }
        
        // Generate new UUID if needed
        if (raidUuid == null && totalCapacity > 0) {
            raidUuid = java.util.UUID.randomUUID().toString()
        }
    }
    
    fun openGui(player: Player) {
        // Open RAID GUI
    }
    
    fun dropContents(level: Level, pos: BlockPos) {
        for (i in 0 until DISK_SLOTS) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                inventory.setStackInSlot(i, ItemStack.EMPTY)
            }
        }
    }
    
    fun getLabel(): String = label
    
    fun setLabel(newLabel: String) {
        label = newLabel.take(32) // Max 32 chars
        setChanged()
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("Inventory", inventory.serializeNBT(registries))
        tag.putString("Label", label)
        raidUuid?.let { tag.putString("RaidUUID", it) }
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"))
        label = tag.getString("Label")
        raidUuid = if (tag.contains("RaidUUID")) tag.getString("RaidUUID") else null
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Rack block entity - houses servers and other rack-mounted components.
 */
class RackBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.RACK.get(), pos, state), Environment {
    
    companion object {
        const val SERVER_SLOTS = 4
        const val SLOTS_PER_SERVER = 8 // Each server has internal slots
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    // Server slots (each can hold a server or terminal server)
    val serverInventory: ItemStackHandler = object : ItemStackHandler(SERVER_SLOTS) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
            onServerChanged(slot)
        }
    }
    
    // Server connection mapping (which external side connects to which server)
    private val connectionMap = mutableMapOf<Direction, Int>()
    
    // Server states
    private val serverRunning = BooleanArray(SERVER_SLOTS) { false }
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(10000.0)
                .withComponent("rack", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    private fun onServerChanged(slot: Int) {
        if (slot in 0 until SERVER_SLOTS) {
            serverRunning[slot] = false
            setChanged()
        }
    }
    
    fun openGui(player: Player) {
        // Open rack GUI
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        // Update running servers
        for (i in 0 until SERVER_SLOTS) {
            if (serverRunning[i]) {
                // Update server
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
    
    fun startServer(slot: Int): Boolean {
        if (slot !in 0 until SERVER_SLOTS) return false
        if (serverInventory.getStackInSlot(slot).isEmpty) return false
        if (serverRunning[slot]) return false
        
        serverRunning[slot] = true
        setChanged()
        return true
    }
    
    fun stopServer(slot: Int): Boolean {
        if (slot !in 0 until SERVER_SLOTS) return false
        if (!serverRunning[slot]) return false
        
        serverRunning[slot] = false
        setChanged()
        return true
    }
    
    fun dropContents(level: Level, pos: BlockPos) {
        for (i in 0 until SERVER_SLOTS) {
            val stack = serverInventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                serverInventory.setStackInSlot(i, ItemStack.EMPTY)
            }
        }
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("ServerInventory", serverInventory.serializeNBT(registries))
        tag.putByteArray("ServerRunning", serverRunning.map { if (it) 1.toByte() else 0.toByte() }.toByteArray())
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        serverInventory.deserializeNBT(registries, tag.getCompound("ServerInventory"))
        
        val runningBytes = tag.getByteArray("ServerRunning")
        for (i in runningBytes.indices.take(SERVER_SLOTS)) {
            serverRunning[i] = runningBytes[i] != 0.toByte()
        }
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Assembler block entity - creates robots and other complex items.
 */
class AssemblerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.ASSEMBLER.get(), pos, state), Environment {
    
    companion object {
        const val INPUT_SLOTS = 20
        const val OUTPUT_SLOT = 20
        const val TOTAL_SLOTS = 21
        
        const val BASE_ASSEMBLY_TIME = 200 // 10 seconds at normal speed
        const val ENERGY_PER_TICK = 50.0
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    val inventory: ItemStackHandler = object : ItemStackHandler(TOTAL_SLOTS) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
        }
    }
    
    // Assembly state
    private var isAssembling: Boolean = false
    private var assemblyProgress: Int = 0
    private var assemblyTotal: Int = BASE_ASSEMBLY_TIME
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(10000.0)
                .withComponent("assembler", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    fun openGui(player: Player) {
        // Open assembler GUI
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        if (isAssembling) {
            val node = _node ?: return
            
            if (node.tryChangeBuffer(-ENERGY_PER_TICK)) {
                assemblyProgress++
                
                if (assemblyProgress >= assemblyTotal) {
                    completeAssembly()
                }
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
    
    fun startAssembly(): Boolean {
        if (isAssembling) return false
        if (!inventory.getStackInSlot(OUTPUT_SLOT).isEmpty) return false
        if (!canAssemble()) return false
        
        isAssembling = true
        assemblyProgress = 0
        assemblyTotal = calculateAssemblyTime()
        
        updateBlockState()
        return true
    }
    
    private fun canAssemble(): Boolean {
        // Check if we have valid components for assembly
        // In full impl, validate component compatibility
        return true
    }
    
    private fun calculateAssemblyTime(): Int {
        // Calculate based on complexity of item being assembled
        return BASE_ASSEMBLY_TIME
    }
    
    private fun completeAssembly() {
        // Create the assembled item
        // In full impl, create robot/drone/etc. with installed components
        
        isAssembling = false
        assemblyProgress = 0
        
        // Clear input slots, put result in output
        for (i in 0 until INPUT_SLOTS) {
            inventory.setStackInSlot(i, ItemStack.EMPTY)
        }
        
        // Set output
        // inventory.setStackInSlot(OUTPUT_SLOT, assembledItem)
        
        updateBlockState()
    }
    
    private fun updateBlockState() {
        level?.let { lvl ->
            val state = lvl.getBlockState(blockPos)
            if (state.block is li.cil.oc.common.block.AssemblerBlock) {
                lvl.setBlock(
                    blockPos,
                    state.setValue(li.cil.oc.common.block.AssemblerBlock.RUNNING, isAssembling),
                    3
                )
            }
        }
    }
    
    fun dropContents(level: Level, pos: BlockPos) {
        for (i in 0 until TOTAL_SLOTS) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                inventory.setStackInSlot(i, ItemStack.EMPTY)
            }
        }
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("Inventory", inventory.serializeNBT(registries))
        tag.putBoolean("IsAssembling", isAssembling)
        tag.putInt("AssemblyProgress", assemblyProgress)
        tag.putInt("AssemblyTotal", assemblyTotal)
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"))
        isAssembling = tag.getBoolean("IsAssembling")
        assemblyProgress = tag.getInt("AssemblyProgress")
        assemblyTotal = tag.getInt("AssemblyTotal")
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Disassembler block entity - breaks down items into components.
 */
class DisassemblerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.DISASSEMBLER.get(), pos, state), Environment {
    
    companion object {
        const val INPUT_SLOT = 0
        const val OUTPUT_SLOTS = 9
        const val TOTAL_SLOTS = 10
        
        const val BASE_DISASSEMBLY_TIME = 100 // 5 seconds
        const val ENERGY_PER_TICK = 25.0
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    val inventory: ItemStackHandler = object : ItemStackHandler(TOTAL_SLOTS) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
        }
    }
    
    private var isDisassembling: Boolean = false
    private var disassemblyProgress: Int = 0
    private var disassemblyTotal: Int = BASE_DISASSEMBLY_TIME
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(5000.0)
                .withComponent("disassembler", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    fun openGui(player: Player) {
        // Open disassembler GUI
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        if (!isDisassembling && !inventory.getStackInSlot(INPUT_SLOT).isEmpty) {
            startDisassembly()
        }
        
        if (isDisassembling) {
            val node = _node ?: return
            
            if (node.tryChangeBuffer(-ENERGY_PER_TICK)) {
                disassemblyProgress++
                
                if (disassemblyProgress >= disassemblyTotal) {
                    completeDisassembly()
                }
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
    
    private fun startDisassembly() {
        isDisassembling = true
        disassemblyProgress = 0
        disassemblyTotal = BASE_DISASSEMBLY_TIME
        updateBlockState()
    }
    
    private fun completeDisassembly() {
        val inputStack = inventory.getStackInSlot(INPUT_SLOT)
        
        // In full impl, break down the item into its components
        // and place them in output slots
        
        inventory.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY)
        
        isDisassembling = false
        disassemblyProgress = 0
        updateBlockState()
    }
    
    private fun updateBlockState() {
        level?.let { lvl ->
            val state = lvl.getBlockState(blockPos)
            if (state.block is li.cil.oc.common.block.DisassemblerBlock) {
                lvl.setBlock(
                    blockPos,
                    state.setValue(li.cil.oc.common.block.DisassemblerBlock.RUNNING, isDisassembling),
                    3
                )
            }
        }
    }
    
    fun dropContents(level: Level, pos: BlockPos) {
        for (i in 0 until TOTAL_SLOTS) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                inventory.setStackInSlot(i, ItemStack.EMPTY)
            }
        }
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("Inventory", inventory.serializeNBT(registries))
        tag.putBoolean("IsDisassembling", isDisassembling)
        tag.putInt("DisassemblyProgress", disassemblyProgress)
        tag.putInt("DisassemblyTotal", disassemblyTotal)
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"))
        isDisassembling = tag.getBoolean("IsDisassembling")
        disassemblyProgress = tag.getInt("DisassemblyProgress")
        disassemblyTotal = tag.getInt("DisassemblyTotal")
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Printer block entity - creates 3D printed objects.
 */
class PrinterBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.PRINTER.get(), pos, state), Environment {
    
    companion object {
        const val CHAMELIUM_SLOT = 0
        const val INK_SLOT = 1
        const val OUTPUT_SLOT = 2
        const val TOTAL_SLOTS = 3
        
        const val BASE_PRINT_TIME = 100
        const val ENERGY_PER_TICK = 10.0
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    val inventory: ItemStackHandler = object : ItemStackHandler(TOTAL_SLOTS) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
        }
    }
    
    // Print data
    private var printData: CompoundTag? = null
    private var isPrinting: Boolean = false
    private var printProgress: Int = 0
    private var printTotal: Int = BASE_PRINT_TIME
    
    // Material levels
    private var chameliumLevel: Int = 0
    private var inkLevel: Int = 0
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(5000.0)
                .withComponent("printer3d", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    fun openGui(player: Player) {
        // Open printer GUI
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        // Process materials
        processMaterials()
        
        if (isPrinting) {
            val node = _node ?: return
            
            if (node.tryChangeBuffer(-ENERGY_PER_TICK)) {
                printProgress++
                
                if (printProgress >= printTotal) {
                    completePrint()
                }
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
    
    private fun processMaterials() {
        // Convert chamelium items to internal level
        val chameliumStack = inventory.getStackInSlot(CHAMELIUM_SLOT)
        if (!chameliumStack.isEmpty && chameliumLevel < 8000) {
            chameliumStack.shrink(1)
            chameliumLevel += 1000
            setChanged()
        }
        
        // Process ink cartridges
        val inkStack = inventory.getStackInSlot(INK_SLOT)
        if (!inkStack.isEmpty && inkLevel < 100000) {
            inkStack.shrink(1)
            inkLevel += 10000
            setChanged()
        }
    }
    
    fun setPrintData(data: CompoundTag): Boolean {
        if (isPrinting) return false
        printData = data
        setChanged()
        return true
    }
    
    fun startPrint(): Boolean {
        if (isPrinting) return false
        if (printData == null) return false
        if (!inventory.getStackInSlot(OUTPUT_SLOT).isEmpty) return false
        
        // Calculate material requirements
        val requiredChamelium = 100 // Based on print complexity
        val requiredInk = 100
        
        if (chameliumLevel < requiredChamelium || inkLevel < requiredInk) {
            return false
        }
        
        chameliumLevel -= requiredChamelium
        inkLevel -= requiredInk
        
        isPrinting = true
        printProgress = 0
        printTotal = BASE_PRINT_TIME
        
        updateBlockState()
        return true
    }
    
    private fun completePrint() {
        // Create printed item from printData
        // In full impl, create a Print item with the stored shape data
        
        printData = null
        isPrinting = false
        printProgress = 0
        
        updateBlockState()
    }
    
    private fun updateBlockState() {
        level?.let { lvl ->
            val state = lvl.getBlockState(blockPos)
            if (state.block is li.cil.oc.common.block.PrinterBlock) {
                lvl.setBlock(
                    blockPos,
                    state.setValue(li.cil.oc.common.block.PrinterBlock.RUNNING, isPrinting),
                    3
                )
            }
        }
    }
    
    fun dropContents(level: Level, pos: BlockPos) {
        for (i in 0 until TOTAL_SLOTS) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                inventory.setStackInSlot(i, ItemStack.EMPTY)
            }
        }
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        when (message.name()) {
            "printer3d.commit" -> {
                val args = message.data()
                if (args.isNotEmpty()) {
                    val copies = (args[0] as? Int) ?: 1
                    startPrint()
                }
            }
            "printer3d.setData" -> {
                val args = message.data()
                if (args.isNotEmpty() && args[0] is CompoundTag) {
                    setPrintData(args[0] as CompoundTag)
                }
            }
        }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("Inventory", inventory.serializeNBT(registries))
        tag.putInt("ChameliumLevel", chameliumLevel)
        tag.putInt("InkLevel", inkLevel)
        tag.putBoolean("IsPrinting", isPrinting)
        tag.putInt("PrintProgress", printProgress)
        tag.putInt("PrintTotal", printTotal)
        
        printData?.let { tag.put("PrintData", it) }
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"))
        chameliumLevel = tag.getInt("ChameliumLevel")
        inkLevel = tag.getInt("InkLevel")
        isPrinting = tag.getBoolean("IsPrinting")
        printProgress = tag.getInt("PrintProgress")
        printTotal = tag.getInt("PrintTotal")
        
        printData = if (tag.contains("PrintData")) tag.getCompound("PrintData") else null
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

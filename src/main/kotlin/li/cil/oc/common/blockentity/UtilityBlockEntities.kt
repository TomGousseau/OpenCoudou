package li.cil.oc.common.blockentity

import li.cil.oc.api.machine.Machine
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.ComponentVisibility
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.NodeBuilder
import li.cil.oc.common.block.MicrocontrollerBlock
import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.machine.MachineImpl
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Geolyzer block entity - scans surrounding terrain.
 * 
 * Provides geological data about blocks in a configurable area around it.
 */
class GeolyzerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.GEOLYZER.get(), pos, state), Environment {
    
    companion object {
        const val MAX_RANGE = 32
        const val SCAN_COST = 10.0 // Energy per block
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(1000.0)
                .withComponent("geolyzer", ComponentVisibility.NEIGHBORS)
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
    
    /**
     * Scans a column of blocks for hardness data.
     * 
     * @param x Relative X from geolyzer
     * @param z Relative Z from geolyzer
     * @param y Start Y (relative)
     * @param height Number of blocks to scan
     * @param options Scan options (noise reduction, etc.)
     * @return Array of hardness values, or null if scan fails
     */
    fun scan(x: Int, z: Int, y: Int, height: Int, options: Map<String, Any>): DoubleArray? {
        val level = level ?: return null
        val node = _node ?: return null
        
        // Validate range
        val dist = sqrt((x * x + z * z).toDouble())
        if (dist > MAX_RANGE) return null
        
        // Calculate energy cost
        val cost = SCAN_COST * height
        if (!node.tryChangeBuffer(-cost)) return null
        
        // Scan blocks
        val result = DoubleArray(height)
        val includeReplaceable = options["includeReplaceable"] as? Boolean ?: false
        val noise = options["noise"] as? Double ?: 0.0
        
        for (i in 0 until height) {
            val scanPos = blockPos.offset(x, y + i, z)
            val scanState = level.getBlockState(scanPos)
            
            if (scanState.isAir) {
                result[i] = 0.0
            } else if (!includeReplaceable && scanState.canBeReplaced()) {
                result[i] = 0.0
            } else {
                // Get block hardness
                var hardness = scanState.getDestroySpeed(level, scanPos).toDouble()
                
                // Add noise if configured
                if (noise > 0) {
                    hardness += (Math.random() - 0.5) * noise
                }
                
                result[i] = hardness.coerceAtLeast(0.0)
            }
        }
        
        return result
    }
    
    /**
     * Analyzes a specific block position.
     */
    fun analyze(side: Direction, options: Map<String, Any>): Map<String, Any>? {
        val level = level ?: return null
        val node = _node ?: return null
        
        if (!node.tryChangeBuffer(-SCAN_COST * 2)) return null
        
        val targetPos = blockPos.relative(side)
        val targetState = level.getBlockState(targetPos)
        
        return mapOf(
            "name" to targetState.block.descriptionId,
            "hardness" to targetState.getDestroySpeed(level, targetPos).toDouble(),
            "harvestLevel" to 0, // In modern MC, this is handled differently
            "solid" to targetState.isSolidRender(level, targetPos),
            "metadata" to 0 // Legacy compatibility
        )
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        when (message.name()) {
            "geolyzer.scan" -> {
                val args = message.data()
                // Parse and handle scan request
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

/**
 * Motion Sensor block entity - detects entity movement.
 */
class MotionSensorBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.MOTION_SENSOR.get(), pos, state), Environment {
    
    companion object {
        const val DEFAULT_SENSITIVITY = 0.5
        const val MAX_RANGE = 8.0
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    private var sensitivity: Double = DEFAULT_SENSITIVITY
    private var lastPositions = mutableMapOf<Int, Triple<Double, Double, Double>>()
    private var tickCounter = 0
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(500.0)
                .withComponent("motion_sensor", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        // Check for motion every 5 ticks
        tickCounter++
        if (tickCounter >= 5) {
            tickCounter = 0
            checkMotion()
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
    
    private fun checkMotion() {
        val level = level ?: return
        val node = _node ?: return
        
        // Get all entities in range
        val range = MAX_RANGE
        val box = net.minecraft.world.phys.AABB(
            blockPos.x - range, blockPos.y - range, blockPos.z - range,
            blockPos.x + range + 1, blockPos.y + range + 1, blockPos.z + range + 1
        )
        
        val entities = level.getEntities(null, box) { entity ->
            entity is net.minecraft.world.entity.LivingEntity
        }
        
        val currentPositions = mutableMapOf<Int, Triple<Double, Double, Double>>()
        
        for (entity in entities) {
            val id = entity.id
            val currentPos = Triple(entity.x, entity.y, entity.z)
            currentPositions[id] = currentPos
            
            val lastPos = lastPositions[id]
            if (lastPos != null) {
                val dx = currentPos.first - lastPos.first
                val dy = currentPos.second - lastPos.second
                val dz = currentPos.third - lastPos.third
                val dist = sqrt(dx * dx + dy * dy + dz * dz)
                
                if (dist >= sensitivity) {
                    // Motion detected!
                    val relX = entity.x - blockPos.x
                    val relY = entity.y - blockPos.y
                    val relZ = entity.z - blockPos.z
                    
                    node.sendToReachable(
                        "computer.signal",
                        "motion",
                        relX, relY, relZ,
                        entity.name.string
                    )
                }
            }
        }
        
        lastPositions = currentPositions
    }
    
    fun getSensitivity(): Double = sensitivity
    
    fun setSensitivity(value: Double): Double {
        val old = sensitivity
        sensitivity = value.coerceIn(0.1, 1.0)
        setChanged()
        return old
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        when (message.name()) {
            "motion_sensor.setSensitivity" -> {
                val args = message.data()
                if (args.isNotEmpty()) {
                    val value = args[0] as? Double ?: return
                    setSensitivity(value)
                }
            }
        }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putDouble("Sensitivity", sensitivity)
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        sensitivity = tag.getDouble("Sensitivity").takeIf { it > 0 } ?: DEFAULT_SENSITIVITY
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Waypoint block entity - navigation marker.
 */
class WaypointBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.WAYPOINT.get(), pos, state), Environment {
    
    companion object {
        val COLORS = listOf(
            0xFF0000, // Red
            0xFFFF00, // Yellow
            0x00FF00, // Green
            0x00FFFF, // Cyan
            0x0000FF, // Blue
            0xFF00FF  // Magenta
        )
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    private var label: String = ""
    private var colorIndex: Int = 0
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withComponent("waypoint", ComponentVisibility.NETWORK)
                .build()
        }
    }
    
    fun getLabel(): String = label
    
    fun setLabel(newLabel: String) {
        label = newLabel.take(32)
        setChanged()
    }
    
    fun cycleLabel() {
        colorIndex = (colorIndex + 1) % COLORS.size
        setChanged()
    }
    
    fun getColor(): Int = COLORS[colorIndex]
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        when (message.name()) {
            "waypoint.setLabel" -> {
                val args = message.data()
                if (args.isNotEmpty()) {
                    val newLabel = args[0] as? String ?: return
                    setLabel(newLabel)
                }
            }
        }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putString("Label", label)
        tag.putInt("ColorIndex", colorIndex)
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        label = tag.getString("Label")
        colorIndex = tag.getInt("ColorIndex") % COLORS.size
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Hologram block entity - projects 3D holograms.
 */
class HologramBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.HOLOGRAM.get(), pos, state), Environment {
    
    companion object {
        const val WIDTH = 48
        const val HEIGHT = 32
        const val DEPTH = 48
        
        // Tier-based features
        val TIER_COLORS = mapOf(
            1 to 1,   // Monochrome
            2 to 3    // 3 color palette
        )
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    private var tier: Int = 1
    
    // Voxel data (packed as bits for efficiency)
    private var voxelData: ByteArray = ByteArray(WIDTH * HEIGHT * DEPTH / 8 + 1)
    
    // Color palette
    private var palette = IntArray(3) { 0x00FF00 } // Default green
    
    // Transform
    private var scale: Double = 1.0
    private var rotationX: Double = 0.0
    private var rotationY: Double = 0.0
    private var rotationZ: Double = 0.0
    private var translationX: Double = 0.0
    private var translationY: Double = 0.0
    private var translationZ: Double = 0.0
    
    // Dirty flag for client sync
    private var isDirty: Boolean = false
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(2000.0)
                .withComponent("hologram", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        // Power consumption based on active voxels
        val activeVoxels = countActiveVoxels()
        if (activeVoxels > 0) {
            val cost = 0.001 * activeVoxels
            _node?.tryChangeBuffer(-cost)
        }
        
        // Sync to clients if dirty
        if (isDirty) {
            level.sendBlockUpdated(blockPos, blockState, blockState, 3)
            isDirty = false
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
    
    // ========================================
    // Hologram API
    // ========================================
    
    fun clear() {
        voxelData.fill(0)
        isDirty = true
        setChanged()
    }
    
    fun set(x: Int, y: Int, z: Int, value: Boolean) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || z < 0 || z >= DEPTH) return
        
        val index = x + y * WIDTH + z * WIDTH * HEIGHT
        val byteIndex = index / 8
        val bitIndex = index % 8
        
        if (value) {
            voxelData[byteIndex] = (voxelData[byteIndex].toInt() or (1 shl bitIndex)).toByte()
        } else {
            voxelData[byteIndex] = (voxelData[byteIndex].toInt() and (1 shl bitIndex).inv()).toByte()
        }
        
        isDirty = true
        setChanged()
    }
    
    fun get(x: Int, y: Int, z: Int): Boolean {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || z < 0 || z >= DEPTH) return false
        
        val index = x + y * WIDTH + z * WIDTH * HEIGHT
        val byteIndex = index / 8
        val bitIndex = index % 8
        
        return (voxelData[byteIndex].toInt() and (1 shl bitIndex)) != 0
    }
    
    fun fill(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int, value: Boolean) {
        val minX = minOf(x1, x2).coerceIn(0, WIDTH - 1)
        val maxX = maxOf(x1, x2).coerceIn(0, WIDTH - 1)
        val minY = minOf(y1, y2).coerceIn(0, HEIGHT - 1)
        val maxY = maxOf(y1, y2).coerceIn(0, HEIGHT - 1)
        val minZ = minOf(z1, z2).coerceIn(0, DEPTH - 1)
        val maxZ = maxOf(z1, z2).coerceIn(0, DEPTH - 1)
        
        for (z in minZ..maxZ) {
            for (y in minY..maxY) {
                for (x in minX..maxX) {
                    set(x, y, z, value)
                }
            }
        }
    }
    
    fun setScale(newScale: Double) {
        scale = newScale.coerceIn(0.33, 3.0)
        isDirty = true
        setChanged()
    }
    
    fun setRotation(angle: Double, x: Double, y: Double, z: Double) {
        rotationX = x * angle
        rotationY = y * angle
        rotationZ = z * angle
        isDirty = true
        setChanged()
    }
    
    fun setTranslation(x: Double, y: Double, z: Double) {
        translationX = x.coerceIn(-1.0, 1.0)
        translationY = y.coerceIn(0.0, 2.0)
        translationZ = z.coerceIn(-1.0, 1.0)
        isDirty = true
        setChanged()
    }
    
    fun setPaletteColor(index: Int, color: Int) {
        if (index in palette.indices) {
            palette[index] = color and 0xFFFFFF
            isDirty = true
            setChanged()
        }
    }
    
    fun getPaletteColor(index: Int): Int {
        return palette.getOrElse(index) { 0x00FF00 }
    }
    
    private fun countActiveVoxels(): Int {
        var count = 0
        for (byte in voxelData) {
            var b = byte.toInt()
            while (b != 0) {
                count += b and 1
                b = b ushr 1
            }
        }
        return count
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        when (message.name()) {
            "hologram.clear" -> clear()
            "hologram.set" -> {
                val args = message.data()
                if (args.size >= 4) {
                    val x = args[0] as? Int ?: return
                    val y = args[1] as? Int ?: return
                    val z = args[2] as? Int ?: return
                    val value = args[3] as? Boolean ?: return
                    set(x, y, z, value)
                }
            }
            "hologram.fill" -> {
                val args = message.data()
                if (args.size >= 7) {
                    val x1 = args[0] as? Int ?: return
                    val y1 = args[1] as? Int ?: return
                    val z1 = args[2] as? Int ?: return
                    val x2 = args[3] as? Int ?: return
                    val y2 = args[4] as? Int ?: return
                    val z2 = args[5] as? Int ?: return
                    val value = args[6] as? Boolean ?: return
                    fill(x1, y1, z1, x2, y2, z2, value)
                }
            }
        }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("Tier", tier)
        tag.putByteArray("VoxelData", voxelData)
        tag.putIntArray("Palette", palette)
        tag.putDouble("Scale", scale)
        tag.putDouble("RotationX", rotationX)
        tag.putDouble("RotationY", rotationY)
        tag.putDouble("RotationZ", rotationZ)
        tag.putDouble("TranslationX", translationX)
        tag.putDouble("TranslationY", translationY)
        tag.putDouble("TranslationZ", translationZ)
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        tier = tag.getInt("Tier").coerceAtLeast(1)
        
        val savedVoxels = tag.getByteArray("VoxelData")
        if (savedVoxels.size == voxelData.size) {
            voxelData = savedVoxels
        }
        
        val savedPalette = tag.getIntArray("Palette")
        if (savedPalette.size == palette.size) {
            palette = savedPalette
        }
        
        scale = tag.getDouble("Scale").takeIf { it > 0 } ?: 1.0
        rotationX = tag.getDouble("RotationX")
        rotationY = tag.getDouble("RotationY")
        rotationZ = tag.getDouble("RotationZ")
        translationX = tag.getDouble("TranslationX")
        translationY = tag.getDouble("TranslationY")
        translationZ = tag.getDouble("TranslationZ")
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
    }
}

/**
 * Microcontroller block entity - simple programmable controller.
 * 
 * Microcontrollers are simpler than full computers:
 * - Fixed architecture (no hot-swapping components)
 * - Limited I/O (mostly redstone)
 * - Lower power consumption
 * - Can be crafted with specific components
 */
class MicrocontrollerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.MICROCONTROLLER.get(), pos, state), MachineHost, Environment {
    
    companion object {
        const val MAX_ENERGY = 5000.0
    }
    
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    private var _machine: MachineImpl? = null
    val machine: Machine?
        get() = _machine
    
    // Internal components (set at assembly time)
    private val internalInventory = mutableListOf<ItemStack>()
    
    // State
    private var isRunning = false
    
    // Redstone I/O
    private val redstoneOutputs = IntArray(6) { 0 }
    private val redstoneInputs = IntArray(6) { 0 }
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(MAX_ENERGY)
                .withComponent("microcontroller", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    // ========================================
    // MachineHost Implementation
    // ========================================
    
    override fun internalComponents(): Array<ItemStack> = internalInventory.toTypedArray()
    
    override fun componentSlot(address: String): Int = -1
    
    override fun markChanged() {
        setChanged()
    }
    
    override fun onMachineConnect(node: Node) {}
    override fun onMachineDisconnect(node: Node) {}
    
    override fun world(): Level? = level
    
    override fun xPosition(): Double = blockPos.x.toDouble()
    override fun yPosition(): Double = blockPos.y.toDouble()
    override fun zPosition(): Double = blockPos.z.toDouble()
    
    // ========================================
    // Control
    // ========================================
    
    fun toggle(): Boolean {
        return if (isRunning) stop() else start()
    }
    
    fun start(): Boolean {
        if (level?.isClientSide == true) return false
        if (isRunning) return false
        
        if (_machine == null) {
            _machine = MachineImpl(this)
        }
        
        val started = _machine?.start() == true
        if (started) {
            isRunning = true
            updateBlockState()
        }
        
        return started
    }
    
    fun stop(): Boolean {
        if (level?.isClientSide == true) return false
        if (!isRunning) return false
        
        val stopped = _machine?.stop() == true
        if (stopped) {
            isRunning = false
            updateBlockState()
        }
        
        return stopped
    }
    
    private fun updateBlockState() {
        level?.let { lvl ->
            val state = lvl.getBlockState(blockPos)
            if (state.block is MicrocontrollerBlock) {
                lvl.setBlock(blockPos, state.setValue(MicrocontrollerBlock.RUNNING, isRunning), 3)
            }
        }
    }
    
    // ========================================
    // Redstone
    // ========================================
    
    fun getRedstoneInput(side: Direction): Int = redstoneInputs[side.ordinal]
    
    fun getRedstoneOutput(side: Direction): Int = redstoneOutputs[side.ordinal]
    
    fun setRedstoneOutput(side: Direction, value: Int) {
        val clamped = value.coerceIn(0, 15)
        if (redstoneOutputs[side.ordinal] != clamped) {
            redstoneOutputs[side.ordinal] = clamped
            setChanged()
            level?.updateNeighborsAt(blockPos, blockState.block)
        }
    }
    
    fun onNeighborChanged() {
        val level = level ?: return
        var changed = false
        
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val signal = level.getSignal(neighborPos, dir)
            if (redstoneInputs[dir.ordinal] != signal) {
                redstoneInputs[dir.ordinal] = signal
                changed = true
            }
        }
        
        if (changed && isRunning) {
            _machine?.signal("redstone_changed")
        }
    }
    
    // ========================================
    // Tick
    // ========================================
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        
        if (isRunning) {
            _machine?.update()
            
            if (_machine?.isRunning == false) {
                isRunning = false
                updateBlockState()
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
    
    fun openGui(player: Player) {
        // Show microcontroller status
    }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putBoolean("Running", isRunning)
        tag.putIntArray("RedstoneOutputs", redstoneOutputs)
        tag.putIntArray("RedstoneInputs", redstoneInputs)
        
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.saveData(nodeTag)
            tag.put("Node", nodeTag)
        }
        
        _machine?.let { machine ->
            val machineTag = CompoundTag()
            machine.save(machineTag)
            tag.put("Machine", machineTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        isRunning = tag.getBoolean("Running")
        
        tag.getIntArray("RedstoneOutputs").copyInto(redstoneOutputs, 0, 0, minOf(6, tag.getIntArray("RedstoneOutputs").size))
        tag.getIntArray("RedstoneInputs").copyInto(redstoneInputs, 0, 0, minOf(6, tag.getIntArray("RedstoneInputs").size))
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
        
        if (tag.contains("Machine") && _node != null) {
            if (_machine == null) {
                _machine = MachineImpl(this)
            }
            _machine?.load(tag.getCompound("Machine"))
        }
    }
}

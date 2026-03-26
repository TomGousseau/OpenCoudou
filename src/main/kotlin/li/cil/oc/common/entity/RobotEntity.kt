package li.cil.oc.common.entity

import li.cil.oc.OpenComputers
import li.cil.oc.api.machine.Machine
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.ComponentVisibility
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.NodeBuilder
import li.cil.oc.server.machine.MachineImpl
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.ItemStackHandler
import kotlin.math.abs

/**
 * Robot entity - a mobile programmable machine.
 * 
 * Robots can:
 * - Move in any direction (one block at a time)
 * - Interact with blocks (mine, place, use)
 * - Carry inventory
 * - Host a computer system
 */
class RobotEntity(
    type: EntityType<out RobotEntity>,
    level: Level
) : PathfinderMob(type, level), MachineHost, Environment {
    
    companion object {
        // Synced data
        private val DATA_RUNNING: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.BOOLEAN
        )
        private val DATA_TIER: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.INT
        )
        private val DATA_SELECTED_SLOT: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.INT
        )
        private val DATA_LIGHT_COLOR: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.INT
        )
        
        // Constants
        const val BASE_ENERGY = 20000.0
        const val BASE_INVENTORY_SIZE = 16
        const val INTERNAL_SLOTS = 8 // CPU, RAM, etc.
        
        // Energy costs
        const val MOVE_COST = 15.0
        const val TURN_COST = 5.0
        const val MINE_COST = 25.0
        const val PLACE_COST = 10.0
        const val ATTACK_COST = 20.0
    }
    
    // ========================================
    // State
    // ========================================
    
    // Tier (determines capabilities)
    var tier: Int = 1
        private set
    
    // Energy system
    private var maxEnergy: Double = BASE_ENERGY
    
    // Machine
    private var _machine: MachineImpl? = null
    val machine: Machine?
        get() = _machine
    
    // Network node
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    // Movement
    private var targetPos: BlockPos? = null
    private var movementProgress: Float = 0f
    private var movementDirection: Direction = Direction.NORTH
    private var isMoving: Boolean = false
    
    // Orientation
    private var facing: Direction = Direction.NORTH
    
    // Inventory (internal components + robot inventory)
    val componentInventory: ItemStackHandler = ItemStackHandler(INTERNAL_SLOTS)
    val mainInventory: ItemStackHandler = ItemStackHandler(BASE_INVENTORY_SIZE)
    
    // Selected tool slot
    private var selectedSlot: Int = 0
    
    // Status light color (0xRRGGBB)
    private var lightColor: Int = 0x00FF00 // Green by default
    
    // Robot name
    private var robotName: String = ""
    
    // Experience (for upgrades)
    private var experienceValue: Double = 0.0
    
    // ========================================
    // Initialization
    // ========================================
    
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_RUNNING, false)
        builder.define(DATA_TIER, 1)
        builder.define(DATA_SELECTED_SLOT, 0)
        builder.define(DATA_LIGHT_COLOR, 0x00FF00)
    }
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(maxEnergy)
                .withComponent("robot", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }
    
    // ========================================
    // MachineHost Implementation
    // ========================================
    
    override fun internalComponents(): Array<ItemStack> {
        return (0 until INTERNAL_SLOTS)
            .map { componentInventory.getStackInSlot(it) }
            .filter { !it.isEmpty }
            .toTypedArray()
    }
    
    override fun componentSlot(address: String): Int {
        // Find slot by address
        return -1
    }
    
    override fun markChanged() {
        // Entity doesn't need this
    }
    
    override fun onMachineConnect(node: Node) {}
    override fun onMachineDisconnect(node: Node) {}
    
    override fun world(): Level? = level()
    
    override fun xPosition(): Double = x
    override fun yPosition(): Double = y
    override fun zPosition(): Double = z
    
    // ========================================
    // Environment Implementation
    // ========================================
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        // Handle robot-specific messages
    }
    
    // ========================================
    // Robot Control API
    // ========================================
    
    /**
     * Starts the robot's computer.
     */
    fun start(): Boolean {
        if (level().isClientSide) return false
        if (entityData.get(DATA_RUNNING)) return false
        
        if (_machine == null) {
            _machine = MachineImpl(this)
        }
        
        val started = _machine?.start() == true
        if (started) {
            entityData.set(DATA_RUNNING, true)
        }
        
        return started
    }
    
    /**
     * Stops the robot's computer.
     */
    fun stop(): Boolean {
        if (level().isClientSide) return false
        if (!entityData.get(DATA_RUNNING)) return false
        
        val stopped = _machine?.stop() == true
        if (stopped) {
            entityData.set(DATA_RUNNING, false)
        }
        
        return stopped
    }
    
    /**
     * Moves the robot one block in the specified direction.
     */
    fun move(direction: Direction): Boolean {
        if (isMoving) return false
        
        val node = _node ?: return false
        if (!node.tryChangeBuffer(-MOVE_COST)) return false
        
        val newPos = blockPosition().relative(direction)
        
        // Check if destination is valid
        if (!canMoveTo(newPos)) {
            return false
        }
        
        targetPos = newPos
        movementDirection = direction
        isMoving = true
        movementProgress = 0f
        
        return true
    }
    
    /**
     * Turns the robot to face a new direction.
     */
    fun turn(clockwise: Boolean): Boolean {
        val node = _node ?: return false
        if (!node.tryChangeBuffer(-TURN_COST)) return false
        
        facing = if (clockwise) {
            facing.clockWise
        } else {
            facing.counterClockWise
        }
        
        yRot = facing.toYRot()
        
        return true
    }
    
    /**
     * Swings the equipped tool (attacks or mines).
     */
    fun swing(side: Direction = facing): Boolean {
        val node = _node ?: return false
        if (!node.tryChangeBuffer(-ATTACK_COST)) return false
        
        val targetPos = blockPosition().relative(side)
        val level = level()
        
        // Check for entities to attack
        val entities = level.getEntities(this, boundingBox.expandTowards(
            side.stepX.toDouble(), side.stepY.toDouble(), side.stepZ.toDouble()
        ))
        
        if (entities.isNotEmpty()) {
            val target = entities.firstOrNull { it is LivingEntity }
            if (target is LivingEntity) {
                doHurtTarget(target)
                return true
            }
        }
        
        // Mine block
        val state = level.getBlockState(targetPos)
        if (!state.isAir && canHarvest(state)) {
            val tool = getSelectedItem()
            // Break block
            level.destroyBlock(targetPos, true, this)
            return true
        }
        
        return false
    }
    
    /**
     * Uses the equipped item (right-click action).
     */
    fun use(side: Direction = facing, sneaking: Boolean = false): Boolean {
        val node = _node ?: return false
        if (!node.tryChangeBuffer(-PLACE_COST)) return false
        
        val targetPos = blockPosition().relative(side)
        val level = level()
        
        // Try placing block
        val stack = getSelectedItem()
        if (!stack.isEmpty) {
            // Place block logic
            return true
        }
        
        return false
    }
    
    /**
     * Gets the currently selected inventory slot.
     */
    fun getSelectedSlot(): Int = selectedSlot
    
    /**
     * Sets the selected inventory slot.
     */
    fun setSelectedSlot(slot: Int): Int {
        val old = selectedSlot
        selectedSlot = slot.coerceIn(0, mainInventory.slots - 1)
        entityData.set(DATA_SELECTED_SLOT, selectedSlot)
        return old
    }
    
    /**
     * Gets the item in the selected slot.
     */
    fun getSelectedItem(): ItemStack = mainInventory.getStackInSlot(selectedSlot)
    
    /**
     * Gets remaining energy.
     */
    fun getEnergy(): Double = _node?.globalBuffer() ?: 0.0
    
    /**
     * Gets maximum energy capacity.
     */
    fun getMaxEnergy(): Double = _node?.globalBufferSize() ?: maxEnergy
    
    /**
     * Sets the status light color.
     */
    fun setLightColor(color: Int) {
        lightColor = color and 0xFFFFFF
        entityData.set(DATA_LIGHT_COLOR, lightColor)
    }
    
    fun getLightColor(): Int = lightColor
    
    // ========================================
    // Movement Helpers
    // ========================================
    
    private fun canMoveTo(pos: BlockPos): Boolean {
        val level = level()
        val state = level.getBlockState(pos)
        
        // Check if block is passable
        if (!state.isAir && state.isSolid) {
            return false
        }
        
        // Check collision
        return true
    }
    
    private fun canHarvest(state: BlockState): Boolean {
        val tool = getSelectedItem()
        // Check if tool can harvest
        return true
    }
    
    // ========================================
    // Tick
    // ========================================
    
    override fun tick() {
        super.tick()
        
        if (level().isClientSide) return
        
        // Initialize node
        if (_node == null) {
            initializeOnLoad()
        }
        
        // Update movement
        if (isMoving) {
            updateMovement()
        }
        
        // Update machine
        if (entityData.get(DATA_RUNNING)) {
            _machine?.update()
            
            if (_machine?.isRunning == false) {
                entityData.set(DATA_RUNNING, false)
            }
        }
    }
    
    private fun updateMovement() {
        val target = targetPos ?: return
        
        movementProgress += 0.05f // Adjust speed as needed
        
        if (movementProgress >= 1f) {
            // Movement complete
            setPos(target.x + 0.5, target.y.toDouble(), target.z + 0.5)
            isMoving = false
            targetPos = null
            movementProgress = 0f
            
            // Send movement complete signal
            _machine?.signal("move_completed")
        } else {
            // Interpolate position
            val start = blockPosition()
            val dx = (target.x - start.x) * movementProgress
            val dy = (target.y - start.y) * movementProgress
            val dz = (target.z - start.z) * movementProgress
            
            setPos(
                start.x + 0.5 + dx,
                start.y + dy,
                start.z + 0.5 + dz
            )
        }
    }
    
    // ========================================
    // Interaction
    // ========================================
    
    override fun interactAt(player: Player, vec: Vec3, hand: InteractionHand): InteractionResult {
        if (player.isCrouching) {
            // Open robot GUI
            if (!level().isClientSide) {
                openGui(player)
            }
            return InteractionResult.sidedSuccess(level().isClientSide)
        }
        
        return super.interactAt(player, vec, hand)
    }
    
    private fun openGui(player: Player) {
        // Open robot inventory/configuration GUI
    }
    
    // ========================================
    // Persistence
    // ========================================
    
    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        
        tag.putInt("Tier", tier)
        tag.putInt("Facing", facing.ordinal)
        tag.putInt("SelectedSlot", selectedSlot)
        tag.putInt("LightColor", lightColor)
        tag.putString("RobotName", robotName)
        tag.putDouble("Experience", experienceValue)
        tag.putBoolean("Running", entityData.get(DATA_RUNNING))
        
        tag.put("ComponentInventory", componentInventory.serializeNBT(level().registryAccess()))
        tag.put("MainInventory", mainInventory.serializeNBT(level().registryAccess()))
        
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
    
    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        
        tier = tag.getInt("Tier").coerceAtLeast(1)
        facing = Direction.entries.getOrElse(tag.getInt("Facing")) { Direction.NORTH }
        selectedSlot = tag.getInt("SelectedSlot")
        lightColor = tag.getInt("LightColor")
        robotName = tag.getString("RobotName")
        experienceValue = tag.getDouble("Experience")
        
        entityData.set(DATA_TIER, tier)
        entityData.set(DATA_SELECTED_SLOT, selectedSlot)
        entityData.set(DATA_LIGHT_COLOR, lightColor)
        
        componentInventory.deserializeNBT(level().registryAccess(), tag.getCompound("ComponentInventory"))
        mainInventory.deserializeNBT(level().registryAccess(), tag.getCompound("MainInventory"))
        
        if (tag.contains("Node")) {
            initializeOnLoad()
            _node?.loadData(tag.getCompound("Node"))
        }
        
        if (tag.contains("Machine")) {
            if (_machine == null) {
                _machine = MachineImpl(this)
            }
            _machine?.load(tag.getCompound("Machine"))
        }
        
        if (tag.getBoolean("Running")) {
            entityData.set(DATA_RUNNING, true)
        }
    }
}

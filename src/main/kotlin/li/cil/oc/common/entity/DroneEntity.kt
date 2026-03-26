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
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.ItemStackHandler
import kotlin.math.abs
import kotlin.math.sign

/**
 * Drone entity - a flying programmable machine.
 * 
 * Drones are lightweight, fast-moving robots that:
 * - Can fly freely in 3D space
 * - Have limited inventory (4 slots)
 * - Use less power than robots
 * - Cannot break blocks naturally
 */
class DroneEntity(
    type: EntityType<out DroneEntity>,
    level: Level
) : Entity(type, level), MachineHost, Environment {
    
    companion object {
        // Synced data
        private val DATA_RUNNING: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.BOOLEAN
        )
        private val DATA_LIGHT_COLOR: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.INT
        )
        private val DATA_TARGET_X: EntityDataAccessor<Float> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.FLOAT
        )
        private val DATA_TARGET_Y: EntityDataAccessor<Float> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.FLOAT
        )
        private val DATA_TARGET_Z: EntityDataAccessor<Float> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.FLOAT
        )
        
        // Constants
        const val BASE_ENERGY = 10000.0
        const val INVENTORY_SIZE = 4
        const val INTERNAL_SLOTS = 4
        
        // Movement
        const val MAX_SPEED = 0.45 // Blocks per tick
        const val ACCELERATION = 0.15
        const val MOVE_COST_PER_TICK = 0.5
    }
    
    // ========================================
    // State
    // ========================================
    
    var tier: Int = 1
        private set
    
    private var maxEnergy: Double = BASE_ENERGY
    
    // Machine
    private var _machine: MachineImpl? = null
    val machine: Machine?
        get() = _machine
    
    // Network
    private var _node: Node? = null
    override fun node(): Node? = _node
    
    // Movement
    private var targetPos: Vec3? = null
    private var velocity: Vec3 = Vec3.ZERO
    
    // Inventory
    val componentInventory: ItemStackHandler = ItemStackHandler(INTERNAL_SLOTS)
    val mainInventory: ItemStackHandler = ItemStackHandler(INVENTORY_SIZE)
    
    // Selected slot
    private var selectedSlot: Int = 0
    
    // Status light
    private var lightColor: Int = 0x00FF00
    
    // Name
    private var droneName: String = ""
    
    // ========================================
    // Initialization
    // ========================================
    
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(DATA_RUNNING, false)
        builder.define(DATA_LIGHT_COLOR, 0x00FF00)
        builder.define(DATA_TARGET_X, 0f)
        builder.define(DATA_TARGET_Y, 0f)
        builder.define(DATA_TARGET_Z, 0f)
    }
    
    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(maxEnergy)
                .withComponent("drone", ComponentVisibility.NEIGHBORS)
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
    
    override fun componentSlot(address: String): Int = -1
    override fun markChanged() {}
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
    override fun onMessage(message: Message) {}
    
    // ========================================
    // Drone Control API
    // ========================================
    
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
     * Moves the drone to an absolute position.
     */
    fun moveToward(x: Double, y: Double, z: Double): Boolean {
        targetPos = Vec3(x, y, z)
        entityData.set(DATA_TARGET_X, x.toFloat())
        entityData.set(DATA_TARGET_Y, y.toFloat())
        entityData.set(DATA_TARGET_Z, z.toFloat())
        return true
    }
    
    /**
     * Moves the drone by a relative offset.
     */
    fun move(dx: Double, dy: Double, dz: Double): Boolean {
        return moveToward(x + dx, y + dy, z + dz)
    }
    
    /**
     * Gets the drone's current offset from its controller.
     */
    fun getOffset(): Triple<Double, Double, Double> {
        return Triple(x, y, z)
    }
    
    /**
     * Gets the drone's current velocity.
     */
    fun getVelocity(): Triple<Double, Double, Double> {
        return Triple(velocity.x, velocity.y, velocity.z)
    }
    
    /**
     * Returns the drone's name.
     */
    fun getName(): String = droneName
    
    /**
     * Sets the drone's name.
     */
    fun setDroneName(name: String) {
        droneName = name.take(32)
    }
    
    fun setSelectedSlot(slot: Int): Int {
        val old = selectedSlot
        selectedSlot = slot.coerceIn(0, INVENTORY_SIZE - 1)
        return old
    }
    
    fun getSelectedSlot(): Int = selectedSlot
    
    fun getSelectedItem(): ItemStack = mainInventory.getStackInSlot(selectedSlot)
    
    fun setLightColor(color: Int) {
        lightColor = color and 0xFFFFFF
        entityData.set(DATA_LIGHT_COLOR, lightColor)
    }
    
    fun getLightColor(): Int = lightColor
    
    fun getEnergy(): Double = _node?.globalBuffer() ?: 0.0
    fun getMaxEnergy(): Double = _node?.globalBufferSize() ?: maxEnergy
    
    // ========================================
    // Tick
    // ========================================
    
    override fun tick() {
        super.tick()
        
        if (level().isClientSide) {
            // Client-side interpolation
            return
        }
        
        if (_node == null) {
            initializeOnLoad()
        }
        
        // Update movement
        updateMovement()
        
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
        val node = _node ?: return
        
        // Check energy
        if (!node.tryChangeBuffer(-MOVE_COST_PER_TICK)) {
            // Out of power - stop
            targetPos = null
            velocity = Vec3.ZERO
            return
        }
        
        // Calculate direction to target
        val dx = target.x - x
        val dy = target.y - y
        val dz = target.z - z
        val distance = target.distanceTo(position())
        
        if (distance < 0.1) {
            // Arrived
            setPos(target.x, target.y, target.z)
            targetPos = null
            velocity = Vec3.ZERO
            _machine?.signal("move_completed")
            return
        }
        
        // Normalize direction and apply acceleration
        val direction = Vec3(dx, dy, dz).normalize()
        
        // Smooth acceleration
        velocity = Vec3(
            approach(velocity.x, direction.x * MAX_SPEED, ACCELERATION),
            approach(velocity.y, direction.y * MAX_SPEED, ACCELERATION),
            approach(velocity.z, direction.z * MAX_SPEED, ACCELERATION)
        )
        
        // Apply movement
        move(MoverType.SELF, velocity)
        
        // Check for collision
        if (horizontalCollision || verticalCollision) {
            _machine?.signal("move_blocked")
        }
    }
    
    private fun approach(current: Double, target: Double, step: Double): Double {
        val diff = target - current
        return if (abs(diff) <= step) {
            target
        } else {
            current + sign(diff) * step
        }
    }
    
    // ========================================
    // Physics
    // ========================================
    
    override fun isNoGravity(): Boolean = true
    
    override fun isPushable(): Boolean = false
    
    // ========================================
    // Persistence
    // ========================================
    
    override fun addAdditionalSaveData(tag: CompoundTag) {
        tag.putInt("Tier", tier)
        tag.putInt("SelectedSlot", selectedSlot)
        tag.putInt("LightColor", lightColor)
        tag.putString("DroneName", droneName)
        tag.putBoolean("Running", entityData.get(DATA_RUNNING))
        
        targetPos?.let {
            tag.putDouble("TargetX", it.x)
            tag.putDouble("TargetY", it.y)
            tag.putDouble("TargetZ", it.z)
        }
        
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
        tier = tag.getInt("Tier").coerceAtLeast(1)
        selectedSlot = tag.getInt("SelectedSlot")
        lightColor = tag.getInt("LightColor")
        droneName = tag.getString("DroneName")
        
        entityData.set(DATA_LIGHT_COLOR, lightColor)
        
        if (tag.contains("TargetX")) {
            targetPos = Vec3(
                tag.getDouble("TargetX"),
                tag.getDouble("TargetY"),
                tag.getDouble("TargetZ")
            )
        }
        
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

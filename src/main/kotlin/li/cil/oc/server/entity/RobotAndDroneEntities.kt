package li.cil.oc.server.entity

import li.cil.oc.OpenComputers
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.init.ModEntities
import li.cil.oc.server.machine.MachineImpl
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemStackHandler
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Drone entity implementation.
 * 
 * Drones are flying robots that can move freely in 3D space.
 * They have limited inventory but can move quickly and navigate complex terrain.
 */
class DroneEntity(
    entityType: EntityType<out DroneEntity>,
    level: Level
) : PathfinderMob(entityType, level), Container {
    
    // === State ===
    private var running = false
    private var tier = 1
    private var maxEnergy = 10000.0
    private var currentEnergy = maxEnergy
    private var energyPerTick = 0.5
    
    // Movement state
    private var targetPosition: Vec3? = null
    private var targetVelocity = Vec3.ZERO
    private var acceleration = 1.5
    private var maxVelocity = 6.0
    private var statusText = ""
    private var statusColor = 0xFFFFFF
    
    // Inventory
    private val inventory = ItemStackHandler(8)
    private var selectedSlot = 0
    
    // Components installed
    private val components = mutableMapOf<String, CompoundTag>()
    
    // Machine reference
    private var machine: MachineImpl? = null
    
    // Owner
    private var ownerUUID: UUID? = null
    private var ownerName = ""
    
    // Lighting
    private var lightColor = 0x66DD55
    private var lightDistance = 8
    
    companion object {
        // Synced data
        private val DATA_RUNNING: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.BOOLEAN
        )
        private val DATA_STATUS_TEXT: EntityDataAccessor<String> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.STRING
        )
        private val DATA_STATUS_COLOR: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.INT
        )
        private val DATA_LIGHT_COLOR: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.INT
        )
        
        fun createAttributes(): AttributeSupplier.Builder = createMobAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FLYING_SPEED, 0.4)
    }
    
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_RUNNING, false)
        builder.define(DATA_STATUS_TEXT, "")
        builder.define(DATA_STATUS_COLOR, 0xFFFFFF)
        builder.define(DATA_LIGHT_COLOR, 0x66DD55)
    }
    
    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, DroneFollowTargetGoal(this))
    }
    
    override fun tick() {
        super.tick()
        
        if (!level().isClientSide) {
            // Energy consumption
            if (running) {
                currentEnergy -= energyPerTick
                if (currentEnergy <= 0) {
                    currentEnergy = 0.0
                    stop()
                }
                
                // Update machine
                machine?.update()
            }
            
            // Movement towards target
            if (targetPosition != null) {
                val current = position()
                val target = targetPosition!!
                val delta = target.subtract(current)
                val distance = delta.length()
                
                if (distance > 0.1) {
                    // Accelerate towards target
                    val direction = delta.normalize()
                    val speed = min(maxVelocity, distance * 0.5)
                    val newVel = direction.scale(speed)
                    
                    // Smooth velocity change
                    targetVelocity = Vec3(
                        lerp(targetVelocity.x, newVel.x, acceleration * 0.1),
                        lerp(targetVelocity.y, newVel.y, acceleration * 0.1),
                        lerp(targetVelocity.z, newVel.z, acceleration * 0.1)
                    )
                    
                    deltaMovement = targetVelocity.scale(0.05)
                    move(MoverType.SELF, deltaMovement)
                } else {
                    targetPosition = null
                    targetVelocity = Vec3.ZERO
                    deltaMovement = Vec3.ZERO
                }
            } else {
                // Hover in place with slight bob
                val hoverBob = kotlin.math.sin(tickCount * 0.1) * 0.01
                deltaMovement = Vec3(0.0, hoverBob, 0.0)
            }
        }
    }
    
    private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
    
    // === State Control ===
    
    fun start(): Boolean {
        if (running) return false
        if (currentEnergy <= 0) return false
        
        running = true
        entityData.set(DATA_RUNNING, true)
        
        // Initialize machine
        if (machine == null) {
            machine = MachineImpl(
                level = level(),
                position = blockPosition(),
                tier = tier,
                accelerated = false
            )
        }
        machine?.start()
        
        return true
    }
    
    fun stop(): Boolean {
        if (!running) return false
        
        running = false
        entityData.set(DATA_RUNNING, false)
        targetPosition = null
        targetVelocity = Vec3.ZERO
        
        machine?.stop()
        
        return true
    }
    
    fun isRunning(): Boolean = running
    
    // === Movement ===
    
    fun moveTo(x: Double, y: Double, z: Double): Boolean {
        if (!running) return false
        
        // Check if target is valid (within range and accessible)
        val target = Vec3(x, y, z)
        val distance = position().distanceTo(target)
        
        // Max move range per tick based on tier
        val maxRange = when (tier) {
            1 -> 16.0
            2 -> 32.0
            else -> 64.0
        }
        
        if (distance > maxRange) {
            return false
        }
        
        // Check collision along path
        val obstructed = level().getBlockCollisions(this, AABB.ofSize(target, 0.5, 0.5, 0.5)).iterator().hasNext()
        if (obstructed) {
            return false
        }
        
        targetPosition = target
        return true
    }
    
    fun getVelocity(): Vec3 = targetVelocity
    
    fun getOffset(): Vec3 = targetPosition?.subtract(position()) ?: Vec3.ZERO
    
    // === Status Display ===
    
    fun setStatusText(text: String) {
        statusText = text.take(32)
        entityData.set(DATA_STATUS_TEXT, statusText)
    }
    
    fun getStatusText(): String = entityData.get(DATA_STATUS_TEXT)
    
    fun setStatusColor(color: Int) {
        statusColor = color and 0xFFFFFF
        entityData.set(DATA_STATUS_COLOR, statusColor)
    }
    
    fun getStatusColor(): Int = entityData.get(DATA_STATUS_COLOR)
    
    fun setLightColor(color: Int) {
        lightColor = color and 0xFFFFFF
        entityData.set(DATA_LIGHT_COLOR, lightColor)
    }
    
    fun getLightColor(): Int = entityData.get(DATA_LIGHT_COLOR)
    
    // === Energy ===
    
    fun getEnergy(): Double = currentEnergy
    fun getMaxEnergy(): Double = maxEnergy
    
    fun addEnergy(amount: Double): Double {
        val added = min(amount, maxEnergy - currentEnergy)
        currentEnergy += added
        return added
    }
    
    // === Inventory ===
    
    override fun getContainerSize(): Int = inventory.slots
    
    override fun isEmpty(): Boolean {
        for (i in 0 until inventory.slots) {
            if (!inventory.getStackInSlot(i).isEmpty) {
                return false
            }
        }
        return true
    }
    
    override fun getItem(slot: Int): ItemStack = inventory.getStackInSlot(slot)
    
    override fun removeItem(slot: Int, amount: Int): ItemStack = inventory.extractItem(slot, amount, false)
    
    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val stack = inventory.getStackInSlot(slot)
        inventory.setStackInSlot(slot, ItemStack.EMPTY)
        return stack
    }
    
    override fun setItem(slot: Int, stack: ItemStack) {
        inventory.setStackInSlot(slot, stack)
    }
    
    override fun setChanged() {}
    
    override fun stillValid(player: Player): Boolean = isAlive && player.distanceToSqr(this) <= 64.0
    
    override fun clearContent() {
        for (i in 0 until inventory.slots) {
            inventory.setStackInSlot(i, ItemStack.EMPTY)
        }
    }
    
    fun getSelectedSlot(): Int = selectedSlot
    
    fun setSelectedSlot(slot: Int) {
        selectedSlot = slot.coerceIn(0, inventory.slots - 1)
    }
    
    fun getItemHandler(): IItemHandler = inventory
    
    // === Interaction ===
    
    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS
        }
        
        val stack = player.getItemInHand(hand)
        
        // Open GUI
        // player.openMenu(this)
        
        return InteractionResult.SUCCESS
    }
    
    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (source.entity is Player && !running) {
            // Drop as item when hit by player while not running
            dropAsItem()
            return true
        }
        return super.hurt(source, amount)
    }
    
    private fun dropAsItem() {
        if (!level().isClientSide) {
            // Create drone item with all data
            val stack = ItemStack(li.cil.oc.common.init.ModItems.DRONE_CASE_TIER1.get())
            
            // Save drone data to item
            val tag = CompoundTag()
            saveData(tag)
            // stack.tag = tag
            
            spawnAtLocation(stack)
            
            // Drop inventory
            for (i in 0 until inventory.slots) {
                val itemStack = inventory.getStackInSlot(i)
                if (!itemStack.isEmpty) {
                    spawnAtLocation(itemStack)
                }
            }
            
            discard()
        }
    }
    
    // === Serialization ===
    
    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        saveData(tag)
    }
    
    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        loadData(tag)
    }
    
    private fun saveData(tag: CompoundTag) {
        tag.putBoolean("running", running)
        tag.putInt("tier", tier)
        tag.putDouble("energy", currentEnergy)
        tag.putDouble("maxEnergy", maxEnergy)
        tag.putInt("selectedSlot", selectedSlot)
        tag.putString("statusText", statusText)
        tag.putInt("statusColor", statusColor)
        tag.putInt("lightColor", lightColor)
        
        if (ownerUUID != null) {
            tag.putUUID("owner", ownerUUID!!)
            tag.putString("ownerName", ownerName)
        }
        
        // Inventory
        tag.put("inventory", inventory.serializeNBT(level().registryAccess()))
        
        // Components
        val componentsList = ListTag()
        for ((address, componentTag) in components) {
            val componentEntry = CompoundTag()
            componentEntry.putString("address", address)
            componentEntry.put("data", componentTag)
            componentsList.add(componentEntry)
        }
        tag.put("components", componentsList)
        
        // Machine state
        machine?.let { m ->
            val machineTag = CompoundTag()
            // m.save(machineTag)
            tag.put("machine", machineTag)
        }
    }
    
    private fun loadData(tag: CompoundTag) {
        running = tag.getBoolean("running")
        tier = tag.getInt("tier")
        currentEnergy = tag.getDouble("energy")
        maxEnergy = tag.getDouble("maxEnergy")
        selectedSlot = tag.getInt("selectedSlot")
        statusText = tag.getString("statusText")
        statusColor = tag.getInt("statusColor")
        lightColor = tag.getInt("lightColor")
        
        if (tag.contains("owner")) {
            ownerUUID = tag.getUUID("owner")
            ownerName = tag.getString("ownerName")
        }
        
        // Inventory
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(level().registryAccess(), tag.getCompound("inventory"))
        }
        
        // Components
        components.clear()
        val componentsList = tag.getList("components", 10)
        for (i in 0 until componentsList.size) {
            val componentEntry = componentsList.getCompound(i)
            val address = componentEntry.getString("address")
            val componentTag = componentEntry.getCompound("data")
            components[address] = componentTag
        }
        
        // Machine state
        if (tag.contains("machine")) {
            // Initialize and load machine
        }
        
        // Sync to client
        entityData.set(DATA_RUNNING, running)
        entityData.set(DATA_STATUS_TEXT, statusText)
        entityData.set(DATA_STATUS_COLOR, statusColor)
        entityData.set(DATA_LIGHT_COLOR, lightColor)
    }
    
    // === Properties ===
    
    override fun isPushable(): Boolean = false
    
    override fun isNoGravity(): Boolean = true
    
    override fun getMyRidingOffset(vehicle: Entity): Float = 0.1f
    
    override fun canBeCollidedWith(): Boolean = true
    
    override fun isPickable(): Boolean = true
    
    // === Factory ===
    
    fun setTier(tier: Int) {
        this.tier = tier.coerceIn(1, 3)
        
        // Update properties based on tier
        when (this.tier) {
            1 -> {
                maxEnergy = 10000.0
                maxVelocity = 4.0
                acceleration = 1.0
            }
            2 -> {
                maxEnergy = 25000.0
                maxVelocity = 6.0
                acceleration = 1.5
            }
            3 -> {
                maxEnergy = 50000.0
                maxVelocity = 8.0
                acceleration = 2.0
            }
        }
        currentEnergy = maxEnergy
    }
    
    fun setOwner(player: Player) {
        ownerUUID = player.uuid
        ownerName = player.name.string
    }
}

/**
 * AI Goal for drone movement.
 */
class DroneFollowTargetGoal(private val drone: DroneEntity) : Goal() {
    
    override fun canUse(): Boolean {
        return drone.isRunning()
    }
    
    override fun tick() {
        // Movement is handled in drone tick
    }
}

/**
 * Represents a robot entity that can interact with the world.
 * 
 * Robots are more powerful than drones but move on blocks only.
 * They have larger inventory and more upgrade slots.
 */
class RobotEntity(
    entityType: EntityType<out RobotEntity>,
    level: Level
) : PathfinderMob(entityType, level), Container {
    
    // === State ===
    private var running = false
    private var tier = 1
    private var maxEnergy = 50000.0
    private var currentEnergy = maxEnergy
    private var energyPerTick = 1.0
    
    // Movement state
    private var moving = false
    private var targetPos: BlockPos? = null
    private var facing: Direction = Direction.NORTH
    
    // Inventory
    private val mainInventory = ItemStackHandler(16)
    private val toolInventory = ItemStackHandler(1)
    private val upgradeInventory = ItemStackHandler(9)
    private var selectedSlot = 0
    
    // Tank (fluid storage)
    private var tankCapacity = 16000
    
    // Experience
    private var storedXP = 0
    
    // Owner
    private var ownerUUID: UUID? = null
    private var ownerName = ""
    
    // Light
    private var lightColor = 0x66DD55
    
    // Machine
    private var machine: MachineImpl? = null
    
    companion object {
        private val DATA_RUNNING: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.BOOLEAN
        )
        private val DATA_FACING: EntityDataAccessor<Direction> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.DIRECTION
        )
        private val DATA_LIGHT_COLOR: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.INT
        )
        private val DATA_MOVING: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.BOOLEAN
        )
        
        fun createAttributes(): AttributeSupplier.Builder = createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
    }
    
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_RUNNING, false)
        builder.define(DATA_FACING, Direction.NORTH)
        builder.define(DATA_LIGHT_COLOR, 0x66DD55)
        builder.define(DATA_MOVING, false)
    }
    
    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, RobotMoveGoal(this))
    }
    
    override fun tick() {
        super.tick()
        
        if (!level().isClientSide) {
            if (running) {
                // Consume energy
                currentEnergy -= energyPerTick
                if (currentEnergy <= 0) {
                    currentEnergy = 0.0
                    stop()
                }
                
                machine?.update()
            }
            
            // Handle movement
            if (moving && targetPos != null) {
                val target = Vec3.atCenterOf(targetPos!!)
                val current = position()
                val distance = current.distanceTo(target)
                
                if (distance > 0.1) {
                    val direction = target.subtract(current).normalize()
                    val speed = 0.15
                    deltaMovement = direction.scale(speed)
                    move(MoverType.SELF, deltaMovement)
                } else {
                    // Reached target
                    setPos(target.x, target.y, target.z)
                    moving = false
                    targetPos = null
                    entityData.set(DATA_MOVING, false)
                }
            }
        }
    }
    
    // === State Control ===
    
    fun start(): Boolean {
        if (running) return false
        if (currentEnergy <= 0) return false
        
        running = true
        entityData.set(DATA_RUNNING, true)
        
        if (machine == null) {
            machine = MachineImpl(
                level = level(),
                position = blockPosition(),
                tier = tier,
                accelerated = false
            )
        }
        machine?.start()
        
        return true
    }
    
    fun stop(): Boolean {
        if (!running) return false
        
        running = false
        entityData.set(DATA_RUNNING, false)
        moving = false
        targetPos = null
        
        machine?.stop()
        
        return true
    }
    
    fun isRunning(): Boolean = running
    
    // === Movement ===
    
    fun move(direction: Direction): Boolean {
        if (!running) return false
        if (moving) return false
        
        val from = blockPosition()
        val to = from.relative(direction)
        
        // Check if target is valid
        if (!canMoveTo(to)) {
            return false
        }
        
        // Check energy cost
        val cost = getMovementEnergyCost(direction)
        if (currentEnergy < cost) {
            return false
        }
        
        currentEnergy -= cost
        targetPos = to
        moving = true
        entityData.set(DATA_MOVING, true)
        
        return true
    }
    
    fun turn(clockwise: Boolean): Boolean {
        if (!running) return false
        if (moving) return false
        
        facing = if (clockwise) {
            facing.clockWise
        } else {
            facing.counterClockWise
        }
        entityData.set(DATA_FACING, facing)
        
        return true
    }
    
    fun turnAround(): Boolean {
        if (!running) return false
        if (moving) return false
        
        facing = facing.opposite
        entityData.set(DATA_FACING, facing)
        
        return true
    }
    
    private fun canMoveTo(pos: BlockPos): Boolean {
        val level = level()
        
        // Check if destination is solid ground or air above ground
        val below = pos.below()
        val atPos = level.getBlockState(pos)
        val belowPos = level.getBlockState(below)
        
        // Need ground below and air at destination
        if (!belowPos.isSolid) {
            return false  // No ground
        }
        
        if (!atPos.isAir && !atPos.canBeReplaced()) {
            return false  // Blocked
        }
        
        // Check space above for 2-block tall entity
        val above = pos.above()
        val aboveState = level.getBlockState(above)
        if (!aboveState.isAir && !aboveState.canBeReplaced()) {
            return false
        }
        
        return true
    }
    
    private fun getMovementEnergyCost(direction: Direction): Double {
        return when (direction) {
            Direction.UP -> 25.0
            Direction.DOWN -> 5.0
            else -> 15.0
        }
    }
    
    fun getFacing(): Direction = entityData.get(DATA_FACING)
    
    fun isMoving(): Boolean = moving
    
    // === Actions ===
    
    fun swing(side: Direction): Boolean {
        if (!running) return false
        
        val target = blockPosition().relative(side)
        val tool = toolInventory.getStackInSlot(0)
        
        // Attack entities
        val entities = level().getEntities(this, AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(target)).inflate(0.5))
        for (entity in entities) {
            if (entity is LivingEntity) {
                // Attack entity
                val damage = if (tool.isEmpty) 2.0f else 4.0f
                entity.hurt(damageSources().mobAttack(this), damage)
                
                currentEnergy -= 5.0
                return true
            }
        }
        
        // Mine block
        val state = level().getBlockState(target)
        if (!state.isAir) {
            val hardness = state.getDestroySpeed(level(), target)
            if (hardness >= 0 && hardness < 50) {
                level().destroyBlock(target, true, this)
                currentEnergy -= hardness * 2
                return true
            }
        }
        
        return false
    }
    
    fun use(side: Direction, sneaking: Boolean): Boolean {
        if (!running) return false
        
        val target = blockPosition().relative(side)
        val tool = toolInventory.getStackInSlot(0)
        
        // Use item on block
        // This would use the robot's currently selected item
        
        currentEnergy -= 2.0
        return true
    }
    
    fun place(side: Direction): Boolean {
        if (!running) return false
        
        val stack = mainInventory.getStackInSlot(selectedSlot)
        if (stack.isEmpty) {
            return false
        }
        
        val target = blockPosition().relative(side)
        
        // Place block
        // ... block placement logic
        
        currentEnergy -= 2.0
        return true
    }
    
    // === Inventory ===
    
    override fun getContainerSize(): Int = mainInventory.slots + toolInventory.slots + upgradeInventory.slots
    
    override fun isEmpty(): Boolean {
        for (i in 0 until mainInventory.slots) {
            if (!mainInventory.getStackInSlot(i).isEmpty) return false
        }
        return true
    }
    
    override fun getItem(slot: Int): ItemStack {
        return when {
            slot < mainInventory.slots -> mainInventory.getStackInSlot(slot)
            slot < mainInventory.slots + toolInventory.slots -> toolInventory.getStackInSlot(slot - mainInventory.slots)
            else -> upgradeInventory.getStackInSlot(slot - mainInventory.slots - toolInventory.slots)
        }
    }
    
    override fun removeItem(slot: Int, amount: Int): ItemStack {
        return when {
            slot < mainInventory.slots -> mainInventory.extractItem(slot, amount, false)
            slot < mainInventory.slots + toolInventory.slots -> toolInventory.extractItem(slot - mainInventory.slots, amount, false)
            else -> upgradeInventory.extractItem(slot - mainInventory.slots - toolInventory.slots, amount, false)
        }
    }
    
    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val stack = getItem(slot)
        setItem(slot, ItemStack.EMPTY)
        return stack
    }
    
    override fun setItem(slot: Int, stack: ItemStack) {
        when {
            slot < mainInventory.slots -> mainInventory.setStackInSlot(slot, stack)
            slot < mainInventory.slots + toolInventory.slots -> toolInventory.setStackInSlot(slot - mainInventory.slots, stack)
            else -> upgradeInventory.setStackInSlot(slot - mainInventory.slots - toolInventory.slots, stack)
        }
    }
    
    override fun setChanged() {}
    
    override fun stillValid(player: Player): Boolean = isAlive && player.distanceToSqr(this) <= 64.0
    
    override fun clearContent() {
        for (i in 0 until mainInventory.slots) mainInventory.setStackInSlot(i, ItemStack.EMPTY)
        for (i in 0 until toolInventory.slots) toolInventory.setStackInSlot(i, ItemStack.EMPTY)
        for (i in 0 until upgradeInventory.slots) upgradeInventory.setStackInSlot(i, ItemStack.EMPTY)
    }
    
    fun getSelectedSlot(): Int = selectedSlot
    
    fun setSelectedSlot(slot: Int) {
        selectedSlot = slot.coerceIn(0, mainInventory.slots - 1)
    }
    
    fun select(slot: Int): Boolean {
        if (slot < 0 || slot >= mainInventory.slots) return false
        selectedSlot = slot
        return true
    }
    
    fun getMainInventory(): IItemHandler = mainInventory
    fun getToolInventory(): IItemHandler = toolInventory
    fun getUpgradeInventory(): IItemHandler = upgradeInventory
    
    // === Energy ===
    
    fun getEnergy(): Double = currentEnergy
    fun getMaxEnergy(): Double = maxEnergy
    
    fun addEnergy(amount: Double): Double {
        val added = min(amount, maxEnergy - currentEnergy)
        currentEnergy += added
        return added
    }
    
    // === XP ===
    
    fun getExperience(): Int = storedXP
    
    fun addExperience(amount: Int) {
        storedXP += amount
    }
    
    // === Serialization ===
    
    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        
        tag.putBoolean("running", running)
        tag.putInt("tier", tier)
        tag.putDouble("energy", currentEnergy)
        tag.putDouble("maxEnergy", maxEnergy)
        tag.putInt("selectedSlot", selectedSlot)
        tag.putString("facing", facing.name)
        tag.putInt("lightColor", lightColor)
        tag.putInt("xp", storedXP)
        
        if (ownerUUID != null) {
            tag.putUUID("owner", ownerUUID!!)
            tag.putString("ownerName", ownerName)
        }
        
        tag.put("mainInventory", mainInventory.serializeNBT(level().registryAccess()))
        tag.put("toolInventory", toolInventory.serializeNBT(level().registryAccess()))
        tag.put("upgradeInventory", upgradeInventory.serializeNBT(level().registryAccess()))
    }
    
    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        
        running = tag.getBoolean("running")
        tier = tag.getInt("tier")
        currentEnergy = tag.getDouble("energy")
        maxEnergy = tag.getDouble("maxEnergy")
        selectedSlot = tag.getInt("selectedSlot")
        facing = Direction.byName(tag.getString("facing")) ?: Direction.NORTH
        lightColor = tag.getInt("lightColor")
        storedXP = tag.getInt("xp")
        
        if (tag.contains("owner")) {
            ownerUUID = tag.getUUID("owner")
            ownerName = tag.getString("ownerName")
        }
        
        if (tag.contains("mainInventory")) {
            mainInventory.deserializeNBT(level().registryAccess(), tag.getCompound("mainInventory"))
        }
        if (tag.contains("toolInventory")) {
            toolInventory.deserializeNBT(level().registryAccess(), tag.getCompound("toolInventory"))
        }
        if (tag.contains("upgradeInventory")) {
            upgradeInventory.deserializeNBT(level().registryAccess(), tag.getCompound("upgradeInventory"))
        }
        
        entityData.set(DATA_RUNNING, running)
        entityData.set(DATA_FACING, facing)
        entityData.set(DATA_LIGHT_COLOR, lightColor)
    }
    
    // === Properties ===
    
    override fun isPushable(): Boolean = false
    
    override fun canBeCollidedWith(): Boolean = true
    
    override fun isPickable(): Boolean = true
    
    // === Factory ===
    
    fun setTier(tier: Int) {
        this.tier = tier.coerceIn(1, 3)
        
        when (this.tier) {
            1 -> {
                maxEnergy = 50000.0
            }
            2 -> {
                maxEnergy = 100000.0
            }
            3 -> {
                maxEnergy = 200000.0
            }
        }
        currentEnergy = maxEnergy
    }
    
    fun setOwner(player: Player) {
        ownerUUID = player.uuid
        ownerName = player.name.string
    }
}

/**
 * AI Goal for robot movement.
 */
class RobotMoveGoal(private val robot: RobotEntity) : Goal() {
    
    override fun canUse(): Boolean {
        return robot.isRunning() && robot.isMoving()
    }
    
    override fun tick() {
        // Movement is handled in robot tick
    }
}

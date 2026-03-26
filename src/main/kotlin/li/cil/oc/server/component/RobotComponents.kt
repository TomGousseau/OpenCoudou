package li.cil.oc.server.component

import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import java.util.*
import kotlin.math.*

/**
 * Robot component - provides robot-specific functionality.
 * Handles movement, interaction, and inventory management.
 */
class RobotComponent(
    private val robot: RobotContext
) : ComponentBase("robot") {
    
    private var selectedSlot = 0
    private var selectedTank = 0
    
    interface RobotContext {
        val level: Level?
        var position: BlockPos
        var facing: Direction
        val inventorySize: Int
        val tankCount: Int
        val energy: Double
        val maxEnergy: Double
        val lightColor: Int
        var name: String
        
        fun move(direction: Direction): MoveResult
        fun turn(clockwise: Boolean): Boolean
        fun swing(side: Direction): InteractionResult
        fun use(side: Direction, sneaking: Boolean, duration: Float): InteractionResult
        fun place(side: Direction, sneaking: Boolean): InteractionResult
        fun select(slot: Int): Boolean
        fun getStackInSlot(slot: Int): ItemSummary?
        fun transferTo(slot: Int, count: Int): Boolean
        fun drop(side: Direction, count: Int): Boolean
        fun suck(side: Direction, count: Int): Boolean
        fun compare(side: Direction): Boolean
        fun detect(side: Direction): DetectResult
    }
    
    enum class MoveResult {
        SUCCESS,
        BLOCKED,
        NO_ENERGY,
        IMPOSSIBLE
    }
    
    enum class InteractionResult {
        SUCCESS,
        BLOCKED,
        NO_ENERGY,
        AIR
    }
    
    enum class DetectResult {
        NOTHING,
        SOLID,
        LIQUID,
        REPLACEABLE,
        PASSABLE,
        ENTITY
    }
    
    data class ItemSummary(
        val name: String,
        val count: Int,
        val maxCount: Int,
        val damage: Int,
        val maxDamage: Int
    )
    
    @Callback(doc = "function(): string -- Returns the robot's name")
    fun name(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(robot.name)
    }
    
    @Callback(doc = "function(name: string): string -- Sets the robot's name")
    fun setName(context: Context, args: Array<Any?>): Array<Any?> {
        val name = args.getOrNull(0) as? String ?: return arrayOf(null, "invalid name")
        if (name.length > 24) return arrayOf(null, "name too long")
        robot.name = name
        return arrayOf(robot.name)
    }
    
    @Callback(doc = "function(): number -- Gets the currently selected slot")
    fun select(context: Context, args: Array<Any?>): Array<Any?> {
        if (args.isNotEmpty()) {
            val slot = (args[0] as? Number)?.toInt() ?: return arrayOf(null, "invalid slot")
            if (robot.select(slot - 1)) {
                selectedSlot = slot - 1
            }
        }
        return arrayOf(selectedSlot + 1)
    }
    
    @Callback(doc = "function(): number -- Gets the inventory size")
    fun inventorySize(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(robot.inventorySize)
    }
    
    @Callback(doc = "function([slot: number]): number -- Gets the count of items in a slot")
    fun count(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: selectedSlot
        val item = robot.getStackInSlot(slot) ?: return arrayOf(0)
        return arrayOf(item.count)
    }
    
    @Callback(doc = "function([slot: number]): number -- Gets the remaining space in a slot")
    fun space(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: selectedSlot
        val item = robot.getStackInSlot(slot) ?: return arrayOf(64)
        return arrayOf(item.maxCount - item.count)
    }
    
    @Callback(doc = "function(slot: number[, count: number]): boolean -- Transfers items to another slot")
    fun transferTo(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return arrayOf(false, "invalid slot")
        val count = (args.getOrNull(1) as? Number)?.toInt() ?: 64
        return arrayOf(robot.transferTo(slot, count))
    }
    
    @Callback(doc = "function([slot: number]): boolean -- Compares selected slot with another slot")
    fun compareTo(context: Context, args: Array<Any?>): Array<Any?> {
        val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return arrayOf(false)
        val selected = robot.getStackInSlot(selectedSlot)
        val target = robot.getStackInSlot(slot)
        
        if (selected == null && target == null) return arrayOf(true)
        if (selected == null || target == null) return arrayOf(false)
        
        return arrayOf(selected.name == target.name)
    }
    
    @Callback(doc = "function(side: number): boolean -- Compares block in front with selected slot")
    fun compare(context: Context, args: Array<Any?>): Array<Any?> {
        val side = getSide(args.getOrNull(0) as? Number) ?: return arrayOf(false, "invalid side")
        return arrayOf(robot.compare(side))
    }
    
    @Callback(doc = "function([count: number]): boolean -- Drops items from selected slot")
    fun drop(context: Context, args: Array<Any?>): Array<Any?> {
        val count = (args.getOrNull(0) as? Number)?.toInt() ?: 64
        return arrayOf(robot.drop(robot.facing, count))
    }
    
    @Callback(doc = "function(side: number[, count: number]): boolean -- Drops items to a specific side")
    fun dropSide(context: Context, args: Array<Any?>): Array<Any?> {
        val side = getSide(args.getOrNull(0) as? Number) ?: return arrayOf(false, "invalid side")
        val count = (args.getOrNull(1) as? Number)?.toInt() ?: 64
        return arrayOf(robot.drop(side, count))
    }
    
    @Callback(doc = "function([count: number]): boolean -- Picks up items")
    fun suck(context: Context, args: Array<Any?>): Array<Any?> {
        val count = (args.getOrNull(0) as? Number)?.toInt() ?: 64
        return arrayOf(robot.suck(robot.facing, count))
    }
    
    @Callback(doc = "function(side: number[, count: number]): boolean -- Picks up items from a side")
    fun suckSide(context: Context, args: Array<Any?>): Array<Any?> {
        val side = getSide(args.getOrNull(0) as? Number) ?: return arrayOf(false, "invalid side")
        val count = (args.getOrNull(1) as? Number)?.toInt() ?: 64
        return arrayOf(robot.suck(side, count))
    }
    
    @Callback(doc = "function(): boolean, string -- Detects block in front")
    fun detect(context: Context, args: Array<Any?>): Array<Any?> {
        val result = robot.detect(robot.facing)
        return arrayOf(result != DetectResult.NOTHING && result != DetectResult.PASSABLE, result.name.lowercase())
    }
    
    @Callback(doc = "function(): boolean, string -- Detects block above")
    fun detectUp(context: Context, args: Array<Any?>): Array<Any?> {
        val result = robot.detect(Direction.UP)
        return arrayOf(result != DetectResult.NOTHING && result != DetectResult.PASSABLE, result.name.lowercase())
    }
    
    @Callback(doc = "function(): boolean, string -- Detects block below")
    fun detectDown(context: Context, args: Array<Any?>): Array<Any?> {
        val result = robot.detect(Direction.DOWN)
        return arrayOf(result != DetectResult.NOTHING && result != DetectResult.PASSABLE, result.name.lowercase())
    }
    
    @Callback(doc = "function(): boolean -- Swings the tool in front")
    fun swing(context: Context, args: Array<Any?>): Array<Any?> {
        val result = robot.swing(robot.facing)
        return arrayOf(result == InteractionResult.SUCCESS, when (result) {
            InteractionResult.SUCCESS -> "swing"
            InteractionResult.AIR -> "air"
            else -> "blocked"
        })
    }
    
    @Callback(doc = "function(): boolean -- Swings the tool upward")
    fun swingUp(context: Context, args: Array<Any?>): Array<Any?> {
        val result = robot.swing(Direction.UP)
        return arrayOf(result == InteractionResult.SUCCESS, when (result) {
            InteractionResult.SUCCESS -> "swing"
            InteractionResult.AIR -> "air"
            else -> "blocked"
        })
    }
    
    @Callback(doc = "function(): boolean -- Swings the tool downward")
    fun swingDown(context: Context, args: Array<Any?>): Array<Any?> {
        val result = robot.swing(Direction.DOWN)
        return arrayOf(result == InteractionResult.SUCCESS, when (result) {
            InteractionResult.SUCCESS -> "swing"
            InteractionResult.AIR -> "air"
            else -> "blocked"
        })
    }
    
    @Callback(doc = "function([side: number, sneaking: boolean, duration: number]): boolean -- Uses the selected item")
    fun use(context: Context, args: Array<Any?>): Array<Any?> {
        val side = getSide(args.getOrNull(0) as? Number) ?: robot.facing
        val sneaking = args.getOrNull(1) as? Boolean ?: false
        val duration = (args.getOrNull(2) as? Number)?.toFloat() ?: 0f
        
        val result = robot.use(side, sneaking, duration)
        return arrayOf(result == InteractionResult.SUCCESS)
    }
    
    @Callback(doc = "function([side: number, sneaking: boolean]): boolean -- Places a block")
    fun place(context: Context, args: Array<Any?>): Array<Any?> {
        val side = getSide(args.getOrNull(0) as? Number) ?: robot.facing
        val sneaking = args.getOrNull(1) as? Boolean ?: false
        
        val result = robot.place(side, sneaking)
        return arrayOf(result == InteractionResult.SUCCESS)
    }
    
    @Callback(doc = "function(): boolean -- Moves forward")
    fun forward(context: Context, args: Array<Any?>): Array<Any?> {
        val result = robot.move(robot.facing)
        return arrayOf(result == MoveResult.SUCCESS, when (result) {
            MoveResult.SUCCESS -> null
            MoveResult.BLOCKED -> "blocked"
            MoveResult.NO_ENERGY -> "not enough energy"
            MoveResult.IMPOSSIBLE -> "impossible"
        })
    }
    
    @Callback(doc = "function(): boolean -- Moves backward")
    fun back(context: Context, args: Array<Any?>): Array<Any?> {
        val result = robot.move(robot.facing.opposite)
        return arrayOf(result == MoveResult.SUCCESS, when (result) {
            MoveResult.SUCCESS -> null
            MoveResult.BLOCKED -> "blocked"
            MoveResult.NO_ENERGY -> "not enough energy"
            MoveResult.IMPOSSIBLE -> "impossible"
        })
    }
    
    @Callback(doc = "function(): boolean -- Moves up")
    fun up(context: Context, args: Array<Any?>): Array<Any?> {
        val result = robot.move(Direction.UP)
        return arrayOf(result == MoveResult.SUCCESS, when (result) {
            MoveResult.SUCCESS -> null
            MoveResult.BLOCKED -> "blocked"
            MoveResult.NO_ENERGY -> "not enough energy"
            MoveResult.IMPOSSIBLE -> "impossible"
        })
    }
    
    @Callback(doc = "function(): boolean -- Moves down")
    fun down(context: Context, args: Array<Any?>): Array<Any?> {
        val result = robot.move(Direction.DOWN)
        return arrayOf(result == MoveResult.SUCCESS, when (result) {
            MoveResult.SUCCESS -> null
            MoveResult.BLOCKED -> "blocked"
            MoveResult.NO_ENERGY -> "not enough energy"
            MoveResult.IMPOSSIBLE -> "impossible"
        })
    }
    
    @Callback(doc = "function(clockwise: boolean): boolean -- Turns the robot")
    fun turn(context: Context, args: Array<Any?>): Array<Any?> {
        val clockwise = args.getOrNull(0) as? Boolean ?: true
        return arrayOf(robot.turn(clockwise))
    }
    
    @Callback(doc = "function(): boolean -- Turns left")
    fun turnLeft(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(robot.turn(false))
    }
    
    @Callback(doc = "function(): boolean -- Turns right")
    fun turnRight(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(robot.turn(true))
    }
    
    @Callback(doc = "function(): number -- Gets the durability of the equipped tool")
    fun durability(context: Context, args: Array<Any?>): Array<Any?> {
        val tool = robot.getStackInSlot(-1) // Tool slot
        if (tool == null || tool.maxDamage == 0) return arrayOf(null, "no tool equipped")
        return arrayOf((tool.maxDamage - tool.damage).toDouble() / tool.maxDamage)
    }
    
    @Callback(doc = "function(): number -- Gets the current energy level")
    fun energy(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(robot.energy)
    }
    
    @Callback(doc = "function(): number -- Gets the maximum energy level")
    fun maxEnergy(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(robot.maxEnergy)
    }
    
    @Callback(doc = "function(): number -- Gets the light color")
    fun getLightColor(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(robot.lightColor)
    }
    
    @Callback(doc = "function(color: number): number -- Sets the light color")
    fun setLightColor(context: Context, args: Array<Any?>): Array<Any?> {
        // Would set light color in actual implementation
        return arrayOf(robot.lightColor)
    }
    
    @Callback(doc = "function(): number -- Gets the number of tanks")
    fun tankCount(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(robot.tankCount)
    }
    
    @Callback(doc = "function([tank: number]): number -- Selects or gets the selected tank")
    fun selectTank(context: Context, args: Array<Any?>): Array<Any?> {
        if (args.isNotEmpty()) {
            val tank = (args[0] as? Number)?.toInt() ?: return arrayOf(selectedTank + 1)
            if (tank in 1..robot.tankCount) {
                selectedTank = tank - 1
            }
        }
        return arrayOf(selectedTank + 1)
    }
    
    private fun getSide(num: Number?): Direction? {
        return when (num?.toInt()) {
            0 -> Direction.DOWN
            1 -> Direction.UP
            2 -> Direction.NORTH
            3 -> Direction.SOUTH
            4 -> Direction.WEST
            5 -> Direction.EAST
            else -> null
        }
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putInt("selectedSlot", selectedSlot)
        tag.putInt("selectedTank", selectedTank)
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        selectedSlot = tag.getInt("selectedSlot")
        selectedTank = tag.getInt("selectedTank")
    }
}

/**
 * Drone component - provides drone-specific functionality.
 * Drones can fly freely and have limited inventory.
 */
class DroneComponent(
    private val drone: DroneContext
) : ComponentBase("drone") {
    
    interface DroneContext {
        val level: Level?
        var position: BlockPos
        var velocity: Triple<Double, Double, Double>
        val energy: Double
        val maxEnergy: Double
        val lightColor: Int
        var name: String
        val acceleration: Double
        val maxVelocity: Double
        
        fun move(dx: Double, dy: Double, dz: Double): Boolean
        fun getOffset(): Triple<Double, Double, Double>
        fun setStatusText(text: String)
        fun getStatusText(): String
    }
    
    @Callback(doc = "function(): string -- Gets the drone's name")
    fun name(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(drone.name)
    }
    
    @Callback(doc = "function(): number, number, number -- Gets the current velocity")
    fun getVelocity(context: Context, args: Array<Any?>): Array<Any?> {
        val (vx, vy, vz) = drone.velocity
        return arrayOf(vx, vy, vz)
    }
    
    @Callback(doc = "function(): number -- Gets the maximum velocity")
    fun getMaxVelocity(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(drone.maxVelocity)
    }
    
    @Callback(doc = "function(): number -- Gets the acceleration")
    fun getAcceleration(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(drone.acceleration)
    }
    
    @Callback(doc = "function(): number, number, number -- Gets the target offset")
    fun getOffset(context: Context, args: Array<Any?>): Array<Any?> {
        val (x, y, z) = drone.getOffset()
        return arrayOf(x, y, z)
    }
    
    @Callback(doc = "function(dx: number, dy: number, dz: number): void -- Sets the movement target")
    fun move(context: Context, args: Array<Any?>): Array<Any?> {
        val dx = (args.getOrNull(0) as? Number)?.toDouble() ?: 0.0
        val dy = (args.getOrNull(1) as? Number)?.toDouble() ?: 0.0
        val dz = (args.getOrNull(2) as? Number)?.toDouble() ?: 0.0
        
        drone.move(dx, dy, dz)
        return arrayOf()
    }
    
    @Callback(doc = "function(): number -- Gets the current energy")
    fun energy(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(drone.energy)
    }
    
    @Callback(doc = "function(): number -- Gets the maximum energy")
    fun maxEnergy(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(drone.maxEnergy)
    }
    
    @Callback(doc = "function(): string -- Gets the status text")
    fun getStatusText(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(drone.getStatusText())
    }
    
    @Callback(doc = "function(text: string): void -- Sets the status text")
    fun setStatusText(context: Context, args: Array<Any?>): Array<Any?> {
        val text = args.getOrNull(0) as? String ?: ""
        drone.setStatusText(text.take(32))
        return arrayOf()
    }
    
    @Callback(doc = "function(): number -- Gets the light color")
    fun getLightColor(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(drone.lightColor)
    }
}

/**
 * Navigation component - provides GPS-like positioning and path finding.
 */
class NavigationComponent(val tier: Int = 1) : ComponentBase("navigation") {
    
    private var mapData: ByteArray? = null
    private var mapCenter: BlockPos? = null
    
    @Callback(doc = "function(): number, number, number -- Gets current position relative to map center")
    fun getPosition(context: Context, args: Array<Any?>): Array<Any?> {
        // Would calculate actual position in real implementation
        return arrayOf(0.0, 0.0, 0.0)
    }
    
    @Callback(doc = "function(): number -- Gets current facing direction")
    fun getFacing(context: Context, args: Array<Any?>): Array<Any?> {
        // Would return actual facing
        return arrayOf(2) // NORTH
    }
    
    @Callback(doc = "function(): number -- Gets the range of the navigation map")
    fun getRange(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(when (tier) {
            1 -> 64
            2 -> 128
            else -> 256
        })
    }
    
    @Callback(doc = "function(x: number, y: number, z: number): table -- Finds waypoints")
    fun findWaypoints(context: Context, args: Array<Any?>): Array<Any?> {
        val range = when (tier) { 1 -> 64; 2 -> 128; else -> 256 }
        // Would search for waypoint blocks in range
        return arrayOf(emptyList<Map<String, Any>>())
    }
}

/**
 * Waypoint component - marks locations that can be detected by navigation.
 */
class WaypointComponent : ComponentBase("waypoint") {
    
    var label: String = ""
    var redstoneOutput: Int = 0
    
    @Callback(doc = "function(): string -- Gets the waypoint label")
    fun getLabel(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(label)
    }
    
    @Callback(doc = "function(label: string): string -- Sets the waypoint label")
    fun setLabel(context: Context, args: Array<Any?>): Array<Any?> {
        val old = label
        label = (args.getOrNull(0) as? String ?: "").take(32)
        return arrayOf(old)
    }
}

/**
 * Experience component - manages robot/drone experience and leveling.
 */
class ExperienceComponent : ComponentBase("experience") {
    
    private var experience = 0.0
    private var level = 0
    
    fun addExperience(amount: Double) {
        experience += amount
        while (experience >= getExperienceForLevel(level + 1)) {
            level++
        }
    }
    
    private fun getExperienceForLevel(level: Int): Double {
        return level * level * 100.0
    }
    
    @Callback(doc = "function(): number -- Gets current experience")
    fun experience(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(experience)
    }
    
    @Callback(doc = "function(): number -- Gets current level")
    fun level(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(level)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putDouble("experience", experience)
        tag.putInt("level", level)
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        experience = tag.getDouble("experience")
        level = tag.getInt("level")
    }
}

/**
 * Leash component - allows robots to leash and lead entities.
 */
class LeashComponent : ComponentBase("leash") {
    
    private val leashedEntities = mutableListOf<UUID>()
    private val maxLeashed = 8
    
    @Callback(doc = "function(): boolean -- Leashes nearby entity")
    fun leash(context: Context, args: Array<Any?>): Array<Any?> {
        if (leashedEntities.size >= maxLeashed) {
            return arrayOf(false, "too many entities leashed")
        }
        // Would find and leash entity in actual implementation
        return arrayOf(true)
    }
    
    @Callback(doc = "function(): void -- Unleashes all entities")
    fun unleash(context: Context, args: Array<Any?>): Array<Any?> {
        leashedEntities.clear()
        return arrayOf()
    }
    
    @Callback(doc = "function(): number -- Gets the number of leashed entities")
    fun count(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(leashedEntities.size)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        // Save leashed entity UUIDs
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        // Load leashed entity UUIDs
    }
}

/**
 * Angel component - allows placing blocks in mid-air.
 */
class AngelComponent : ComponentBase("angel") {
    
    @Callback(doc = "function(): string -- Gets the upgrade name")
    fun name(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf("angel")
    }
}

/**
 * Hover component - allows hovering over gaps.
 */
class HoverComponent(val tier: Int = 1) : ComponentBase("hover") {
    
    val hoverHeight: Int get() = when (tier) {
        1 -> 8
        else -> 64
    }
    
    @Callback(doc = "function(): number -- Gets the maximum hover height")
    fun getHeight(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(hoverHeight)
    }
}

/**
 * Piston component - allows pushing/pulling blocks.
 */
class PistonComponent : ComponentBase("piston") {
    
    @Callback(doc = "function(side: number): boolean -- Pushes a block")
    fun push(context: Context, args: Array<Any?>): Array<Any?> {
        // Would push block in actual implementation
        return arrayOf(true)
    }
}

/**
 * Tank controller component - allows robot interaction with fluid tanks.
 */
class TankControllerComponent : ComponentBase("tank_controller") {
    
    @Callback(doc = "function([slot: number]): number -- Gets the fluid level in a tank")
    fun getTankLevel(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(0)
    }
    
    @Callback(doc = "function([slot: number]): number -- Gets the capacity of a tank")
    fun getTankCapacity(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(16000)
    }
    
    @Callback(doc = "function([slot: number]): table -- Gets info about fluid in a tank")
    fun getFluidInTank(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(null)
    }
    
    @Callback(doc = "function(amount: number): boolean -- Drains fluid from the world")
    fun drain(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(false)
    }
    
    @Callback(doc = "function(amount: number): boolean -- Fills fluid into the world")
    fun fill(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(false)
    }
}

/**
 * Inventory controller component - allows detailed inventory interaction.
 */
class InventoryControllerComponent : ComponentBase("inventory_controller") {
    
    @Callback(doc = "function(side: number): number -- Gets the inventory size on a side")
    fun getInventorySize(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(0)
    }
    
    @Callback(doc = "function(side: number, slot: number): table -- Gets info about an item")
    fun getStackInSlot(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(null)
    }
    
    @Callback(doc = "function(side: number, slot: number): string -- Gets the name of item in slot")
    fun getItemName(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(null)
    }
    
    @Callback(doc = "function(side: number, fromSlot: number, toSlot: number, count: number): number -- Moves items between slots")
    fun transferItem(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(0)
    }
    
    @Callback(doc = "function(): table -- Gets info about the equipped tool")
    fun getEquippedItemInfo(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(null)
    }
    
    @Callback(doc = "function(slot: number): boolean -- Equips an item as tool")
    fun equip(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(false)
    }
}

/**
 * Crafting component - allows crafting items.
 */
class CraftingComponent : ComponentBase("crafting") {
    
    @Callback(doc = "function([count: number]): boolean -- Crafts items using the robot inventory")
    fun craft(context: Context, args: Array<Any?>): Array<Any?> {
        val count = (args.getOrNull(0) as? Number)?.toInt() ?: 64
        // Would perform crafting in actual implementation
        return arrayOf(true)
    }
}

/**
 * Generator component - generates power from fuel items.
 */
class GeneratorComponent : ComponentBase("generator") {
    
    private var fuelRemaining = 0
    private var generating = false
    
    @Callback(doc = "function(): number -- Gets the remaining fuel")
    fun count(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(fuelRemaining)
    }
    
    @Callback(doc = "function([count: number]): boolean -- Inserts fuel")
    fun insert(context: Context, args: Array<Any?>): Array<Any?> {
        val count = (args.getOrNull(0) as? Number)?.toInt() ?: 64
        // Would insert fuel in actual implementation
        return arrayOf(true)
    }
    
    @Callback(doc = "function([count: number]): boolean -- Removes fuel")
    fun remove(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(true)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putInt("fuel", fuelRemaining)
        tag.putBoolean("generating", generating)
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        fuelRemaining = tag.getInt("fuel")
        generating = tag.getBoolean("generating")
    }
}

/**
 * Solar generator component - generates power from sunlight.
 */
class SolarGeneratorComponent(val tier: Int = 1) : ComponentBase("solar_generator") {
    
    private var generating = false
    
    val energyPerTick: Double get() = when (tier) {
        1 -> 1.0
        else -> 2.0
    }
    
    @Callback(doc = "function(): boolean -- Checks if currently generating power")
    fun isSunShining(context: Context, args: Array<Any?>): Array<Any?> {
        return arrayOf(generating)
    }
}

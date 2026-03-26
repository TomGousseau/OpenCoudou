package li.cil.oc.api.machine

import li.cil.oc.api.network.Component
import li.cil.oc.api.network.Node
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/**
 * MachineHost is the physical container for a machine.
 * 
 * This is typically a computer case, robot, or other block/entity that
 * contains a computer. The host provides:
 * - Access to the world for the machine
 * - Inventory of components (CPU, memory, etc.)
 * - Power management
 * 
 * @see Machine
 */
interface MachineHost {
    /**
     * Gets the world this host exists in.
     */
    fun world(): Level?
    
    /**
     * Gets the position of this host in the world.
     * For entities, this should return the current block position.
     */
    fun position(): BlockPos
    
    /**
     * Gets the node for this host.
     * This is the node that the machine uses to communicate with the network.
     */
    fun node(): Node?
    
    /**
     * Gets the machine running in this host.
     */
    fun machine(): Machine?
    
    /**
     * Gets all component items currently installed in this host.
     * This includes CPU, memory, cards, etc.
     */
    fun components(): Iterable<Component>
    
    /**
     * Called when the machine state changes.
     * Hosts can use this to update visuals, sounds, etc.
     * 
     * @param state The new machine state
     */
    fun onMachineStateChanged(state: MachineState)
    
    /**
     * Called when a component is connected to the machine.
     * 
     * @param component The component that connected
     */
    fun onComponentConnected(component: Component)
    
    /**
     * Called when a component is disconnected from the machine.
     * 
     * @param component The component that disconnected
     */
    fun onComponentDisconnected(component: Component)
    
    /**
     * Checks if this host allows execution.
     * Return false to prevent the machine from running (e.g., if unpowered).
     */
    fun canRun(): Boolean = true
    
    /**
     * Gets the tier of this host (0-3 for normal, 4+ for creative).
     * Higher tiers can support more components and have fewer restrictions.
     */
    fun tier(): Int = 0
    
    /**
     * Marks the host as needing to save its state.
     */
    fun markDirty()
}

/**
 * Extended host interface for hosts that can provide additional services.
 */
interface RobotHost : MachineHost {
    /**
     * Gets the robot's current x-axis movement progress (0-1).
     */
    fun moveProgressX(): Double
    
    /**
     * Gets the robot's current y-axis movement progress (0-1).
     */
    fun moveProgressY(): Double
    
    /**
     * Gets the robot's current z-axis movement progress (0-1).
     */
    fun moveProgressZ(): Double
    
    /**
     * Gets the direction the robot is facing.
     */
    fun facing(): net.minecraft.core.Direction
    
    /**
     * Initiates robot movement in a direction.
     * 
     * @param direction The direction to move
     * @return True if movement started successfully
     */
    fun move(direction: net.minecraft.core.Direction): Boolean
    
    /**
     * Initiates robot rotation.
     * 
     * @param clockwise Whether to rotate clockwise
     * @return True if rotation started successfully
     */
    fun turn(clockwise: Boolean): Boolean
}

/**
 * Extended host interface for tablet-like devices.
 */
interface TabletHost : MachineHost {
    /**
     * Gets the player currently holding this tablet, if any.
     */
    fun player(): net.minecraft.world.entity.player.Player?
}

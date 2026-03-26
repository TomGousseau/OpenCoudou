package li.cil.oc.api.machine

import li.cil.oc.api.network.Component
import li.cil.oc.api.network.Node
import net.minecraft.world.item.ItemStack

/**
 * The Machine is the central abstraction for a running computer.
 * 
 * A machine manages:
 * - The execution architecture (e.g., Lua 5.4)
 * - The component network for this computer
 * - The signal queue for events
 * - Power consumption and state management
 * 
 * Machines are created from MachineHosts (like computer cases or robots) and
 * run code provided by an Architecture. The architecture is responsible for
 * the actual execution of user code.
 * 
 * @see Architecture
 * @see MachineHost
 * @see Context
 */
interface Machine : Context {
    // ========================================
    // Machine Properties
    // ========================================
    
    /**
     * The host that contains this machine (e.g., computer case, robot).
     */
    val host: MachineHost
    
    /**
     * The architecture running on this machine (e.g., Lua).
     */
    val architecture: Architecture?
    
    /**
     * The current state of this machine.
     */
    val state: MachineState
    
    /**
     * Whether this machine has crashed since the last successful run.
     */
    val hasCrashed: Boolean
    
    /**
     * The error message from the last crash, if any.
     */
    val crashMessage: String?
    
    /**
     * All connected components visible to this machine.
     * Keys are component addresses, values are component types.
     */
    val components: Map<String, String>
    
    /**
     * The amount of memory available to this machine in bytes.
     */
    val totalMemory: Int
    
    /**
     * Gets the current execution time in this tick.
     */
    val cpuTime: Double
    
    /**
     * The remaining call budget for direct calls this tick.
     */
    val callBudget: Int
    
    /**
     * The list of usernames allowed to interact with this computer.
     * Empty list means anyone can interact.
     */
    val users: List<String>
    
    /**
     * The world time when this machine was last started.
     */
    val lastStartTime: Long
    
    /**
     * The uptime of this machine in seconds since last boot.
     */
    val uptime: Double
    
    // ========================================
    // Machine Control
    // ========================================
    
    /**
     * Starts the machine.
     * This initializes the architecture and begins execution.
     * 
     * @return True if the machine started successfully
     */
    override fun start(): Boolean
    
    /**
     * Pauses the machine.
     * The machine will stop executing but retain its state.
     * 
     * @param duration The duration to pause for in seconds, or 0 for indefinite
     * @return True if the machine was paused
     */
    override fun pause(duration: Double): Boolean
    
    /**
     * Stops the machine.
     * This terminates the architecture and clears the state.
     * 
     * @return True if the machine was stopped
     */
    override fun stop(): Boolean
    
    /**
     * Crashes the machine with an error message.
     * This is like stop() but marks the machine as crashed.
     * 
     * @param message The error message
     */
    fun crash(message: String)
    
    /**
     * Performs a single step of execution.
     * This is called by the host each tick to advance the machine state.
     * 
     * @return True if the machine is still running
     */
    fun update(): Boolean
    
    /**
     * Called on the synchronized (server) thread for synchronized calls.
     * Architecture implementations use this for non-thread-safe operations.
     */
    fun onSynchronizedCall()
    
    // ========================================
    // Memory Management
    // ========================================
    
    /**
     * Recalculates the available memory based on installed RAM.
     * Call this when memory modules are added or removed.
     * 
     * @param memory The list of memory item stacks
     * @return True if the memory configuration is valid
     */
    fun recomputeMemory(memory: Iterable<ItemStack>): Boolean
    
    // ========================================
    // Component Management
    // ========================================
    
    /**
     * Gets a component by its address.
     * 
     * @param address The component address
     * @return The component, or null if not found
     */
    fun component(address: String): Component?
    
    /**
     * Gets all components of a specific type.
     * 
     * @param type The component type name
     * @return All matching components
     */
    fun componentsOf(type: String): List<Component>
    
    /**
     * Invokes a method on a component.
     * 
     * @param address The component address
     * @param method The method name
     * @param args The arguments
     * @return The return values
     */
    fun invoke(address: String, method: String, vararg args: Any?): Array<Any?>
    
    /**
     * Invokes a method and yields the coroutine.
     * Use this for long-running operations that should not block.
     * 
     * @param address The component address
     * @param method The method name
     * @param args The arguments
     * @return An iterator for the result values
     */
    fun invokeAsync(address: String, method: String, vararg args: Any?): Iterator<Array<Any?>>
    
    // ========================================
    // User Management
    // ========================================
    
    /**
     * Adds a user to the allowed users list.
     * 
     * @param name The username
     * @return True if the user was added
     */
    fun addUser(name: String): Boolean
    
    /**
     * Removes a user from the allowed users list.
     * 
     * @param name The username
     * @return True if the user was removed
     */
    fun removeUser(name: String): Boolean
    
    companion object {
        /**
         * Creates a new machine for the given host.
         * 
         * @param host The host that will contain the machine
         * @return The new machine
         */
        @JvmStatic
        fun create(host: MachineHost): Machine {
            return li.cil.oc.server.machine.MachineImpl(host)
        }
    }
}

/**
 * The possible states of a machine.
 */
enum class MachineState {
    /** Machine is not running and can be started */
    STOPPED,
    
    /** Machine is initializing after start() */
    STARTING,
    
    /** Machine is running normally */
    RUNNING,
    
    /** Machine is paused and waiting */
    PAUSED,
    
    /** Machine is sleeping (yielded) and waiting for a signal */
    SLEEPING,
    
    /** Machine is synchronizing - waiting for server thread */
    SYNCHRONIZING,
    
    /** Machine is shutting down after stop() */
    STOPPING,
    
    /** Machine has crashed due to an error */
    CRASHED;
    
    /**
     * Whether the machine is in a state where it can execute code.
     */
    val isRunning: Boolean
        get() = this == RUNNING || this == SLEEPING || this == SYNCHRONIZING
    
    /**
     * Whether the machine is in a transitional state.
     */
    val isTransitioning: Boolean
        get() = this == STARTING || this == STOPPING
}

/**
 * Result of a machine execution step.
 */
sealed class ExecutionResult {
    /** Execution completed successfully, continue running */
    data class Sleep(val ticks: Int) : ExecutionResult()
    
    /** Execution completed, machine should synchronize with server thread */
    data object SynchronizedCall : ExecutionResult()
    
    /** Execution should continue next tick */
    data object Reschedule : ExecutionResult()
    
    /** Execution completed, machine has shut down */
    data object Shutdown : ExecutionResult()
    
    /** Execution failed with an error */
    data class Error(val message: String) : ExecutionResult()
}

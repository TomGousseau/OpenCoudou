package li.cil.oc.server.machine

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.machine.*
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.item.ItemStack
import java.util.*
import java.util.concurrent.*

/**
 * Implementation of the Machine interface.
 * 
 * This class manages the lifecycle of a computer, including:
 * - Starting/stopping the architecture
 * - Managing the signal queue
 * - Tracking connected components
 * - Executing code on worker threads
 * - Managing power consumption
 */
class MachineImpl(override val host: MachineHost) : Machine {
    
    // ========================================
    // State
    // ========================================
    
    override var state: MachineState = MachineState.STOPPED
        private set
    
    override var architecture: Architecture? = null
        private set
    
    override var hasCrashed: Boolean = false
        private set
    
    override var crashMessage: String? = null
        private set
    
    private val _components = mutableMapOf<String, String>()
    override val components: Map<String, String>
        get() = _components.toMap()
    
    override var totalMemory: Int = 0
        private set
    
    private var _cpuTime: Double = 0.0
    override val cpuTime: Double
        get() = _cpuTime
    
    private var _callBudget: Int = Settings.maxCallBudget
    override val callBudget: Int
        get() = _callBudget
    
    private val _users = mutableListOf<String>()
    override val users: List<String>
        get() = _users.toList()
    
    override var lastStartTime: Long = 0
        private set
    
    override val uptime: Double
        get() {
            val level = host.world() ?: return 0.0
            return if (state.isRunning) {
                (level.gameTime - lastStartTime) / 20.0
            } else 0.0
        }
    
    // ========================================
    // Internal State
    // ========================================
    
    private val signalQueue = ConcurrentLinkedQueue<Signal>()
    private var _node: Node? = null
    private var executorFuture: Future<*>? = null
    private var synchronizedCall: (() -> Unit)? = null
    private var sleepUntil: Long = 0
    private var remainingPause: Double = 0.0
    
    data class Signal(val name: String, val args: Array<out Any?>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Signal) return false
            return name == other.name && args.contentEquals(other.args)
        }
        override fun hashCode(): Int = 31 * name.hashCode() + args.contentHashCode()
    }
    
    // ========================================
    // Context Implementation
    // ========================================
    
    override fun node(): Node = _node ?: throw IllegalStateException("Machine has no node")
    
    override fun signal(name: String, vararg args: Any?): Boolean {
        if (signalQueue.size >= Settings.signalQueueSize) {
            return false
        }
        // Convert arguments for Lua
        val convertedArgs = args.map { convertForLua(it) }.toTypedArray()
        signalQueue.offer(Signal(name, convertedArgs))
        architecture?.onSignal()
        return true
    }
    
    override fun canInteract(player: String): Boolean {
        if (_users.isEmpty()) return true
        return _users.contains(player)
    }
    
    override fun consumeCallBudget(cost: Int): Boolean {
        if (_callBudget >= cost) {
            _callBudget -= cost
            return true
        }
        return false
    }
    
    // ========================================
    // Machine Control
    // ========================================
    
    override fun start(): Boolean {
        if (state != MachineState.STOPPED && state != MachineState.CRASHED) {
            return false
        }
        
        hasCrashed = false
        crashMessage = null
        signalQueue.clear()
        
        // Create architecture
        val arch = ArchitectureRegistry.createDefault(this) ?: run {
            crash("No architecture available")
            return false
        }
        architecture = arch
        
        // Initialize architecture
        state = MachineState.STARTING
        host.onMachineStateChanged(state)
        
        if (!arch.initialize()) {
            crash("Failed to initialize architecture")
            return false
        }
        
        lastStartTime = host.world()?.gameTime ?: 0
        state = MachineState.RUNNING
        host.onMachineStateChanged(state)
        
        // Notify about components
        for ((address, name) in _components) {
            signal(Signals.COMPONENT_ADDED, address, name)
        }
        
        OpenComputers.LOGGER.debug("Machine started at {}", host.position())
        return true
    }
    
    override fun pause(duration: Double): Boolean {
        if (!state.isRunning) return false
        
        remainingPause = duration
        state = MachineState.PAUSED
        host.onMachineStateChanged(state)
        return true
    }
    
    override fun stop(): Boolean {
        if (state == MachineState.STOPPED) return false
        
        state = MachineState.STOPPING
        host.onMachineStateChanged(state)
        
        // Cancel any pending execution
        executorFuture?.cancel(true)
        executorFuture = null
        
        // Close architecture
        architecture?.close()
        architecture = null
        
        signalQueue.clear()
        state = MachineState.STOPPED
        host.onMachineStateChanged(state)
        
        OpenComputers.LOGGER.debug("Machine stopped at {}", host.position())
        return true
    }
    
    override fun crash(message: String) {
        OpenComputers.LOGGER.warn("Machine crashed at {}: {}", host.position(), message)
        
        hasCrashed = true
        crashMessage = message
        
        executorFuture?.cancel(true)
        executorFuture = null
        
        architecture?.close()
        architecture = null
        
        signalQueue.clear()
        state = MachineState.CRASHED
        host.onMachineStateChanged(state)
    }
    
    // ========================================
    // Update Cycle
    // ========================================
    
    override fun update(): Boolean {
        if (!host.canRun()) {
            if (state.isRunning) pause(0.0)
            return false
        }
        
        // Reset call budget each tick
        _callBudget = Settings.maxCallBudget
        
        when (state) {
            MachineState.STOPPED, MachineState.CRASHED -> return false
            
            MachineState.PAUSED -> {
                if (remainingPause > 0) {
                    remainingPause -= 0.05 // 1 tick = 0.05 seconds
                } else {
                    state = MachineState.RUNNING
                    host.onMachineStateChanged(state)
                }
            }
            
            MachineState.SLEEPING -> {
                val level = host.world() ?: return false
                if (level.gameTime >= sleepUntil || signalQueue.isNotEmpty()) {
                    state = MachineState.RUNNING
                    host.onMachineStateChanged(state)
                    scheduleExecution(false)
                }
            }
            
            MachineState.SYNCHRONIZING -> {
                onSynchronizedCall()
            }
            
            MachineState.RUNNING -> {
                // Check if execution has completed
                val future = executorFuture
                if (future == null || future.isDone) {
                    scheduleExecution(false)
                }
            }
            
            else -> {} // Starting/stopping handled elsewhere
        }
        
        return state.isRunning
    }
    
    private fun scheduleExecution(isSynchronizedReturn: Boolean) {
        val arch = architecture ?: return
        
        executorFuture = MachineExecutor.submit {
            val startTime = System.nanoTime()
            try {
                when (val result = arch.runThreaded(isSynchronizedReturn)) {
                    is ExecutionResult.Sleep -> {
                        val level = host.world()
                        if (level != null) {
                            sleepUntil = level.gameTime + result.ticks
                            state = MachineState.SLEEPING
                            host.onMachineStateChanged(state)
                        }
                    }
                    is ExecutionResult.SynchronizedCall -> {
                        state = MachineState.SYNCHRONIZING
                        host.onMachineStateChanged(state)
                    }
                    is ExecutionResult.Reschedule -> {
                        // Stay in RUNNING state, will be rescheduled next tick
                    }
                    is ExecutionResult.Shutdown -> {
                        stop()
                    }
                    is ExecutionResult.Error -> {
                        crash(result.message)
                    }
                }
            } catch (e: Exception) {
                OpenComputers.LOGGER.error("Error executing machine", e)
                crash(e.message ?: "Unknown error")
            } finally {
                _cpuTime += (System.nanoTime() - startTime) / 1_000_000_000.0
            }
        }
    }
    
    override fun onSynchronizedCall() {
        if (state != MachineState.SYNCHRONIZING) return
        
        try {
            synchronizedCall?.invoke()
            synchronizedCall = null
            architecture?.runSynchronized()
            state = MachineState.RUNNING
            host.onMachineStateChanged(state)
            scheduleExecution(true)
        } catch (e: Exception) {
            OpenComputers.LOGGER.error("Error in synchronized call", e)
            crash(e.message ?: "Synchronized call error")
        }
    }
    
    // ========================================
    // Memory Management
    // ========================================
    
    override fun recomputeMemory(memory: Iterable<ItemStack>): Boolean {
        val arch = architecture ?: return false
        return arch.recomputeMemory(memory)
    }
    
    // ========================================
    // Component Management
    // ========================================
    
    override fun component(address: String): Component? {
        if (address !in _components) return null
        return _node?.network?.node(address) as? Component
    }
    
    override fun componentsOf(type: String): List<Component> {
        return _components.filterValues { it == type }
            .keys
            .mapNotNull { component(it) }
    }
    
    override fun invoke(address: String, method: String, vararg args: Any?): Array<Any?> {
        val component = component(address)
            ?: throw IllegalArgumentException("No such component: $address")
        return component.invoke(method, this, *args)
    }
    
    override fun invokeAsync(address: String, method: String, vararg args: Any?): Iterator<Array<Any?>> {
        // For now, just return the synchronous result
        // A full implementation would yield and resume
        return listOf(invoke(address, method, *args)).iterator()
    }
    
    fun addComponent(component: Component) {
        _components[component.address] = component.name
        host.onComponentConnected(component)
        if (state.isRunning) {
            signal(Signals.COMPONENT_ADDED, component.address, component.name)
        }
    }
    
    fun removeComponent(component: Component) {
        _components.remove(component.address)
        host.onComponentDisconnected(component)
        if (state.isRunning) {
            signal(Signals.COMPONENT_REMOVED, component.address, component.name)
        }
    }
    
    // ========================================
    // User Management
    // ========================================
    
    override fun addUser(name: String): Boolean {
        if (_users.size >= 16) return false
        if (name in _users) return false
        _users.add(name)
        host.markDirty()
        return true
    }
    
    override fun removeUser(name: String): Boolean {
        val removed = _users.remove(name)
        if (removed) host.markDirty()
        return removed
    }
    
    // ========================================
    // Signal Queue
    // ========================================
    
    /**
     * Pops the next signal from the queue.
     * Called by the architecture when pulling signals in Lua.
     */
    fun popSignal(): Signal? = signalQueue.poll()
    
    /**
     * Peeks at the next signal without removing it.
     */
    fun peekSignal(): Signal? = signalQueue.peek()
    
    // ========================================
    // Serialization
    // ========================================
    
    fun load(tag: CompoundTag) {
        state = MachineState.entries.getOrNull(tag.getInt("state")) ?: MachineState.STOPPED
        hasCrashed = tag.getBoolean("crashed")
        crashMessage = if (tag.contains("crashMessage")) tag.getString("crashMessage") else null
        lastStartTime = tag.getLong("lastStartTime")
        remainingPause = tag.getDouble("remainingPause")
        sleepUntil = tag.getLong("sleepUntil")
        
        _components.clear()
        val componentsTag = tag.getCompound("components")
        for (key in componentsTag.allKeys) {
            _components[key] = componentsTag.getString(key)
        }
        
        _users.clear()
        val usersTag = tag.getList("users", 8) // 8 = String tag type
        for (i in 0 until usersTag.size) {
            _users.add(usersTag.getString(i))
        }
        
        // Load architecture state
        if (state.isRunning && tag.contains("architecture")) {
            val arch = ArchitectureRegistry.createDefault(this)
            if (arch != null) {
                architecture = arch
                arch.load(tag.getCompound("architecture"))
            }
        }
    }
    
    fun save(tag: CompoundTag) {
        tag.putInt("state", state.ordinal)
        tag.putBoolean("crashed", hasCrashed)
        crashMessage?.let { tag.putString("crashMessage", it) }
        tag.putLong("lastStartTime", lastStartTime)
        tag.putDouble("remainingPause", remainingPause)
        tag.putLong("sleepUntil", sleepUntil)
        
        val componentsTag = CompoundTag()
        for ((address, name) in _components) {
            componentsTag.putString(address, name)
        }
        tag.put("components", componentsTag)
        
        val usersTag = ListTag()
        for (user in _users) {
            usersTag.add(net.minecraft.nbt.StringTag.valueOf(user))
        }
        tag.put("users", usersTag)
        
        // Save architecture state
        architecture?.let { arch ->
            val archTag = CompoundTag()
            arch.save(archTag)
            tag.put("architecture", archTag)
        }
    }
    
    fun setNode(node: Node?) {
        _node = node
    }
    
    // ========================================
    // Value Conversion
    // ========================================
    
    private fun convertForLua(value: Any?): Any? {
        return when (value) {
            null -> null
            is Boolean, is Number, is String -> value
            is ByteArray -> String(value, Charsets.UTF_8)
            is Array<*> -> value.mapIndexed { i, v -> (i + 1) to convertForLua(v) }.toMap()
            is List<*> -> value.mapIndexed { i, v -> (i + 1) to convertForLua(v) }.toMap()
            is Map<*, *> -> value.mapKeys { convertForLua(it.key) }.mapValues { convertForLua(it.value) }
            else -> value.toString()
        }
    }
}

/**
 * Thread pool for executing machine code.
 */
object MachineExecutor {
    private val executor: ExecutorService by lazy {
        Executors.newFixedThreadPool(Settings.executorThreads) { r ->
            Thread(r, "OpenComputers-Machine-Worker").apply {
                isDaemon = true
                priority = Thread.MIN_PRIORITY
            }
        }
    }
    
    fun submit(task: () -> Unit): Future<*> = executor.submit(task)
    
    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
}

/**
 * Registry for machine architectures and related initialization.
 */
object MachineRegistry {
    /**
     * Registers the default architectures.
     */
    fun registerDefaults() {
        // Register Lua 5.4 architecture
        ArchitectureRegistry.register("lua54", object : ArchitectureFactory {
            override val name = "Lua 5.4"
            override val isNative = false
            override fun create(machine: Machine) = LuaJArchitecture(machine)
        })
        
        // Register Lua 5.3 compatibility mode
        ArchitectureRegistry.register("lua53", object : ArchitectureFactory {
            override val name = "Lua 5.3 (Compat)"
            override val isNative = false
            override fun create(machine: Machine) = LuaJArchitecture(machine)
        })
        
        ArchitectureRegistry.setDefault("lua54")
        
        OpenComputers.LOGGER.info("Registered default architectures")
    }
}

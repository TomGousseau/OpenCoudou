package li.cil.oc.api.machine

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import kotlin.reflect.KClass

/**
 * Architecture defines an execution environment for a machine.
 * 
 * An architecture is responsible for:
 * - Initializing the language runtime (e.g., Lua VM)
 * - Executing code on worker threads
 * - Handling synchronized calls on the server thread
 * - Managing memory allocation
 * - Persisting state across saves
 * 
 * The default architecture is Lua (5.4), but others could be implemented
 * for different languages (Python, JavaScript, etc.).
 * 
 * Thread Safety:
 * - initialize() is called on the server thread
 * - runThreaded() is called on a worker thread
 * - runSynchronized() is called on the server thread
 * - close() is called on the server thread
 * 
 * @see Machine
 */
interface Architecture {
    /**
     * The machine this architecture is running on.
     */
    val machine: Machine
    
    /**
     * Whether this architecture has been initialized.
     */
    val isInitialized: Boolean
    
    /**
     * Initializes the architecture.
     * This creates the language runtime and sets up the initial state.
     * Called on the server thread when the machine starts.
     * 
     * @return True if initialization succeeded
     */
    fun initialize(): Boolean
    
    /**
     * Closes the architecture and releases resources.
     * Called on the server thread when the machine stops.
     */
    fun close()
    
    /**
     * Executes code on a worker thread.
     * This is the main execution method called repeatedly while the
     * machine is running.
     * 
     * @param isSynchronizedReturn Whether this call is returning from a synchronized call
     * @return The result of the execution
     */
    fun runThreaded(isSynchronizedReturn: Boolean): ExecutionResult
    
    /**
     * Executes a synchronized operation on the server thread.
     * This is called when runThreaded returns SynchronizedCall.
     */
    fun runSynchronized()
    
    /**
     * Recalculates memory allocation based on installed RAM.
     * 
     * @param memory The memory stacks installed
     * @return True if the memory configuration is valid
     */
    fun recomputeMemory(memory: Iterable<ItemStack>): Boolean
    
    /**
     * Called when a signal is received.
     * This will wake the architecture if it's sleeping.
     */
    fun onSignal()
    
    /**
     * Loads the architecture state from NBT.
     * 
     * @param tag The tag to load from
     */
    fun load(tag: CompoundTag)
    
    /**
     * Saves the architecture state to NBT.
     * 
     * @param tag The tag to save to
     */
    fun save(tag: CompoundTag)
}

/**
 * Annotation to mark a class as an architecture implementation.
 * Architectures must have a constructor that takes a Machine parameter.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ArchitectureName(
    /**
     * The display name for this architecture.
     */
    val value: String,
    
    /**
     * Whether this architecture uses native libraries.
     * Native architectures may not work on all platforms.
     */
    val isNative: Boolean = false
)

/**
 * Factory for creating architecture instances.
 */
interface ArchitectureFactory {
    /**
     * The display name for this architecture.
     */
    val name: String
    
    /**
     * Whether this architecture requires native libraries.
     */
    val isNative: Boolean
    
    /**
     * Creates a new architecture instance for the given machine.
     */
    fun create(machine: Machine): li.cil.oc.api.machine.Architecture
}

/**
 * Registry for available architectures.
 */
object ArchitectureRegistry {
    private val architectures = mutableMapOf<String, ArchitectureFactory>()
    private var defaultArchitecture: String = "lua54"
    
    /**
     * Registers an architecture factory.
     * 
     * @param id The unique ID for this architecture
     * @param factory The factory to create instances
     */
    fun register(id: String, factory: ArchitectureFactory) {
        architectures[id] = factory
    }
    
    /**
     * Registers an architecture class directly.
     * The class must be annotated with @Architecture and have a constructor
     * that takes a Machine parameter.
     */
    fun register(id: String, architectureClass: KClass<out li.cil.oc.api.machine.Architecture>) {
        val annotation = architectureClass.annotations.filterIsInstance<ArchitectureName>().firstOrNull()
            ?: throw IllegalArgumentException("Architecture class must be annotated with @ArchitectureName")
        
        register(id, object : ArchitectureFactory {
            override val name = annotation.value
            override val isNative = annotation.isNative
            
            override fun create(machine: Machine): li.cil.oc.api.machine.Architecture {
                return architectureClass.constructors.first { 
                    it.parameters.size == 1 && it.parameters[0].type.classifier == Machine::class 
                }.call(machine)
            }
        })
    }
    
    /**
     * Gets an architecture factory by ID.
     */
    fun get(id: String): ArchitectureFactory? = architectures[id]
    
    /**
     * Gets all registered architectures.
     */
    fun all(): Map<String, ArchitectureFactory> = architectures.toMap()
    
    /**
     * Gets the default architecture ID.
     */
    fun getDefault(): String = defaultArchitecture
    
    /**
     * Sets the default architecture ID.
     */
    fun setDefault(id: String) {
        if (id in architectures) {
            defaultArchitecture = id
        }
    }
    
    /**
     * Creates an instance of the default architecture.
     */
    fun createDefault(machine: Machine): li.cil.oc.api.machine.Architecture? {
        return architectures[defaultArchitecture]?.create(machine)
    }
}

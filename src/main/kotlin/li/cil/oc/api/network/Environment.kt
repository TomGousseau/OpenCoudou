package li.cil.oc.api.network

import net.minecraft.nbt.CompoundTag

/**
 * Represents an environment that hosts a node.
 * 
 * This is typically implemented by block entities and entities that participate
 * in the OpenComputers network. The environment provides lifecycle callbacks
 * for when nodes connect or disconnect, and for handling messages.
 * 
 * @see Node
 * @see ManagedEnvironment
 */
interface Environment {
    /**
     * The node hosted by this environment.
     * This is the node that represents this environment in the network.
     */
    val node: Node?
    
    /**
     * Called when a node connects to this environment's node.
     * This can be used to perform initialization or send welcome messages.
     * 
     * @param node The node that connected
     */
    fun onConnect(node: Node)
    
    /**
     * Called when a node disconnects from this environment's node.
     * This can be used for cleanup or to notify other systems.
     * 
     * @param node The node that disconnected
     */
    fun onDisconnect(node: Node)
    
    /**
     * Called when a message is received by this environment's node.
     * 
     * @param message The message received
     * @return A response to send back, or null for no response
     */
    fun onMessage(message: Message): Any?
}

/**
 * A managed environment is an environment whose lifecycle is managed by a host.
 * 
 * This is typically used for items that are placed inside computers, such as
 * cards, drives, and upgrades. The host (the computer) manages the lifecycle
 * of these environments, calling update() each tick and saving/loading their state.
 * 
 * @see Environment
 */
interface ManagedEnvironment : Environment {
    /**
     * Whether this environment should receive update ticks.
     * Return true if this environment needs to do work each tick.
     */
    fun canUpdate(): Boolean = false
    
    /**
     * Called each tick if canUpdate() returns true.
     * Use this for time-based operations like cooling, charging, etc.
     */
    fun update() {}
    
    /**
     * Loads this environment's state from NBT.
     * This is called when the world loads or when the item is placed.
     * 
     * @param tag The tag to load from
     */
    fun loadData(tag: CompoundTag)
    
    /**
     * Saves this environment's state to NBT.
     * This is called when the world saves or when the item is removed.
     * 
     * @param tag The tag to save to
     */
    fun saveData(tag: CompoundTag)
}

/**
 * Base implementation of ManagedEnvironment with sensible defaults.
 */
abstract class AbstractManagedEnvironment : ManagedEnvironment {
    protected var _node: Node? = null
    
    override val node: Node?
        get() = _node
    
    override fun onConnect(node: Node) {
        // Default: no-op
    }
    
    override fun onDisconnect(node: Node) {
        // Default: no-op
    }
    
    override fun onMessage(message: Message): Any? {
        // Default: no response
        return null
    }
    
    override fun loadData(tag: CompoundTag) {
        _node?.load(tag.getCompound("node"))
    }
    
    override fun saveData(tag: CompoundTag) {
        _node?.let { node ->
            val nodeTag = CompoundTag()
            node.save(nodeTag)
            tag.put("node", nodeTag)
        }
    }
    
    /**
     * Creates and initializes the node for this environment.
     * Call this in your constructor after setting up the node builder.
     */
    protected fun createNode(builder: NodeBuilder) {
        _node = builder.create()
        // Register methods if this is a component
        (_node as? ComponentImpl)?.registerMethods(this)
        (_node as? ComponentConnectorImpl)?.registerMethods(this)
    }
}

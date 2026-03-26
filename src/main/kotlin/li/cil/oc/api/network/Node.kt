package li.cil.oc.api.network

import net.minecraft.nbt.CompoundTag
import java.util.UUID

/**
 * Represents a node in the component network.
 * 
 * Nodes are the fundamental units of the OpenComputers network system. Each node
 * has a unique address and can send/receive messages to/from other nodes in the
 * same network.
 * 
 * Nodes can be:
 * - Simple nodes: Just connect things together (like cables)
 * - Connectors: Simple nodes that also distribute power
 * - Components: Nodes that expose methods to computers
 * 
 * The network forms a graph where nodes are connected via edges. When nodes connect
 * or disconnect, the network topology changes, potentially splitting or merging
 * networks.
 * 
 * @see Component
 * @see Connector
 * @see Network
 */
interface Node {
    /**
     * The unique address of this node.
     * This is a UUID that uniquely identifies this node in any network.
     * The address is persistent across saves and loads.
     */
    val address: String
    
    /**
     * The network this node belongs to, or null if not connected.
     * A node can only belong to one network at a time.
     */
    val network: Network?
    
    /**
     * The environment hosting this node.
     * This is typically a block entity or item that contains the node.
     */
    val host: Environment
    
    /**
     * The reachability setting for this node.
     * This determines which other nodes can send messages to this node.
     */
    val reachability: Reachability
    
    /**
     * Whether this node is currently connected to a network.
     */
    val isConnected: Boolean
        get() = network != null
    
    /**
     * Connects this node to another node.
     * If the nodes are in different networks, the networks will be merged.
     * If neither node has a network, a new network will be created.
     * 
     * @param other The node to connect to
     * @return True if the connection was successful
     */
    fun connect(other: Node): Boolean
    
    /**
     * Disconnects this node from another node.
     * If this causes the network to split, two new networks will be created.
     * 
     * @param other The node to disconnect from
     * @return True if the disconnection was successful
     */
    fun disconnect(other: Node): Boolean
    
    /**
     * Removes this node from its network entirely.
     * All connections to other nodes will be severed.
     */
    fun remove()
    
    /**
     * Gets all nodes directly connected to this node (neighbors).
     */
    fun neighbors(): Collection<Node>
    
    /**
     * Gets all nodes reachable from this node in the network.
     * This considers the visibility settings of nodes.
     */
    fun reachableNodes(): Collection<Node>
    
    /**
     * Sends a message to a specific node address.
     * 
     * @param target The address of the target node
     * @param name The message name
     * @param args The message arguments
     * @return The response from the target node, or null if no response
     */
    fun sendToAddress(target: String, name: String, vararg args: Any?): Any?
    
    /**
     * Sends a message to all directly connected neighbor nodes.
     * 
     * @param name The message name
     * @param args The message arguments
     */
    fun sendToNeighbors(name: String, vararg args: Any?)
    
    /**
     * Sends a message to all reachable nodes in the network.
     * 
     * @param name The message name
     * @param args The message arguments
     */
    fun sendToReachable(name: String, vararg args: Any?)
    
    /**
     * Sends a message to all visible nodes from this node.
     * Visibility is determined by the node's reachability setting.
     * 
     * @param name The message name
     * @param args The message arguments
     */
    fun sendToVisible(name: String, vararg args: Any?)
    
    /**
     * Loads this node's state from NBT.
     * 
     * @param tag The tag to load from
     */
    fun load(tag: CompoundTag)
    
    /**
     * Saves this node's state to NBT.
     * 
     * @param tag The tag to save to
     */
    fun save(tag: CompoundTag)
}

/**
 * Message sent between nodes in the network.
 */
data class Message(
    /** The source node that sent the message */
    val source: Node,
    /** The name/type of the message */
    val name: String,
    /** The arguments passed with the message */
    val data: Array<out Any?>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Message) return false
        return source == other.source && name == other.name && data.contentEquals(other.data)
    }
    
    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Controls which nodes can see and communicate with a node.
 */
enum class Reachability {
    /** Node is not visible to any computers */
    NONE,
    
    /** Node is only visible to directly adjacent computers */
    NEIGHBORS,
    
    /** Node is visible to all computers in the network */
    NETWORK
}

/**
 * Builder for creating nodes with various capabilities.
 */
class NodeBuilder(private val host: Environment) {
    private var reachability: Reachability = Reachability.NETWORK
    private var componentName: String? = null
    private var componentVisibility: Visibility = Visibility.OTHERS
    private var connectorBufferSize: Double = 0.0
    
    /**
     * Sets the reachability for this node.
     */
    fun withReachability(reachability: Reachability): NodeBuilder {
        this.reachability = reachability
        return this
    }
    
    /**
     * Makes this node a component with the given name.
     * Components can expose methods to computers.
     */
    fun withComponent(name: String, visibility: Visibility = Visibility.OTHERS): NodeBuilder {
        this.componentName = name
        this.componentVisibility = visibility
        return this
    }
    
    /**
     * Makes this node a connector with the given power buffer size.
     * Connectors can distribute power through the network.
     */
    fun withConnector(bufferSize: Double): NodeBuilder {
        this.connectorBufferSize = bufferSize
        return this
    }
    
    /**
     * Creates the node with the configured settings.
     */
    fun create(): Node {
        val name = componentName
        return when {
            name != null && connectorBufferSize > 0 -> {
                ComponentConnectorImpl(host, reachability, name, componentVisibility, connectorBufferSize)
            }
            name != null -> {
                ComponentImpl(host, reachability, name, componentVisibility)
            }
            connectorBufferSize > 0 -> {
                ConnectorImpl(host, reachability, connectorBufferSize)
            }
            else -> {
                NodeImpl(host, reachability)
            }
        }
    }
}

// Implementation classes are internal
internal open class NodeImpl(
    override val host: Environment,
    override val reachability: Reachability
) : Node {
    override var address: String = UUID.randomUUID().toString()
        protected set
    
    override var network: Network? = null
        internal set
    
    private val _neighbors = mutableSetOf<Node>()
    
    override fun connect(other: Node): Boolean {
        if (other == this) return false
        if (_neighbors.contains(other)) return true
        
        // Connect the nodes
        _neighbors.add(other)
        (other as? NodeImpl)?._neighbors?.add(this)
        
        // Join or merge networks
        val myNetwork = network
        val otherNetwork = other.network
        
        when {
            myNetwork == null && otherNetwork == null -> {
                // Create new network
                val newNetwork = NetworkImpl()
                newNetwork.addNode(this)
                newNetwork.addNode(other)
            }
            myNetwork == null -> {
                // Join other's network
                (otherNetwork as? NetworkImpl)?.addNode(this)
            }
            otherNetwork == null -> {
                // Other joins my network
                (myNetwork as? NetworkImpl)?.addNode(other)
            }
            myNetwork != otherNetwork -> {
                // Merge networks
                (myNetwork as? NetworkImpl)?.merge(otherNetwork as NetworkImpl)
            }
        }
        
        // Notify environments
        host.onConnect(other)
        other.host.onConnect(this)
        
        return true
    }
    
    override fun disconnect(other: Node): Boolean {
        if (!_neighbors.contains(other)) return false
        
        _neighbors.remove(other)
        (other as? NodeImpl)?._neighbors?.remove(this)
        
        // Check if network needs to split
        network?.let { net ->
            (net as? NetworkImpl)?.checkSplit(this, other)
        }
        
        // Notify environments
        host.onDisconnect(other)
        other.host.onDisconnect(this)
        
        return true
    }
    
    override fun remove() {
        val neighborsCopy = _neighbors.toList()
        for (neighbor in neighborsCopy) {
            disconnect(neighbor)
        }
        network?.let { net ->
            (net as? NetworkImpl)?.removeNode(this)
        }
        network = null
    }
    
    override fun neighbors(): Collection<Node> = _neighbors.toList()
    
    override fun reachableNodes(): Collection<Node> {
        val net = network ?: return emptyList()
        return net.nodes.filter { node ->
            node != this && when (node.reachability) {
                Reachability.NONE -> false
                Reachability.NEIGHBORS -> _neighbors.contains(node)
                Reachability.NETWORK -> true
            }
        }
    }
    
    override fun sendToAddress(target: String, name: String, vararg args: Any?): Any? {
        val targetNode = network?.node(target) ?: return null
        return targetNode.host.onMessage(Message(this, name, args))
    }
    
    override fun sendToNeighbors(name: String, vararg args: Any?) {
        val message = Message(this, name, args)
        for (neighbor in _neighbors) {
            neighbor.host.onMessage(message)
        }
    }
    
    override fun sendToReachable(name: String, vararg args: Any?) {
        val message = Message(this, name, args)
        for (node in reachableNodes()) {
            node.host.onMessage(message)
        }
    }
    
    override fun sendToVisible(name: String, vararg args: Any?) {
        val message = Message(this, name, args)
        val net = network ?: return
        for (node in net.nodes) {
            if (node != this && isVisible(node)) {
                node.host.onMessage(message)
            }
        }
    }
    
    protected open fun isVisible(other: Node): Boolean {
        return when (reachability) {
            Reachability.NONE -> false
            Reachability.NEIGHBORS -> _neighbors.contains(other)
            Reachability.NETWORK -> true
        }
    }
    
    override fun load(tag: CompoundTag) {
        if (tag.contains("address")) {
            address = tag.getString("address")
        }
    }
    
    override fun save(tag: CompoundTag) {
        tag.putString("address", address)
    }
}

internal class ConnectorImpl(
    host: Environment,
    reachability: Reachability,
    bufferSize: Double
) : NodeImpl(host, reachability), Connector {
    
    override val bufferSize: Double = bufferSize
    override var localBuffer: Double = 0.0
        private set
    
    override val globalBuffer: Double
        get() = network?.let { net ->
            (net as? NetworkImpl)?.globalEnergy ?: localBuffer
        } ?: localBuffer
    
    override val globalBufferSize: Double
        get() = network?.let { net ->
            (net as? NetworkImpl)?.globalEnergyCapacity ?: bufferSize
        } ?: bufferSize
    
    override fun changeBuffer(delta: Double): Double {
        val net = network as? NetworkImpl
        return if (net != null) {
            net.changeEnergy(delta)
        } else {
            val oldBuffer = localBuffer
            localBuffer = (localBuffer + delta).coerceIn(0.0, bufferSize)
            localBuffer - oldBuffer
        }
    }
    
    override fun tryChangeBuffer(delta: Double): Boolean {
        return if (delta < 0) {
            globalBuffer >= -delta && changeBuffer(delta) != 0.0
        } else {
            globalBuffer + delta <= globalBufferSize && changeBuffer(delta) != 0.0
        }
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        localBuffer = tag.getDouble("buffer").coerceIn(0.0, bufferSize)
    }
    
    override fun save(tag: CompoundTag) {
        super.save(tag)
        tag.putDouble("buffer", localBuffer)
    }
}

internal open class ComponentImpl(
    host: Environment,
    reachability: Reachability,
    override val name: String,
    override val visibility: Visibility
) : NodeImpl(host, reachability), Component {
    
    private val _methods = mutableMapOf<String, ComponentMethod>()
    
    override val methods: Map<String, ComponentMethod>
        get() = _methods.toMap()
    
    override fun canBeSeenFrom(other: Node): Boolean {
        return when (visibility) {
            Visibility.NONE -> false
            Visibility.NETWORK -> network?.contains(other) == true
            Visibility.NEIGHBORS -> neighbors().contains(other)
            Visibility.OTHERS -> network?.contains(other) == true && other != this
        }
    }
    
    override fun invoke(method: String, context: li.cil.oc.api.machine.Context, vararg args: Any?): Array<Any?> {
        val m = _methods[method] ?: throw NoSuchMethodException("No such method: $method")
        return m.invoke(context, args)
    }
    
    fun registerMethod(name: String, method: ComponentMethod) {
        _methods[name] = method
    }
    
    fun registerMethods(target: Any) {
        // Use reflection to find @Callback annotated methods
        for (method in target::class.java.methods) {
            val callback = method.getAnnotation(li.cil.oc.api.machine.Callback::class.java) ?: continue
            val methodName = callback.value.ifEmpty { method.name }
            _methods[methodName] = ReflectionComponentMethod(target, method, callback)
        }
    }
}

internal class ComponentConnectorImpl(
    host: Environment,
    reachability: Reachability,
    override val name: String,
    override val visibility: Visibility,
    bufferSize: Double
) : ConnectorImpl(host, reachability, bufferSize), Component {
    
    private val _methods = mutableMapOf<String, ComponentMethod>()
    
    override val methods: Map<String, ComponentMethod>
        get() = _methods.toMap()
    
    override fun canBeSeenFrom(other: Node): Boolean {
        return when (visibility) {
            Visibility.NONE -> false
            Visibility.NETWORK -> network?.contains(other) == true
            Visibility.NEIGHBORS -> neighbors().contains(other)
            Visibility.OTHERS -> network?.contains(other) == true && other != this
        }
    }
    
    override fun invoke(method: String, context: li.cil.oc.api.machine.Context, vararg args: Any?): Array<Any?> {
        val m = _methods[method] ?: throw NoSuchMethodException("No such method: $method")
        return m.invoke(context, args)
    }
    
    fun registerMethod(name: String, method: ComponentMethod) {
        _methods[name] = method
    }
    
    fun registerMethods(target: Any) {
        for (method in target::class.java.methods) {
            val callback = method.getAnnotation(li.cil.oc.api.machine.Callback::class.java) ?: continue
            val methodName = callback.value.ifEmpty { method.name }
            _methods[methodName] = ReflectionComponentMethod(target, method, callback)
        }
    }
}

/**
 * Internal network implementation
 */
internal class NetworkImpl : Network {
    private val _nodes = mutableSetOf<Node>()
    
    override val nodes: Collection<Node>
        get() = _nodes.toList()
    
    var globalEnergy: Double = 0.0
        private set
    
    var globalEnergyCapacity: Double = 0.0
        private set
    
    override fun contains(node: Node): Boolean = _nodes.contains(node)
    
    override fun node(address: String): Node? = _nodes.find { it.address == address }
    
    override fun <T : Node> nodes(type: Class<T>): Collection<T> {
        @Suppress("UNCHECKED_CAST")
        return _nodes.filter { type.isInstance(it) }.map { it as T }
    }
    
    override fun components(): Collection<Component> = nodes(Component::class.java)
    
    override fun connectors(): Collection<Connector> = nodes(Connector::class.java)
    
    fun addNode(node: Node) {
        if (_nodes.add(node)) {
            (node as? NodeImpl)?.network = this
            if (node is Connector) {
                globalEnergyCapacity += node.bufferSize
            }
            // Notify all existing nodes of the new connection
            for (existing in _nodes) {
                if (existing != node) {
                    existing.host.onConnect(node)
                }
            }
        }
    }
    
    fun removeNode(node: Node) {
        if (_nodes.remove(node)) {
            (node as? NodeImpl)?.network = null
            if (node is Connector) {
                globalEnergyCapacity -= node.bufferSize
            }
            // Notify all remaining nodes of the disconnection
            for (remaining in _nodes) {
                remaining.host.onDisconnect(node)
            }
        }
    }
    
    fun merge(other: NetworkImpl) {
        // Transfer all nodes from other network to this one
        for (node in other._nodes.toList()) {
            other.removeNode(node)
            addNode(node)
        }
        // Transfer energy
        globalEnergy += other.globalEnergy
    }
    
    fun checkSplit(node1: Node, node2: Node) {
        // Check if removing the edge between node1 and node2 splits the network
        val reachable = mutableSetOf<Node>()
        val queue = ArrayDeque<Node>()
        queue.add(node1)
        reachable.add(node1)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (neighbor in current.neighbors()) {
                if (neighbor !in reachable) {
                    reachable.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }
        
        if (!reachable.contains(node2)) {
            // Network has split - create new network for unreachable nodes
            val newNetwork = NetworkImpl()
            val unreachable = _nodes.filter { it !in reachable }
            for (node in unreachable) {
                _nodes.remove(node)
                if (node is Connector) {
                    globalEnergyCapacity -= node.bufferSize
                }
                newNetwork.addNode(node)
            }
            // Split energy proportionally
            if (globalEnergyCapacity > 0) {
                val ratio = newNetwork.globalEnergyCapacity / (globalEnergyCapacity + newNetwork.globalEnergyCapacity)
                val transfer = globalEnergy * ratio
                globalEnergy -= transfer
                newNetwork.globalEnergy += transfer
            }
        }
    }
    
    fun changeEnergy(delta: Double): Double {
        val oldEnergy = globalEnergy
        globalEnergy = (globalEnergy + delta).coerceIn(0.0, globalEnergyCapacity)
        return globalEnergy - oldEnergy
    }
}

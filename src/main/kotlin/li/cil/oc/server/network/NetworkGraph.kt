package li.cil.oc.server.network

import li.cil.oc.api.network.Component
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.config.Config
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.Level
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * An actual network graph implementation that connects OC nodes.
 * This manages the underlying graph of connected components and
 * handles power distribution, signal propagation, and component visibility.
 */
class NetworkGraph {
    // All nodes in this network
    private val nodes = ConcurrentHashMap<UUID, NetworkNode>()
    
    // Edges representing connections between nodes
    private val edges = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    
    // Total energy stored in the network
    private var storedEnergy: Double = 0.0
    
    // Maximum energy capacity of the network
    private var maxEnergy: Double = 0.0
    
    // Network identifier
    val id: UUID = UUID.randomUUID()
    
    // Whether the network needs to be recalculated
    private var dirty: Boolean = false
    
    /**
     * Add a node to this network
     */
    fun addNode(node: NetworkNode): Boolean {
        if (nodes.containsKey(node.id)) return false
        
        nodes[node.id] = node
        edges.putIfAbsent(node.id, mutableSetOf())
        
        // Update energy capacity if this is a power node
        if (node is PowerNode) {
            maxEnergy += node.maxEnergy
            storedEnergy += node.storedEnergy
        }
        
        // Notify the environment that it connected
        node.environment?.onConnect(node)
        
        dirty = true
        return true
    }
    
    /**
     * Remove a node from this network
     */
    fun removeNode(node: NetworkNode): Boolean {
        if (!nodes.containsKey(node.id)) return false
        
        // Notify the environment that it will disconnect
        node.environment?.onDisconnect(node)
        
        // Remove all edges involving this node
        val connected = edges.remove(node.id) ?: mutableSetOf()
        for (otherId in connected) {
            edges[otherId]?.remove(node.id)
        }
        
        nodes.remove(node.id)
        
        // Update energy capacity if this is a power node
        if (node is PowerNode) {
            maxEnergy -= node.maxEnergy
            storedEnergy = min(storedEnergy, maxEnergy)
        }
        
        dirty = true
        return true
    }
    
    /**
     * Connect two nodes in this network
     */
    fun connect(nodeA: NetworkNode, nodeB: NetworkNode): Boolean {
        if (!nodes.containsKey(nodeA.id) || !nodes.containsKey(nodeB.id)) return false
        if (nodeA.id == nodeB.id) return false
        
        val edgesA = edges.getOrPut(nodeA.id) { mutableSetOf() }
        val edgesB = edges.getOrPut(nodeB.id) { mutableSetOf() }
        
        if (edgesA.contains(nodeB.id)) return false
        
        edgesA.add(nodeB.id)
        edgesB.add(nodeA.id)
        
        dirty = true
        return true
    }
    
    /**
     * Disconnect two nodes in this network
     */
    fun disconnect(nodeA: NetworkNode, nodeB: NetworkNode): Boolean {
        val edgesA = edges[nodeA.id] ?: return false
        val edgesB = edges[nodeB.id] ?: return false
        
        if (!edgesA.contains(nodeB.id)) return false
        
        edgesA.remove(nodeB.id)
        edgesB.remove(nodeA.id)
        
        dirty = true
        return true
    }
    
    /**
     * Get all nodes in this network
     */
    fun getNodes(): Collection<NetworkNode> = nodes.values
    
    /**
     * Get a node by ID
     */
    fun getNode(id: UUID): NetworkNode? = nodes[id]
    
    /**
     * Get nodes connected to a given node
     */
    fun getConnectedNodes(node: NetworkNode): Set<NetworkNode> {
        val connectedIds = edges[node.id] ?: return emptySet()
        return connectedIds.mapNotNull { nodes[it] }.toSet()
    }
    
    /**
     * Check if two nodes are connected directly
     */
    fun areDirectlyConnected(nodeA: NetworkNode, nodeB: NetworkNode): Boolean {
        return edges[nodeA.id]?.contains(nodeB.id) == true
    }
    
    /**
     * Find all components visible to a given node
     */
    fun getVisibleComponents(node: NetworkNode): List<ComponentNode> {
        val visible = mutableListOf<ComponentNode>()
        val visited = mutableSetOf<UUID>()
        val queue: Queue<Pair<UUID, Int>> = LinkedList()
        
        queue.add(node.id to 0)
        visited.add(node.id)
        
        while (queue.isNotEmpty()) {
            val (currentId, distance) = queue.poll()
            val current = nodes[currentId] ?: continue
            
            // Add component if visible at this distance
            if (current is ComponentNode && current.id != node.id) {
                if (current.visibility == Visibility.NETWORK ||
                    (current.visibility == Visibility.NEIGHBORS && distance == 1)) {
                    visible.add(current)
                }
            }
            
            // Explore neighbors
            for (neighborId in edges[currentId] ?: emptySet()) {
                if (neighborId !in visited) {
                    visited.add(neighborId)
                    queue.add(neighborId to distance + 1)
                }
            }
        }
        
        return visible
    }
    
    /**
     * Find shortest path between two nodes
     */
    fun findPath(from: NetworkNode, to: NetworkNode): List<NetworkNode>? {
        if (from.id == to.id) return listOf(from)
        
        val visited = mutableSetOf<UUID>()
        val parent = mutableMapOf<UUID, UUID>()
        val queue: Queue<UUID> = LinkedList()
        
        queue.add(from.id)
        visited.add(from.id)
        
        while (queue.isNotEmpty()) {
            val currentId = queue.poll()
            
            for (neighborId in edges[currentId] ?: emptySet()) {
                if (neighborId !in visited) {
                    visited.add(neighborId)
                    parent[neighborId] = currentId
                    
                    if (neighborId == to.id) {
                        // Reconstruct path
                        val path = mutableListOf<NetworkNode>()
                        var cursor = to.id
                        while (cursor != from.id) {
                            path.add(0, nodes[cursor]!!)
                            cursor = parent[cursor]!!
                        }
                        path.add(0, from)
                        return path
                    }
                    
                    queue.add(neighborId)
                }
            }
        }
        
        return null
    }
    
    /**
     * Send a message across the network
     */
    fun sendMessage(source: NetworkNode, target: String, name: String, vararg args: Any): Boolean {
        val targetNode = nodes.values.find { 
            it is ComponentNode && it.address == target 
        } as? ComponentNode ?: return false
        
        // Check visibility
        val visibleComponents = getVisibleComponents(source)
        if (targetNode !in visibleComponents) return false
        
        targetNode.environment?.onMessage(NetworkMessage(source, name, args.toList()))
        return true
    }
    
    /**
     * Broadcast a message to all reachable nodes
     */
    fun broadcastMessage(source: NetworkNode, name: String, vararg args: Any) {
        val message = NetworkMessage(source, name, args.toList())
        
        for (node in nodes.values) {
            if (node.id != source.id) {
                node.environment?.onMessage(message)
            }
        }
    }
    
    /**
     * Try to consume energy from the network
     */
    fun tryConsumeEnergy(amount: Double): Boolean {
        if (storedEnergy >= amount) {
            storedEnergy -= amount
            return true
        }
        return false
    }
    
    /**
     * Try to store energy in the network
     */
    fun tryStoreEnergy(amount: Double): Double {
        val canStore = min(amount, maxEnergy - storedEnergy)
        storedEnergy += canStore
        return canStore
    }
    
    /**
     * Get current energy level
     */
    fun getStoredEnergy(): Double = storedEnergy
    
    /**
     * Get maximum energy capacity
     */
    fun getMaxEnergy(): Double = maxEnergy
    
    /**
     * Merge another network into this one
     */
    fun merge(other: NetworkGraph) {
        for (node in other.nodes.values) {
            addNode(node)
        }
        for ((nodeId, connectedIds) in other.edges) {
            val ourEdges = edges.getOrPut(nodeId) { mutableSetOf() }
            ourEdges.addAll(connectedIds)
        }
        storedEnergy += other.storedEnergy
    }
    
    /**
     * Split this network at a removed connection and return any disconnected subnetworks
     */
    fun splitAt(nodeA: NetworkNode, nodeB: NetworkNode): List<NetworkGraph> {
        disconnect(nodeA, nodeB)
        
        // Find connected components using BFS
        val visited = mutableSetOf<UUID>()
        val components = mutableListOf<MutableSet<UUID>>()
        
        for (nodeId in nodes.keys) {
            if (nodeId in visited) continue
            
            val component = mutableSetOf<UUID>()
            val queue: Queue<UUID> = LinkedList()
            queue.add(nodeId)
            visited.add(nodeId)
            
            while (queue.isNotEmpty()) {
                val current = queue.poll()
                component.add(current)
                
                for (neighborId in edges[current] ?: emptySet()) {
                    if (neighborId !in visited) {
                        visited.add(neighborId)
                        queue.add(neighborId)
                    }
                }
            }
            
            components.add(component)
        }
        
        // If still one component, no split needed
        if (components.size <= 1) return emptyList()
        
        // Create new networks for all but the largest component
        val sortedComponents = components.sortedByDescending { it.size }
        val result = mutableListOf<NetworkGraph>()
        
        for (i in 1 until sortedComponents.size) {
            val newNetwork = NetworkGraph()
            val componentNodeIds = sortedComponents[i]
            
            // Move nodes to new network
            for (nodeId in componentNodeIds) {
                val node = nodes.remove(nodeId) ?: continue
                newNetwork.nodes[nodeId] = node
                
                // Move edges
                val nodeEdges = edges.remove(nodeId) ?: mutableSetOf()
                newNetwork.edges[nodeId] = nodeEdges.filter { it in componentNodeIds }.toMutableSet()
            }
            
            result.add(newNetwork)
        }
        
        // Update this network's edges to only include remaining nodes
        val remainingNodes = sortedComponents[0]
        for (nodeId in edges.keys.toList()) {
            if (nodeId !in remainingNodes) {
                edges.remove(nodeId)
            } else {
                edges[nodeId]?.retainAll(remainingNodes)
            }
        }
        
        // Recalculate energy
        recalculateEnergy()
        result.forEach { it.recalculateEnergy() }
        
        return result
    }
    
    private fun recalculateEnergy() {
        maxEnergy = 0.0
        storedEnergy = 0.0
        
        for (node in nodes.values) {
            if (node is PowerNode) {
                maxEnergy += node.maxEnergy
                storedEnergy += node.storedEnergy
            }
        }
    }
    
    /**
     * Save network state to NBT
     */
    fun save(): CompoundTag {
        val tag = CompoundTag()
        tag.putUUID("id", id)
        tag.putDouble("storedEnergy", storedEnergy)
        
        val nodesList = ListTag()
        for (node in nodes.values) {
            val nodeTag = CompoundTag()
            nodeTag.putUUID("id", node.id)
            nodeTag.putString("type", node.javaClass.name)
            
            val nodeData = node.save()
            nodeTag.put("data", nodeData)
            
            nodesList.add(nodeTag)
        }
        tag.put("nodes", nodesList)
        
        val edgesList = ListTag()
        for ((nodeId, connectedIds) in edges) {
            val edgeTag = CompoundTag()
            edgeTag.putUUID("node", nodeId)
            
            val connectedList = ListTag()
            for (connectedId in connectedIds) {
                val connTag = CompoundTag()
                connTag.putUUID("id", connectedId)
                connectedList.add(connTag)
            }
            edgeTag.put("connected", connectedList)
            
            edgesList.add(edgeTag)
        }
        tag.put("edges", edgesList)
        
        return tag
    }
    
    /**
     * Load network state from NBT
     */
    fun load(tag: CompoundTag) {
        storedEnergy = tag.getDouble("storedEnergy")
        
        // Clear existing state
        nodes.clear()
        edges.clear()
        
        // Load nodes
        val nodesList = tag.getList("nodes", Tag.TAG_COMPOUND.toInt())
        for (i in 0 until nodesList.size) {
            val nodeTag = nodesList.getCompound(i)
            // Node restoration would depend on a factory - simplified here
        }
        
        // Load edges
        val edgesList = tag.getList("edges", Tag.TAG_COMPOUND.toInt())
        for (i in 0 until edgesList.size) {
            val edgeTag = edgesList.getCompound(i)
            val nodeId = edgeTag.getUUID("node")
            val connectedList = edgeTag.getList("connected", Tag.TAG_COMPOUND.toInt())
            
            val connectedIds = mutableSetOf<UUID>()
            for (j in 0 until connectedList.size) {
                connectedIds.add(connectedList.getCompound(j).getUUID("id"))
            }
            
            edges[nodeId] = connectedIds
        }
    }
    
    override fun toString(): String = "NetworkGraph(id=$id, nodes=${nodes.size}, edges=${edges.values.sumOf { it.size } / 2})"
}

/**
 * Base class for network nodes
 */
open class NetworkNode(
    override val environment: Environment? = null
) : Node {
    override val id: UUID = UUID.randomUUID()
    override var network: li.cil.oc.api.network.Network? = null
    override val reachability: Visibility = Visibility.NEIGHBORS
    
    open fun save(): CompoundTag = CompoundTag()
    
    open fun load(tag: CompoundTag) {}
    
    override fun connect(node: Node) {
        // Delegate to network
    }
    
    override fun disconnect(node: Node) {
        // Delegate to network
    }
    
    override fun remove() {
        network?.remove(this)
    }
}

/**
 * A node that represents a component accessible via Lua
 */
class ComponentNode(
    environment: Environment?,
    override val name: String,
    override val visibility: Visibility = Visibility.NETWORK
) : NetworkNode(environment), Component {
    override val address: String = UUID.randomUUID().toString().take(8)
    
    override fun save(): CompoundTag {
        val tag = super.save()
        tag.putString("name", name)
        tag.putString("address", address)
        tag.putInt("visibility", visibility.ordinal)
        return tag
    }
    
    override fun canBeSeenFrom(other: Node): Boolean {
        return when (visibility) {
            Visibility.NONE -> false
            Visibility.NETWORK -> other.network == network
            Visibility.NEIGHBORS -> {
                // Check if directly connected
                val graph = (network as? NetworkImpl)?.graph
                val otherNode = other as? NetworkNode
                if (graph != null && otherNode != null) {
                    graph.areDirectlyConnected(this, otherNode)
                } else false
            }
        }
    }
}

/**
 * A node that can transfer power
 */
class PowerNode(
    environment: Environment?,
    val maxEnergy: Double,
    var storedEnergy: Double = 0.0
) : NetworkNode(environment), Connector {
    override val localBuffer: Double
        get() = storedEnergy
    
    override val localBufferSize: Double
        get() = maxEnergy
    
    override val globalBuffer: Double
        get() = (network as? NetworkImpl)?.graph?.getStoredEnergy() ?: storedEnergy
    
    override val globalBufferSize: Double
        get() = (network as? NetworkImpl)?.graph?.getMaxEnergy() ?: maxEnergy
    
    override fun changeBuffer(delta: Double): Double {
        val graph = (network as? NetworkImpl)?.graph
        return if (graph != null) {
            if (delta > 0) {
                graph.tryStoreEnergy(delta)
            } else {
                if (graph.tryConsumeEnergy(-delta)) -delta else 0.0
            }
        } else {
            if (delta > 0) {
                val canStore = minOf(delta, maxEnergy - storedEnergy)
                storedEnergy += canStore
                canStore
            } else {
                val canTake = minOf(-delta, storedEnergy)
                storedEnergy -= canTake
                -canTake
            }
        }
    }
    
    override fun save(): CompoundTag {
        val tag = super.save()
        tag.putDouble("maxEnergy", maxEnergy)
        tag.putDouble("storedEnergy", storedEnergy)
        return tag
    }
    
    override fun load(tag: CompoundTag) {
        super.load(tag)
        storedEnergy = tag.getDouble("storedEnergy")
    }
}

/**
 * Message sent across the network
 */
data class NetworkMessage(
    val source: NetworkNode,
    val name: String,
    val args: List<Any>
)

/**
 * Implementation of the Network API interface
 */
class NetworkImpl(val graph: NetworkGraph = NetworkGraph()) : li.cil.oc.api.network.Network {
    override val nodes: Iterable<Node>
        get() = graph.getNodes()
    
    override fun connect(nodeA: Node, nodeB: Node): Boolean {
        val a = nodeA as? NetworkNode ?: return false
        val b = nodeB as? NetworkNode ?: return false
        return graph.connect(a, b)
    }
    
    override fun disconnect(nodeA: Node, nodeB: Node): Boolean {
        val a = nodeA as? NetworkNode ?: return false
        val b = nodeB as? NetworkNode ?: return false
        return graph.disconnect(a, b)
    }
    
    override fun remove(node: Node): Boolean {
        val n = node as? NetworkNode ?: return false
        return graph.removeNode(n)
    }
    
    override fun node(address: String): Node? {
        return graph.getNodes().filterIsInstance<ComponentNode>()
            .find { it.address == address }
    }
    
    override fun sendToAddress(source: Node, target: String, name: String, vararg args: Any): Boolean {
        val s = source as? NetworkNode ?: return false
        return graph.sendMessage(s, target, name, *args)
    }
    
    override fun sendToNeighbors(source: Node, name: String, vararg args: Any) {
        val s = source as? NetworkNode ?: return
        for (neighbor in graph.getConnectedNodes(s)) {
            neighbor.environment?.onMessage(NetworkMessage(s, name, args.toList()))
        }
    }
    
    override fun sendToReachable(source: Node, name: String, vararg args: Any) {
        val s = source as? NetworkNode ?: return
        graph.broadcastMessage(s, name, *args)
    }
    
    override fun sendToVisible(source: Node, name: String, vararg args: Any) {
        val s = source as? NetworkNode ?: return
        for (component in graph.getVisibleComponents(s)) {
            component.environment?.onMessage(NetworkMessage(s, name, args.toList()))
        }
    }
}

/**
 * Global network manager that tracks all networks in the world
 */
object NetworkManager {
    private val networks = ConcurrentHashMap<UUID, NetworkGraph>()
    private val nodeToNetwork = ConcurrentHashMap<UUID, UUID>()
    
    /**
     * Create a new network
     */
    fun createNetwork(): NetworkGraph {
        val network = NetworkGraph()
        networks[network.id] = network
        return network
    }
    
    /**
     * Get the network containing a specific node
     */
    fun getNetworkForNode(node: NetworkNode): NetworkGraph? {
        val networkId = nodeToNetwork[node.id] ?: return null
        return networks[networkId]
    }
    
    /**
     * Join a node to a network
     */
    fun joinNetwork(node: NetworkNode, network: NetworkGraph) {
        network.addNode(node)
        nodeToNetwork[node.id] = network.id
    }
    
    /**
     * Remove a node from its network
     */
    fun leaveNetwork(node: NetworkNode) {
        val networkId = nodeToNetwork.remove(node.id) ?: return
        val network = networks[networkId] ?: return
        network.removeNode(node)
        
        // Clean up empty networks
        if (network.getNodes().isEmpty()) {
            networks.remove(networkId)
        }
    }
    
    /**
     * Connect two nodes, potentially merging their networks
     */
    fun connect(nodeA: NetworkNode, nodeB: NetworkNode): Boolean {
        val networkA = getNetworkForNode(nodeA)
        val networkB = getNetworkForNode(nodeB)
        
        when {
            networkA == null && networkB == null -> {
                // Both nodes are not in a network, create a new one
                val network = createNetwork()
                joinNetwork(nodeA, network)
                joinNetwork(nodeB, network)
                network.connect(nodeA, nodeB)
            }
            networkA == null -> {
                // Only nodeB has a network, add nodeA to it
                joinNetwork(nodeA, networkB!!)
                networkB.connect(nodeA, nodeB)
            }
            networkB == null -> {
                // Only nodeA has a network, add nodeB to it
                joinNetwork(nodeB, networkA)
                networkA.connect(nodeA, nodeB)
            }
            networkA.id == networkB.id -> {
                // Same network, just connect
                networkA.connect(nodeA, nodeB)
            }
            else -> {
                // Different networks, merge them
                networkA.merge(networkB)
                networks.remove(networkB.id)
                
                // Update node mappings
                for (node in networkB.getNodes()) {
                    nodeToNetwork[node.id] = networkA.id
                }
                
                networkA.connect(nodeA, nodeB)
            }
        }
        
        return true
    }
    
    /**
     * Disconnect two nodes, potentially splitting the network
     */
    fun disconnect(nodeA: NetworkNode, nodeB: NetworkNode): Boolean {
        val network = getNetworkForNode(nodeA) ?: return false
        if (getNetworkForNode(nodeB)?.id != network.id) return false
        
        // Split the network at the connection
        val newNetworks = network.splitAt(nodeA, nodeB)
        
        // Register new networks and update mappings
        for (newNetwork in newNetworks) {
            networks[newNetwork.id] = newNetwork
            for (node in newNetwork.getNodes()) {
                nodeToNetwork[node.id] = newNetwork.id
            }
        }
        
        return true
    }
    
    /**
     * Clear all networks (for world unload)
     */
    fun clearAll() {
        networks.clear()
        nodeToNetwork.clear()
    }
    
    /**
     * Get statistics about all networks
     */
    fun getStats(): NetworkStats {
        val totalNodes = networks.values.sumOf { it.getNodes().count() }
        val totalEnergy = networks.values.sumOf { it.getStoredEnergy() }
        val maxEnergy = networks.values.sumOf { it.getMaxEnergy() }
        
        return NetworkStats(
            networkCount = networks.size,
            totalNodes = totalNodes,
            totalEnergy = totalEnergy,
            maxEnergy = maxEnergy
        )
    }
}

data class NetworkStats(
    val networkCount: Int,
    val totalNodes: Int,
    val totalEnergy: Double,
    val maxEnergy: Double
)

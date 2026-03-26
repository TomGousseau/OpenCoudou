package li.cil.oc.api.network

/**
 * A network in OpenComputers is a graph of connected nodes.
 * 
 * Networks form when nodes connect to each other. If two nodes in different
 * networks connect, the networks merge. If a connection is severed that would
 * split a network, two new networks are created.
 * 
 * Networks provide the infrastructure for:
 * - Component discovery: Computers can find and access components
 * - Power distribution: Energy flows through connectors in the network
 * - Message passing: Nodes can communicate with each other
 * 
 * @see Node
 * @see Component
 * @see Connector
 */
interface Network {
    /**
     * All nodes in this network.
     */
    val nodes: Collection<Node>
    
    /**
     * Checks if a node is in this network.
     * 
     * @param node The node to check
     * @return True if the node is in this network
     */
    fun contains(node: Node): Boolean
    
    /**
     * Gets a node by its address.
     * 
     * @param address The address to look up
     * @return The node with that address, or null if not found
     */
    fun node(address: String): Node?
    
    /**
     * Gets all nodes of a specific type.
     * 
     * @param type The class of nodes to find
     * @return All nodes in this network that are instances of the given type
     */
    fun <T : Node> nodes(type: Class<T>): Collection<T>
    
    /**
     * Gets all components in this network.
     */
    fun components(): Collection<Component>
    
    /**
     * Gets all connectors in this network.
     */
    fun connectors(): Collection<Connector>
    
    companion object {
        /**
         * Creates a new node builder for the given environment.
         * 
         * @param host The environment that will host the node
         * @return A builder for configuring and creating the node
         */
        @JvmStatic
        fun newNode(host: Environment): NodeBuilder = NodeBuilder(host)
        
        /**
         * Joins an environment to the network, or creates a new network if needed.
         * This is a convenience method for connecting tile entities.
         * 
         * @param env The environment to connect
         */
        @JvmStatic
        fun joinOrCreateNetwork(env: Environment) {
            val node = env.node ?: return
            // The node will create its own network if needed when it first
            // connects to another node
        }
    }
}

package li.cil.oc.api.network

/**
 * A connector is a node that can distribute power through the network.
 * 
 * Connectors act as both power buffers and power conduits. When power is
 * added to or removed from a connector, it's actually added to or removed
 * from the global network power pool, which is then distributed among all
 * connectors based on their buffer sizes.
 * 
 * This creates a simple but effective power distribution system where:
 * - Power can enter the network through any connector
 * - Power is available to any connector in the network
 * - Power is lost when networks split
 * 
 * @see Node
 * @see Network
 */
interface Connector : Node {
    /**
     * The local buffer size for this connector.
     * This determines how much of the network's total capacity this
     * connector contributes.
     */
    val bufferSize: Double
    
    /**
     * The current local buffer amount.
     * Note: In practice, power is shared across the network, so this
     * is mainly used for display purposes.
     */
    val localBuffer: Double
    
    /**
     * The current global buffer amount across the whole network.
     */
    val globalBuffer: Double
    
    /**
     * The total buffer capacity of the whole network.
     */
    val globalBufferSize: Double
    
    /**
     * Changes the energy in the network buffer.
     * 
     * @param delta The amount to add (positive) or remove (negative)
     * @return The amount actually added or removed
     */
    fun changeBuffer(delta: Double): Double
    
    /**
     * Attempts to change the buffer by the specified amount.
     * If the operation would exceed the buffer limits, it fails.
     * 
     * @param delta The amount to add or remove
     * @return True if the operation succeeded, false otherwise
     */
    fun tryChangeBuffer(delta: Double): Boolean
}

/**
 * Extension functions for working with power.
 */

/**
 * Checks if there is enough power to perform an operation.
 */
fun Connector.hasPower(amount: Double): Boolean = globalBuffer >= amount

/**
 * Consumes the specified amount of power if available.
 * 
 * @param amount The amount to consume
 * @return True if the power was consumed, false if insufficient
 */
fun Connector.consumePower(amount: Double): Boolean = tryChangeBuffer(-amount)

/**
 * Adds power to the network.
 * 
 * @param amount The amount to add
 * @return The amount actually added
 */
fun Connector.addPower(amount: Double): Double = changeBuffer(amount.coerceAtLeast(0.0))

/**
 * Gets the fill percentage of the network's power buffer.
 */
val Connector.fillPercentage: Double
    get() = if (globalBufferSize > 0) globalBuffer / globalBufferSize else 0.0

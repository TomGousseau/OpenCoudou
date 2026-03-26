package li.cil.oc.api.machine

import li.cil.oc.api.network.Node

/**
 * Context provides information and capabilities to callbacks.
 * 
 * When a component method is invoked from Lua, the context provides:
 * - Access to the computer's network node
 * - Ability to push signals
 * - Methods to control the machine state
 * - Permission checking for player interactions
 * 
 * @see Machine
 * @see Callback
 */
interface Context {
    /**
     * The node of the computer running this code.
     * Use this to send messages to other nodes in the network.
     */
    fun node(): Node
    
    /**
     * Pushes a signal to this computer's signal queue.
     * Signals are the primary way for components to communicate
     * events back to running programs.
     * 
     * Common signal names:
     * - "component_added": A new component was connected
     * - "component_removed": A component was disconnected
     * - "key_down": A key was pressed on a keyboard
     * - "touch": A screen was clicked
     * - "redstone_changed": Redstone input changed
     * - "modem_message": Network message received
     * 
     * @param name The signal name
     * @param args The signal arguments
     * @return True if the signal was queued successfully
     */
    fun signal(name: String, vararg args: Any?): Boolean
    
    /**
     * Checks if a player is allowed to interact with this computer.
     * This checks the computer's user list and op permissions.
     * 
     * @param player The player's name
     * @return True if the player can interact
     */
    fun canInteract(player: String): Boolean
    
    /**
     * Starts the computer.
     * 
     * @return True if the computer started successfully
     */
    fun start(): Boolean
    
    /**
     * Pauses the computer for the specified duration.
     * 
     * @param duration The pause duration in seconds
     * @return True if the computer was paused
     */
    fun pause(duration: Double): Boolean
    
    /**
     * Stops the computer.
     * 
     * @return True if the computer was stopped
     */
    fun stop(): Boolean
    
    /**
     * Consumes a call from the call budget.
     * Direct calls use the call budget to limit the rate of calls per tick.
     * 
     * @param cost The cost of the call
     * @return True if the call budget had enough remaining
     */
    fun consumeCallBudget(cost: Int): Boolean
}

/**
 * Arguments passed to a callback from Lua.
 * 
 * This provides type-checked access to the arguments passed from Lua code.
 * Methods like checkInteger() will throw an exception if the argument is
 * missing or of the wrong type, providing clear error messages to users.
 */
interface Arguments : Iterable<Any?> {
    /**
     * The number of arguments passed.
     */
    fun count(): Int
    
    /**
     * Checks if an argument at the given index exists.
     */
    fun exists(index: Int): Boolean = index in 0 until count()
    
    // ========================================
    // Required Argument Accessors
    // These throw if the argument is missing or wrong type
    // ========================================
    
    /**
     * Gets a boolean argument.
     * @throws IllegalArgumentException if missing or wrong type
     */
    fun checkBoolean(index: Int): Boolean
    
    /**
     * Gets an integer argument.
     * @throws IllegalArgumentException if missing or wrong type
     */
    fun checkInteger(index: Int): Int
    
    /**
     * Gets a long argument.
     * @throws IllegalArgumentException if missing or wrong type
     */
    fun checkLong(index: Int): Long
    
    /**
     * Gets a double argument.
     * @throws IllegalArgumentException if missing or wrong type
     */
    fun checkDouble(index: Int): Double
    
    /**
     * Gets a string argument.
     * @throws IllegalArgumentException if missing or wrong type
     */
    fun checkString(index: Int): String
    
    /**
     * Gets a byte array argument.
     * @throws IllegalArgumentException if missing or wrong type
     */
    fun checkByteArray(index: Int): ByteArray
    
    /**
     * Gets a table argument as a map.
     * @throws IllegalArgumentException if missing or wrong type
     */
    fun checkTable(index: Int): Map<Any?, Any?>
    
    /**
     * Gets any value at the index.
     * @throws IllegalArgumentException if index is out of bounds
     */
    fun checkAny(index: Int): Any?
    
    // ========================================
    // Optional Argument Accessors
    // These return a default if missing
    // ========================================
    
    /**
     * Gets an optional boolean argument.
     */
    fun optBoolean(index: Int, default: Boolean): Boolean
    
    /**
     * Gets an optional integer argument.
     */
    fun optInteger(index: Int, default: Int): Int
    
    /**
     * Gets an optional long argument.
     */
    fun optLong(index: Int, default: Long): Long
    
    /**
     * Gets an optional double argument.
     */
    fun optDouble(index: Int, default: Double): Double
    
    /**
     * Gets an optional string argument.
     */
    fun optString(index: Int, default: String): String
    
    /**
     * Gets an optional byte array argument.
     */
    fun optByteArray(index: Int, default: ByteArray): ByteArray
    
    /**
     * Gets an optional table argument.
     */
    fun optTable(index: Int, default: Map<Any?, Any?>): Map<Any?, Any?>
    
    /**
     * Gets an optional value at the index.
     */
    fun optAny(index: Int, default: Any?): Any?
    
    // ========================================
    // Type Checking
    // ========================================
    
    /**
     * Checks if the argument at the index is a boolean.
     */
    fun isBoolean(index: Int): Boolean
    
    /**
     * Checks if the argument at the index is a number (integer or double).
     */
    fun isNumber(index: Int): Boolean
    
    /**
     * Checks if the argument at the index is a string.
     */
    fun isString(index: Int): Boolean
    
    /**
     * Checks if the argument at the index is a table.
     */
    fun isTable(index: Int): Boolean
    
    /**
     * Converts this Arguments to an array.
     */
    fun toArray(): Array<Any?>
    
    companion object {
        /**
         * Creates an Arguments instance from an array.
         */
        @JvmStatic
        fun create(args: Array<out Any?>): Arguments = ArgumentsImpl(args)
    }
}

/**
 * Default implementation of Arguments.
 */
internal class ArgumentsImpl(private val args: Array<out Any?>) : Arguments {
    override fun count(): Int = args.size
    
    override fun iterator(): Iterator<Any?> = args.iterator()
    
    override fun checkBoolean(index: Int): Boolean {
        val value = checkAny(index)
        return value as? Boolean 
            ?: throw IllegalArgumentException("bad argument #${index + 1} (boolean expected, got ${typeName(value)})")
    }
    
    override fun checkInteger(index: Int): Int {
        val value = checkAny(index)
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.toInt()
            is Float -> value.toInt()
            else -> throw IllegalArgumentException("bad argument #${index + 1} (number expected, got ${typeName(value)})")
        }
    }
    
    override fun checkLong(index: Int): Long {
        val value = checkAny(index)
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Float -> value.toLong()
            else -> throw IllegalArgumentException("bad argument #${index + 1} (number expected, got ${typeName(value)})")
        }
    }
    
    override fun checkDouble(index: Int): Double {
        val value = checkAny(index)
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            else -> throw IllegalArgumentException("bad argument #${index + 1} (number expected, got ${typeName(value)})")
        }
    }
    
    override fun checkString(index: Int): String {
        val value = checkAny(index)
        return when (value) {
            is String -> value
            is ByteArray -> String(value, Charsets.UTF_8)
            else -> throw IllegalArgumentException("bad argument #${index + 1} (string expected, got ${typeName(value)})")
        }
    }
    
    override fun checkByteArray(index: Int): ByteArray {
        val value = checkAny(index)
        return when (value) {
            is ByteArray -> value
            is String -> value.toByteArray(Charsets.UTF_8)
            else -> throw IllegalArgumentException("bad argument #${index + 1} (string expected, got ${typeName(value)})")
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun checkTable(index: Int): Map<Any?, Any?> {
        val value = checkAny(index)
        return value as? Map<Any?, Any?>
            ?: throw IllegalArgumentException("bad argument #${index + 1} (table expected, got ${typeName(value)})")
    }
    
    override fun checkAny(index: Int): Any? {
        if (index !in 0 until args.size) {
            throw IllegalArgumentException("bad argument #${index + 1} (value expected)")
        }
        return args[index]
    }
    
    override fun optBoolean(index: Int, default: Boolean): Boolean =
        if (exists(index)) checkBoolean(index) else default
    
    override fun optInteger(index: Int, default: Int): Int =
        if (exists(index)) checkInteger(index) else default
    
    override fun optLong(index: Int, default: Long): Long =
        if (exists(index)) checkLong(index) else default
    
    override fun optDouble(index: Int, default: Double): Double =
        if (exists(index)) checkDouble(index) else default
    
    override fun optString(index: Int, default: String): String =
        if (exists(index)) checkString(index) else default
    
    override fun optByteArray(index: Int, default: ByteArray): ByteArray =
        if (exists(index)) checkByteArray(index) else default
    
    override fun optTable(index: Int, default: Map<Any?, Any?>): Map<Any?, Any?> =
        if (exists(index)) checkTable(index) else default
    
    override fun optAny(index: Int, default: Any?): Any? =
        if (exists(index)) args[index] else default
    
    override fun isBoolean(index: Int): Boolean = exists(index) && args[index] is Boolean
    
    override fun isNumber(index: Int): Boolean = exists(index) && args[index] is Number
    
    override fun isString(index: Int): Boolean = 
        exists(index) && (args[index] is String || args[index] is ByteArray)
    
    override fun isTable(index: Int): Boolean = exists(index) && args[index] is Map<*, *>
    
    override fun toArray(): Array<Any?> = args.clone() as Array<Any?>
    
    private fun typeName(value: Any?): String = when (value) {
        null -> "nil"
        is Boolean -> "boolean"
        is Number -> "number"
        is String, is ByteArray -> "string"
        is Map<*, *> -> "table"
        else -> value::class.simpleName ?: "unknown"
    }
}

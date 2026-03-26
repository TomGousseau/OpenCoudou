package li.cil.oc.api.machine

/**
 * Annotation for methods that should be exposed to Lua.
 * 
 * Methods annotated with @Callback will be automatically registered as
 * component methods when the component is created. The method signature
 * must be:
 * 
 * ```kotlin
 * @Callback
 * fun methodName(context: Context, args: Arguments): Array<Any?>
 * ```
 * 
 * Example usage:
 * ```kotlin
 * @Callback(doc = "function():number -- Returns the current value")
 * fun getValue(context: Context, args: Arguments): Array<Any?> {
 *     return arrayOf(currentValue)
 * }
 * 
 * @Callback(value = "setValue", direct = true, limit = 10)
 * fun setValueInternal(context: Context, args: Arguments): Array<Any?> {
 *     currentValue = args.checkDouble(0)
 *     return arrayOf()
 * }
 * ```
 * 
 * @see Context
 * @see Arguments
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Callback(
    /**
     * The name of the method as it appears in Lua.
     * If empty, uses the actual method name.
     */
    val value: String = "",
    
    /**
     * Whether this method runs directly on the worker thread.
     * 
     * Direct methods are faster but must be thread-safe - they cannot
     * interact with the world or modify shared state.
     * 
     * Indirect (non-direct) methods yield the Lua thread and continue
     * on the next server tick, allowing safe world interaction.
     */
    val direct: Boolean = true,
    
    /**
     * The call budget cost for this method.
     * 
     * Each computer has a limited call budget per tick. Complex or
     * expensive direct methods should have a higher limit to prevent
     * abuse. The default (MAX_VALUE) means no limit.
     */
    val limit: Int = Int.MAX_VALUE,
    
    /**
     * Documentation string for this method.
     * 
     * Format: "function(arg1:type, arg2:type):returnType -- Description"
     * 
     * This is shown in-game when calling component.doc() and in the
     * manual for documented components.
     */
    val doc: String = ""
)

/**
 * Helper class for building return values from callbacks.
 */
object CallbackResult {
    /**
     * Returns a successful result with no values.
     */
    fun success(): Array<Any?> = emptyArray()
    
    /**
     * Returns a successful result with a single value.
     */
    fun success(value: Any?): Array<Any?> = arrayOf(value)
    
    /**
     * Returns a successful result with multiple values.
     */
    fun success(vararg values: Any?): Array<Any?> = arrayOf(*values)
    
    /**
     * Returns an error result (nil, message).
     * This is the Lua convention for indicating errors.
     */
    fun error(message: String): Array<Any?> = arrayOf(null, message)
    
    /**
     * Returns a boolean result.
     */
    fun bool(value: Boolean): Array<Any?> = arrayOf(value)
    
    /**
     * Returns a table (map) result.
     */
    fun table(value: Map<*, *>): Array<Any?> = arrayOf(value)
    
    /**
     * Returns a list as a table.
     */
    fun list(value: List<*>): Array<Any?> {
        val table = value.mapIndexed { index, item -> 
            (index + 1) to item  // Lua tables are 1-indexed
        }.toMap()
        return arrayOf(table)
    }
}

/**
 * Signals that can be pushed by components.
 */
object Signals {
    // Component signals
    const val COMPONENT_ADDED = "component_added"
    const val COMPONENT_REMOVED = "component_removed"
    const val COMPONENT_AVAILABLE = "component_available"
    const val COMPONENT_UNAVAILABLE = "component_unavailable"
    
    // Input signals
    const val KEY_DOWN = "key_down"
    const val KEY_UP = "key_up"
    const val CLIPBOARD = "clipboard"
    const val TOUCH = "touch"
    const val DRAG = "drag"
    const val DROP = "drop"
    const val SCROLL = "scroll"
    const val WALK = "walk"
    
    // Network signals
    const val MODEM_MESSAGE = "modem_message"
    const val HTTP_RESPONSE = "http_response"
    
    // Redstone signals
    const val REDSTONE_CHANGED = "redstone_changed"
    
    // Robot/Drone signals
    const val MOVE_DONE = "move_done"
    const val INVENTORY_CHANGED = "inventory_changed"
    const val TANK_CHANGED = "tank_changed"
    
    // System signals
    const val TERM_AVAILABLE = "term_available"
    const val TERM_UNAVAILABLE = "term_unavailable"
}

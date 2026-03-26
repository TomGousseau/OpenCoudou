package li.cil.oc.api.network

import li.cil.oc.api.machine.Context

/**
 * A component is a node that exposes methods to computers.
 * 
 * Components are the primary way for computers to interact with the world.
 * Each component has a type name (like "gpu", "screen", "modem") and exposes
 * a set of methods that can be called from Lua.
 * 
 * The visibility setting controls which computers can see this component:
 * - NONE: Not visible to any computers
 * - NETWORK: Visible to all computers in the network
 * - NEIGHBORS: Visible only to directly adjacent computers
 * - OTHERS: Visible to all computers except the one hosting this component
 * 
 * @see Node
 * @see Visibility
 */
interface Component : Node {
    /**
     * The type name of this component.
     * This is how computers identify the component type (e.g., "gpu", "modem").
     */
    val name: String
    
    /**
     * The visibility setting for this component.
     */
    val visibility: Visibility
    
    /**
     * All methods exposed by this component.
     * Keys are method names, values are the method implementations.
     */
    val methods: Map<String, ComponentMethod>
    
    /**
     * Checks if this component can be seen from the given node.
     * 
     * @param other The node to check visibility from
     * @return True if this component is visible from the other node
     */
    fun canBeSeenFrom(other: Node): Boolean
    
    /**
     * Invokes a method on this component.
     * 
     * @param method The name of the method to invoke
     * @param context The machine context for the call
     * @param args The arguments to pass to the method
     * @return The return values from the method
     * @throws NoSuchMethodException If the method doesn't exist
     */
    fun invoke(method: String, context: Context, vararg args: Any?): Array<Any?>
}

/**
 * Visibility levels for components.
 */
enum class Visibility {
    /** Not visible to any computers */
    NONE,
    
    /** Visible to all computers in the network */
    NETWORK,
    
    /** Visible only to directly adjacent computers */
    NEIGHBORS,
    
    /** Visible to all computers except the one hosting this component */
    OTHERS
}

/**
 * Represents a method that can be called on a component.
 */
interface ComponentMethod {
    /**
     * The name of this method as it appears in Lua.
     */
    val name: String
    
    /**
     * Whether this method is direct (can run on the worker thread).
     * Direct methods are faster but must be thread-safe.
     */
    val isDirect: Boolean
    
    /**
     * The call budget cost for this method.
     * Higher costs mean fewer calls per tick.
     */
    val limit: Int
    
    /**
     * Documentation string for this method.
     */
    val doc: String
    
    /**
     * Invokes this method.
     * 
     * @param context The machine context
     * @param args The arguments from Lua
     * @return The return values to pass back to Lua
     */
    fun invoke(context: Context, args: Array<out Any?>): Array<Any?>
}

/**
 * Implementation of ComponentMethod using reflection.
 */
internal class ReflectionComponentMethod(
    private val target: Any,
    private val method: java.lang.reflect.Method,
    private val callback: li.cil.oc.api.machine.Callback
) : ComponentMethod {
    
    override val name: String = callback.value.ifEmpty { method.name }
    override val isDirect: Boolean = callback.direct
    override val limit: Int = callback.limit
    override val doc: String = callback.doc
    
    override fun invoke(context: Context, args: Array<out Any?>): Array<Any?> {
        val arguments = li.cil.oc.api.machine.Arguments.create(args)
        val result = method.invoke(target, context, arguments)
        return when (result) {
            null -> emptyArray()
            is Array<*> -> result.map { it }.toTypedArray()
            else -> arrayOf(result)
        }
    }
}

/**
 * Simple lambda-based component method.
 */
class LambdaComponentMethod(
    override val name: String,
    override val isDirect: Boolean = true,
    override val limit: Int = Int.MAX_VALUE,
    override val doc: String = "",
    private val handler: (Context, Array<out Any?>) -> Array<Any?>
) : ComponentMethod {
    
    override fun invoke(context: Context, args: Array<out Any?>): Array<Any?> {
        return handler(context, args)
    }
}

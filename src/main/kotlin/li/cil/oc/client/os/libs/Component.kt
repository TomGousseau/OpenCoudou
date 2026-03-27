package li.cil.oc.client.os.libs

/**
 * Component library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Component.lua.
 * Provides access to OC components with auto-proxy and type checking.
 */
object Component {
    
    /**
     * Component proxy interface.
     */
    interface ComponentProxy {
        val address: String
        val type: String
        fun invoke(method: String, vararg args: Any?): Any?
        fun methods(): List<String>
        fun doc(method: String): String?
    }
    
    /**
     * Component information.
     */
    data class ComponentInfo(
        val address: String,
        val type: String,
        val slot: Int = -1
    )
    
    // Registered components (simulated for client-side)
    private val components = mutableMapOf<String, ComponentInfo>()
    private val proxies = mutableMapOf<String, ComponentProxy>()
    
    // Primary components by type
    private val primaryComponents = mutableMapOf<String, String>()
    
    /**
     * List all components of a given type.
     * Returns iterator of (address, type) pairs.
     */
    fun list(type: String? = null, exact: Boolean = false): Sequence<Pair<String, String>> {
        return components.values.asSequence()
            .filter { comp ->
                when {
                    type == null -> true
                    exact -> comp.type == type
                    else -> comp.type.contains(type, ignoreCase = true)
                }
            }
            .map { it.address to it.type }
    }
    
    /**
     * Get the first available component of a type.
     */
    fun get(type: String, exact: Boolean = false): ComponentProxy? {
        // Check primary first
        primaryComponents[type]?.let { addr ->
            return proxy(addr)
        }
        
        // Find first matching
        val address = list(type, exact).firstOrNull()?.first ?: return null
        return proxy(address)
    }
    
    /**
     * Get component proxy by address.
     */
    fun proxy(address: String): ComponentProxy? {
        return proxies[address]
    }
    
    /**
     * Check if a component type is available.
     */
    fun isAvailable(type: String): Boolean {
        return list(type).any()
    }
    
    /**
     * Get component type by address.
     */
    fun type(address: String): String? {
        return components[address]?.type
    }
    
    /**
     * Get component slot by address.
     */
    fun slot(address: String): Int {
        return components[address]?.slot ?: -1
    }
    
    /**
     * Get all methods of a component.
     */
    fun methods(address: String): List<String>? {
        return proxies[address]?.methods()
    }
    
    /**
     * Get documentation for a method.
     */
    fun doc(address: String, method: String): String? {
        return proxies[address]?.doc(method)
    }
    
    /**
     * Invoke a method on a component.
     */
    fun invoke(address: String, method: String, vararg args: Any?): Any? {
        return proxies[address]?.invoke(method, *args)
    }
    
    /**
     * Get primary component of a type.
     */
    fun primary(type: String): ComponentProxy? {
        val address = primaryComponents[type] ?: list(type).firstOrNull()?.first ?: return null
        return proxy(address)
    }
    
    /**
     * Set primary component for a type.
     */
    fun setPrimary(type: String, address: String?) {
        if (address == null) {
            primaryComponents.remove(type)
        } else {
            primaryComponents[type] = address
        }
    }
    
    // ==================== Registration (for simulation) ====================
    
    /**
     * Register a component (called by computer system).
     */
    fun register(address: String, type: String, slot: Int = -1, proxy: ComponentProxy) {
        components[address] = ComponentInfo(address, type, slot)
        proxies[address] = proxy
    }
    
    /**
     * Unregister a component.
     */
    fun unregister(address: String) {
        val info = components.remove(address)
        proxies.remove(address)
        
        // Remove from primary if it was primary
        info?.let { 
            if (primaryComponents[it.type] == address) {
                primaryComponents.remove(it.type)
            }
        }
    }
    
    /**
     * Clear all components.
     */
    fun clear() {
        components.clear()
        proxies.clear()
        primaryComponents.clear()
    }
    
    // ==================== Common Component Types ====================
    
    object Types {
        const val GPU = "gpu"
        const val SCREEN = "screen"
        const val KEYBOARD = "keyboard"
        const val FILESYSTEM = "filesystem"
        const val EEPROM = "eeprom"
        const val COMPUTER = "computer"
        const val MODEM = "modem"
        const val INTERNET = "internet"
        const val REDSTONE = "redstone"
        const val ROBOT = "robot"
        const val DRONE = "drone"
        const val DATA = "data"
        const val CRAFTING = "crafting"
        const val INVENTORY_CONTROLLER = "inventory_controller"
        const val GEOLYZER = "geolyzer"
        const val HOLOGRAM = "hologram"
        const val NAVIGATION = "navigation"
        const val SIGN = "sign"
        const val TANK_CONTROLLER = "tank_controller"
        const val TRANSPOSER = "transposer"
        const val TUNNEL = "tunnel"
        const val WORLD_SENSOR = "world_sensor"
        const val DEBUG = "debug"
    }
    
    // ==================== Shortcut Properties ====================
    
    /** Get GPU component */
    val gpu: ComponentProxy? get() = get(Types.GPU)
    
    /** Get Screen component */
    val screen: ComponentProxy? get() = get(Types.SCREEN)
    
    /** Get Keyboard component */
    val keyboard: ComponentProxy? get() = get(Types.KEYBOARD)
    
    /** Get Filesystem component */
    val filesystem: ComponentProxy? get() = get(Types.FILESYSTEM)
    
    /** Get EEPROM component */
    val eeprom: ComponentProxy? get() = get(Types.EEPROM)
    
    /** Get Modem component */
    val modem: ComponentProxy? get() = get(Types.MODEM)
    
    /** Get Internet component */
    val internet: ComponentProxy? get() = get(Types.INTERNET)
    
    /** Get Redstone component */
    val redstone: ComponentProxy? get() = get(Types.REDSTONE)
}

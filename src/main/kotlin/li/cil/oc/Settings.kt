package li.cil.oc

import net.minecraft.resources.ResourceLocation

/**
 * Configuration settings for OpenComputers.
 * These can be modified via config files or programmatically.
 */
object Settings {
    // ========================================
    // Computer Settings
    // ========================================
    
    /** Base timeout in seconds before a computer is considered unresponsive */
    var timeout: Double = 5.0
    
    /** Maximum call budget per tick for direct (non-yielding) calls */
    var maxCallBudget: Int = 256
    
    /** Size of the signal queue for each computer */
    var signalQueueSize: Int = 256
    
    /** Maximum number of threads in the machine executor pool */
    var executorThreads: Int = 4
    
    /** Whether to allow native Lua libraries (requires native binaries) */
    var allowNativeLua: Boolean = true
    
    /** Default Lua version to use (52, 53, or 54) */
    var luaVersion: Int = 54
    
    // ========================================
    // Component Settings
    // ========================================
    
    /** Maximum number of components that can connect to a single computer */
    object ComponentLimits {
        var tier1: Int = 8
        var tier2: Int = 12
        var tier3: Int = 16
        var creative: Int = 1024
        
        fun forTier(tier: Int): Int = when (tier) {
            0 -> tier1
            1 -> tier2
            2 -> tier3
            else -> creative
        }
    }
    
    /** Maximum network packet sizes */
    object PacketSizes {
        var tier1: Int = 8
        var tier2: Int = 16
        var tier3: Int = 32
        var creative: Int = 8192
        
        fun forTier(tier: Int): Int = when (tier) {
            0 -> tier1
            1 -> tier2
            2 -> tier3
            else -> creative
        }
    }
    
    // ========================================
    // Memory Settings
    // ========================================
    
    /** RAM sizes in bytes for each tier */
    object MemorySizes {
        var tier1: Int = 192 * 1024      // 192 KB
        var tier2: Int = 384 * 1024      // 384 KB
        var tier3: Int = 512 * 1024      // 512 KB
        var tier3_5: Int = 768 * 1024    // 768 KB
        var tier4: Int = 1024 * 1024     // 1 MB
        var tier5: Int = 2 * 1024 * 1024 // 2 MB
        var tier6: Int = 4 * 1024 * 1024 // 4 MB
        
        fun forTier(tier: Int): Int = when (tier) {
            0 -> tier1
            1 -> tier2
            2 -> tier3
            3 -> tier3_5
            4 -> tier4
            5 -> tier5
            else -> tier6
        }
    }
    
    // ========================================
    // Graphics Settings
    // ========================================
    
    /** Screen resolutions for each tier */
    object ScreenResolutions {
        var tier1: Pair<Int, Int> = 50 to 16
        var tier2: Pair<Int, Int> = 80 to 25
        var tier3: Pair<Int, Int> = 160 to 50
        
        fun forTier(tier: Int): Pair<Int, Int> = when (tier) {
            0 -> tier1
            1 -> tier2
            else -> tier3
        }
    }
    
    /** Color depths for each tier */
    object ColorDepths {
        var tier1: Int = 1   // Monochrome
        var tier2: Int = 4   // 16 colors
        var tier3: Int = 8   // 256 colors
        
        fun forTier(tier: Int): Int = when (tier) {
            0 -> tier1
            1 -> tier2
            else -> tier3
        }
    }
    
    // ========================================
    // Power Settings
    // ========================================
    
    /** Power cost multiplier for various operations */
    object Power {
        /** Whether power is enabled at all */
        var enabled: Boolean = true
        
        /** Base power buffer size in RF */
        var bufferSize: Double = 10000.0
        
        /** Power cost per tick while a computer is running */
        var computerCost: Double = 0.5
        
        /** Power cost per screen character lit */
        var screenCost: Double = 0.01
        
        /** Power cost per wireless message sent */
        var wirelessCost: Double = 0.05
        
        /** Power cost per HTTP request */
        var httpCost: Double = 1.0
        
        /** Power cost for robot movement per block */
        var robotMoveCost: Double = 15.0
        
        /** Power cost for robot tool use */
        var robotUseCost: Double = 2.5
        
        /** Conversion rate from Forge Energy to OC power */
        var feConversionRate: Double = 1.0
    }
    
    // ========================================
    // Robot Settings
    // ========================================
    
    object Robot {
        /** Whether robots can break blocks */
        var canBreakBlocks: Boolean = true
        
        /** Whether robots can place blocks */
        var canPlaceBlocks: Boolean = true
        
        /** Whether robots can attack entities */
        var canAttackEntities: Boolean = true
        
        /** Whether robots can use items */
        var canUseItems: Boolean = true
        
        /** Delay in ticks between robot movements */
        var moveDelay: Int = 10
        
        /** Delay in ticks between robot tool uses */
        var useDelay: Int = 5
        
        /** Delay in ticks between robot turns */
        var turnDelay: Int = 5
        
        /** Maximum experience level a robot can accumulate */
        var maxExperience: Double = 30.0
        
        /** Experience boost from upgrades */
        var experienceBoost: Double = 1.0
    }
    
    // ========================================
    // Drone Settings
    // ========================================
    
    object Drone {
        /** Maximum speed for drones in blocks/tick */
        var maxSpeed: Double = 0.4
        
        /** Acceleration rate for drones */
        var acceleration: Double = 0.05
        
        /** Maximum altitude for drones (relative to world height) */
        var maxAltitude: Int = 320
        
        /** Power cost per tick while flying */
        var flightCost: Double = 0.25
    }
    
    // ========================================
    // Network Settings
    // ========================================
    
    object Network {
        /** Maximum distance for wireless communication per tier */
        var wirelessRange: Map<Int, Int> = mapOf(
            0 to 16,
            1 to 64,
            2 to 400
        )
        
        /** Maximum signal strength for wireless */
        var maxWirelessStrength: Int = 400
        
        /** Whether to enable HTTP access */
        var httpEnabled: Boolean = true
        
        /** Whether to enable TCP access */
        var tcpEnabled: Boolean = true
        
        /** Blacklist for HTTP/TCP hosts (regex patterns) */
        var httpBlacklist: List<String> = listOf(
            "^10\\.",
            "^127\\.",
            "^172\\.(1[6-9]|2[0-9]|3[01])\\.",
            "^192\\.168\\.",
            "^0\\.0\\.0\\.0$",
            "^localhost$"
        )
        
        /** Whitelist for HTTP/TCP hosts (takes precedence over blacklist) */
        var httpWhitelist: List<String> = emptyList()
        
        /** Maximum concurrent HTTP requests per computer */
        var maxHttpRequests: Int = 4
        
        /** Maximum concurrent TCP connections per computer */
        var maxTcpConnections: Int = 4
    }
    
    // ========================================
    // File System Settings
    // ========================================
    
    object FileSystem {
        /** Size of tier 1 floppy disks in KB */
        var floppySize: Int = 512
        
        /** Sizes of hard drives per tier in KB */
        var hddSizes: Map<Int, Int> = mapOf(
            0 to 1024,      // 1 MB
            1 to 2048,      // 2 MB
            2 to 4096       // 4 MB
        )
        
        /** Maximum number of open file handles per filesystem */
        var maxHandles: Int = 16
        
        /** Maximum file read/write chunk size */
        var chunkSize: Int = 8192
        
        /** Buffer size for file operations */
        var bufferSize: Int = 4096
    }
    
    // ========================================
    // Miscellaneous Settings
    // ========================================
    
    object Misc {
        /** Whether to enable the in-game manual */
        var enableManual: Boolean = true
        
        /** Whether to play typing sounds */
        var enableTypingSounds: Boolean = true
        
        /** Whether to show power usage in tooltips */
        var showPowerInTooltips: Boolean = true
        
        /** Maximum length for computer labels */
        var maxLabelLength: Int = 24
        
        /** Maximum number of tablets per player */
        var maxTabletsPerPlayer: Int = 4
    }
    
    // ========================================
    // Internet Card Settings (top-level aliases for convenience)
    // ========================================

    /** Whether the internet card is allowed to make connections */
    var internetEnabled: Boolean
        get() = Network.httpEnabled
        set(v) { Network.httpEnabled = v }

    /** Timeout in milliseconds for internet connections */
    var internetTimeout: Int = 10_000

    /**
     * Whitelist of allowed hostnames.
     * Empty list means all non-blacklisted hosts are allowed.
     */
    var internetWhitelist: List<String>
        get() = Network.httpWhitelist
        set(v) { Network.httpWhitelist = v }

    // ========================================
    // Utility Functions
    // ========================================
    
    /**
     * Creates a ResourceLocation with the OpenComputers namespace
     */
    fun resource(path: String): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, path)
}

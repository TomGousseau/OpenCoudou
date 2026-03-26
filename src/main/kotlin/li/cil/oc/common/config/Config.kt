package li.cil.oc.common.config

import net.neoforged.neoforge.common.ModConfigSpec
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.config.ModConfig as NeoModConfig
import org.apache.logging.log4j.LogManager

/**
 * OpenComputers configuration system.
 * Uses NeoForge's modern config spec system with TOML files.
 */
object Config {
    private val LOGGER = LogManager.getLogger("OpenComputers")
    
    private val BUILDER = ModConfigSpec.Builder()
    
    // ========================================
    // Computer Settings
    // ========================================
    
    val maxComputersPerDimension: ModConfigSpec.IntValue
    val executionTimeout: ModConfigSpec.IntValue
    val enablePersistence: ModConfigSpec.BooleanValue
    val crashOnTimeout: ModConfigSpec.BooleanValue
    
    // ========================================
    // Power Settings
    // ========================================
    
    val powerEnabled: ModConfigSpec.BooleanValue
    val feRatio: ModConfigSpec.DoubleValue
    val computerCostPerTick: ModConfigSpec.DoubleValue
    val robotMoveCost: ModConfigSpec.DoubleValue
    val screenCostPerCharacter: ModConfigSpec.DoubleValue
    val hologramCostPerVoxel: ModConfigSpec.DoubleValue
    
    // ========================================
    // Filesystem Settings
    // ========================================
    
    val maxFileHandles: ModConfigSpec.IntValue
    val maxFileSize: ModConfigSpec.IntValue
    val hddTier1Size: ModConfigSpec.IntValue
    val hddTier2Size: ModConfigSpec.IntValue
    val hddTier3Size: ModConfigSpec.IntValue
    val floppySize: ModConfigSpec.IntValue
    val enableTmpfs: ModConfigSpec.BooleanValue
    val tmpfsSize: ModConfigSpec.IntValue
    
    // ========================================
    // Network Settings
    // ========================================
    
    val maxWiredDistance: ModConfigSpec.IntValue
    val maxWirelessRange: ModConfigSpec.IntValue
    val enableHttp: ModConfigSpec.BooleanValue
    val enableTcp: ModConfigSpec.BooleanValue
    val httpTimeout: ModConfigSpec.IntValue
    val maxHttpConnections: ModConfigSpec.IntValue
    
    // ========================================
    // Robot Settings
    // ========================================
    
    val robotCanBreakBlocks: ModConfigSpec.BooleanValue
    val robotCanPlaceBlocks: ModConfigSpec.BooleanValue
    val robotCanInteractWithEntities: ModConfigSpec.BooleanValue
    val robotMaxItemsPerOperation: ModConfigSpec.IntValue
    val robotMoveDelay: ModConfigSpec.IntValue
    val robotTurnDelay: ModConfigSpec.IntValue
    val robotSwingDelay: ModConfigSpec.IntValue
    val xpToLevelRatio: ModConfigSpec.IntValue
    
    // ========================================
    // Drone Settings
    // ========================================
    
    val droneMaxFlightHeight: ModConfigSpec.IntValue
    val droneMoveSpeed: ModConfigSpec.DoubleValue
    val droneNameTagRange: ModConfigSpec.IntValue
    
    // ========================================
    // Screen Settings
    // ========================================
    
    val tier1Width: ModConfigSpec.IntValue
    val tier1Height: ModConfigSpec.IntValue
    val tier2Width: ModConfigSpec.IntValue
    val tier2Height: ModConfigSpec.IntValue
    val tier3Width: ModConfigSpec.IntValue
    val tier3Height: ModConfigSpec.IntValue
    val tier1ColorDepth: ModConfigSpec.IntValue
    val tier2ColorDepth: ModConfigSpec.IntValue
    val tier3ColorDepth: ModConfigSpec.IntValue
    val maxScreenWidth: ModConfigSpec.IntValue
    val maxScreenHeight: ModConfigSpec.IntValue
    
    // ========================================
    // Hologram Settings
    // ========================================
    
    val hologramResolutionX: ModConfigSpec.IntValue
    val hologramResolutionY: ModConfigSpec.IntValue
    val hologramResolutionZ: ModConfigSpec.IntValue
    val hologramRenderDistance: ModConfigSpec.IntValue
    val hologramTier1Colors: ModConfigSpec.IntValue
    val hologramTier2Colors: ModConfigSpec.IntValue
    
    // ========================================
    // GPU Settings
    // ========================================
    
    val gpuTier1Budget: ModConfigSpec.IntValue
    val gpuTier2Budget: ModConfigSpec.IntValue
    val gpuTier3Budget: ModConfigSpec.IntValue
    
    // ========================================
    // Debug Settings
    // ========================================
    
    val debugCardInSurvival: ModConfigSpec.BooleanValue
    val verboseLogging: ModConfigSpec.BooleanValue
    val logComponentAccess: ModConfigSpec.BooleanValue
    
    // ========================================
    // Misc Settings
    // ========================================
    
    val enableLootDisks: ModConfigSpec.BooleanValue
    val lootDiskChance: ModConfigSpec.DoubleValue
    val enableNanomachines: ModConfigSpec.BooleanValue
    val geolyzerRange: ModConfigSpec.IntValue
    val waypointRange: ModConfigSpec.IntValue
    
    val SPEC: ModConfigSpec
    
    init {
        // Computer
        BUILDER.push("computer")
        maxComputersPerDimension = BUILDER
            .comment("Maximum number of active computers per dimension")
            .defineInRange("maxComputersPerDimension", 1000, 1, 10000)
        executionTimeout = BUILDER
            .comment("Base timeout for computer execution (milliseconds)")
            .defineInRange("executionTimeout", 5000, 1000, 60000)
        enablePersistence = BUILDER
            .comment("Enable persistence of computer state on chunk unload")
            .define("enablePersistence", true)
        crashOnTimeout = BUILDER
            .comment("Enable computer crash when execution takes too long")
            .define("crashOnTimeout", true)
        BUILDER.pop()
        
        // Power
        BUILDER.push("power")
        powerEnabled = BUILDER
            .comment("Enable power consumption")
            .define("enabled", true)
        feRatio = BUILDER
            .comment("FE/RF per OC power unit")
            .defineInRange("feRatio", 100.0, 1.0, 10000.0)
        computerCostPerTick = BUILDER
            .comment("Power cost per tick for running computer")
            .defineInRange("computerCostPerTick", 0.5, 0.0, 100.0)
        robotMoveCost = BUILDER
            .comment("Power cost per block for robot movement")
            .defineInRange("robotMoveCost", 15.0, 0.0, 1000.0)
        screenCostPerCharacter = BUILDER
            .comment("Power cost per character for screen update")
            .defineInRange("screenCostPerCharacter", 0.01, 0.0, 1.0)
        hologramCostPerVoxel = BUILDER
            .comment("Power cost per voxel for hologram update")
            .defineInRange("hologramCostPerVoxel", 0.0001, 0.0, 0.1)
        BUILDER.pop()
        
        // Filesystem
        BUILDER.push("filesystem")
        maxFileHandles = BUILDER
            .comment("Maximum number of file handles per filesystem")
            .defineInRange("maxHandles", 256, 16, 1024)
        maxFileSize = BUILDER
            .comment("Maximum file size in bytes")
            .defineInRange("maxFileSize", 4194304, 65536, 134217728)
        hddTier1Size = BUILDER
            .comment("Size of tier 1 HDD in KB")
            .defineInRange("hddTier1Size", 1024, 128, 65536)
        hddTier2Size = BUILDER
            .comment("Size of tier 2 HDD in KB")
            .defineInRange("hddTier2Size", 2048, 256, 131072)
        hddTier3Size = BUILDER
            .comment("Size of tier 3 HDD in KB")
            .defineInRange("hddTier3Size", 4096, 512, 262144)
        floppySize = BUILDER
            .comment("Size of floppy disk in KB")
            .defineInRange("floppySize", 512, 64, 4096)
        enableTmpfs = BUILDER
            .comment("Enable temporary filesystem access")
            .define("enableTmpfs", true)
        tmpfsSize = BUILDER
            .comment("Temporary filesystem size in KB")
            .defineInRange("tmpfsSize", 64, 16, 512)
        BUILDER.pop()
        
        // Network
        BUILDER.push("network")
        maxWiredDistance = BUILDER
            .comment("Maximum distance for wired network connections (blocks)")
            .defineInRange("maxWiredDistance", 16, 1, 64)
        maxWirelessRange = BUILDER
            .comment("Maximum distance for wireless network messages (blocks)")
            .defineInRange("maxWirelessRange", 400, 16, 10000)
        enableHttp = BUILDER
            .comment("Enable HTTP access from computers")
            .define("enableHttp", true)
        enableTcp = BUILDER
            .comment("Enable TCP/UDP connections")
            .define("enableTcp", true)
        httpTimeout = BUILDER
            .comment("HTTP request timeout (milliseconds)")
            .defineInRange("httpTimeout", 10000, 1000, 60000)
        maxHttpConnections = BUILDER
            .comment("Maximum concurrent HTTP requests per computer")
            .defineInRange("maxHttpConnections", 4, 1, 16)
        BUILDER.pop()
        
        // Robot
        BUILDER.push("robot")
        robotCanBreakBlocks = BUILDER
            .comment("Enable robot block breaking")
            .define("canBreakBlocks", true)
        robotCanPlaceBlocks = BUILDER
            .comment("Enable robot block placement")
            .define("canPlaceBlocks", true)
        robotCanInteractWithEntities = BUILDER
            .comment("Enable robot entity interaction")
            .define("canInteractWithEntities", true)
        robotMaxItemsPerOperation = BUILDER
            .comment("Maximum items robot can pick up per operation")
            .defineInRange("maxItemsPerOperation", 64, 1, 64)
        robotMoveDelay = BUILDER
            .comment("Robot movement delay (ticks)")
            .defineInRange("moveDelay", 10, 1, 100)
        robotTurnDelay = BUILDER
            .comment("Robot turn delay (ticks)")
            .defineInRange("turnDelay", 5, 1, 50)
        robotSwingDelay = BUILDER
            .comment("Robot swing delay (ticks)")
            .defineInRange("swingDelay", 10, 1, 100)
        xpToLevelRatio = BUILDER
            .comment("Experience upgrade: XP to level ratio")
            .defineInRange("xpToLevelRatio", 30, 1, 1000)
        BUILDER.pop()
        
        // Drone
        BUILDER.push("drone")
        droneMaxFlightHeight = BUILDER
            .comment("Maximum flight height above ground")
            .defineInRange("maxFlightHeight", 256, 8, 512)
        droneMoveSpeed = BUILDER
            .comment("Movement speed (blocks per tick)")
            .defineInRange("moveSpeed", 0.45, 0.1, 2.0)
        droneNameTagRange = BUILDER
            .comment("Name tag visible range (blocks)")
            .defineInRange("nameTagRange", 32, 8, 128)
        BUILDER.pop()
        
        // Screen
        BUILDER.push("screen")
        tier1Width = BUILDER
            .comment("Tier 1 screen width")
            .defineInRange("tier1Width", 50, 20, 160)
        tier1Height = BUILDER
            .comment("Tier 1 screen height")
            .defineInRange("tier1Height", 16, 6, 50)
        tier2Width = BUILDER
            .comment("Tier 2 screen width")
            .defineInRange("tier2Width", 80, 40, 160)
        tier2Height = BUILDER
            .comment("Tier 2 screen height")
            .defineInRange("tier2Height", 25, 12, 50)
        tier3Width = BUILDER
            .comment("Tier 3 screen width")
            .defineInRange("tier3Width", 160, 80, 320)
        tier3Height = BUILDER
            .comment("Tier 3 screen height")
            .defineInRange("tier3Height", 50, 25, 100)
        tier1ColorDepth = BUILDER
            .comment("Tier 1 color depth (1=2 colors, 4=16 colors, 8=256 colors)")
            .defineInRange("tier1ColorDepth", 1, 1, 8)
        tier2ColorDepth = BUILDER
            .comment("Tier 2 color depth")
            .defineInRange("tier2ColorDepth", 4, 1, 8)
        tier3ColorDepth = BUILDER
            .comment("Tier 3 color depth")
            .defineInRange("tier3ColorDepth", 8, 1, 8)
        maxScreenWidth = BUILDER
            .comment("Maximum screen multi-block width")
            .defineInRange("maxScreenWidth", 8, 1, 16)
        maxScreenHeight = BUILDER
            .comment("Maximum screen multi-block height")
            .defineInRange("maxScreenHeight", 6, 1, 12)
        BUILDER.pop()
        
        // Hologram
        BUILDER.push("hologram")
        hologramResolutionX = BUILDER
            .comment("Hologram X resolution")
            .defineInRange("resolutionX", 48, 16, 128)
        hologramResolutionY = BUILDER
            .comment("Hologram Y resolution")
            .defineInRange("resolutionY", 32, 16, 128)
        hologramResolutionZ = BUILDER
            .comment("Hologram Z resolution")
            .defineInRange("resolutionZ", 48, 16, 128)
        hologramRenderDistance = BUILDER
            .comment("Maximum render distance (blocks)")
            .defineInRange("renderDistance", 64, 16, 256)
        hologramTier1Colors = BUILDER
            .comment("Tier 1 hologram color count")
            .defineInRange("tier1Colors", 2, 1, 16)
        hologramTier2Colors = BUILDER
            .comment("Tier 2 hologram color count")
            .defineInRange("tier2Colors", 3, 1, 16)
        BUILDER.pop()
        
        // GPU
        BUILDER.push("gpu")
        gpuTier1Budget = BUILDER
            .comment("Tier 1 GPU operations budget per tick")
            .defineInRange("tier1Budget", 128, 32, 1024)
        gpuTier2Budget = BUILDER
            .comment("Tier 2 GPU operations budget per tick")
            .defineInRange("tier2Budget", 512, 128, 4096)
        gpuTier3Budget = BUILDER
            .comment("Tier 3 GPU operations budget per tick")
            .defineInRange("tier3Budget", 2048, 512, 16384)
        BUILDER.pop()
        
        // Debug
        BUILDER.push("debug")
        debugCardInSurvival = BUILDER
            .comment("Enable debug card in survival mode (normally creative only)")
            .define("debugCardInSurvival", false)
        verboseLogging = BUILDER
            .comment("Enable verbose logging")
            .define("verboseLogging", false)
        logComponentAccess = BUILDER
            .comment("Enable component access logging")
            .define("logComponentAccess", false)
        BUILDER.pop()
        
        // Misc
        BUILDER.push("misc")
        enableLootDisks = BUILDER
            .comment("Enable loot disks in dungeon chests")
            .define("enableLootDisks", true)
        lootDiskChance = BUILDER
            .comment("Loot disk spawn chance (0.0-1.0)")
            .defineInRange("lootDiskChance", 0.2, 0.0, 1.0)
        enableNanomachines = BUILDER
            .comment("Enable nanomachines")
            .define("enableNanomachines", true)
        geolyzerRange = BUILDER
            .comment("Geolyzer scan range (blocks in each direction)")
            .defineInRange("geolyzerRange", 32, 8, 128)
        waypointRange = BUILDER
            .comment("Waypoint detection range (blocks)")
            .defineInRange("waypointRange", 64, 16, 512)
        BUILDER.pop()
        
        SPEC = BUILDER.build()
    }
    
    fun register(context: ModLoadingContext) {
        context.registerConfig(NeoModConfig.Type.COMMON, SPEC, "opencomputers.toml")
        LOGGER.info("OpenComputers config registered")
    }
    
    // Convenience accessors
    object Computer {
        val maxPerDimension get() = maxComputersPerDimension.get()
        val timeout get() = executionTimeout.get()
        val persistence get() = enablePersistence.get()
        val crashTimeout get() = crashOnTimeout.get()
    }
    
    object Power {
        val enabled get() = powerEnabled.get()
        val fePerUnit get() = feRatio.get()
        val computerCost get() = computerCostPerTick.get()
        val robotMove get() = robotMoveCost.get()
        val screenChar get() = screenCostPerCharacter.get()
        val hologramVoxel get() = hologramCostPerVoxel.get()
    }
    
    object Filesystem {
        val maxHandles get() = maxFileHandles.get()
        val maxSize get() = maxFileSize.get()
        val hdd1 get() = hddTier1Size.get() * 1024L
        val hdd2 get() = hddTier2Size.get() * 1024L
        val hdd3 get() = hddTier3Size.get() * 1024L
        val floppy get() = floppySize.get() * 1024L
        val tmpfs get() = if (enableTmpfs.get()) tmpfsSize.get() * 1024L else 0L
    }
    
    object Network {
        val wiredDistance get() = maxWiredDistance.get()
        val wirelessRange get() = maxWirelessRange.get()
        val http get() = enableHttp.get()
        val tcp get() = enableTcp.get()
        val httpTimeoutMs get() = httpTimeout.get()
        val maxHttp get() = maxHttpConnections.get()
    }
    
    object Robot {
        val canBreak get() = robotCanBreakBlocks.get()
        val canPlace get() = robotCanPlaceBlocks.get()
        val canInteract get() = robotCanInteractWithEntities.get()
        val maxItems get() = robotMaxItemsPerOperation.get()
        val moveDelay get() = robotMoveDelay.get()
        val turnDelay get() = robotTurnDelay.get()
        val swingDelay get() = robotSwingDelay.get()
        val xpRatio get() = xpToLevelRatio.get()
    }
    
    object Drone {
        val maxHeight get() = droneMaxFlightHeight.get()
        val speed get() = droneMoveSpeed.get()
        val nameTagRange get() = droneNameTagRange.get()
    }
    
    object Screen {
        fun width(tier: Int) = when(tier) {
            1 -> tier1Width.get()
            2 -> tier2Width.get()
            else -> tier3Width.get()
        }
        fun height(tier: Int) = when(tier) {
            1 -> tier1Height.get()
            2 -> tier2Height.get()
            else -> tier3Height.get()
        }
        fun colorDepth(tier: Int) = when(tier) {
            1 -> tier1ColorDepth.get()
            2 -> tier2ColorDepth.get()
            else -> tier3ColorDepth.get()
        }
        val maxWidth get() = maxScreenWidth.get()
        val maxHeight get() = maxScreenHeight.get()
    }
    
    object Hologram {
        val resX get() = hologramResolutionX.get()
        val resY get() = hologramResolutionY.get()
        val resZ get() = hologramResolutionZ.get()
        val renderDist get() = hologramRenderDistance.get()
        fun colors(tier: Int) = if (tier <= 1) hologramTier1Colors.get() else hologramTier2Colors.get()
    }
    
    object GPU {
        fun budget(tier: Int) = when(tier) {
            1 -> gpuTier1Budget.get()
            2 -> gpuTier2Budget.get()
            else -> gpuTier3Budget.get()
        }
    }
    
    object Debug {
        val inSurvival get() = debugCardInSurvival.get()
        val verbose get() = verboseLogging.get()
        val logAccess get() = logComponentAccess.get()
    }
    
    object Misc {
        val lootDisks get() = enableLootDisks.get()
        val lootChance get() = lootDiskChance.get()
        val nanomachines get() = enableNanomachines.get()
        val geolyzer get() = geolyzerRange.get()
        val waypoint get() = waypointRange.get()
    }
}

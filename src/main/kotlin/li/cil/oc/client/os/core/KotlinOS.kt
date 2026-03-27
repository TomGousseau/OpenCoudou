package li.cil.oc.client.os.core

import li.cil.oc.client.os.gui.WindowManager
import li.cil.oc.client.os.gui.Desktop
import li.cil.oc.client.os.gui.DesktopShell
import li.cil.oc.client.os.gui.WindowManagerIntegration
import li.cil.oc.client.os.filesystem.VirtualFileSystem
import li.cil.oc.client.os.apps.ApplicationRegistry
import li.cil.oc.client.os.components.NativeComponentBus
import li.cil.oc.client.os.components.HardwareAbstractionLayer
import li.cil.oc.client.os.network.NetworkStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager

/**
 * SkibidiOS2 - High-performance native Kotlin operating system for OpenComputers.
 * 
 * Features:
 * - Instant boot (<16ms)
 * - GPU-accelerated rendering (60 FPS)
 * - Native multitasking via Kotlin coroutines
 * - Zero-copy component access
 * - Modern declarative UI framework
 * - Built-in web browser
 * - Full Lua compatibility layer
 * 
 * @author OpenComputers Rewrite Team
 * @version 2.0.0
 */
class KotlinOS(
    val machineAddress: UUID,
    val screenWidth: Int = 160,
    val screenHeight: Int = 50
) {
    companion object {
        const val VERSION = "2.0.0"
        const val NAME = "SkibidiOS2"
        
        private val LOGGER = LogManager.getLogger("SkibidiOS2")
        private val instances = ConcurrentHashMap<UUID, KotlinOS>()
        
        fun getInstance(machineAddress: UUID): KotlinOS? = instances[machineAddress]
        
        fun createInstance(machineAddress: UUID, width: Int, height: Int): KotlinOS {
            val os = KotlinOS(machineAddress, width, height)
            instances[machineAddress] = os
            return os
        }
        
        fun destroyInstance(machineAddress: UUID) {
            instances.remove(machineAddress)?.shutdown()
        }
    }
    
    // Frame buffer for GPU rendering
    val frameBuffer = FrameBuffer(screenWidth, screenHeight)
    
    // Core subsystems (lazy initialization to avoid circular references)
    val filesystem: VirtualFileSystem by lazy { VirtualFileSystem() }
    val componentBus: NativeComponentBus by lazy { NativeComponentBus() }
    val processManager: ProcessManager by lazy { ProcessManager() }
    val scheduler: ProcessScheduler by lazy { ProcessScheduler() }
    val hal: HardwareAbstractionLayer by lazy { HardwareAbstractionLayer(componentBus) }
    val network: NetworkStack by lazy { NetworkStack() }
    val windowManager: WindowManager by lazy { WindowManager(this) }
    val desktop: Desktop by lazy { Desktop(this) }
    val shell: DesktopShell by lazy { DesktopShell(this) }
    val appRegistry: ApplicationRegistry by lazy { ApplicationRegistry(this) }
    
    // Window manager integration (connects windows, apps, processes)
    val wmIntegration: WindowManagerIntegration by lazy {
        WindowManagerIntegration(this, windowManager, shell, scheduler)
    }
    
    // OS state
    var isRunning = false
        private set
    var bootTime: Long = 0
        private set
    private var bootStartTime: Long = 0
    
    // Coroutine scope for async operations
    private val osScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Boot the operating system.
     * Target: <16ms boot time for instant startup.
     */
    fun boot() {
        val startTime = System.nanoTime()
        bootStartTime = System.currentTimeMillis()
        
        LOGGER.info("$NAME v$VERSION booting...")
        
        // Phase 1: Initialize core systems (parallel)
        runBlocking {
            listOf(
                async { componentBus.initializeDefaults() },
                async { hal.initialize() },
                async { scheduler.start() },
                async { /* network ready */ }
            ).awaitAll()
        }
        
        // Phase 2: Initialize UI (must be sequential)
        windowManager.initialize()
        desktop.initialize()
        shell.initialize()
        wmIntegration.initialize()
        
        // Phase 3: Start system
        isRunning = true
        bootTime = (System.nanoTime() - startTime) / 1_000_000 // Convert to ms
        
        LOGGER.info("$NAME booted in ${bootTime}ms")
        
        // Start main loop
        osScope.launch {
            mainLoop()
        }
    }
    
    /**
     * Main OS loop - runs at 60 FPS.
     */
    private suspend fun mainLoop() {
        val targetFrameTime = 16_666_666L // ~60 FPS in nanoseconds
        
        while (isRunning) {
            val frameStart = System.nanoTime()
            
            // Execute scheduler for process management
            scheduler.tick()
            
            // Update UI
            windowManager.update()
            desktop.update()
            shell.update()
            
            // Render frame
            render()
            
            // Frame timing
            val frameEnd = System.nanoTime()
            val frameTime = frameEnd - frameStart
            if (frameTime < targetFrameTime) {
                delay((targetFrameTime - frameTime) / 1_000_000)
            }
        }
    }
    
    /**
     * Render current frame to the screen.
     */
    fun render() {
        frameBuffer.clear()
        shell.render(frameBuffer)  // Shell renders desktop background, icons, taskbar
        windowManager.render(frameBuffer)  // Windows render on top
    }
    
    /**
     * Shutdown the operating system.
     */
    fun shutdown() {
        LOGGER.info("$NAME shutting down...")
        isRunning = false
        osScope.cancel()
        
        scheduler.shutdown()
        hal.shutdown()
        processManager.killAll()
        windowManager.closeAll()
        network.shutdown()
        
        LOGGER.info("$NAME shutdown complete")
    }
    
    /**
     * Reboot the operating system.
     */
    fun reboot() {
        shutdown()
        boot()
    }
    
    /**
     * Get system information.
     */
    fun getSystemInfo(): SystemInfo = SystemInfo(
        osName = NAME,
        osVersion = VERSION,
        bootTime = bootTime,
        uptime = if (isRunning) System.currentTimeMillis() - bootStartTime else 0,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        processCount = processManager.listProcesses().size,
        schedulerProcessCount = scheduler.getProcessCount(),
        memoryUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
        memoryTotal = Runtime.getRuntime().totalMemory()
    )
    
    /**
     * Handle a click event from the screen.
     */
    fun handleClick(x: Int, y: Int, button: Int) {
        // First try windows
        if (!windowManager.handleClick(x, y, button)) {
            // Then shell (taskbar, notifications, etc.)
            if (!shell.handleClick(x, y, button)) {
                // Then legacy desktop
                desktop.handleClick(x, y, button)
            }
        }
    }
    
    /**
     * Handle a key event from the keyboard.
     */
    fun handleKey(keyCode: Int, char: Char) {
        // First try shell shortcuts
        if (!shell.handleKey(keyCode, char)) {
            windowManager.handleKey(keyCode, char)
        }
    }
    
    /**
     * Handle mouse move event.
     */
    fun handleMouseMove(x: Int, y: Int) {
        shell.handleMouseMove(x, y)
        windowManager.handleMouseMove(x, y)
    }
    
    /**
     * Handle a drag event from the screen.
     */
    fun handleDrag(x: Int, y: Int, button: Int) {
        windowManager.handleDrag(x, y, button)
    }
}

/**
 * System information data class.
 */
data class SystemInfo(
    val osName: String,
    val osVersion: String,
    val bootTime: Long,
    val uptime: Long,
    val screenWidth: Int,
    val screenHeight: Int,
    val processCount: Int,
    val schedulerProcessCount: Int,
    val memoryUsed: Long,
    val memoryTotal: Long
)

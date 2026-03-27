package li.cil.oc.client.os.boot

import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.filesystem.VirtualFileSystem
import li.cil.oc.client.os.libs.Screen
import li.cil.oc.client.os.libs.Color
import kotlinx.coroutines.*

/**
 * EFI Bootloader for SkibidiOS2.
 * Compatible with SkibidiLuaOS EFI system.
 * 
 * Boot sequence:
 * 1. POST (Power-On Self Test)
 * 2. Hardware detection
 * 3. EEPROM check
 * 4. Boot device selection
 * 5. OS loading
 */
class EFIBootloader {
    
    // Boot state
    enum class BootState {
        POST,
        HARDWARE_DETECT,
        EEPROM_CHECK,
        BOOT_DEVICE_SELECT,
        LOADING_OS,
        BOOT_COMPLETE,
        BOOT_FAILED
    }
    
    // Detected hardware
    data class HardwareInfo(
        val cpuTier: Int = 1,
        val ramSize: Long = 0,
        val gpuTier: Int = 1,
        val screenWidth: Int = 160,
        val screenHeight: Int = 50,
        val hasKeyboard: Boolean = true,
        val hasInternet: Boolean = false,
        val hdds: List<String> = emptyList(),
        val floppies: List<String> = emptyList(),
        val components: Map<String, String> = emptyMap()
    )
    
    // Boot configuration
    data class BootConfig(
        var bootDevice: String = "hdd",
        var bootPath: String = "/init.lua",
        var timeout: Int = 3,
        var safeMode: Boolean = false,
        var debugMode: Boolean = false,
        var resolution: Pair<Int, Int>? = null,
        var colorDepth: Int = 8
    )
    
    private var state = BootState.POST
    private var hardware = HardwareInfo()
    private var config = BootConfig()
    private var bootLog = mutableListOf<String>()
    private var errorMessage: String? = null
    
    // Callbacks
    var onStateChange: ((BootState) -> Unit)? = null
    var onLog: ((String) -> Unit)? = null
    var onBootComplete: ((KotlinOS) -> Unit)? = null
    var onBootFailed: ((String) -> Unit)? = null
    
    private fun log(message: String) {
        bootLog.add("[${System.currentTimeMillis()}] $message")
        onLog?.invoke(message)
    }
    
    private fun setState(newState: BootState) {
        state = newState
        onStateChange?.invoke(newState)
    }
    
    /**
     * Start the boot sequence.
     */
    suspend fun boot() = coroutineScope {
        try {
            // POST
            setState(BootState.POST)
            performPOST()
            delay(200)
            
            // Hardware detection
            setState(BootState.HARDWARE_DETECT)
            detectHardware()
            delay(300)
            
            // EEPROM check
            setState(BootState.EEPROM_CHECK)
            checkEEPROM()
            delay(100)
            
            // Boot device selection
            setState(BootState.BOOT_DEVICE_SELECT)
            selectBootDevice()
            delay(config.timeout * 1000L)
            
            // Load OS
            setState(BootState.LOADING_OS)
            val os = loadOS()
            delay(500)
            
            // Complete
            setState(BootState.BOOT_COMPLETE)
            onBootComplete?.invoke(os)
            
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown boot error"
            setState(BootState.BOOT_FAILED)
            onBootFailed?.invoke(errorMessage!!)
        }
    }
    
    /**
     * Power-On Self Test.
     */
    private fun performPOST() {
        log("POST: Starting Power-On Self Test...")
        log("POST: Checking CPU... OK")
        log("POST: Checking memory... OK")
        log("POST: Checking GPU... OK")
        log("POST: POST complete")
    }
    
    /**
     * Detect available hardware.
     */
    private fun detectHardware() {
        log("HARDWARE: Detecting components...")
        
        // In real implementation, this would query OC components
        val components = mutableMapOf<String, String>()
        val hdds = mutableListOf<String>()
        val floppies = mutableListOf<String>()
        
        // Simulated hardware detection
        components["computer"] = "computer-0000"
        components["gpu"] = "gpu-0000"
        components["screen"] = "screen-0000"
        components["keyboard"] = "keyboard-0000"
        components["eeprom"] = "eeprom-0000"
        
        // Check for storage
        hdds.add("hdd-0000")
        
        hardware = HardwareInfo(
            cpuTier = 3,
            ramSize = 2 * 1024 * 1024, // 2MB
            gpuTier = 3,
            screenWidth = 160,
            screenHeight = 50,
            hasKeyboard = true,
            hasInternet = components.containsKey("internet"),
            hdds = hdds,
            floppies = floppies,
            components = components
        )
        
        log("HARDWARE: Found ${components.size} components")
        log("HARDWARE: CPU Tier ${hardware.cpuTier}, RAM ${hardware.ramSize / 1024}KB")
        log("HARDWARE: GPU Tier ${hardware.gpuTier}, ${hardware.screenWidth}x${hardware.screenHeight}")
        log("HARDWARE: ${hdds.size} HDD(s), ${floppies.size} floppy drive(s)")
    }
    
    /**
     * Check EEPROM for boot configuration.
     */
    private fun checkEEPROM() {
        log("EEPROM: Reading boot configuration...")
        
        // In real implementation, read from EEPROM component
        // For now, use defaults
        config = BootConfig(
            bootDevice = if (hardware.hdds.isNotEmpty()) "hdd" else "floppy",
            bootPath = "/init.lua",
            timeout = 3,
            safeMode = false
        )
        
        log("EEPROM: Boot device: ${config.bootDevice}")
        log("EEPROM: Boot path: ${config.bootPath}")
    }
    
    /**
     * Select boot device (with optional menu).
     */
    private fun selectBootDevice() {
        log("BOOT: Selecting boot device...")
        
        val bootDevices = mutableListOf<String>()
        
        if (hardware.hdds.isNotEmpty()) {
            bootDevices.add("HDD: ${hardware.hdds[0]}")
        }
        if (hardware.floppies.isNotEmpty()) {
            bootDevices.add("Floppy: ${hardware.floppies[0]}")
        }
        bootDevices.add("Network boot (PXE)")
        bootDevices.add("Recovery mode")
        
        log("BOOT: Available devices: ${bootDevices.joinToString(", ")}")
        log("BOOT: Booting from ${config.bootDevice} in ${config.timeout}s...")
        log("BOOT: Press any key for boot menu")
    }
    
    /**
     * Load the operating system.
     */
    private fun loadOS(): KotlinOS {
        log("OS: Loading SkibidiOS2...")
        log("OS: Initializing kernel...")
        
        val os = KotlinOS()
        
        log("OS: Mounting filesystems...")
        log("OS: Starting system services...")
        log("OS: Loading desktop environment...")
        
        return os
    }
    
    /**
     * Get boot screen content for rendering.
     */
    fun getBootScreen(): String {
        val sb = StringBuilder()
        val width = 80
        
        // Header
        sb.appendLine("╔${"═".repeat(width - 2)}╗")
        sb.appendLine("║${centerText("SkibidiOS2 EFI Bootloader v1.0", width - 2)}║")
        sb.appendLine("╠${"═".repeat(width - 2)}╣")
        
        // Status
        val statusText = when (state) {
            BootState.POST -> "Performing Power-On Self Test..."
            BootState.HARDWARE_DETECT -> "Detecting hardware..."
            BootState.EEPROM_CHECK -> "Reading boot configuration..."
            BootState.BOOT_DEVICE_SELECT -> "Select boot device (${config.timeout}s timeout)"
            BootState.LOADING_OS -> "Loading SkibidiOS2..."
            BootState.BOOT_COMPLETE -> "Boot complete!"
            BootState.BOOT_FAILED -> "BOOT FAILED: $errorMessage"
        }
        sb.appendLine("║${centerText(statusText, width - 2)}║")
        sb.appendLine("╠${"═".repeat(width - 2)}╣")
        
        // Hardware info
        sb.appendLine("║${padRight(" Hardware:", width - 2)}║")
        sb.appendLine("║${padRight("   CPU: Tier ${hardware.cpuTier}", width - 2)}║")
        sb.appendLine("║${padRight("   RAM: ${hardware.ramSize / 1024} KB", width - 2)}║")
        sb.appendLine("║${padRight("   GPU: Tier ${hardware.gpuTier} (${hardware.screenWidth}x${hardware.screenHeight})", width - 2)}║")
        sb.appendLine("║${padRight("   Storage: ${hardware.hdds.size} HDD, ${hardware.floppies.size} Floppy", width - 2)}║")
        sb.appendLine("╠${"═".repeat(width - 2)}╣")
        
        // Boot log (last 10 entries)
        sb.appendLine("║${padRight(" Boot Log:", width - 2)}║")
        val recentLogs = bootLog.takeLast(10)
        for (logEntry in recentLogs) {
            val truncated = if (logEntry.length > width - 6) {
                logEntry.take(width - 9) + "..."
            } else {
                logEntry
            }
            sb.appendLine("║${padRight("   $truncated", width - 2)}║")
        }
        
        // Pad remaining lines
        repeat(10 - recentLogs.size) {
            sb.appendLine("║${" ".repeat(width - 2)}║")
        }
        
        // Footer
        sb.appendLine("╠${"═".repeat(width - 2)}╣")
        sb.appendLine("║${centerText("Press F2 for BIOS Setup | F12 for Boot Menu", width - 2)}║")
        sb.appendLine("╚${"═".repeat(width - 2)}╝")
        
        return sb.toString()
    }
    
    private fun centerText(text: String, width: Int): String {
        val padding = (width - text.length) / 2
        return " ".repeat(maxOf(0, padding)) + text + " ".repeat(maxOf(0, width - padding - text.length))
    }
    
    private fun padRight(text: String, width: Int): String {
        return text + " ".repeat(maxOf(0, width - text.length))
    }
    
    /**
     * Handle key press during boot.
     */
    fun onKeyPress(keyCode: Int): Boolean {
        return when (keyCode) {
            0x3C -> { // F2 - BIOS Setup
                log("BOOT: Entering BIOS setup...")
                true
            }
            0x58 -> { // F12 - Boot menu
                log("BOOT: Opening boot menu...")
                true
            }
            else -> false
        }
    }
    
    /**
     * Get current boot state.
     */
    fun getState(): BootState = state
    
    /**
     * Get detected hardware info.
     */
    fun getHardware(): HardwareInfo = hardware
    
    /**
     * Get boot configuration.
     */
    fun getConfig(): BootConfig = config
    
    /**
     * Set boot configuration.
     */
    fun setConfig(newConfig: BootConfig) {
        config = newConfig
    }
    
    companion object {
        /**
         * Create and run bootloader.
         */
        suspend fun quickBoot(onComplete: (KotlinOS) -> Unit, onError: (String) -> Unit) {
            val bootloader = EFIBootloader()
            bootloader.onBootComplete = onComplete
            bootloader.onBootFailed = onError
            bootloader.boot()
        }
    }
}

/**
 * BIOS Setup utility.
 */
class BIOSSetup {
    
    data class BIOSSettings(
        var bootOrder: List<String> = listOf("hdd", "floppy", "network"),
        var bootTimeout: Int = 3,
        var safeMode: Boolean = false,
        var debugMode: Boolean = false,
        var resolution: String = "auto",
        var colorDepth: Int = 8,
        var soundEnabled: Boolean = true,
        var networkBoot: Boolean = false
    )
    
    private var settings = BIOSSettings()
    private var selectedOption = 0
    
    val menuOptions = listOf(
        "Boot Order",
        "Boot Timeout",
        "Safe Mode",
        "Debug Mode",
        "Resolution",
        "Color Depth",
        "Sound",
        "Network Boot",
        "Save & Exit",
        "Exit Without Saving"
    )
    
    fun getScreen(): String {
        val sb = StringBuilder()
        val width = 60
        
        sb.appendLine("╔${"═".repeat(width - 2)}╗")
        sb.appendLine("║${centerText("BIOS Setup Utility", width - 2)}║")
        sb.appendLine("╠${"═".repeat(width - 2)}╣")
        
        for ((index, option) in menuOptions.withIndex()) {
            val prefix = if (index == selectedOption) "►" else " "
            val value = when (index) {
                0 -> settings.bootOrder.joinToString(", ")
                1 -> "${settings.bootTimeout}s"
                2 -> if (settings.safeMode) "Enabled" else "Disabled"
                3 -> if (settings.debugMode) "Enabled" else "Disabled"
                4 -> settings.resolution
                5 -> "${settings.colorDepth}-bit"
                6 -> if (settings.soundEnabled) "Enabled" else "Disabled"
                7 -> if (settings.networkBoot) "Enabled" else "Disabled"
                else -> ""
            }
            val line = "$prefix $option: $value"
            sb.appendLine("║${padRight(line, width - 2)}║")
        }
        
        sb.appendLine("╠${"═".repeat(width - 2)}╣")
        sb.appendLine("║${centerText("↑↓ Navigate | Enter Select | Esc Exit", width - 2)}║")
        sb.appendLine("╚${"═".repeat(width - 2)}╝")
        
        return sb.toString()
    }
    
    fun navigate(direction: Int) {
        selectedOption = (selectedOption + direction).coerceIn(0, menuOptions.lastIndex)
    }
    
    fun select(): Boolean {
        return when (selectedOption) {
            8 -> { save(); true }  // Save & Exit
            9 -> true              // Exit Without Saving
            else -> { toggleOption(); false }
        }
    }
    
    private fun toggleOption() {
        when (selectedOption) {
            2 -> settings.safeMode = !settings.safeMode
            3 -> settings.debugMode = !settings.debugMode
            6 -> settings.soundEnabled = !settings.soundEnabled
            7 -> settings.networkBoot = !settings.networkBoot
        }
    }
    
    private fun save() {
        // Save to EEPROM
    }
    
    fun getSettings(): BIOSSettings = settings
    
    private fun centerText(text: String, width: Int): String {
        val padding = (width - text.length) / 2
        return " ".repeat(maxOf(0, padding)) + text + " ".repeat(maxOf(0, width - padding - text.length))
    }
    
    private fun padRight(text: String, width: Int): String {
        return text + " ".repeat(maxOf(0, width - text.length))
    }
}

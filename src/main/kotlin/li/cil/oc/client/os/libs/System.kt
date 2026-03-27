package li.cil.oc.client.os.libs

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * System library for SkibidiOS2.
 * Compatible with SkibidiLuaOS System.lua.
 * Provides system utilities, user management, and settings.
 */
object System {
    
    // Boot time
    private val bootTime = java.lang.System.currentTimeMillis()
    private var bootRealTime = java.lang.System.currentTimeMillis()
    
    // Current user
    private var currentUser = "user"
    private var userSettings = UserSettings()
    
    // System paths
    object Paths {
        const val ROOT = "/"
        const val HOME = "/home/"
        const val USER = "/home/user/"
        const val SYSTEM = "/System/"
        const val APPLICATIONS = "/Applications/"
        const val LIBRARIES = "/Libraries/"
        const val ICONS = "/System/Icons/"
        const val WALLPAPERS = "/System/Wallpapers/"
        const val LOCALIZATIONS = "/System/Localizations/"
        const val TEMPORARY = "/tmp/"
        const val DESKTOP = "/home/user/Desktop/"
        const val DOCUMENTS = "/home/user/Documents/"
        const val DOWNLOADS = "/home/user/Downloads/"
        const val TRASH = "/home/user/.Trash/"
    }
    
    // User settings
    data class UserSettings(
        var localizationLanguage: String = "English",
        var timeFormat: String = "%d %b %Y %H:%M:%S",
        var timeRealTimestamp: Boolean = true,
        var timeTimezone: Long = 0,
        
        // Network
        var networkName: String = "Computer",
        var networkEnabled: Boolean = true,
        var networkSignalStrength: Int = 512,
        
        // Interface
        var wallpaperEnabled: Boolean = true,
        var wallpaperPath: String = "${Paths.WALLPAPERS}Stars.wlp/",
        var wallpaperMode: Int = 1,
        var wallpaperBrightness: Float = 0.9f,
        
        var transparencyEnabled: Boolean = true,
        var transparencyDock: Float = 0.4f,
        var transparencyMenu: Float = 0.2f,
        var transparencyContextMenu: Float = 0.2f,
        var blurEnabled: Boolean = false,
        var blurRadius: Int = 3,
        var blurTransparency: Float = 0.6f,
        
        // Files
        var filesShowExtension: Boolean = false,
        var filesShowHidden: Boolean = false,
        var filesShowApplicationIcon: Boolean = true,
        
        // Security
        var securityPasswordEnabled: Boolean = false,
        var securityPassword: String = ""
    )
    
    // Icon dimensions
    const val ICON_IMAGE_WIDTH = 8
    const val ICON_IMAGE_HEIGHT = 4
    
    // Localization
    private var localization = mutableMapOf<String, String>()
    
    // ==================== Time ====================
    
    /**
     * Get real timestamp in seconds.
     */
    fun getTime(): Long {
        return bootRealTime + uptime() + userSettings.timeTimezone
    }
    
    /**
     * Get formatted time string.
     */
    fun getFormattedTime(format: String = userSettings.timeFormat): String {
        val instant = Instant.ofEpochMilli(getTime())
        val zdt = instant.atZone(ZoneOffset.UTC)
        
        // Convert Lua-style format to Java
        val javaFormat = format
            .replace("%Y", "yyyy")
            .replace("%m", "MM")
            .replace("%d", "dd")
            .replace("%H", "HH")
            .replace("%M", "mm")
            .replace("%S", "ss")
            .replace("%b", "MMM")
            .replace("%B", "MMMM")
            .replace("%a", "EEE")
            .replace("%A", "EEEE")
        
        return try {
            DateTimeFormatter.ofPattern(javaFormat, Locale.ENGLISH).format(zdt)
        } catch (e: Exception) {
            zdt.toString()
        }
    }
    
    /**
     * Get computer uptime in milliseconds.
     */
    fun uptime(): Long {
        return java.lang.System.currentTimeMillis() - bootTime
    }
    
    /**
     * Get computer uptime in seconds.
     */
    fun uptimeSeconds(): Double {
        return uptime() / 1000.0
    }
    
    // ==================== User ====================
    
    /**
     * Get current user name.
     */
    fun getUser(): String = currentUser
    
    /**
     * Set current user.
     */
    fun setUser(name: String) {
        currentUser = name
    }
    
    /**
     * Get user settings.
     */
    fun getUserSettings(): UserSettings = userSettings
    
    /**
     * Set user settings.
     */
    fun setUserSettings(settings: UserSettings) {
        userSettings = settings
    }
    
    /**
     * Get default user settings.
     */
    fun getDefaultUserSettings(): UserSettings = UserSettings()
    
    /**
     * Get user home directory.
     */
    fun getUserHome(): String = "${Paths.HOME}$currentUser/"
    
    // ==================== Localization ====================
    
    /**
     * Get localization string.
     */
    fun localize(key: String, vararg args: Any): String {
        val template = localization[key] ?: return key
        return if (args.isEmpty()) {
            template
        } else {
            var result = template
            args.forEachIndexed { index, arg ->
                result = result.replace("{${index + 1}}", arg.toString())
            }
            result
        }
    }
    
    /**
     * Set localization table.
     */
    fun setLocalization(table: Map<String, String>) {
        localization.clear()
        localization.putAll(table)
    }
    
    /**
     * Get current localization table.
     */
    fun getLocalization(): Map<String, String> = localization.toMap()
    
    // ==================== Computer Info ====================
    
    /**
     * Get total memory.
     */
    fun totalMemory(): Long = Runtime.getRuntime().maxMemory()
    
    /**
     * Get free memory.
     */
    fun freeMemory(): Long = Runtime.getRuntime().freeMemory()
    
    /**
     * Get used memory.
     */
    fun usedMemory(): Long = totalMemory() - freeMemory()
    
    /**
     * Get energy (always full for client simulation).
     */
    fun energy(): Double = 10000.0
    
    /**
     * Get max energy.
     */
    fun maxEnergy(): Double = 10000.0
    
    /**
     * Get computer address.
     */
    fun address(): String = "client-computer"
    
    // ==================== Power ====================
    
    /**
     * Shutdown the computer.
     */
    fun shutdown(reboot: Boolean = false) {
        if (reboot) {
            // Signal reboot
            println("System: Rebooting...")
        } else {
            println("System: Shutting down...")
        }
    }
    
    /**
     * Reboot the computer.
     */
    fun reboot() {
        shutdown(reboot = true)
    }
    
    // ==================== Alerts ====================
    
    /**
     * Beep (visual/audio alert).
     */
    fun beep(frequency: Int = 440, duration: Double = 0.2) {
        // Would trigger audio in full implementation
    }
    
    /**
     * Show error dialog.
     */
    fun error(message: String, options: List<String> = listOf("OK")): Int {
        println("ERROR: $message")
        return 0
    }
    
    // ==================== System Events ====================
    
    /**
     * Event names.
     */
    object Events {
        const val INIT = "system_init"
        const val SHUTDOWN = "system_shutdown"
        const val USER_LOGGED_IN = "user_logged_in"
        const val USER_LOGGED_OUT = "user_logged_out"
        const val COMPONENT_ADDED = "component_added"
        const val COMPONENT_REMOVED = "component_removed"
        const val SCREEN_RESIZED = "screen_resized"
        const val NETWORK_CONNECTED = "network_connected"
        const val NETWORK_DISCONNECTED = "network_disconnected"
    }
}

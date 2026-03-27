package li.cil.oc.client.os.resources

import li.cil.oc.OpenComputers
import li.cil.oc.client.os.libs.Image
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Resource manager for SkibidiOS2.
 * Handles loading icons, wallpapers, themes, and other OS resources.
 */
object OSResources {
    
    private const val OS_PATH = "os"
    private const val ICONS_PATH = "$OS_PATH/icons"
    private const val WALLPAPERS_PATH = "$OS_PATH/wallpapers"
    private const val THEMES_PATH = "$OS_PATH/themes"
    private const val LOCALIZATIONS_PATH = "$OS_PATH/localizations"
    
    // Caches
    private val iconCache = ConcurrentHashMap<String, Image.ImageData?>()
    private val wallpaperCache = ConcurrentHashMap<String, WallpaperData?>()
    private val themeCache = ConcurrentHashMap<String, ThemeData?>()
    private val localizationCache = ConcurrentHashMap<String, Map<String, String>>()
    
    // Resource manager reference
    private var resourceManager: ResourceManager? = null
    
    /**
     * Initialize with Minecraft resource manager.
     */
    fun init(manager: ResourceManager) {
        resourceManager = manager
        clearCaches()
    }
    
    /**
     * Clear all caches.
     */
    fun clearCaches() {
        iconCache.clear()
        wallpaperCache.clear()
        themeCache.clear()
        localizationCache.clear()
    }
    
    // ==================== Icons ====================
    
    /**
     * Available system icons.
     */
    enum class SystemIcon(val filename: String) {
        APPLICATION("Application.pic"),
        ARCHIVE("Archive.pic"),
        FILE_NOT_EXISTS("FileNotExists.pic"),
        FLOPPY("Floppy.pic"),
        FOLDER("Folder.pic"),
        HDD("HDD.pic"),
        SCRIPT("Script.pic"),
        TRASH("Trash.pic"),
        USER("User.pic")
    }
    
    /**
     * Load an icon by name.
     */
    fun loadIcon(name: String): Image.ImageData? {
        return iconCache.getOrPut(name) {
            val path = "$ICONS_PATH/$name"
            loadOCIFImage(path)
        }
    }
    
    /**
     * Load a system icon.
     */
    fun loadIcon(icon: SystemIcon): Image.ImageData? {
        return loadIcon(icon.filename)
    }
    
    /**
     * Get all available icons.
     */
    fun getAvailableIcons(): List<String> {
        return SystemIcon.entries.map { it.filename }
    }
    
    // ==================== Wallpapers ====================
    
    /**
     * Wallpaper data class.
     */
    data class WallpaperData(
        val name: String,
        val type: WallpaperType,
        val staticImage: Image.ImageData? = null,
        val script: String? = null,
        val preview: Image.ImageData? = null
    )
    
    enum class WallpaperType {
        STATIC,     // Static image
        ANIMATED,   // Lua script for animation
        SOLID       // Solid color
    }
    
    /**
     * Available wallpapers.
     */
    val availableWallpapers = listOf(
        "DVD.wlp",
        "Lines.wlp",
        "NyanCat.wlp",
        "Rain.wlp",
        "Snow.wlp",
        "Solid color.wlp",
        "Sphere.wlp",
        "Stars.wlp",
        "Static picture.wlp"
    )
    
    /**
     * Load a wallpaper.
     */
    fun loadWallpaper(name: String): WallpaperData? {
        return wallpaperCache.getOrPut(name) {
            val basePath = "$WALLPAPERS_PATH/$name"
            
            // Try to load Main.lua for animated wallpapers
            val script = loadTextResource("$basePath/Main.lua")
            
            // Try to load preview image
            val preview = loadOCIFImage("$basePath/Preview.pic")
            
            // Try to load static image
            val staticImage = loadOCIFImage("$basePath/Wallpaper.pic")
            
            val type = when {
                script != null -> WallpaperType.ANIMATED
                staticImage != null -> WallpaperType.STATIC
                name.contains("Solid") -> WallpaperType.SOLID
                else -> WallpaperType.STATIC
            }
            
            WallpaperData(
                name = name.removeSuffix(".wlp"),
                type = type,
                staticImage = staticImage,
                script = script,
                preview = preview
            )
        }
    }
    
    /**
     * Get wallpaper names.
     */
    fun getWallpaperNames(): List<String> = availableWallpapers
    
    // ==================== Themes ====================
    
    /**
     * Theme data class.
     */
    data class ThemeData(
        val name: String,
        val colors: ThemeColors,
        val font: String = "default",
        val windowStyle: String = "modern"
    )
    
    data class ThemeColors(
        val background: Int = 0x1E1E1E,
        val foreground: Int = 0xFFFFFF,
        val accent: Int = 0x3399FF,
        val error: Int = 0xFF5555,
        val success: Int = 0x55FF55,
        val warning: Int = 0xFFFF55,
        val windowBackground: Int = 0x2D2D2D,
        val windowBorder: Int = 0x555555,
        val windowTitle: Int = 0x3C3C3C,
        val buttonBackground: Int = 0x404040,
        val buttonHover: Int = 0x505050,
        val buttonPressed: Int = 0x606060,
        val inputBackground: Int = 0x1A1A1A,
        val inputBorder: Int = 0x444444,
        val selection: Int = 0x264F78,
        val scrollbar: Int = 0x555555
    )
    
    /**
     * Built-in themes.
     */
    val defaultTheme = ThemeData(
        name = "Default Dark",
        colors = ThemeColors()
    )
    
    val lightTheme = ThemeData(
        name = "Light",
        colors = ThemeColors(
            background = 0xF0F0F0,
            foreground = 0x1E1E1E,
            accent = 0x0078D4,
            windowBackground = 0xFFFFFF,
            windowBorder = 0xCCCCCC,
            windowTitle = 0xE0E0E0,
            buttonBackground = 0xE0E0E0,
            buttonHover = 0xD0D0D0,
            buttonPressed = 0xC0C0C0,
            inputBackground = 0xFFFFFF,
            inputBorder = 0xAAAAAA,
            selection = 0xADD6FF,
            scrollbar = 0xCCCCCC
        )
    )
    
    val retroTheme = ThemeData(
        name = "Retro",
        colors = ThemeColors(
            background = 0x000080,
            foreground = 0xFFFF00,
            accent = 0x00FFFF,
            windowBackground = 0x000080,
            windowBorder = 0xFFFFFF,
            windowTitle = 0xAAAAAA,
            buttonBackground = 0xAAAAAA,
            buttonHover = 0xFFFFFF,
            buttonPressed = 0x555555
        )
    )
    
    val hackerTheme = ThemeData(
        name = "Hacker",
        colors = ThemeColors(
            background = 0x000000,
            foreground = 0x00FF00,
            accent = 0x00FF00,
            error = 0xFF0000,
            success = 0x00FF00,
            warning = 0xFFFF00,
            windowBackground = 0x0A0A0A,
            windowBorder = 0x00FF00,
            windowTitle = 0x003300,
            buttonBackground = 0x003300,
            buttonHover = 0x004400,
            inputBackground = 0x001100,
            inputBorder = 0x00FF00
        )
    )
    
    /**
     * Load a theme.
     */
    fun loadTheme(name: String): ThemeData {
        return when (name.lowercase()) {
            "default", "dark" -> defaultTheme
            "light" -> lightTheme
            "retro" -> retroTheme
            "hacker" -> hackerTheme
            else -> defaultTheme
        }
    }
    
    /**
     * Get available themes.
     */
    fun getAvailableThemes(): List<String> = listOf(
        "Default Dark", "Light", "Retro", "Hacker"
    )
    
    // ==================== Localizations ====================
    
    /**
     * Available languages.
     */
    val availableLanguages = listOf(
        "English", "Russian", "German", "French", "Spanish",
        "Italian", "Portuguese", "Chinese", "Japanese", "Korean",
        "Arabic", "Hindi", "Polish", "Dutch", "Ukrainian",
        "Belarusian", "Bengali", "Bulgarian", "Finnish", "Slovak"
    )
    
    /**
     * Current language.
     */
    var currentLanguage = "English"
        private set
    
    /**
     * Set current language.
     */
    fun setLanguage(language: String) {
        if (language in availableLanguages) {
            currentLanguage = language
        }
    }
    
    /**
     * Load localization strings for a language.
     */
    fun loadLocalization(language: String): Map<String, String> {
        return localizationCache.getOrPut(language) {
            val path = "$LOCALIZATIONS_PATH/${language.lowercase()}.lang"
            val content = loadTextResource(path) ?: return@getOrPut getDefaultLocalization()
            
            content.lines()
                .filter { it.isNotBlank() && !it.startsWith("#") && "=" in it }
                .associate { line ->
                    val (key, value) = line.split("=", limit = 2)
                    key.trim() to value.trim()
                }
        }
    }
    
    /**
     * Get a localized string.
     */
    fun getString(key: String, vararg args: Any): String {
        val strings = loadLocalization(currentLanguage)
        val template = strings[key] ?: getDefaultLocalization()[key] ?: key
        return if (args.isEmpty()) template else String.format(template, *args)
    }
    
    private fun getDefaultLocalization(): Map<String, String> = mapOf(
        // Common
        "ok" to "OK",
        "cancel" to "Cancel",
        "apply" to "Apply",
        "save" to "Save",
        "close" to "Close",
        "yes" to "Yes",
        "no" to "No",
        "error" to "Error",
        "warning" to "Warning",
        "info" to "Information",
        
        // Apps
        "app.files" to "Files",
        "app.editor" to "Editor",
        "app.terminal" to "Terminal",
        "app.browser" to "Browser",
        "app.settings" to "Settings",
        "app.calculator" to "Calculator",
        "app.notes" to "Notes",
        "app.monitor" to "System Monitor",
        
        // File manager
        "files.new_folder" to "New Folder",
        "files.new_file" to "New File",
        "files.delete" to "Delete",
        "files.rename" to "Rename",
        "files.copy" to "Copy",
        "files.paste" to "Paste",
        "files.cut" to "Cut",
        "files.properties" to "Properties",
        
        // Settings
        "settings.appearance" to "Appearance",
        "settings.language" to "Language",
        "settings.theme" to "Theme",
        "settings.wallpaper" to "Wallpaper",
        "settings.system" to "System",
        "settings.network" to "Network",
        "settings.about" to "About"
    )
    
    // ==================== Resource Loading Helpers ====================
    
    /**
     * Load OCIF image from resources.
     */
    private fun loadOCIFImage(path: String): Image.ImageData? {
        val data = loadBinaryResource(path) ?: return null
        return Image.load(data)
    }
    
    /**
     * Load text resource.
     */
    private fun loadTextResource(path: String): String? {
        return try {
            val location = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, path)
            resourceManager?.getResource(location)?.orElse(null)?.open()?.use { stream ->
                stream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Load binary resource.
     */
    private fun loadBinaryResource(path: String): ByteArray? {
        return try {
            val location = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, path)
            resourceManager?.getResource(location)?.orElse(null)?.open()?.use { stream ->
                stream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get resource as stream.
     */
    fun getResourceStream(path: String): InputStream? {
        return try {
            val location = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, path)
            resourceManager?.getResource(location)?.orElse(null)?.open()
        } catch (e: Exception) {
            null
        }
    }
}

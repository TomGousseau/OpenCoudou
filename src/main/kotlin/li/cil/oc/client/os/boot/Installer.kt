package li.cil.oc.client.os.boot

import li.cil.oc.client.os.filesystem.PersistentFileSystem
import li.cil.oc.client.os.libs.*
import kotlinx.coroutines.*

/**
 * OS Installer for SkibidiOS2.
 * Compatible with SkibidiLuaOS Installer.
 * 
 * Handles fresh installation, upgrades, and recovery.
 */
class Installer {
    
    enum class InstallState {
        WELCOME,
        LICENSE,
        DISK_SELECT,
        INSTALL_TYPE,
        INSTALLING,
        COMPLETE,
        ERROR
    }
    
    enum class InstallType {
        FRESH,          // Clean install
        UPGRADE,        // Keep user data
        REPAIR,         // Fix system files
        RECOVERY        // Restore from backup
    }
    
    data class InstallConfig(
        var targetDisk: String = "",
        var installType: InstallType = InstallType.FRESH,
        var language: String = "English",
        var timezone: String = "UTC",
        var hostname: String = "skibidi-pc",
        var username: String = "user",
        var installApps: Boolean = true,
        var installWallpapers: Boolean = true
    )
    
    data class DiskInfo(
        val address: String,
        val label: String,
        val capacity: Long,
        val used: Long,
        val hasOS: Boolean
    )
    
    private var state = InstallState.WELCOME
    private var config = InstallConfig()
    private var availableDisks = mutableListOf<DiskInfo>()
    private var selectedDiskIndex = 0
    private var installProgress = 0
    private var installStatus = ""
    private var errorMessage: String? = null
    
    // Callbacks
    var onStateChange: ((InstallState) -> Unit)? = null
    var onProgress: ((Int, String) -> Unit)? = null
    var onComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    /**
     * Available languages.
     */
    val languages = listOf(
        "English", "Russian", "German", "French", "Spanish",
        "Italian", "Portuguese", "Chinese", "Japanese", "Korean",
        "Arabic", "Hindi", "Polish", "Dutch", "Ukrainian"
    )
    
    /**
     * Start the installer.
     */
    fun start() {
        detectDisks()
        setState(InstallState.WELCOME)
    }
    
    private fun setState(newState: InstallState) {
        state = newState
        onStateChange?.invoke(newState)
    }
    
    private fun setProgress(progress: Int, status: String) {
        installProgress = progress
        installStatus = status
        onProgress?.invoke(progress, status)
    }
    
    /**
     * Detect available disks.
     */
    private fun detectDisks() {
        availableDisks.clear()
        
        // In real implementation, query OC components
        // Simulated disks
        availableDisks.add(DiskInfo(
            address = "hdd-0000-0000-0001",
            label = "Hard Disk (Tier 1)",
            capacity = 1024 * 1024,
            used = 0,
            hasOS = false
        ))
        availableDisks.add(DiskInfo(
            address = "hdd-0000-0000-0002",
            label = "Hard Disk (Tier 2)",
            capacity = 2 * 1024 * 1024,
            used = 512 * 1024,
            hasOS = true
        ))
    }
    
    /**
     * Navigate between screens.
     */
    fun next(): Boolean {
        return when (state) {
            InstallState.WELCOME -> { setState(InstallState.LICENSE); true }
            InstallState.LICENSE -> { setState(InstallState.DISK_SELECT); true }
            InstallState.DISK_SELECT -> {
                if (availableDisks.isEmpty()) {
                    errorMessage = "No disks available"
                    setState(InstallState.ERROR)
                    false
                } else {
                    config.targetDisk = availableDisks[selectedDiskIndex].address
                    setState(InstallState.INSTALL_TYPE)
                    true
                }
            }
            InstallState.INSTALL_TYPE -> {
                setState(InstallState.INSTALLING)
                true
            }
            InstallState.COMPLETE -> {
                onComplete?.invoke()
                true
            }
            else -> false
        }
    }
    
    fun back(): Boolean {
        return when (state) {
            InstallState.LICENSE -> { setState(InstallState.WELCOME); true }
            InstallState.DISK_SELECT -> { setState(InstallState.LICENSE); true }
            InstallState.INSTALL_TYPE -> { setState(InstallState.DISK_SELECT); true }
            else -> false
        }
    }
    
    /**
     * Perform the installation.
     */
    suspend fun install() = coroutineScope {
        if (state != InstallState.INSTALLING) return@coroutineScope
        
        try {
            val fs = PersistentFileSystem.getOrCreate(config.targetDisk)
            
            // Phase 1: Prepare disk
            setProgress(0, "Preparing disk...")
            if (config.installType == InstallType.FRESH) {
                prepareDisk(fs)
            }
            delay(500)
            
            // Phase 2: Create directory structure
            setProgress(10, "Creating directories...")
            createDirectories(fs)
            delay(300)
            
            // Phase 3: Install system files
            setProgress(20, "Installing system files...")
            installSystemFiles(fs)
            delay(800)
            
            // Phase 4: Install libraries
            setProgress(40, "Installing libraries...")
            installLibraries(fs)
            delay(600)
            
            // Phase 5: Install applications
            if (config.installApps) {
                setProgress(60, "Installing applications...")
                installApplications(fs)
                delay(700)
            }
            
            // Phase 6: Install resources
            setProgress(75, "Installing resources...")
            installResources(fs)
            delay(400)
            
            // Phase 7: Configure system
            setProgress(85, "Configuring system...")
            configureSystem(fs)
            delay(300)
            
            // Phase 8: Install bootloader
            setProgress(95, "Installing bootloader...")
            installBootloader(fs)
            delay(200)
            
            // Complete
            setProgress(100, "Installation complete!")
            delay(500)
            
            setState(InstallState.COMPLETE)
            
        } catch (e: Exception) {
            errorMessage = e.message ?: "Installation failed"
            setState(InstallState.ERROR)
            onError?.invoke(errorMessage!!)
        }
    }
    
    private fun prepareDisk(fs: PersistentFileSystem) {
        // Clear existing data for fresh install
        fs.delete("/", recursive = true)
    }
    
    private fun createDirectories(fs: PersistentFileSystem) {
        val dirs = listOf(
            "/bin",
            "/boot",
            "/boot/efi",
            "/etc",
            "/etc/config",
            "/home",
            "/home/${config.username}",
            "/home/${config.username}/Desktop",
            "/home/${config.username}/Documents",
            "/home/${config.username}/Downloads",
            "/home/${config.username}/Pictures",
            "/lib",
            "/mnt",
            "/opt",
            "/opt/apps",
            "/sys",
            "/sys/icons",
            "/sys/wallpapers",
            "/sys/themes",
            "/sys/fonts",
            "/sys/sounds",
            "/tmp",
            "/usr",
            "/usr/share",
            "/usr/share/locale",
            "/var",
            "/var/log",
            "/var/cache"
        )
        
        dirs.forEach { fs.mkdir(it) }
    }
    
    private fun installSystemFiles(fs: PersistentFileSystem) {
        // Kernel
        fs.writeText("/boot/kernel.sys", "SkibidiOS2 Kernel v1.0\n")
        
        // Init script
        fs.writeText("/boot/init.lua", """
            -- SkibidiOS2 Init Script
            local computer = require("computer")
            local component = require("component")
            
            print("SkibidiOS2 starting...")
            
            -- Load kernel
            dofile("/sys/kernel.lua")
            
            -- Start services
            dofile("/sys/services.lua")
            
            -- Launch desktop
            dofile("/sys/desktop.lua")
        """.trimIndent())
        
        // System configuration
        fs.writeText("/etc/hostname", config.hostname)
        fs.writeText("/etc/timezone", config.timezone)
        fs.writeText("/etc/locale", config.language)
        
        // User configuration
        fs.writeText("/etc/passwd", "${config.username}:x:1000:1000:${config.username}:/home/${config.username}:/bin/sh\n")
        
        // fstab
        fs.writeText("/etc/fstab", """
            # SkibidiOS2 Filesystem Table
            # <device> <mount> <type> <options>
            ${config.targetDisk} / ext2 defaults 0 0
        """.trimIndent())
    }
    
    private fun installLibraries(fs: PersistentFileSystem) {
        val libs = listOf(
            "Base64", "BigLetters", "Bit32", "Color", "Component",
            "Compressor", "Crypto", "Event", "FTP", "GUI",
            "Image", "Internet", "JSON", "Keyboard", "Network",
            "Number", "Paths", "Screen", "Sides", "System",
            "Text", "Vector", "XML"
        )
        
        libs.forEach { lib ->
            fs.writeText("/lib/$lib.lua", "-- $lib library stub\nreturn {}\n")
        }
    }
    
    private fun installApplications(fs: PersistentFileSystem) {
        val apps = mapOf(
            "files" to "File Manager",
            "editor" to "Text Editor",
            "terminal" to "Terminal",
            "browser" to "Web Browser",
            "settings" to "Settings",
            "calculator" to "Calculator",
            "notes" to "Notes",
            "monitor" to "System Monitor",
            "lua" to "Lua Console"
        )
        
        apps.forEach { (id, name) ->
            fs.mkdir("/opt/apps/$id.app")
            fs.writeText("/opt/apps/$id.app/main.lua", """
                -- $name Application
                local app = {}
                app.name = "$name"
                app.icon = "📁"
                
                function app.run()
                    print("$name started")
                end
                
                return app
            """.trimIndent())
            fs.writeText("/opt/apps/$id.app/icon.pic", "ICON")
        }
    }
    
    private fun installResources(fs: PersistentFileSystem) {
        // Icons
        val icons = listOf(
            "Application", "Archive", "FileNotExists", "Floppy",
            "Folder", "HDD", "Script", "Trash", "User"
        )
        icons.forEach { icon ->
            fs.writeText("/sys/icons/$icon.pic", "OCIF\u0006\u0010\u0010ICON_DATA_$icon")
        }
        
        // Wallpapers
        if (config.installWallpapers) {
            val wallpapers = listOf(
                "Default", "Mountains", "Abstract", "Space", "Nature"
            )
            wallpapers.forEach { wp ->
                fs.writeText("/sys/wallpapers/$wp.pic", "OCIF\u0006\u00A0\u0032WALLPAPER_$wp")
            }
        }
        
        // Themes
        fs.writeText("/sys/themes/default.theme", """
            {
                "name": "Default",
                "colors": {
                    "background": "0x1E1E1E",
                    "foreground": "0xFFFFFF",
                    "accent": "0x3399FF",
                    "error": "0xFF5555",
                    "success": "0x55FF55",
                    "warning": "0xFFFF55"
                },
                "font": "default",
                "windowStyle": "modern"
            }
        """.trimIndent())
        
        // Localizations
        languages.forEach { lang ->
            fs.writeText("/usr/share/locale/${lang.lowercase()}.lang", """
                # $lang localization
                app.files=Files
                app.editor=Editor
                app.terminal=Terminal
                app.settings=Settings
                btn.ok=OK
                btn.cancel=Cancel
                btn.apply=Apply
            """.trimIndent())
        }
    }
    
    private fun configureSystem(fs: PersistentFileSystem) {
        // System config
        fs.writeText("/etc/config/system.cfg", """
            {
                "version": "1.0.0",
                "language": "${config.language}",
                "timezone": "${config.timezone}",
                "hostname": "${config.hostname}",
                "theme": "default",
                "wallpaper": "/sys/wallpapers/Default.pic",
                "resolution": "auto",
                "colorDepth": 8
            }
        """.trimIndent())
        
        // User preferences
        fs.writeText("/home/${config.username}/.config/preferences.cfg", """
            {
                "theme": "default",
                "wallpaper": "/sys/wallpapers/Default.pic",
                "showDesktopIcons": true,
                "taskbarPosition": "bottom",
                "fontSize": 1
            }
        """.trimIndent())
    }
    
    private fun installBootloader(fs: PersistentFileSystem) {
        fs.writeText("/boot/efi/boot.lua", """
            -- SkibidiOS2 EFI Bootloader
            local eeprom = component.proxy(component.list("eeprom")())
            local gpu = component.proxy(component.list("gpu")())
            local screen = component.list("screen")()
            
            gpu.bind(screen)
            gpu.setResolution(80, 25)
            gpu.setBackground(0x000000)
            gpu.setForeground(0xFFFFFF)
            gpu.fill(1, 1, 80, 25, " ")
            
            gpu.set(30, 12, "SkibidiOS2 Loading...")
            
            -- Load init
            dofile("/boot/init.lua")
        """.trimIndent())
    }
    
    /**
     * Get install screen content.
     */
    fun getScreen(): String {
        return when (state) {
            InstallState.WELCOME -> getWelcomeScreen()
            InstallState.LICENSE -> getLicenseScreen()
            InstallState.DISK_SELECT -> getDiskSelectScreen()
            InstallState.INSTALL_TYPE -> getInstallTypeScreen()
            InstallState.INSTALLING -> getInstallingScreen()
            InstallState.COMPLETE -> getCompleteScreen()
            InstallState.ERROR -> getErrorScreen()
        }
    }
    
    private fun getWelcomeScreen(): String = """
        ╔══════════════════════════════════════════════════════════════╗
        ║                                                              ║
        ║         ███████╗██╗  ██╗██╗██████╗ ██╗██████╗ ██╗            ║
        ║         ██╔════╝██║ ██╔╝██║██╔══██╗██║██╔══██╗██║            ║
        ║         ███████╗█████╔╝ ██║██████╔╝██║██║  ██║██║            ║
        ║         ╚════██║██╔═██╗ ██║██╔══██╗██║██║  ██║██║            ║
        ║         ███████║██║  ██╗██║██████╔╝██║██████╔╝██║            ║
        ║         ╚══════╝╚═╝  ╚═╝╚═╝╚═════╝ ╚═╝╚═════╝ ╚═╝            ║
        ║                        O S 2                                 ║
        ║                                                              ║
        ║                  Welcome to SkibidiOS2 Installer             ║
        ║                                                              ║
        ║     This wizard will guide you through the installation      ║
        ║     of SkibidiOS2 on your OpenComputers system.             ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ║                  Press ENTER to continue...                  ║
        ║                  Press ESC to cancel                         ║
        ║                                                              ║
        ╚══════════════════════════════════════════════════════════════╝
    """.trimIndent()
    
    private fun getLicenseScreen(): String = """
        ╔══════════════════════════════════════════════════════════════╗
        ║                     License Agreement                        ║
        ╠══════════════════════════════════════════════════════════════╣
        ║                                                              ║
        ║  SkibidiOS2 - Open Source Operating System                   ║
        ║  Copyright (c) 2026                                          ║
        ║                                                              ║
        ║  Permission is hereby granted, free of charge, to any        ║
        ║  person obtaining a copy of this software and associated     ║
        ║  documentation files, to deal in the Software without        ║
        ║  restriction, including without limitation the rights to     ║
        ║  use, copy, modify, merge, publish, distribute, sublicense,  ║
        ║  and/or sell copies of the Software.                         ║
        ║                                                              ║
        ║  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF       ║
        ║  ANY KIND, EXPRESS OR IMPLIED.                               ║
        ║                                                              ║
        ║                                                              ║
        ║  [X] I accept the terms of the license agreement             ║
        ║                                                              ║
        ║           Press ENTER to continue | ESC to go back           ║
        ╚══════════════════════════════════════════════════════════════╝
    """.trimIndent()
    
    private fun getDiskSelectScreen(): String {
        val sb = StringBuilder()
        sb.appendLine("╔══════════════════════════════════════════════════════════════╗")
        sb.appendLine("║                    Select Installation Disk                   ║")
        sb.appendLine("╠══════════════════════════════════════════════════════════════╣")
        
        if (availableDisks.isEmpty()) {
            sb.appendLine("║                                                              ║")
            sb.appendLine("║              No disks available for installation!            ║")
            sb.appendLine("║                                                              ║")
        } else {
            availableDisks.forEachIndexed { index, disk ->
                val prefix = if (index == selectedDiskIndex) "►" else " "
                val osLabel = if (disk.hasOS) " [OS]" else ""
                val capacityMB = disk.capacity / 1024 / 1024
                val usedMB = disk.used / 1024 / 1024
                sb.appendLine("║ $prefix ${disk.label}$osLabel".padEnd(63) + "║")
                sb.appendLine("║     ${disk.address}".padEnd(63) + "║")
                sb.appendLine("║     ${usedMB}MB / ${capacityMB}MB used".padEnd(63) + "║")
                sb.appendLine("║                                                              ║")
            }
        }
        
        repeat(maxOf(0, 12 - availableDisks.size * 4)) {
            sb.appendLine("║                                                              ║")
        }
        
        sb.appendLine("╠══════════════════════════════════════════════════════════════╣")
        sb.appendLine("║        ↑↓ Navigate | ENTER Select | ESC Back                 ║")
        sb.appendLine("╚══════════════════════════════════════════════════════════════╝")
        return sb.toString()
    }
    
    private fun getInstallTypeScreen(): String = """
        ╔══════════════════════════════════════════════════════════════╗
        ║                    Installation Type                         ║
        ╠══════════════════════════════════════════════════════════════╣
        ║                                                              ║
        ║  ${if (config.installType == InstallType.FRESH) "►" else " "} Fresh Install                                           ║
        ║      Erase disk and install clean SkibidiOS2                 ║
        ║                                                              ║
        ║  ${if (config.installType == InstallType.UPGRADE) "►" else " "} Upgrade                                                  ║
        ║      Keep user files, upgrade system                         ║
        ║                                                              ║
        ║  ${if (config.installType == InstallType.REPAIR) "►" else " "} Repair                                                   ║
        ║      Fix system files, keep everything                       ║
        ║                                                              ║
        ║  ${if (config.installType == InstallType.RECOVERY) "►" else " "} Recovery                                                 ║
        ║      Restore from backup                                     ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ╠══════════════════════════════════════════════════════════════╣
        ║        ↑↓ Navigate | ENTER Install | ESC Back               ║
        ╚══════════════════════════════════════════════════════════════╝
    """.trimIndent()
    
    private fun getInstallingScreen(): String {
        val progressBar = "█".repeat(installProgress / 2) + "░".repeat(50 - installProgress / 2)
        return """
        ╔══════════════════════════════════════════════════════════════╗
        ║                    Installing SkibidiOS2                     ║
        ╠══════════════════════════════════════════════════════════════╣
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ║  $installStatus
        ║                                                              ║
        ║  [$progressBar]  ║
        ║                          $installProgress%                              ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ║              Please wait, do not turn off computer           ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ╚══════════════════════════════════════════════════════════════╝
    """.trimIndent()
    }
    
    private fun getCompleteScreen(): String = """
        ╔══════════════════════════════════════════════════════════════╗
        ║                  Installation Complete!                      ║
        ╠══════════════════════════════════════════════════════════════╣
        ║                                                              ║
        ║                         ✓                                    ║
        ║                                                              ║
        ║        SkibidiOS2 has been successfully installed!           ║
        ║                                                              ║
        ║                                                              ║
        ║        Your computer is now ready to use.                    ║
        ║                                                              ║
        ║        Please remove the installation media and              ║
        ║        restart your computer.                                ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ╠══════════════════════════════════════════════════════════════╣
        ║                    Press ENTER to reboot                     ║
        ╚══════════════════════════════════════════════════════════════╝
    """.trimIndent()
    
    private fun getErrorScreen(): String = """
        ╔══════════════════════════════════════════════════════════════╗
        ║                    Installation Error                        ║
        ╠══════════════════════════════════════════════════════════════╣
        ║                                                              ║
        ║                         ✗                                    ║
        ║                                                              ║
        ║        An error occurred during installation:                ║
        ║                                                              ║
        ║        $errorMessage
        ║                                                              ║
        ║                                                              ║
        ║        Please check your hardware and try again.             ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ║                                                              ║
        ╠══════════════════════════════════════════════════════════════╣
        ║           Press ENTER to retry | ESC to cancel               ║
        ╚══════════════════════════════════════════════════════════════╝
    """.trimIndent()
    
    fun selectDisk(index: Int) {
        selectedDiskIndex = index.coerceIn(0, availableDisks.lastIndex)
    }
    
    fun setInstallType(type: InstallType) {
        config.installType = type
    }
    
    fun getConfig(): InstallConfig = config
    fun getState(): InstallState = state
    fun getProgress(): Int = installProgress
    fun getAvailableDisks(): List<DiskInfo> = availableDisks
}

package li.cil.oc.client.os.gui

import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.core.FrameBuffer
import li.cil.oc.client.os.apps.AppInfo
import li.cil.oc.client.os.apps.AppCategory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Desktop Shell for SkibidiOS2.
 * Full-featured shell with taskbar, system tray, notifications, 
 * context menus, and workspace management.
 */
class DesktopShell(private val os: KotlinOS) {
    
    companion object {
        const val TASKBAR_HEIGHT = 1
        const val ICON_WIDTH = 10
        const val ICON_HEIGHT = 4
        const val NOTIFICATION_WIDTH = 30
        const val NOTIFICATION_HEIGHT = 4
        const val MAX_NOTIFICATIONS = 5
        const val NOTIFICATION_TIMEOUT_MS = 5000L
    }
    
    // Desktop state
    private val icons = mutableListOf<DesktopIcon>()
    private val shortcuts = mutableMapOf<String, ShortcutAction>()
    private var selectedIcon: DesktopIcon? = null
    private var hoveredIcon: DesktopIcon? = null
    
    // Taskbar
    private val taskbarItems = mutableListOf<TaskbarItem>()
    private var hoveredTaskbarItem: TaskbarItem? = null
    private var startMenuOpen = false
    private var startMenuSelectedIndex = 0
    
    // System tray
    private val systemTrayItems = mutableListOf<SystemTrayItem>()
    private var systemTrayOpen = false
    private var systemTrayMenuIndex = 0
    
    // Notifications
    private val notifications = ConcurrentLinkedQueue<Notification>()
    private val notificationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Context menu
    private var contextMenu: ContextMenu? = null
    
    // Workspaces
    private val workspaces = mutableListOf<Workspace>()
    private var currentWorkspace = 0
    
    // Wallpaper
    var wallpaper: Wallpaper = Wallpaper.Solid(0x1E1E2E)
    
    // Time
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    // Clipboard
    private var clipboard: ClipboardData? = null
    
    /**
     * Initialize the shell.
     */
    fun initialize() {
        // Create default workspaces
        workspaces.add(Workspace("Main", mutableListOf()))
        workspaces.add(Workspace("Work", mutableListOf()))
        workspaces.add(Workspace("Games", mutableListOf()))
        
        // Create default desktop icons
        createDefaultIcons()
        
        // Create system tray items
        createSystemTray()
        
        // Register keyboard shortcuts
        registerShortcuts()
    }
    
    /**
     * Create default desktop icons.
     */
    private fun createDefaultIcons() {
        var iconY = 2
        
        icons.add(DesktopIcon(
            id = "file_manager",
            symbol = "📁",
            label = "Files",
            appId = "file_manager",
            x = 2, y = iconY
        ))
        iconY += ICON_HEIGHT
        
        icons.add(DesktopIcon(
            id = "terminal",
            symbol = "💻",
            label = "Terminal",
            appId = "terminal",
            x = 2, y = iconY
        ))
        iconY += ICON_HEIGHT
        
        icons.add(DesktopIcon(
            id = "browser",
            symbol = "🌐",
            label = "Browser",
            appId = "web_browser",
            x = 2, y = iconY
        ))
        iconY += ICON_HEIGHT
        
        icons.add(DesktopIcon(
            id = "settings",
            symbol = "⚙️",
            label = "Settings",
            appId = "settings",
            x = 2, y = iconY
        ))
        iconY += ICON_HEIGHT
        
        icons.add(DesktopIcon(
            id = "app_market",
            symbol = "🛒",
            label = "Apps",
            appId = "app_market",
            x = 2, y = iconY
        ))
    }
    
    /**
     * Create system tray items.
     */
    private fun createSystemTray() {
        systemTrayItems.add(SystemTrayItem(
            id = "network",
            icon = "📶",
            tooltip = "Network",
            menu = listOf(
                MenuAction("Status", "network_status"),
                MenuAction("Settings", "network_settings")
            )
        ))
        
        systemTrayItems.add(SystemTrayItem(
            id = "power",
            icon = "🔋",
            tooltip = "Power",
            menu = listOf(
                MenuAction("Power Settings", "power_settings"),
                MenuAction.separator(),
                MenuAction("Shutdown", "shutdown"),
                MenuAction("Restart", "restart"),
                MenuAction("Sleep", "sleep")
            )
        ))
        
        systemTrayItems.add(SystemTrayItem(
            id = "volume",
            icon = "🔊",
            tooltip = "Volume",
            menu = listOf(
                MenuAction("Mixer", "volume_mixer"),
                MenuAction("Mute", "volume_mute")
            )
        ))
    }
    
    /**
     * Register keyboard shortcuts.
     */
    private fun registerShortcuts() {
        // Super key = Start menu
        shortcuts["SUPER"] = ShortcutAction { toggleStartMenu() }
        
        // Alt+Tab = Window switcher
        shortcuts["ALT+TAB"] = ShortcutAction { os.windowManager.cycleFocus() }
        
        // Alt+F4 = Close window
        shortcuts["ALT+F4"] = ShortcutAction { os.windowManager.getFocusedWindow()?.close() }
        
        // Ctrl+Alt+T = Terminal
        shortcuts["CTRL+ALT+T"] = ShortcutAction { os.appRegistry.launch("terminal") }
        
        // Super+D = Show desktop
        shortcuts["SUPER+D"] = ShortcutAction { toggleShowDesktop() }
        
        // Super+E = File manager
        shortcuts["SUPER+E"] = ShortcutAction { os.appRegistry.launch("file_manager") }
        
        // Ctrl+Alt+Del = System monitor
        shortcuts["CTRL+ALT+DELETE"] = ShortcutAction { os.appRegistry.launch("system_monitor") }
        
        // Workspace switching
        shortcuts["SUPER+1"] = ShortcutAction { switchWorkspace(0) }
        shortcuts["SUPER+2"] = ShortcutAction { switchWorkspace(1) }
        shortcuts["SUPER+3"] = ShortcutAction { switchWorkspace(2) }
    }
    
    // ============================================================
    // Rendering
    // ============================================================
    
    /**
     * Render the desktop shell.
     */
    fun render(buffer: FrameBuffer) {
        // Draw wallpaper
        renderWallpaper(buffer)
        
        // Draw desktop icons
        icons.forEach { icon ->
            if (icon.visible) renderIcon(buffer, icon)
        }
        
        // Draw taskbar
        renderTaskbar(buffer)
        
        // Draw start menu
        if (startMenuOpen) {
            renderStartMenu(buffer)
        }
        
        // Draw system tray menu
        if (systemTrayOpen) {
            renderSystemTrayMenu(buffer)
        }
        
        // Draw context menu
        contextMenu?.let { renderContextMenu(buffer, it) }
        
        // Draw notifications
        renderNotifications(buffer)
    }
    
    /**
     * Render wallpaper.
     */
    private fun renderWallpaper(buffer: FrameBuffer) {
        when (val wp = wallpaper) {
            is Wallpaper.Solid -> {
                buffer.clear(wp.color, FrameBuffer.TEXT)
            }
            is Wallpaper.Gradient -> {
                for (y in 0 until os.screenHeight - TASKBAR_HEIGHT) {
                    val ratio = y.toFloat() / os.screenHeight
                    val r = lerp((wp.topColor shr 16) and 0xFF, (wp.bottomColor shr 16) and 0xFF, ratio)
                    val g = lerp((wp.topColor shr 8) and 0xFF, (wp.bottomColor shr 8) and 0xFF, ratio)
                    val b = lerp(wp.topColor and 0xFF, wp.bottomColor and 0xFF, ratio)
                    val color = (r shl 16) or (g shl 8) or b
                    buffer.fillRect(0, y, os.screenWidth, 1, ' ', FrameBuffer.TEXT, color)
                }
            }
            is Wallpaper.Pattern -> {
                buffer.clear(wp.bgColor, FrameBuffer.TEXT)
                val pattern = wp.patternChar
                for (y in 0 until os.screenHeight - TASKBAR_HEIGHT step 2) {
                    for (x in 0 until os.screenWidth step 4) {
                        buffer.setChar(x, y, pattern, wp.fgColor, wp.bgColor)
                    }
                }
            }
            is Wallpaper.Image -> {
                // Render image from picture file
                buffer.clear(0x1E1E2E, FrameBuffer.TEXT)
            }
        }
    }
    
    /**
     * Render a desktop icon.
     */
    private fun renderIcon(buffer: FrameBuffer, icon: DesktopIcon) {
        val isSelected = icon == selectedIcon
        val isHovered = icon == hoveredIcon
        
        // Background
        if (isSelected || isHovered) {
            val bgColor = if (isSelected) 0x3A3A5A else 0x2A2A4A
            buffer.fillRect(icon.x - 1, icon.y - 1, ICON_WIDTH, ICON_HEIGHT, ' ', FrameBuffer.TEXT, bgColor)
        }
        
        // Symbol
        val symbolX = icon.x + (ICON_WIDTH - 2) / 2 - 1
        buffer.drawString(symbolX, icon.y, icon.symbol, FrameBuffer.TEXT, getIconBgColor(icon))
        
        // Label (centered, truncated)
        val label = if (icon.label.length > ICON_WIDTH - 2) {
            icon.label.take(ICON_WIDTH - 3) + "…"
        } else {
            icon.label
        }
        val labelX = icon.x + (ICON_WIDTH - label.length) / 2 - 1
        buffer.drawString(labelX, icon.y + 2, label, FrameBuffer.TEXT, getIconBgColor(icon))
    }
    
    private fun getIconBgColor(icon: DesktopIcon): Int {
        return when {
            icon == selectedIcon -> 0x3A3A5A
            icon == hoveredIcon -> 0x2A2A4A
            else -> (wallpaper as? Wallpaper.Solid)?.color ?: 0x1E1E2E
        }
    }
    
    /**
     * Render taskbar.
     */
    private fun renderTaskbar(buffer: FrameBuffer) {
        val y = os.screenHeight - TASKBAR_HEIGHT
        
        // Taskbar background
        buffer.fillRect(0, y, os.screenWidth, TASKBAR_HEIGHT, ' ', FrameBuffer.TEXT, FrameBuffer.WINDOW_TITLE)
        
        // Start button
        val startBg = if (startMenuOpen) FrameBuffer.ACCENT else FrameBuffer.WINDOW_TITLE
        val startFg = if (startMenuOpen) FrameBuffer.BLACK else FrameBuffer.ACCENT
        buffer.drawString(1, y, "⊞", startFg, startBg)
        buffer.drawString(3, y, "Start", FrameBuffer.TEXT, startBg)
        
        // Workspace indicator
        buffer.drawString(10, y, "[${currentWorkspace + 1}]", FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_TITLE)
        
        // Running apps
        var appX = 15
        taskbarItems.forEach { item ->
            val isFocused = os.windowManager.getFocusedWindow()?.id == item.windowId
            val isHovered = item == hoveredTaskbarItem
            
            val bg = when {
                isFocused -> FrameBuffer.ACCENT
                isHovered -> 0x3A3A5A
                else -> FrameBuffer.WINDOW_TITLE
            }
            val fg = if (isFocused) FrameBuffer.BLACK else FrameBuffer.TEXT
            
            val displayName = "${item.icon} ${item.name.take(8)}"
            buffer.drawString(appX, y, displayName, fg, bg)
            appX += displayName.length + 2
        }
        
        // System tray
        var trayX = os.screenWidth - 20
        systemTrayItems.forEach { item ->
            buffer.drawString(trayX, y, item.icon, FrameBuffer.TEXT, FrameBuffer.WINDOW_TITLE)
            trayX += 2
        }
        
        // Clock
        val time = LocalDateTime.now().format(timeFormatter)
        buffer.drawString(os.screenWidth - time.length - 1, y, time, FrameBuffer.TEXT, FrameBuffer.WINDOW_TITLE)
    }
    
    /**
     * Render start menu.
     */
    private fun renderStartMenu(buffer: FrameBuffer) {
        val menuX = 0
        val menuY = os.screenHeight - 16
        val menuWidth = 28
        val menuHeight = 15
        
        // Menu background with shadow
        buffer.fillRect(menuX + 1, menuY + 1, menuWidth, menuHeight, ' ', FrameBuffer.TEXT, 0x0A0A0A)
        buffer.fillRect(menuX, menuY, menuWidth, menuHeight, ' ', FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        buffer.drawRect(menuX, menuY, menuWidth, menuHeight, FrameBuffer.ACCENT, FrameBuffer.WINDOW_BG)
        
        // Header
        buffer.drawString(menuX + 2, menuY + 1, "⊞ SkibidiOS2", FrameBuffer.ACCENT, FrameBuffer.WINDOW_BG)
        buffer.drawHLine(menuX + 1, menuY + 2, menuWidth - 2, '─', FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
        
        // App categories
        val categories = listOf("📱 All Apps", "⚙️ System", "🎮 Games", "🔧 Utilities", "🌐 Network")
        categories.forEachIndexed { index, category ->
            val isSelected = index == startMenuSelectedIndex
            val bg = if (isSelected) FrameBuffer.ACCENT else FrameBuffer.WINDOW_BG
            val fg = if (isSelected) FrameBuffer.BLACK else FrameBuffer.TEXT
            
            buffer.fillRect(menuX + 1, menuY + 3 + index, menuWidth - 2, 1, ' ', fg, bg)
            buffer.drawString(menuX + 2, menuY + 3 + index, category, fg, bg)
        }
        
        // Separator
        buffer.drawHLine(menuX + 1, menuY + 9, menuWidth - 2, '─', FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
        
        // Quick actions
        buffer.drawString(menuX + 2, menuY + 10, "📁 File Manager", FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        buffer.drawString(menuX + 2, menuY + 11, "⚙️ Settings", FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        buffer.drawString(menuX + 2, menuY + 12, "🔒 Lock Screen", FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        
        // Power options
        buffer.drawHLine(menuX + 1, menuY + 13, menuWidth - 2, '─', FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
        buffer.drawString(menuX + 2, menuY + 14, "⏻ Power", FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
    }
    
    /**
     * Render system tray menu.
     */
    private fun renderSystemTrayMenu(buffer: FrameBuffer) {
        val menuX = os.screenWidth - 25
        val menuY = os.screenHeight - 10
        val menuWidth = 24
        val menuHeight = 9
        
        buffer.fillRect(menuX, menuY, menuWidth, menuHeight, ' ', FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        buffer.drawRect(menuX, menuY, menuWidth, menuHeight, FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
        
        // System info
        val date = LocalDateTime.now().format(dateFormatter)
        val time = LocalDateTime.now().format(timeFormatter)
        
        buffer.drawString(menuX + 2, menuY + 1, "📅 $date", FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        buffer.drawString(menuX + 2, menuY + 2, "🕐 $time", FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        
        buffer.drawHLine(menuX + 1, menuY + 3, menuWidth - 2, '─', FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
        
        // Network
        buffer.drawString(menuX + 2, menuY + 4, "📶 Connected", FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        
        // Power
        buffer.drawString(menuX + 2, menuY + 5, "🔋 100%", 0x00FF00, FrameBuffer.WINDOW_BG)
        
        buffer.drawHLine(menuX + 1, menuY + 6, menuWidth - 2, '─', FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
        
        // Quick toggles
        buffer.drawString(menuX + 2, menuY + 7, "🔊 Sound  🌙 Night", FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
    }
    
    /**
     * Render context menu.
     */
    private fun renderContextMenu(buffer: FrameBuffer, menu: ContextMenu) {
        val width = menu.items.maxOfOrNull { it.label.length }?.plus(4) ?: 20
        val height = menu.items.size + 2
        
        // Shadow
        buffer.fillRect(menu.x + 1, menu.y + 1, width, height, ' ', FrameBuffer.TEXT, 0x0A0A0A)
        
        // Menu background
        buffer.fillRect(menu.x, menu.y, width, height, ' ', FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        buffer.drawRect(menu.x, menu.y, width, height, FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
        
        // Items
        menu.items.forEachIndexed { index, item ->
            val itemY = menu.y + 1 + index
            val isSelected = index == menu.selectedIndex
            
            if (item.isSeparator) {
                buffer.drawHLine(menu.x + 1, itemY, width - 2, '─', FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
            } else {
                val bg = if (isSelected) FrameBuffer.ACCENT else FrameBuffer.WINDOW_BG
                val fg = if (isSelected) FrameBuffer.BLACK else if (item.enabled) FrameBuffer.TEXT else FrameBuffer.TEXT_DIM
                
                buffer.fillRect(menu.x + 1, itemY, width - 2, 1, ' ', fg, bg)
                buffer.drawString(menu.x + 2, itemY, item.label, fg, bg)
                
                // Shortcut hint
                item.shortcut?.let { shortcut ->
                    buffer.drawString(menu.x + width - shortcut.length - 2, itemY, shortcut, 
                        if (isSelected) FrameBuffer.BLACK else FrameBuffer.TEXT_DIM, bg)
                }
            }
        }
    }
    
    /**
     * Render notifications.
     */
    private fun renderNotifications(buffer: FrameBuffer) {
        val notifs = notifications.toList().takeLast(MAX_NOTIFICATIONS)
        var notifY = 2
        
        notifs.forEach { notif ->
            val x = os.screenWidth - NOTIFICATION_WIDTH - 2
            
            // Background with shadow
            buffer.fillRect(x + 1, notifY + 1, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, ' ', 0, 0x0A0A0A)
            buffer.fillRect(x, notifY, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, ' ', FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
            buffer.drawRect(x, notifY, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 
                getNotificationColor(notif.type), FrameBuffer.WINDOW_BG)
            
            // Icon and title
            val icon = getNotificationIcon(notif.type)
            buffer.drawString(x + 1, notifY, icon, getNotificationColor(notif.type), FrameBuffer.WINDOW_BG)
            buffer.drawString(x + 3, notifY, notif.title.take(NOTIFICATION_WIDTH - 6), FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
            
            // Close button
            buffer.setChar(x + NOTIFICATION_WIDTH - 2, notifY, '×', FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
            
            // Message (truncated)
            val message = if (notif.message.length > NOTIFICATION_WIDTH - 4) {
                notif.message.take(NOTIFICATION_WIDTH - 7) + "..."
            } else {
                notif.message
            }
            buffer.drawString(x + 2, notifY + 1, message, FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
            
            notifY += NOTIFICATION_HEIGHT + 1
        }
    }
    
    private fun getNotificationIcon(type: NotificationType): String = when (type) {
        NotificationType.INFO -> "ℹ"
        NotificationType.SUCCESS -> "✓"
        NotificationType.WARNING -> "⚠"
        NotificationType.ERROR -> "✗"
    }
    
    private fun getNotificationColor(type: NotificationType): Int = when (type) {
        NotificationType.INFO -> 0x5599FF
        NotificationType.SUCCESS -> 0x55FF55
        NotificationType.WARNING -> 0xFFAA00
        NotificationType.ERROR -> 0xFF5555
    }
    
    // ============================================================
    // Input Handling
    // ============================================================
    
    /**
     * Handle mouse click.
     */
    fun handleClick(x: Int, y: Int, button: Int): Boolean {
        // Close context menu on click elsewhere
        contextMenu?.let { menu ->
            if (!menu.containsPoint(x, y)) {
                contextMenu = null
            } else {
                handleContextMenuClick(menu, x, y)
                return true
            }
        }
        
        // Check notifications
        if (handleNotificationClick(x, y)) return true
        
        // Check system tray menu
        if (systemTrayOpen && handleSystemTrayMenuClick(x, y)) return true
        
        // Check start menu
        if (startMenuOpen && handleStartMenuClick(x, y)) return true
        
        // Check taskbar
        if (y == os.screenHeight - 1) {
            return handleTaskbarClick(x, button)
        }
        
        // Check desktop icons
        val clickedIcon = icons.find { icon ->
            x >= icon.x - 1 && x < icon.x + ICON_WIDTH - 1 &&
            y >= icon.y - 1 && y < icon.y + ICON_HEIGHT - 1
        }
        
        if (clickedIcon != null) {
            when (button) {
                0 -> { // Left click
                    if (clickedIcon == selectedIcon) {
                        // Double click - launch app
                        launchIcon(clickedIcon)
                    } else {
                        selectedIcon = clickedIcon
                    }
                }
                1 -> { // Right click
                    selectedIcon = clickedIcon
                    showIconContextMenu(clickedIcon, x, y)
                }
            }
            return true
        }
        
        // Click on empty desktop area
        if (button == 1) {
            showDesktopContextMenu(x, y)
            return true
        }
        
        // Deselect
        selectedIcon = null
        startMenuOpen = false
        systemTrayOpen = false
        return false
    }
    
    /**
     * Handle taskbar click.
     */
    private fun handleTaskbarClick(x: Int, button: Int): Boolean {
        // Start button (x: 1-8)
        if (x < 9) {
            toggleStartMenu()
            return true
        }
        
        // System tray area
        if (x > os.screenWidth - 20) {
            systemTrayOpen = !systemTrayOpen
            startMenuOpen = false
            return true
        }
        
        // Running apps
        var appX = 15
        taskbarItems.forEach { item ->
            val itemWidth = item.icon.length + item.name.take(8).length + 2
            if (x >= appX && x < appX + itemWidth) {
                // Click on app
                val window = os.windowManager.getWindows().find { it.id == item.windowId }
                if (window != null) {
                    if (os.windowManager.getFocusedWindow() == window) {
                        // Minimize if focused
                        window.minimized = !window.minimized
                    } else {
                        // Focus
                        window.minimized = false
                        os.windowManager.focus(window)
                    }
                }
                return true
            }
            appX += itemWidth
        }
        
        return false
    }
    
    /**
     * Handle start menu click.
     */
    private fun handleStartMenuClick(x: Int, y: Int): Boolean {
        val menuY = os.screenHeight - 16
        val menuWidth = 28
        
        if (x < 0 || x > menuWidth || y < menuY) {
            startMenuOpen = false
            return false
        }
        
        // Check categories
        val categoryY = y - menuY - 3
        if (categoryY in 0..4) {
            startMenuSelectedIndex = categoryY
            launchCategoryApps(categoryY)
            return true
        }
        
        // Quick actions
        when (y - menuY) {
            10 -> { os.appRegistry.launch("file_manager"); startMenuOpen = false }
            11 -> { os.appRegistry.launch("settings"); startMenuOpen = false }
            12 -> { /* Lock screen */ }
            14 -> { showPowerMenu(x, y) }
        }
        
        return true
    }
    
    /**
     * Handle context menu click.
     */
    private fun handleContextMenuClick(menu: ContextMenu, x: Int, y: Int) {
        val itemIndex = y - menu.y - 1
        if (itemIndex in menu.items.indices) {
            val item = menu.items[itemIndex]
            if (!item.isSeparator && item.enabled) {
                item.action?.invoke()
            }
        }
        contextMenu = null
    }
    
    /**
     * Handle notification click.
     */
    private fun handleNotificationClick(x: Int, y: Int): Boolean {
        val notifX = os.screenWidth - NOTIFICATION_WIDTH - 2
        if (x < notifX) return false
        
        val notifs = notifications.toList()
        var notifY = 2
        
        for (notif in notifs.takeLast(MAX_NOTIFICATIONS)) {
            if (y >= notifY && y < notifY + NOTIFICATION_HEIGHT) {
                // Check close button
                if (x == notifX + NOTIFICATION_WIDTH - 2 && y == notifY) {
                    notifications.remove(notif)
                } else {
                    notif.onClick?.invoke()
                }
                return true
            }
            notifY += NOTIFICATION_HEIGHT + 1
        }
        
        return false
    }
    
    /**
     * Handle system tray menu click.
     */
    private fun handleSystemTrayMenuClick(x: Int, y: Int): Boolean {
        val menuX = os.screenWidth - 25
        val menuY = os.screenHeight - 10
        
        if (x < menuX || x > menuX + 24 || y < menuY || y > menuY + 9) {
            systemTrayOpen = false
            return false
        }
        
        // Handle menu item clicks
        return true
    }
    
    /**
     * Handle mouse move.
     */
    fun handleMouseMove(x: Int, y: Int) {
        // Update icon hover state
        hoveredIcon = icons.find { icon ->
            x >= icon.x - 1 && x < icon.x + ICON_WIDTH - 1 &&
            y >= icon.y - 1 && y < icon.y + ICON_HEIGHT - 1
        }
        
        // Update taskbar hover state
        if (y == os.screenHeight - 1) {
            var appX = 15
            hoveredTaskbarItem = null
            taskbarItems.forEach { item ->
                val itemWidth = item.icon.length + item.name.take(8).length + 2
                if (x >= appX && x < appX + itemWidth) {
                    hoveredTaskbarItem = item
                }
                appX += itemWidth
            }
        } else {
            hoveredTaskbarItem = null
        }
        
        // Update context menu selection
        contextMenu?.let { menu ->
            if (menu.containsPoint(x, y)) {
                menu.selectedIndex = (y - menu.y - 1).coerceIn(0, menu.items.size - 1)
            }
        }
    }
    
    /**
     * Handle key press.
     */
    fun handleKey(keyCode: Int, char: Char): Boolean {
        // Check shortcuts
        // This is simplified - real implementation would check modifiers
        
        // Start menu navigation
        if (startMenuOpen) {
            when (keyCode) {
                258 -> { // Down
                    startMenuSelectedIndex = (startMenuSelectedIndex + 1).coerceAtMost(4)
                    return true
                }
                259 -> { // Up
                    startMenuSelectedIndex = (startMenuSelectedIndex - 1).coerceAtLeast(0)
                    return true
                }
                257 -> { // Enter
                    launchCategoryApps(startMenuSelectedIndex)
                    return true
                }
                256 -> { // Escape
                    startMenuOpen = false
                    return true
                }
            }
        }
        
        return false
    }
    
    // ============================================================
    // Actions
    // ============================================================
    
    /**
     * Launch an icon's associated app.
     */
    private fun launchIcon(icon: DesktopIcon) {
        os.appRegistry.launch(icon.appId)
    }
    
    /**
     * Launch category apps.
     */
    private fun launchCategoryApps(categoryIndex: Int) {
        // For now, just show all apps
        val category = when (categoryIndex) {
            1 -> AppCategory.SYSTEM
            2 -> AppCategory.GAMES
            3 -> AppCategory.UTILITIES
            4 -> AppCategory.NETWORK
            else -> null
        }
        
        // Could open app launcher here
        startMenuOpen = false
    }
    
    /**
     * Toggle start menu.
     */
    fun toggleStartMenu() {
        startMenuOpen = !startMenuOpen
        systemTrayOpen = false
        contextMenu = null
    }
    
    /**
     * Toggle show desktop (minimize all).
     */
    fun toggleShowDesktop() {
        val windows = os.windowManager.getWindows()
        val allMinimized = windows.all { it.minimized }
        
        windows.forEach { window ->
            window.minimized = !allMinimized
        }
    }
    
    /**
     * Switch workspace.
     */
    fun switchWorkspace(index: Int) {
        if (index in workspaces.indices && index != currentWorkspace) {
            // Save current windows to workspace
            workspaces[currentWorkspace].windowIds = os.windowManager.getWindows().map { it.id }.toMutableList()
            
            // Switch
            currentWorkspace = index
            
            // Restore windows (hide others)
            os.windowManager.getWindows().forEach { window ->
                window.minimized = window.id !in workspaces[currentWorkspace].windowIds
            }
            
            showNotification("Workspace", "Switched to ${workspaces[index].name}", NotificationType.INFO)
        }
    }
    
    /**
     * Show icon context menu.
     */
    private fun showIconContextMenu(icon: DesktopIcon, x: Int, y: Int) {
        contextMenu = ContextMenu(x, y, listOf(
            ContextMenuItem("Open", shortcut = "Enter") { launchIcon(icon) },
            ContextMenuItem.separator(),
            ContextMenuItem("Rename") { /* rename dialog */ },
            ContextMenuItem("Remove from Desktop") { icons.remove(icon) },
            ContextMenuItem.separator(),
            ContextMenuItem("Properties") { /* properties dialog */ }
        ))
    }
    
    /**
     * Show desktop context menu.
     */
    private fun showDesktopContextMenu(x: Int, y: Int) {
        contextMenu = ContextMenu(x, y, listOf(
            ContextMenuItem("New Folder", shortcut = "Ctrl+Shift+N") { /* create folder */ },
            ContextMenuItem("New File") { /* create file */ },
            ContextMenuItem.separator(),
            ContextMenuItem("Paste", shortcut = "Ctrl+V", enabled = clipboard != null) { paste() },
            ContextMenuItem.separator(),
            ContextMenuItem("Sort Icons") {},
            ContextMenuItem("Refresh", shortcut = "F5") { /* refresh */ },
            ContextMenuItem.separator(),
            ContextMenuItem("Change Wallpaper") { os.appRegistry.launch("settings") },
            ContextMenuItem("Display Settings") { os.appRegistry.launch("settings") }
        ))
    }
    
    /**
     * Show power menu.
     */
    private fun showPowerMenu(x: Int, y: Int) {
        contextMenu = ContextMenu(x, y - 5, listOf(
            ContextMenuItem("Shutdown") { os.shutdown() },
            ContextMenuItem("Restart") { os.reboot() },
            ContextMenuItem("Sleep") { /* sleep */ },
            ContextMenuItem.separator(),
            ContextMenuItem("Lock Screen", shortcut = "Super+L") { /* lock */ }
        ))
        startMenuOpen = false
    }
    
    /**
     * Paste from clipboard.
     */
    private fun paste() {
        // Handle paste action
    }
    
    // ============================================================
    // Taskbar Management
    // ============================================================
    
    /**
     * Add app to taskbar.
     */
    fun addToTaskbar(appInfo: AppInfo, windowId: Int) {
        taskbarItems.add(TaskbarItem(
            name = appInfo.name,
            icon = appInfo.icon,
            windowId = windowId,
            appId = appInfo.id
        ))
    }
    
    /**
     * Remove from taskbar.
     */
    fun removeFromTaskbar(windowId: Int) {
        taskbarItems.removeAll { it.windowId == windowId }
    }
    
    // ============================================================
    // Notifications
    // ============================================================
    
    /**
     * Show a notification.
     */
    fun showNotification(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO,
        onClick: (() -> Unit)? = null
    ) {
        val notification = Notification(
            id = System.currentTimeMillis(),
            title = title,
            message = message,
            type = type,
            onClick = onClick
        )
        
        notifications.add(notification)
        
        // Auto-dismiss
        notificationScope.launch {
            delay(NOTIFICATION_TIMEOUT_MS)
            notifications.remove(notification)
        }
    }
    
    /**
     * Clear all notifications.
     */
    fun clearNotifications() {
        notifications.clear()
    }
    
    // ============================================================
    // Clipboard
    // ============================================================
    
    /**
     * Copy to clipboard.
     */
    fun copyToClipboard(data: ClipboardData) {
        clipboard = data
    }
    
    /**
     * Get clipboard data.
     */
    fun getClipboard(): ClipboardData? = clipboard
    
    // ============================================================
    // Update
    // ============================================================
    
    /**
     * Update the shell.
     */
    fun update() {
        // Update animations, etc.
    }
    
    // ============================================================
    // Utilities
    // ============================================================
    
    private fun lerp(a: Int, b: Int, t: Float): Int {
        return (a + (b - a) * t).toInt()
    }
}

// ============================================================
// Data Classes
// ============================================================

data class DesktopIcon(
    val id: String,
    val symbol: String,
    val label: String,
    val appId: String,
    var x: Int,
    var y: Int,
    var visible: Boolean = true
)

data class TaskbarItem(
    val name: String,
    val icon: String,
    val windowId: Int,
    val appId: String
)

data class SystemTrayItem(
    val id: String,
    var icon: String,
    var tooltip: String,
    val menu: List<MenuAction>
)

data class MenuAction(
    val label: String,
    val actionId: String,
    val enabled: Boolean = true
) {
    companion object {
        fun separator() = MenuAction("", "", false)
    }
    
    val isSeparator get() = label.isEmpty()
}

data class Workspace(
    val name: String,
    var windowIds: MutableList<Int>
)

data class Notification(
    val id: Long,
    val title: String,
    val message: String,
    val type: NotificationType,
    val onClick: (() -> Unit)? = null
)

enum class NotificationType {
    INFO, SUCCESS, WARNING, ERROR
}

data class ContextMenu(
    val x: Int,
    val y: Int,
    val items: List<ContextMenuItem>,
    var selectedIndex: Int = 0
) {
    fun containsPoint(px: Int, py: Int): Boolean {
        val width = items.maxOfOrNull { it.label.length }?.plus(4) ?: 20
        val height = items.size + 2
        return px >= x && px < x + width && py >= y && py < y + height
    }
}

data class ContextMenuItem(
    val label: String,
    val shortcut: String? = null,
    val enabled: Boolean = true,
    val action: (() -> Unit)? = null
) {
    companion object {
        fun separator() = ContextMenuItem("", enabled = false)
    }
    
    val isSeparator get() = label.isEmpty() && action == null
}

sealed class Wallpaper {
    data class Solid(val color: Int) : Wallpaper()
    data class Gradient(val topColor: Int, val bottomColor: Int) : Wallpaper()
    data class Pattern(val patternChar: Char, val fgColor: Int, val bgColor: Int) : Wallpaper()
    data class Image(val path: String) : Wallpaper()
}

sealed class ClipboardData {
    data class Text(val content: String) : ClipboardData()
    data class Files(val paths: List<String>) : ClipboardData()
}

fun interface ShortcutAction {
    fun invoke()
}

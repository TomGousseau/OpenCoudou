package li.cil.oc.client.os.gui

import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.core.ProcessScheduler
import li.cil.oc.client.os.core.FrameBuffer
import li.cil.oc.client.os.apps.AppInfo
import li.cil.oc.client.os.apps.Application
import li.cil.oc.client.os.apps.ApplicationRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Window Manager Integration Layer.
 * Connects the window manager with applications, processes, desktop shell,
 * and provides unified management of the graphical environment.
 */
class WindowManagerIntegration(
    private val os: KotlinOS,
    private val windowManager: WindowManager,
    private val shell: DesktopShell,
    private val scheduler: ProcessScheduler
) {
    companion object {
        const val MIN_WINDOW_WIDTH = 20
        const val MIN_WINDOW_HEIGHT = 8
    }
    
    // Window-to-process mapping
    private val windowProcessMap = ConcurrentHashMap<Int, Int>() // windowId -> processId
    private val processWindowMap = ConcurrentHashMap<Int, MutableList<Int>>() // processId -> windowIds
    
    // Window-to-app mapping
    private val windowAppMap = ConcurrentHashMap<Int, String>() // windowId -> appId
    
    // Window state tracking
    private val windowStates = ConcurrentHashMap<Int, WindowState>()
    
    // Event listeners
    private val listeners = mutableListOf<WindowManagerListener>()
    
    // Window ID generator
    private val windowIdGen = AtomicInteger(1000)
    
    /**
     * Initialize the integration layer.
     */
    fun initialize() {
        // Register for window events
        registerWindowCallbacks()
        
        // Register for process events
        registerProcessCallbacks()
    }
    
    /**
     * Register window event handlers.
     */
    private fun registerWindowCallbacks() {
        // Hook into window manager for lifecycle events
        // In a real implementation, WindowManager would have an event system
    }
    
    /**
     * Register process event handlers.
     */
    private fun registerProcessCallbacks() {
        // Hook into scheduler for process lifecycle events
        scheduler.setProcessTerminationCallback { pid ->
            handleProcessTerminated(pid)
        }
    }
    
    // ============================================================
    // Window Creation
    // ============================================================
    
    /**
     * Create a window for an application.
     */
    fun createAppWindow(
        app: Application,
        appInfo: AppInfo,
        processId: Int,
        title: String = appInfo.name,
        x: Int? = null,
        y: Int? = null,
        width: Int = appInfo.preferredWidth,
        height: Int = appInfo.preferredHeight,
        flags: WindowFlags = WindowFlags()
    ): Window {
        val windowId = windowIdGen.getAndIncrement()
        
        // Calculate position if not specified
        val actualX = x ?: calculateWindowX(width)
        val actualY = y ?: calculateWindowY(height)
        
        // Create window
        val window = windowManager.createWindow(
            id = windowId,
            title = title,
            x = actualX,
            y = actualY,
            width = width.coerceAtLeast(MIN_WINDOW_WIDTH),
            height = height.coerceAtLeast(MIN_WINDOW_HEIGHT)
        )
        
        // Apply flags
        if (flags.resizable) {
            window.setProperty("resizable", true)
        }
        if (flags.modal) {
            window.setProperty("modal", true)
        }
        if (flags.alwaysOnTop) {
            windowManager.setAlwaysOnTop(window, true)
        }
        if (flags.maximized) {
            maximizeWindow(windowId)
        }
        if (flags.frameless) {
            window.setProperty("frameless", true)
        }
        
        // Track mappings
        windowProcessMap[windowId] = processId
        processWindowMap.getOrPut(processId) { mutableListOf() }.add(windowId)
        windowAppMap[windowId] = appInfo.id
        
        // Initialize state
        windowStates[windowId] = WindowState(
            x = actualX,
            y = actualY,
            width = width,
            height = height,
            normalBounds = WindowBounds(actualX, actualY, width, height)
        )
        
        // Add to taskbar
        shell.addToTaskbar(appInfo, windowId)
        
        // Notify listeners
        notifyWindowCreated(windowId, appInfo.id, processId)
        
        return window
    }
    
    /**
     * Create a child window.
     */
    fun createChildWindow(
        parentWindowId: Int,
        title: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Window? {
        val processId = windowProcessMap[parentWindowId] ?: return null
        val appId = windowAppMap[parentWindowId] ?: return null
        
        val windowId = windowIdGen.getAndIncrement()
        
        val window = windowManager.createWindow(
            id = windowId,
            title = title,
            x = x,
            y = y,
            width = width,
            height = height
        )
        
        // Track as part of same process
        windowProcessMap[windowId] = processId
        processWindowMap[processId]?.add(windowId)
        windowAppMap[windowId] = appId
        
        windowStates[windowId] = WindowState(
            x = x, y = y, width = width, height = height,
            normalBounds = WindowBounds(x, y, width, height),
            parentWindowId = parentWindowId
        )
        
        return window
    }
    
    /**
     * Create a dialog window.
     */
    fun createDialog(
        parentWindowId: Int,
        title: String,
        width: Int,
        height: Int,
        type: DialogType = DialogType.NORMAL
    ): Window? {
        val parentWindow = windowManager.getWindow(parentWindowId) ?: return null
        
        // Center dialog over parent
        val x = parentWindow.x + (parentWindow.width - width) / 2
        val y = parentWindow.y + (parentWindow.height - height) / 2
        
        val dialog = createChildWindow(parentWindowId, title, x, y, width, height)
        dialog?.apply {
            setProperty("dialog", true)
            setProperty("dialogType", type.name)
            setProperty("modal", type == DialogType.MODAL)
        }
        
        return dialog
    }
    
    // ============================================================
    // Window Management
    // ============================================================
    
    /**
     * Close a window.
     */
    fun closeWindow(windowId: Int, force: Boolean = false) {
        val window = windowManager.getWindow(windowId) ?: return
        val processId = windowProcessMap[windowId]
        val appId = windowAppMap[windowId]
        
        // Check for child windows
        val childWindows = windowStates.values.filter { it.parentWindowId == windowId }
        if (childWindows.isNotEmpty() && !force) {
            // Close children first
            windowStates.entries.filter { it.value.parentWindowId == windowId }
                .forEach { closeWindow(it.key, force) }
        }
        
        // Notify listeners (allows cancellation)
        if (!force) {
            val canClose = notifyWindowClosing(windowId)
            if (!canClose) return
        }
        
        // Remove from taskbar (only if main window)
        val state = windowStates[windowId]
        if (state?.parentWindowId == null) {
            shell.removeFromTaskbar(windowId)
        }
        
        // Close window
        windowManager.closeWindow(windowId)
        
        // Clean up mappings
        windowProcessMap.remove(windowId)
        processWindowMap[processId]?.remove(windowId)
        windowAppMap.remove(windowId)
        windowStates.remove(windowId)
        
        // Check if this was the last window for the process
        if (processId != null) {
            val remainingWindows = processWindowMap[processId]
            if (remainingWindows == null || remainingWindows.isEmpty()) {
                processWindowMap.remove(processId)
                // Notify process that all windows are closed
                notifyAllWindowsClosed(processId)
            }
        }
        
        // Notify listeners
        notifyWindowClosed(windowId)
    }
    
    /**
     * Minimize a window.
     */
    fun minimizeWindow(windowId: Int) {
        val window = windowManager.getWindow(windowId) ?: return
        val state = windowStates[windowId] ?: return
        
        if (!state.minimized) {
            state.minimized = true
            window.minimized = true
            notifyWindowMinimized(windowId)
        }
    }
    
    /**
     * Restore a window from minimized state.
     */
    fun restoreWindow(windowId: Int) {
        val window = windowManager.getWindow(windowId) ?: return
        val state = windowStates[windowId] ?: return
        
        if (state.minimized) {
            state.minimized = false
            window.minimized = false
            windowManager.focus(window)
            notifyWindowRestored(windowId)
        }
    }
    
    /**
     * Maximize a window.
     */
    fun maximizeWindow(windowId: Int) {
        val window = windowManager.getWindow(windowId) ?: return
        val state = windowStates[windowId] ?: return
        
        if (!state.maximized) {
            // Save normal bounds
            state.normalBounds = WindowBounds(window.x, window.y, window.width, window.height)
            
            // Maximize
            state.maximized = true
            window.x = 0
            window.y = 0
            window.width = os.screenWidth
            window.height = os.screenHeight - 1 // Leave room for taskbar
            window.maximized = true
            
            notifyWindowMaximized(windowId)
        }
    }
    
    /**
     * Restore a window from maximized state.
     */
    fun unmaximizeWindow(windowId: Int) {
        val window = windowManager.getWindow(windowId) ?: return
        val state = windowStates[windowId] ?: return
        
        if (state.maximized) {
            state.maximized = false
            val bounds = state.normalBounds
            window.x = bounds.x
            window.y = bounds.y
            window.width = bounds.width
            window.height = bounds.height
            window.maximized = false
            
            notifyWindowUnmaximized(windowId)
        }
    }
    
    /**
     * Toggle maximize state.
     */
    fun toggleMaximize(windowId: Int) {
        val state = windowStates[windowId] ?: return
        if (state.maximized) {
            unmaximizeWindow(windowId)
        } else {
            maximizeWindow(windowId)
        }
    }
    
    /**
     * Move a window.
     */
    fun moveWindow(windowId: Int, newX: Int, newY: Int) {
        val window = windowManager.getWindow(windowId) ?: return
        val state = windowStates[windowId] ?: return
        
        // Don't move while maximized
        if (state.maximized) return
        
        // Constrain to screen
        val x = newX.coerceIn(0, os.screenWidth - window.width)
        val y = newY.coerceIn(0, os.screenHeight - window.height - 1)
        
        window.x = x
        window.y = y
        state.x = x
        state.y = y
    }
    
    /**
     * Resize a window.
     */
    fun resizeWindow(windowId: Int, newWidth: Int, newHeight: Int) {
        val window = windowManager.getWindow(windowId) ?: return
        val state = windowStates[windowId] ?: return
        
        // Don't resize while maximized
        if (state.maximized) return
        
        // Check if resizable
        if (window.getProperty("resizable") == false) return
        
        // Apply minimum size
        val width = newWidth.coerceAtLeast(MIN_WINDOW_WIDTH)
        val height = newHeight.coerceAtLeast(MIN_WINDOW_HEIGHT)
        
        // Constrain to screen
        val maxWidth = os.screenWidth - window.x
        val maxHeight = os.screenHeight - window.y - 1
        
        window.width = width.coerceAtMost(maxWidth)
        window.height = height.coerceAtMost(maxHeight)
        state.width = window.width
        state.height = window.height
        
        notifyWindowResized(windowId)
    }
    
    /**
     * Focus a window.
     */
    fun focusWindow(windowId: Int) {
        val window = windowManager.getWindow(windowId) ?: return
        val state = windowStates[windowId] ?: return
        
        // Restore if minimized
        if (state.minimized) {
            restoreWindow(windowId)
        }
        
        windowManager.focus(window)
        notifyWindowFocused(windowId)
    }
    
    /**
     * Set window title.
     */
    fun setWindowTitle(windowId: Int, title: String) {
        val window = windowManager.getWindow(windowId) ?: return
        window.title = title
    }
    
    // ============================================================
    // Window Arrangement
    // ============================================================
    
    /**
     * Tile windows vertically.
     */
    fun tileVertically() {
        val windows = windowManager.getWindows().filter { !it.minimized }
        if (windows.isEmpty()) return
        
        val width = os.screenWidth / windows.size
        val height = os.screenHeight - 1
        
        windows.forEachIndexed { index, window ->
            val state = windowStates[window.id]
            state?.maximized = false
            state?.normalBounds = WindowBounds(window.x, window.y, window.width, window.height)
            
            window.x = index * width
            window.y = 0
            window.width = width
            window.height = height
            window.maximized = false
        }
    }
    
    /**
     * Tile windows horizontally.
     */
    fun tileHorizontally() {
        val windows = windowManager.getWindows().filter { !it.minimized }
        if (windows.isEmpty()) return
        
        val width = os.screenWidth
        val height = (os.screenHeight - 1) / windows.size
        
        windows.forEachIndexed { index, window ->
            val state = windowStates[window.id]
            state?.maximized = false
            state?.normalBounds = WindowBounds(window.x, window.y, window.width, window.height)
            
            window.x = 0
            window.y = index * height
            window.width = width
            window.height = height
            window.maximized = false
        }
    }
    
    /**
     * Cascade windows.
     */
    fun cascadeWindows() {
        val windows = windowManager.getWindows().filter { !it.minimized }
        var offsetX = 2
        var offsetY = 1
        
        windows.forEach { window ->
            val state = windowStates[window.id]
            state?.maximized = false
            state?.normalBounds = WindowBounds(window.x, window.y, window.width, window.height)
            
            window.x = offsetX
            window.y = offsetY
            window.width = os.screenWidth * 2 / 3
            window.height = (os.screenHeight - 1) * 2 / 3
            window.maximized = false
            
            offsetX += 3
            offsetY += 2
            
            if (offsetX > os.screenWidth / 3) {
                offsetX = 2
                offsetY = 1
            }
        }
    }
    
    /**
     * Minimize all windows.
     */
    fun minimizeAll() {
        windowManager.getWindows().forEach { window ->
            minimizeWindow(window.id)
        }
    }
    
    /**
     * Snap window to half of screen.
     */
    fun snapWindow(windowId: Int, side: SnapSide) {
        val window = windowManager.getWindow(windowId) ?: return
        val state = windowStates[windowId] ?: return
        
        // Save normal bounds
        if (!state.maximized && state.snapSide == null) {
            state.normalBounds = WindowBounds(window.x, window.y, window.width, window.height)
        }
        
        when (side) {
            SnapSide.LEFT -> {
                window.x = 0
                window.y = 0
                window.width = os.screenWidth / 2
                window.height = os.screenHeight - 1
            }
            SnapSide.RIGHT -> {
                window.x = os.screenWidth / 2
                window.y = 0
                window.width = os.screenWidth / 2
                window.height = os.screenHeight - 1
            }
            SnapSide.TOP -> {
                window.x = 0
                window.y = 0
                window.width = os.screenWidth
                window.height = (os.screenHeight - 1) / 2
            }
            SnapSide.BOTTOM -> {
                window.x = 0
                window.y = (os.screenHeight - 1) / 2
                window.width = os.screenWidth
                window.height = (os.screenHeight - 1) / 2
            }
        }
        
        state.snapSide = side
        state.maximized = false
        window.maximized = false
    }
    
    /**
     * Unsnap window.
     */
    fun unsnapWindow(windowId: Int) {
        val window = windowManager.getWindow(windowId) ?: return
        val state = windowStates[windowId] ?: return
        
        if (state.snapSide != null) {
            val bounds = state.normalBounds
            window.x = bounds.x
            window.y = bounds.y
            window.width = bounds.width
            window.height = bounds.height
            state.snapSide = null
        }
    }
    
    // ============================================================
    // Process Integration
    // ============================================================
    
    /**
     * Handle process termination.
     */
    private fun handleProcessTerminated(processId: Int) {
        // Close all windows for this process
        val windowIds = processWindowMap[processId]?.toList() ?: return
        
        windowIds.forEach { windowId ->
            closeWindow(windowId, force = true)
        }
        
        processWindowMap.remove(processId)
    }
    
    /**
     * Get process ID for a window.
     */
    fun getProcessId(windowId: Int): Int? = windowProcessMap[windowId]
    
    /**
     * Get windows for a process.
     */
    fun getWindowsForProcess(processId: Int): List<Window> {
        val windowIds = processWindowMap[processId] ?: return emptyList()
        return windowIds.mapNotNull { windowManager.getWindow(it) }
    }
    
    /**
     * Get app ID for a window.
     */
    fun getAppId(windowId: Int): String? = windowAppMap[windowId]
    
    // ============================================================
    // Window Positioning
    // ============================================================
    
    private fun calculateWindowX(width: Int): Int {
        val existingWindows = windowManager.getWindows()
        if (existingWindows.isEmpty()) {
            return (os.screenWidth - width) / 2
        }
        
        // Offset from last window
        val lastWindow = existingWindows.last()
        val newX = lastWindow.x + 3
        return if (newX + width > os.screenWidth) {
            2
        } else {
            newX
        }
    }
    
    private fun calculateWindowY(height: Int): Int {
        val existingWindows = windowManager.getWindows()
        if (existingWindows.isEmpty()) {
            return (os.screenHeight - height - 1) / 2
        }
        
        // Offset from last window
        val lastWindow = existingWindows.last()
        val newY = lastWindow.y + 2
        return if (newY + height > os.screenHeight - 1) {
            1
        } else {
            newY
        }
    }
    
    // ============================================================
    // Events
    // ============================================================
    
    fun addListener(listener: WindowManagerListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: WindowManagerListener) {
        listeners.remove(listener)
    }
    
    private fun notifyWindowCreated(windowId: Int, appId: String, processId: Int) {
        listeners.forEach { it.onWindowCreated(windowId, appId, processId) }
    }
    
    private fun notifyWindowClosing(windowId: Int): Boolean {
        return listeners.all { it.onWindowClosing(windowId) }
    }
    
    private fun notifyWindowClosed(windowId: Int) {
        listeners.forEach { it.onWindowClosed(windowId) }
    }
    
    private fun notifyWindowMinimized(windowId: Int) {
        listeners.forEach { it.onWindowMinimized(windowId) }
    }
    
    private fun notifyWindowRestored(windowId: Int) {
        listeners.forEach { it.onWindowRestored(windowId) }
    }
    
    private fun notifyWindowMaximized(windowId: Int) {
        listeners.forEach { it.onWindowMaximized(windowId) }
    }
    
    private fun notifyWindowUnmaximized(windowId: Int) {
        listeners.forEach { it.onWindowUnmaximized(windowId) }
    }
    
    private fun notifyWindowFocused(windowId: Int) {
        listeners.forEach { it.onWindowFocused(windowId) }
    }
    
    private fun notifyWindowResized(windowId: Int) {
        listeners.forEach { it.onWindowResized(windowId) }
    }
    
    private fun notifyAllWindowsClosed(processId: Int) {
        listeners.forEach { it.onAllWindowsClosed(processId) }
    }
    
    // ============================================================
    // Input Forwarding
    // ============================================================
    
    /**
     * Forward mouse event to focused window's app.
     */
    fun forwardMouseEvent(x: Int, y: Int, button: Int, eventType: MouseEventType): Boolean {
        val focusedWindow = windowManager.getFocusedWindow() ?: return false
        val windowId = focusedWindow.id
        
        // Convert to window-local coordinates
        val localX = x - focusedWindow.x - 1  // Account for border
        val localY = y - focusedWindow.y - 1  // Account for title bar
        
        // Check if inside content area
        if (localX < 0 || localY < 0 ||
            localX >= focusedWindow.width - 2 ||
            localY >= focusedWindow.height - 2) {
            return false
        }
        
        // Forward to app
        val appId = windowAppMap[windowId] ?: return false
        // App would receive the event here
        
        return true
    }
    
    /**
     * Forward key event to focused window's app.
     */
    fun forwardKeyEvent(keyCode: Int, char: Char, modifiers: Int): Boolean {
        val focusedWindow = windowManager.getFocusedWindow() ?: return false
        val windowId = focusedWindow.id
        
        val appId = windowAppMap[windowId] ?: return false
        // App would receive the event here
        
        return true
    }
}

// ============================================================
// Data Classes
// ============================================================

class WindowState(
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var normalBounds: WindowBounds,
    var minimized: Boolean = false,
    var maximized: Boolean = false,
    var snapSide: SnapSide? = null,
    var parentWindowId: Int? = null
)

data class WindowBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class WindowFlags(
    val resizable: Boolean = true,
    val closable: Boolean = true,
    val minimizable: Boolean = true,
    val maximizable: Boolean = true,
    val modal: Boolean = false,
    val alwaysOnTop: Boolean = false,
    val maximized: Boolean = false,
    val frameless: Boolean = false
)

enum class SnapSide {
    LEFT, RIGHT, TOP, BOTTOM
}

enum class DialogType {
    NORMAL, MODAL, OPEN_FILE, SAVE_FILE, COLOR_PICKER, FONT_PICKER
}

enum class MouseEventType {
    CLICK, DOUBLE_CLICK, DRAG, SCROLL, MOVE
}

// ============================================================
// Listener Interface
// ============================================================

interface WindowManagerListener {
    fun onWindowCreated(windowId: Int, appId: String, processId: Int) {}
    fun onWindowClosing(windowId: Int): Boolean = true
    fun onWindowClosed(windowId: Int) {}
    fun onWindowMinimized(windowId: Int) {}
    fun onWindowRestored(windowId: Int) {}
    fun onWindowMaximized(windowId: Int) {}
    fun onWindowUnmaximized(windowId: Int) {}
    fun onWindowFocused(windowId: Int) {}
    fun onWindowResized(windowId: Int) {}
    fun onAllWindowsClosed(processId: Int) {}
}

// ============================================================
// Extensions to Window class
// ============================================================

private val windowProperties = ConcurrentHashMap<Int, MutableMap<String, Any?>>()

fun Window.setProperty(key: String, value: Any?) {
    windowProperties.getOrPut(id) { mutableMapOf() }[key] = value
}

fun Window.getProperty(key: String): Any? {
    return windowProperties[id]?.get(key)
}

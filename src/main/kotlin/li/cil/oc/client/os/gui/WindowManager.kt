package li.cil.oc.client.os.gui

import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.core.FrameBuffer
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Window manager for SkibidiOS2.
 * Handles window lifecycle, z-ordering, focus, and input routing.
 */
class WindowManager(private val os: KotlinOS) {
    
    private val windows = CopyOnWriteArrayList<Window>()
    private var focusedWindow: Window? = null
    private var dragWindow: Window? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    
    var nextWindowId = 1
        private set
    
    fun initialize() {
        // Window manager initialization
    }
    
    /**
     * Create a new window.
     */
    fun createWindow(
        title: String,
        x: Int = 10,
        y: Int = 5,
        width: Int = 60,
        height: Int = 20,
        resizable: Boolean = true,
        closeable: Boolean = true
    ): Window {
        val window = Window(
            id = nextWindowId++,
            title = title,
            x = x,
            y = y,
            width = width,
            height = height,
            resizable = resizable,
            closeable = closeable,
            windowManager = this
        )
        windows.add(window)
        focus(window)
        return window
    }
    
    /**
     * Close a window.
     */
    fun closeWindow(window: Window) {
        window.onClose?.invoke()
        windows.remove(window)
        if (focusedWindow == window) {
            focusedWindow = windows.lastOrNull()
        }
    }
    
    /**
     * Close all windows.
     */
    fun closeAll() {
        windows.forEach { it.onClose?.invoke() }
        windows.clear()
        focusedWindow = null
    }
    
    /**
     * Focus a window (bring to front).
     */
    fun focus(window: Window) {
        if (window in windows) {
            windows.remove(window)
            windows.add(window)
            focusedWindow = window
        }
    }
    
    /**
     * Get the focused window.
     */
    fun getFocusedWindow(): Window? = focusedWindow
    
    /**
     * Get all windows.
     */
    fun getWindows(): List<Window> = windows.toList()
    
    /**
     * Update all windows.
     */
    fun update() {
        windows.forEach { it.update() }
    }
    
    /**
     * Render all windows to frame buffer.
     */
    fun render(buffer: FrameBuffer) {
        // Render windows from back to front
        windows.forEach { window ->
            window.render(buffer)
        }
    }
    
    /**
     * Handle mouse click at position.
     */
    fun handleClick(x: Int, y: Int, button: Int): Boolean {
        // Check windows from front to back
        for (window in windows.reversed()) {
            if (window.containsPoint(x, y)) {
                focus(window)
                
                // Check if clicking on title bar (for dragging)
                if (y == window.y && x >= window.x && x < window.x + window.width) {
                    // Check close button
                    if (window.closeable && x == window.x + window.width - 2) {
                        closeWindow(window)
                        return true
                    }
                    // Start drag
                    dragWindow = window
                    dragOffsetX = x - window.x
                    dragOffsetY = y - window.y
                    return true
                }
                
                // Pass click to window content
                window.handleClick(x - window.x - 1, y - window.y - 1, button)
                return true
            }
        }
        return false
    }
    
    /**
     * Handle mouse drag.
     */
    fun handleDrag(x: Int, y: Int) {
        dragWindow?.let { window ->
            window.x = (x - dragOffsetX).coerceIn(0, os.screenWidth - window.width)
            window.y = (y - dragOffsetY).coerceIn(1, os.screenHeight - window.height)
        }
    }
    
    /**
     * Handle mouse release.
     */
    fun handleRelease() {
        dragWindow = null
    }
    
    /**
     * Handle key press.
     */
    fun handleKey(keyCode: Int, char: Char): Boolean {
        focusedWindow?.let { window ->
            return window.handleKey(keyCode, char)
        }
        return false
    }
    
    /**
     * Handle mouse move.
     */
    fun handleMouseMove(x: Int, y: Int) {
        // Update hover state on windows
        windows.forEach { window ->
            window.hovering = window.containsPoint(x, y)
        }
    }
    
    /**
     * Handle drag with button.
     */
    fun handleDrag(x: Int, y: Int, button: Int) {
        handleDrag(x, y)
    }
    
    /**
     * Cycle focus to next window.
     */
    fun cycleFocus() {
        if (windows.size <= 1) return
        
        val currentIndex = focusedWindow?.let { windows.indexOf(it) } ?: -1
        val nextIndex = (currentIndex + 1) % windows.size
        focus(windows[nextIndex])
    }
    
    /**
     * Create a window with specific ID.
     */
    fun createWindow(
        id: Int,
        title: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Window {
        val window = Window(
            id = id,
            title = title,
            x = x,
            y = y,
            width = width,
            height = height,
            resizable = true,
            closeable = true,
            windowManager = this
        )
        windows.add(window)
        focus(window)
        return window
    }
    
    /**
     * Get window by ID.
     */
    fun getWindow(id: Int): Window? = windows.find { it.id == id }
    
    /**
     * Close window by ID.
     */
    fun closeWindow(id: Int) {
        getWindow(id)?.let { closeWindow(it) }
    }
    
    /**
     * Set window always on top.
     */
    fun setAlwaysOnTop(window: Window, alwaysOnTop: Boolean) {
        window.alwaysOnTop = alwaysOnTop
        if (alwaysOnTop) {
            // Move to top of z-order
            windows.remove(window)
            windows.add(window)
        }
    }
}

/**
 * A window in the OS.
 */
class Window(
    val id: Int,
    var title: String,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    val resizable: Boolean,
    val closeable: Boolean,
    private val windowManager: WindowManager
) {
    // Window state
    var minimized: Boolean = false
    var maximized: Boolean = false
    var hovering: Boolean = false
    var alwaysOnTop: Boolean = false
    
    // Window content
    private val components = mutableListOf<UIComponent>()
    
    // Event handlers
    var onClose: (() -> Unit)? = null
    var onResize: ((Int, Int) -> Unit)? = null
    var onFocus: (() -> Unit)? = null
    var onBlur: (() -> Unit)? = null
    
    // Content buffer for double buffering
    val contentBuffer = FrameBuffer(width - 2, height - 2)
    
    /**
     * Add a UI component to the window.
     */
    fun addComponent(component: UIComponent) {
        components.add(component)
    }
    
    /**
     * Remove a UI component.
     */
    fun removeComponent(component: UIComponent) {
        components.remove(component)
    }
    
    /**
     * Clear all components.
     */
    fun clearComponents() {
        components.clear()
    }
    
    /**
     * Check if point is inside window.
     */
    fun containsPoint(px: Int, py: Int): Boolean {
        return px >= x && px < x + width && py >= y && py < y + height
    }
    
    /**
     * Update window state.
     */
    fun update() {
        components.forEach { it.update() }
    }
    
    /**
     * Render window to buffer.
     */
    fun render(buffer: FrameBuffer) {
        val isFocused = windowManager.getFocusedWindow() == this
        
        // Window background
        buffer.fillRect(x, y, width, height, ' ', FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        
        // Window border
        val borderColor = if (isFocused) FrameBuffer.ACCENT else FrameBuffer.TEXT_DIM
        buffer.drawRect(x, y, width, height, borderColor, FrameBuffer.WINDOW_BG)
        
        // Title bar
        buffer.fillRect(x + 1, y, width - 2, 1, ' ', FrameBuffer.TEXT, FrameBuffer.WINDOW_TITLE)
        val displayTitle = if (title.length > width - 6) title.take(width - 9) + "..." else title
        buffer.drawString(x + 2, y, displayTitle, FrameBuffer.TEXT, FrameBuffer.WINDOW_TITLE)
        
        // Close button
        if (closeable) {
            buffer.setChar(x + width - 2, y, '×', FrameBuffer.RED, FrameBuffer.WINDOW_TITLE)
        }
        
        // Render content
        contentBuffer.clear(FrameBuffer.WINDOW_BG, FrameBuffer.TEXT)
        components.forEach { it.render(contentBuffer) }
        buffer.blit(contentBuffer, 0, 0, x + 1, y + 1, width - 2, height - 2)
    }
    
    /**
     * Handle click inside window content area.
     */
    fun handleClick(localX: Int, localY: Int, button: Int): Boolean {
        components.forEach { component ->
            if (component.containsPoint(localX, localY)) {
                return component.handleClick(localX - component.x, localY - component.y, button)
            }
        }
        return false
    }
    
    /**
     * Handle key press.
     */
    fun handleKey(keyCode: Int, char: Char): Boolean {
        // Find focused component and pass key
        components.forEach { component ->
            if (component.focused && component.handleKey(keyCode, char)) {
                return true
            }
        }
        return false
    }
    
    /**
     * Close this window.
     */
    fun close() {
        windowManager.closeWindow(this)
    }
}

/**
 * Base class for UI components.
 */
abstract class UIComponent(
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int
) {
    var visible = true
    var enabled = true
    var focused = false
    
    abstract fun render(buffer: FrameBuffer)
    abstract fun update()
    
    open fun containsPoint(px: Int, py: Int): Boolean {
        return px >= x && px < x + width && py >= y && py < y + height
    }
    
    open fun handleClick(localX: Int, localY: Int, button: Int): Boolean = false
    open fun handleKey(keyCode: Int, char: Char): Boolean = false
}

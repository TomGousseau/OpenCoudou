package li.cil.oc.client.os.libs

/**
 * GUI library for SkibidiOS2.
 * Compatible with SkibidiLuaOS GUI.lua.
 * Provides widget toolkit for building user interfaces.
 */
object GUI {
    
    // ==================== Alignment Constants ====================
    const val ALIGNMENT_HORIZONTAL_LEFT = 1
    const val ALIGNMENT_HORIZONTAL_CENTER = 2
    const val ALIGNMENT_HORIZONTAL_RIGHT = 3
    const val ALIGNMENT_VERTICAL_TOP = 4
    const val ALIGNMENT_VERTICAL_CENTER = 5
    const val ALIGNMENT_VERTICAL_BOTTOM = 6
    
    const val DIRECTION_HORIZONTAL = 7
    const val DIRECTION_VERTICAL = 8
    
    const val SIZE_POLICY_ABSOLUTE = 9
    const val SIZE_POLICY_RELATIVE = 10
    
    const val IO_MODE_FILE = 11
    const val IO_MODE_DIRECTORY = 12
    const val IO_MODE_BOTH = 13
    const val IO_MODE_OPEN = 14
    const val IO_MODE_SAVE = 15
    
    // ==================== Animation Durations ====================
    const val BUTTON_PRESS_DURATION = 0.2
    const val BUTTON_ANIMATION_DURATION = 0.2
    const val SWITCH_ANIMATION_DURATION = 0.3
    const val FILESYSTEM_DIALOG_ANIMATION_DURATION = 0.5
    
    // ==================== Default Colors ====================
    object Colors {
        // Context Menu
        const val CONTEXT_MENU_DEFAULT_BACKGROUND = 0x1E1E1E
        const val CONTEXT_MENU_DEFAULT_ICON = 0x696969
        const val CONTEXT_MENU_DEFAULT_TEXT = 0xD2D2D2
        const val CONTEXT_MENU_PRESSED_BACKGROUND = 0x3366CC
        const val CONTEXT_MENU_PRESSED_ICON = 0xB4B4B4
        const val CONTEXT_MENU_PRESSED_TEXT = 0xFFFFFF
        const val CONTEXT_MENU_DISABLED_ICON = 0x5A5A5A
        const val CONTEXT_MENU_DISABLED_TEXT = 0x5A5A5A
        const val CONTEXT_MENU_SEPARATOR = 0x2D2D2D
        const val CONTEXT_MENU_SHADOW_TRANSPARENCY = 0.4f
        
        // Background Container
        const val BACKGROUND_PANEL = 0x000000
        const val BACKGROUND_TITLE = 0xE1E1E1
        const val BACKGROUND_PANEL_TRANSPARENCY = 0.3f
        
        // Window
        const val WINDOW_BACKGROUND_PANEL = 0xF0F0F0
        const val WINDOW_SHADOW_TRANSPARENCY = 0.6f
        const val WINDOW_TITLE_BACKGROUND = 0xE1E1E1
        const val WINDOW_TITLE_TEXT = 0x2D2D2D
        const val WINDOW_TAB_BAR_DEFAULT_BACKGROUND = 0x2D2D2D
        const val WINDOW_TAB_BAR_DEFAULT_TEXT = 0xF0F0F0
        const val WINDOW_TAB_BAR_SELECTED_BACKGROUND = 0xF0F0F0
        const val WINDOW_TAB_BAR_SELECTED_TEXT = 0x2D2D2D
    }
    
    // ==================== Syntax Highlighting ====================
    object LuaSyntax {
        val colorScheme = mapOf(
            "background" to 0x1E1E1E,
            "text" to 0xE1E1E1,
            "strings" to 0x99FF80,
            "loops" to 0xFFFF98,
            "comments" to 0x898989,
            "boolean" to 0xFFDB40,
            "logic" to 0xFFCC66,
            "numbers" to 0x66DBFF,
            "functions" to 0xFFCC66,
            "compares" to 0xFFCC66,
            "lineNumbersBackground" to 0x2D2D2D,
            "lineNumbersText" to 0xC3C3C3,
            "scrollBarBackground" to 0x2D2D2D,
            "scrollBarForeground" to 0x5A5A5A,
            "selection" to 0x4B4B4B,
            "indentation" to 0x2D2D2D
        )
    }
    
    // ==================== Base Widget ====================
    
    /**
     * Base class for all GUI objects.
     */
    open class Widget(
        var x: Int = 1,
        var y: Int = 1,
        var width: Int = 1,
        var height: Int = 1
    ) {
        var parent: Container? = null
        var visible = true
        var disabled = false
        var blockScreenEvents = true
        
        var horizontalAlignment = ALIGNMENT_HORIZONTAL_LEFT
        var verticalAlignment = ALIGNMENT_VERTICAL_TOP
        
        // Event handlers
        var onClick: ((Int, Int, Int, String) -> Unit)? = null
        var onDoubleClick: ((Int, Int, Int, String) -> Unit)? = null
        var onDrag: ((Int, Int, Int, Int, Int, String) -> Unit)? = null
        var onScroll: ((Int, Int, Int, Int, String) -> Unit)? = null
        var onKeyDown: ((Int, Char) -> Unit)? = null
        var onFocus: (() -> Unit)? = null
        var onBlur: (() -> Unit)? = null
        
        open fun isPointInside(px: Int, py: Int): Boolean {
            return px >= x && px < x + width && py >= y && py < y + height
        }
        
        open fun draw() {
            // Override in subclasses
        }
        
        fun localPosition(gx: Int, gy: Int): Pair<Int, Int> {
            return (gx - x + 1) to (gy - y + 1)
        }
        
        fun globalPosition(): Pair<Int, Int> {
            var gx = x
            var gy = y
            var p = parent
            while (p != null) {
                gx += p.x - 1
                gy += p.y - 1
                p = p.parent
            }
            return gx to gy
        }
        
        fun remove() {
            parent?.removeChild(this)
        }
        
        fun moveToFront() {
            parent?.moveChildToFront(this)
        }
        
        fun moveToBack() {
            parent?.moveChildToBack(this)
        }
    }
    
    // ==================== Container ====================
    
    /**
     * Container that holds child widgets.
     */
    open class Container(x: Int = 1, y: Int = 1, width: Int = 1, height: Int = 1) : Widget(x, y, width, height) {
        val children = mutableListOf<Widget>()
        
        fun addChild(child: Widget): Widget {
            child.parent = this
            children.add(child)
            return child
        }
        
        fun removeChild(child: Widget): Boolean {
            child.parent = null
            return children.remove(child)
        }
        
        fun removeChildren() {
            children.forEach { it.parent = null }
            children.clear()
        }
        
        fun moveChildToFront(child: Widget) {
            if (children.remove(child)) {
                children.add(child)
            }
        }
        
        fun moveChildToBack(child: Widget) {
            if (children.remove(child)) {
                children.add(0, child)
            }
        }
        
        fun indexOf(child: Widget): Int = children.indexOf(child)
        
        override fun draw() {
            if (!visible) return
            children.filter { it.visible }.forEach { it.draw() }
        }
        
        fun getChildAt(px: Int, py: Int): Widget? {
            for (i in children.indices.reversed()) {
                val child = children[i]
                if (child.visible && child.isPointInside(px, py)) {
                    return child
                }
            }
            return null
        }
    }
    
    // ==================== Panel ====================
    
    /**
     * Simple colored panel/rectangle.
     */
    class Panel(
        x: Int, y: Int, width: Int, height: Int,
        var color: Int = 0xE1E1E1,
        var transparency: Float? = null
    ) : Widget(x, y, width, height) {
        
        override fun draw() {
            if (!visible) return
            Screen.setBackground(color)
            Screen.fill(x, y, width, height, ' ')
        }
    }
    
    // ==================== Label ====================
    
    /**
     * Text label.
     */
    class Label(
        x: Int, y: Int, width: Int, height: Int,
        var text: String,
        var textColor: Int = 0x000000,
        var backgroundColor: Int? = null
    ) : Widget(x, y, width, height) {
        
        override fun draw() {
            if (!visible) return
            
            backgroundColor?.let {
                Screen.setBackground(it)
                Screen.fill(x, y, width, height, ' ')
            }
            
            Screen.setForeground(textColor)
            
            val (alignedX, alignedY) = getAlignedPosition(text.length, 1)
            Screen.set(alignedX, alignedY, text.take(width))
        }
        
        private fun getAlignedPosition(textWidth: Int, textHeight: Int): Pair<Int, Int> {
            val ax = when (horizontalAlignment) {
                ALIGNMENT_HORIZONTAL_CENTER -> x + (width - textWidth) / 2
                ALIGNMENT_HORIZONTAL_RIGHT -> x + width - textWidth
                else -> x
            }
            val ay = when (verticalAlignment) {
                ALIGNMENT_VERTICAL_CENTER -> y + (height - textHeight) / 2
                ALIGNMENT_VERTICAL_BOTTOM -> y + height - textHeight
                else -> y
            }
            return ax to ay
        }
    }
    
    // ==================== Button ====================
    
    /**
     * Clickable button.
     */
    class Button(
        x: Int, y: Int, width: Int, height: Int,
        var text: String,
        var buttonColor: Int = 0xE1E1E1,
        var textColor: Int = 0x000000,
        var pressedButtonColor: Int = 0x3366CC,
        var pressedTextColor: Int = 0xFFFFFF,
        var disabledButtonColor: Int = 0x878787,
        var disabledTextColor: Int = 0x5A5A5A
    ) : Widget(x, y, width, height) {
        
        var pressed = false
        var onPress: (() -> Unit)? = null
        
        override fun draw() {
            if (!visible) return
            
            val bg: Int
            val fg: Int
            
            when {
                disabled -> { bg = disabledButtonColor; fg = disabledTextColor }
                pressed -> { bg = pressedButtonColor; fg = pressedTextColor }
                else -> { bg = buttonColor; fg = textColor }
            }
            
            Screen.setBackground(bg)
            Screen.fill(x, y, width, height, ' ')
            
            Screen.setForeground(fg)
            val tx = x + (width - text.length) / 2
            val ty = y + height / 2
            Screen.set(tx, ty, text.take(width))
        }
        
        fun press() {
            if (!disabled) {
                pressed = true
                onPress?.invoke()
            }
        }
        
        fun release() {
            pressed = false
        }
    }
    
    // ==================== TextField ====================
    
    /**
     * Text input field.
     */
    class TextField(
        x: Int, y: Int, width: Int, height: Int,
        var placeholder: String = "",
        var backgroundColor: Int = 0xFFFFFF,
        var textColor: Int = 0x000000,
        var placeholderColor: Int = 0x878787,
        var focusedBorderColor: Int = 0x3366CC
    ) : Widget(x, y, width, height) {
        
        var text = ""
        var cursorPosition = 0
        var focused = false
        var password = false
        var maxLength = Int.MAX_VALUE
        var onChange: ((String) -> Unit)? = null
        var onSubmit: ((String) -> Unit)? = null
        
        private var scrollOffset = 0
        
        override fun draw() {
            if (!visible) return
            
            Screen.setBackground(backgroundColor)
            Screen.fill(x, y, width, height, ' ')
            
            val displayText = if (text.isEmpty()) {
                Screen.setForeground(placeholderColor)
                placeholder
            } else {
                Screen.setForeground(textColor)
                if (password) "•".repeat(text.length) else text
            }
            
            val visibleText = displayText.drop(scrollOffset).take(width - 2)
            Screen.set(x + 1, y + height / 2, visibleText)
            
            // Draw cursor if focused
            if (focused && text.isNotEmpty()) {
                val cursorX = x + 1 + cursorPosition - scrollOffset
                if (cursorX in x until x + width) {
                    Screen.setForeground(focusedBorderColor)
                    Screen.set(cursorX, y + height / 2, "│")
                }
            }
        }
        
        fun setText(newText: String) {
            text = newText.take(maxLength)
            cursorPosition = text.length
            onChange?.invoke(text)
        }
        
        fun insert(char: Char) {
            if (text.length < maxLength) {
                text = text.substring(0, cursorPosition) + char + text.substring(cursorPosition)
                cursorPosition++
                onChange?.invoke(text)
            }
        }
        
        fun delete() {
            if (cursorPosition > 0) {
                text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition)
                cursorPosition--
                onChange?.invoke(text)
            }
        }
        
        fun moveCursor(delta: Int) {
            cursorPosition = (cursorPosition + delta).coerceIn(0, text.length)
        }
    }
    
    // ==================== List ====================
    
    /**
     * Scrollable list.
     */
    class ItemList(
        x: Int, y: Int, width: Int, height: Int,
        var backgroundColor: Int = 0xFFFFFF,
        var textColor: Int = 0x000000,
        var selectedBackgroundColor: Int = 0x3366CC,
        var selectedTextColor: Int = 0xFFFFFF
    ) : Widget(x, y, width, height) {
        
        val items = mutableListOf<String>()
        var selectedIndex = -1
        var scrollOffset = 0
        var onSelect: ((Int, String) -> Unit)? = null
        var onDoubleClick: ((Int, String) -> Unit)? = null
        
        override fun draw() {
            if (!visible) return
            
            Screen.setBackground(backgroundColor)
            Screen.fill(x, y, width, height, ' ')
            
            val visibleItems = height
            for (i in 0 until visibleItems) {
                val itemIndex = scrollOffset + i
                if (itemIndex >= items.size) break
                
                val item = items[itemIndex]
                val isSelected = itemIndex == selectedIndex
                
                if (isSelected) {
                    Screen.setBackground(selectedBackgroundColor)
                    Screen.setForeground(selectedTextColor)
                    Screen.fill(x, y + i, width, 1, ' ')
                } else {
                    Screen.setBackground(backgroundColor)
                    Screen.setForeground(textColor)
                }
                
                Screen.set(x + 1, y + i, item.take(width - 2))
            }
            
            // Draw scrollbar if needed
            if (items.size > height) {
                val scrollbarHeight = maxOf(1, height * height / items.size)
                val scrollbarY = y + (scrollOffset * (height - scrollbarHeight) / (items.size - height))
                Screen.setBackground(0x878787)
                Screen.fill(x + width - 1, scrollbarY, 1, scrollbarHeight, ' ')
            }
        }
        
        fun select(index: Int) {
            if (index in items.indices) {
                selectedIndex = index
                onSelect?.invoke(index, items[index])
            }
        }
        
        fun addItem(item: String) {
            items.add(item)
        }
        
        fun removeItem(index: Int) {
            if (index in items.indices) {
                items.removeAt(index)
                if (selectedIndex >= items.size) {
                    selectedIndex = items.size - 1
                }
            }
        }
        
        fun clear() {
            items.clear()
            selectedIndex = -1
            scrollOffset = 0
        }
        
        fun scrollTo(index: Int) {
            scrollOffset = index.coerceIn(0, maxOf(0, items.size - height))
        }
    }
    
    // ==================== Progress Bar ====================
    
    /**
     * Progress bar.
     */
    class ProgressBar(
        x: Int, y: Int, width: Int, height: Int = 1,
        var value: Float = 0f,
        var backgroundColor: Int = 0x2D2D2D,
        var foregroundColor: Int = 0x3366CC,
        var showPercentage: Boolean = true
    ) : Widget(x, y, width, height) {
        
        override fun draw() {
            if (!visible) return
            
            Screen.setBackground(backgroundColor)
            Screen.fill(x, y, width, height, ' ')
            
            val filledWidth = (width * value.coerceIn(0f, 1f)).toInt()
            if (filledWidth > 0) {
                Screen.setBackground(foregroundColor)
                Screen.fill(x, y, filledWidth, height, ' ')
            }
            
            if (showPercentage && height >= 1) {
                val percentage = "${(value * 100).toInt()}%"
                val textX = x + (width - percentage.length) / 2
                val textY = y + height / 2
                Screen.setForeground(0xFFFFFF)
                Screen.set(textX, textY, percentage)
            }
        }
    }
    
    // ==================== Checkbox ====================
    
    /**
     * Checkbox toggle.
     */
    class Checkbox(
        x: Int, y: Int,
        var text: String = "",
        var checked: Boolean = false,
        var textColor: Int = 0x000000,
        var checkColor: Int = 0x3366CC
    ) : Widget(x, y, text.length + 4, 1) {
        
        var onChange: ((Boolean) -> Unit)? = null
        
        override fun draw() {
            if (!visible) return
            
            val box = if (checked) "☑" else "☐"
            
            Screen.setForeground(if (checked) checkColor else textColor)
            Screen.set(x, y, box)
            
            if (text.isNotEmpty()) {
                Screen.setForeground(textColor)
                Screen.set(x + 2, y, text)
            }
        }
        
        fun toggle() {
            checked = !checked
            onChange?.invoke(checked)
        }
    }
    
    // ==================== Switch ====================
    
    /**
     * Toggle switch.
     */
    class Switch(
        x: Int, y: Int,
        var state: Boolean = false,
        var activeColor: Int = 0x3366CC,
        var inactiveColor: Int = 0x878787,
        var handleColor: Int = 0xFFFFFF
    ) : Widget(x, y, 4, 1) {
        
        var onChange: ((Boolean) -> Unit)? = null
        
        override fun draw() {
            if (!visible) return
            
            val bgColor = if (state) activeColor else inactiveColor
            Screen.setBackground(bgColor)
            Screen.fill(x, y, 4, 1, ' ')
            
            Screen.setBackground(handleColor)
            val handleX = if (state) x + 2 else x
            Screen.fill(handleX, y, 2, 1, ' ')
        }
        
        fun toggle() {
            state = !state
            onChange?.invoke(state)
        }
    }
    
    // ==================== Helper Functions ====================
    
    /**
     * Calculate aligned coordinates.
     */
    fun getAlignmentCoordinates(
        x: Int, y: Int, width1: Int, height1: Int,
        horizontalAlignment: Int, verticalAlignment: Int,
        width2: Int, height2: Int
    ): Pair<Int, Int> {
        val ax = when (horizontalAlignment) {
            ALIGNMENT_HORIZONTAL_CENTER -> x + width1 / 2 - width2 / 2
            ALIGNMENT_HORIZONTAL_RIGHT -> x + width1 - width2
            else -> x
        }
        val ay = when (verticalAlignment) {
            ALIGNMENT_VERTICAL_CENTER -> y + height1 / 2 - height2 / 2
            ALIGNMENT_VERTICAL_BOTTOM -> y + height1 - height2
            else -> y
        }
        return ax to ay
    }
    
    /**
     * Create a basic object.
     */
    fun createObject(x: Int, y: Int, width: Int, height: Int): Widget {
        return Widget(x, y, width, height)
    }
    
    /**
     * Create a container.
     */
    fun createContainer(x: Int, y: Int, width: Int, height: Int): Container {
        return Container(x, y, width, height)
    }
    
    /**
     * Create a fullscreen workspace.
     */
    fun createWorkspace(): Container {
        val (w, h) = Screen.getResolution()
        return Container(1, 1, w, h)
    }
}

package li.cil.oc.client.os.libs

import java.util.concurrent.ConcurrentHashMap

/**
 * Keyboard library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Keyboard.lua.
 * Provides key code constants and keyboard state tracking.
 */
object Keyboard {
    
    // ==================== Control Keys ====================
    const val LEFT_CONTROL = 29
    const val RIGHT_CONTROL = 157
    const val LEFT_SHIFT = 42
    const val RIGHT_SHIFT = 54
    const val LEFT_ALT = 56
    const val RIGHT_ALT = 184
    const val COMMAND_KEY = 219
    const val WINDOWS_KEY = 219
    
    // ==================== Number Keys (Top Row) ====================
    const val ONE = 2
    const val TWO = 3
    const val THREE = 4
    const val FOUR = 5
    const val FIVE = 6
    const val SIX = 7
    const val SEVEN = 8
    const val EIGHT = 9
    const val NINE = 10
    const val ZERO = 11
    
    // ==================== Letter Keys ====================
    const val A = 30
    const val B = 48
    const val C = 46
    const val D = 32
    const val E = 18
    const val F = 33
    const val G = 34
    const val H = 35
    const val I = 23
    const val J = 36
    const val K = 37
    const val L = 38
    const val M = 50
    const val N = 49
    const val O = 24
    const val P = 25
    const val Q = 16
    const val R = 19
    const val S = 31
    const val T = 20
    const val U = 22
    const val V = 47
    const val W = 17
    const val X = 45
    const val Y = 21
    const val Z = 44
    
    // ==================== Function Keys ====================
    const val F1 = 59
    const val F2 = 60
    const val F3 = 61
    const val F4 = 62
    const val F5 = 63
    const val F6 = 64
    const val F7 = 65
    const val F8 = 66
    const val F9 = 67
    const val F10 = 68
    const val F11 = 87
    const val F12 = 88
    
    // ==================== Misc Keys ====================
    const val ESCAPE = 1
    const val MINUS = 12
    const val PLUS = 13
    const val EQUALS = 13
    const val BACKSPACE = 14
    const val TAB = 15
    const val OPEN_BRACKET = 26
    const val CLOSE_BRACKET = 27
    const val ENTER = 28
    const val SEMICOLON = 39
    const val QUOTE = 40
    const val BACK_QUOTE = 41
    const val BACKSLASH = 43
    const val COMMA = 51
    const val PERIOD = 52
    const val DOT = 52
    const val SLASH = 53
    const val SPACE = 57
    const val CAPS_LOCK = 58
    
    // ==================== Arrow Keys ====================
    const val UP = 200
    const val DOWN = 208
    const val LEFT = 203
    const val RIGHT = 205
    
    // ==================== Navigation Keys ====================
    const val INSERT = 210
    const val DELETE = 211
    const val HOME = 199
    const val END = 207
    const val PAGE_UP = 201
    const val PAGE_DOWN = 209
    
    // ==================== Numpad Keys ====================
    const val NUMPAD_0 = 82
    const val NUMPAD_1 = 79
    const val NUMPAD_2 = 80
    const val NUMPAD_3 = 81
    const val NUMPAD_4 = 75
    const val NUMPAD_5 = 76
    const val NUMPAD_6 = 77
    const val NUMPAD_7 = 71
    const val NUMPAD_8 = 72
    const val NUMPAD_9 = 73
    const val NUMPAD_MULTIPLY = 55
    const val NUMPAD_SUBTRACT = 74
    const val NUMPAD_ADD = 78
    const val NUMPAD_DECIMAL = 83
    const val NUMPAD_ENTER = 156
    const val NUMPAD_DIVIDE = 181
    const val NUM_LOCK = 69
    const val SCROLL_LOCK = 70
    
    // ==================== State Tracking ====================
    private val pressedKeys = ConcurrentHashMap<Int, Boolean>()
    
    /**
     * Check if a key is currently pressed.
     */
    fun isKeyDown(code: Int): Boolean {
        return pressedKeys[code] == true
    }
    
    /**
     * Check if the character is a control character.
     */
    fun isControl(code: Int): Boolean {
        return code < 32 || (code in 127..159)
    }
    
    /**
     * Check if Alt is pressed.
     */
    fun isAltDown(): Boolean {
        return isKeyDown(LEFT_ALT) || isKeyDown(RIGHT_ALT)
    }
    
    /**
     * Check if Control is pressed.
     */
    fun isControlDown(): Boolean {
        return isKeyDown(LEFT_CONTROL) || isKeyDown(RIGHT_CONTROL)
    }
    
    /**
     * Check if Shift is pressed.
     */
    fun isShiftDown(): Boolean {
        return isKeyDown(LEFT_SHIFT) || isKeyDown(RIGHT_SHIFT)
    }
    
    /**
     * Called when a key is pressed.
     */
    fun onKeyDown(code: Int) {
        pressedKeys[code] = true
    }
    
    /**
     * Called when a key is released.
     */
    fun onKeyUp(code: Int) {
        pressedKeys.remove(code)
    }
    
    /**
     * Clear all pressed keys.
     */
    fun clearState() {
        pressedKeys.clear()
    }
    
    /**
     * Get all currently pressed keys.
     */
    fun getPressedKeys(): Set<Int> {
        return pressedKeys.keys.toSet()
    }
    
    /**
     * Convert key code to name.
     */
    fun getKeyName(code: Int): String {
        return keyNames[code] ?: "KEY_$code"
    }
    
    /**
     * Convert key name to code.
     */
    fun getKeyCode(name: String): Int? {
        return keyNames.entries.find { it.value == name }?.key
    }
    
    private val keyNames = mapOf(
        ESCAPE to "ESCAPE",
        ONE to "1", TWO to "2", THREE to "3", FOUR to "4", FIVE to "5",
        SIX to "6", SEVEN to "7", EIGHT to "8", NINE to "9", ZERO to "0",
        A to "A", B to "B", C to "C", D to "D", E to "E", F to "F", G to "G",
        H to "H", I to "I", J to "J", K to "K", L to "L", M to "M", N to "N",
        O to "O", P to "P", Q to "Q", R to "R", S to "S", T to "T", U to "U",
        V to "V", W to "W", X to "X", Y to "Y", Z to "Z",
        F1 to "F1", F2 to "F2", F3 to "F3", F4 to "F4", F5 to "F5", F6 to "F6",
        F7 to "F7", F8 to "F8", F9 to "F9", F10 to "F10", F11 to "F11", F12 to "F12",
        SPACE to "SPACE", ENTER to "ENTER", BACKSPACE to "BACKSPACE", TAB to "TAB",
        UP to "UP", DOWN to "DOWN", LEFT to "LEFT", RIGHT to "RIGHT",
        LEFT_SHIFT to "LSHIFT", RIGHT_SHIFT to "RSHIFT",
        LEFT_CONTROL to "LCTRL", RIGHT_CONTROL to "RCTRL",
        LEFT_ALT to "LALT", RIGHT_ALT to "RALT"
    )
}

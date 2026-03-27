package li.cil.oc.client.os.libs

/**
 * Big letters library for SkibidiOS2.
 * Compatible with SkibidiLuaOS BigLetters.lua.
 * Renders large ASCII art text for displays.
 */
object BigLetters {
    
    /**
     * Font data: each character is a list of strings forming its shape.
     * Standard height: 5 rows.
     */
    private val font5x5 = mapOf(
        'A' to listOf(
            " ███ ",
            "█   █",
            "█████",
            "█   █",
            "█   █"
        ),
        'B' to listOf(
            "████ ",
            "█   █",
            "████ ",
            "█   █",
            "████ "
        ),
        'C' to listOf(
            " ████",
            "█    ",
            "█    ",
            "█    ",
            " ████"
        ),
        'D' to listOf(
            "████ ",
            "█   █",
            "█   █",
            "█   █",
            "████ "
        ),
        'E' to listOf(
            "█████",
            "█    ",
            "███  ",
            "█    ",
            "█████"
        ),
        'F' to listOf(
            "█████",
            "█    ",
            "███  ",
            "█    ",
            "█    "
        ),
        'G' to listOf(
            " ████",
            "█    ",
            "█  ██",
            "█   █",
            " ████"
        ),
        'H' to listOf(
            "█   █",
            "█   █",
            "█████",
            "█   █",
            "█   █"
        ),
        'I' to listOf(
            "█████",
            "  █  ",
            "  █  ",
            "  █  ",
            "█████"
        ),
        'J' to listOf(
            "█████",
            "   █ ",
            "   █ ",
            "█  █ ",
            " ██  "
        ),
        'K' to listOf(
            "█   █",
            "█  █ ",
            "███  ",
            "█  █ ",
            "█   █"
        ),
        'L' to listOf(
            "█    ",
            "█    ",
            "█    ",
            "█    ",
            "█████"
        ),
        'M' to listOf(
            "█   █",
            "██ ██",
            "█ █ █",
            "█   █",
            "█   █"
        ),
        'N' to listOf(
            "█   █",
            "██  █",
            "█ █ █",
            "█  ██",
            "█   █"
        ),
        'O' to listOf(
            " ███ ",
            "█   █",
            "█   █",
            "█   █",
            " ███ "
        ),
        'P' to listOf(
            "████ ",
            "█   █",
            "████ ",
            "█    ",
            "█    "
        ),
        'Q' to listOf(
            " ███ ",
            "█   █",
            "█ █ █",
            "█  █ ",
            " ██ █"
        ),
        'R' to listOf(
            "████ ",
            "█   █",
            "████ ",
            "█  █ ",
            "█   █"
        ),
        'S' to listOf(
            " ████",
            "█    ",
            " ███ ",
            "    █",
            "████ "
        ),
        'T' to listOf(
            "█████",
            "  █  ",
            "  █  ",
            "  █  ",
            "  █  "
        ),
        'U' to listOf(
            "█   █",
            "█   █",
            "█   █",
            "█   █",
            " ███ "
        ),
        'V' to listOf(
            "█   █",
            "█   █",
            "█   █",
            " █ █ ",
            "  █  "
        ),
        'W' to listOf(
            "█   █",
            "█   █",
            "█ █ █",
            "██ ██",
            "█   █"
        ),
        'X' to listOf(
            "█   █",
            " █ █ ",
            "  █  ",
            " █ █ ",
            "█   █"
        ),
        'Y' to listOf(
            "█   █",
            " █ █ ",
            "  █  ",
            "  █  ",
            "  █  "
        ),
        'Z' to listOf(
            "█████",
            "   █ ",
            "  █  ",
            " █   ",
            "█████"
        ),
        '0' to listOf(
            " ███ ",
            "█  ██",
            "█ █ █",
            "██  █",
            " ███ "
        ),
        '1' to listOf(
            "  █  ",
            " ██  ",
            "  █  ",
            "  █  ",
            "█████"
        ),
        '2' to listOf(
            " ███ ",
            "█   █",
            "  ██ ",
            " █   ",
            "█████"
        ),
        '3' to listOf(
            "████ ",
            "    █",
            " ███ ",
            "    █",
            "████ "
        ),
        '4' to listOf(
            "█   █",
            "█   █",
            "█████",
            "    █",
            "    █"
        ),
        '5' to listOf(
            "█████",
            "█    ",
            "████ ",
            "    █",
            "████ "
        ),
        '6' to listOf(
            " ████",
            "█    ",
            "████ ",
            "█   █",
            " ███ "
        ),
        '7' to listOf(
            "█████",
            "    █",
            "   █ ",
            "  █  ",
            "  █  "
        ),
        '8' to listOf(
            " ███ ",
            "█   █",
            " ███ ",
            "█   █",
            " ███ "
        ),
        '9' to listOf(
            " ███ ",
            "█   █",
            " ████",
            "    █",
            "████ "
        ),
        ' ' to listOf(
            "     ",
            "     ",
            "     ",
            "     ",
            "     "
        ),
        '!' to listOf(
            "  █  ",
            "  █  ",
            "  █  ",
            "     ",
            "  █  "
        ),
        '?' to listOf(
            " ███ ",
            "█   █",
            "  ██ ",
            "     ",
            "  █  "
        ),
        '.' to listOf(
            "     ",
            "     ",
            "     ",
            "     ",
            "  █  "
        ),
        ',' to listOf(
            "     ",
            "     ",
            "     ",
            "  █  ",
            " █   "
        ),
        ':' to listOf(
            "     ",
            "  █  ",
            "     ",
            "  █  ",
            "     "
        ),
        ';' to listOf(
            "     ",
            "  █  ",
            "     ",
            "  █  ",
            " █   "
        ),
        '-' to listOf(
            "     ",
            "     ",
            "█████",
            "     ",
            "     "
        ),
        '+' to listOf(
            "     ",
            "  █  ",
            "█████",
            "  █  ",
            "     "
        ),
        '=' to listOf(
            "     ",
            "█████",
            "     ",
            "█████",
            "     "
        ),
        '/' to listOf(
            "    █",
            "   █ ",
            "  █  ",
            " █   ",
            "█    "
        ),
        '\\' to listOf(
            "█    ",
            " █   ",
            "  █  ",
            "   █ ",
            "    █"
        ),
        '(' to listOf(
            "  █  ",
            " █   ",
            " █   ",
            " █   ",
            "  █  "
        ),
        ')' to listOf(
            "  █  ",
            "   █ ",
            "   █ ",
            "   █ ",
            "  █  "
        ),
        '[' to listOf(
            " ██  ",
            " █   ",
            " █   ",
            " █   ",
            " ██  "
        ),
        ']' to listOf(
            "  ██ ",
            "   █ ",
            "   █ ",
            "   █ ",
            "  ██ "
        ),
        '<' to listOf(
            "   █ ",
            "  █  ",
            " █   ",
            "  █  ",
            "   █ "
        ),
        '>' to listOf(
            " █   ",
            "  █  ",
            "   █ ",
            "  █  ",
            " █   "
        ),
        '*' to listOf(
            "     ",
            "█ █ █",
            " ███ ",
            "█ █ █",
            "     "
        ),
        '#' to listOf(
            " █ █ ",
            "█████",
            " █ █ ",
            "█████",
            " █ █ "
        ),
        '@' to listOf(
            " ███ ",
            "█ ███",
            "█ █ █",
            "█ ██ ",
            " ████"
        ),
        '_' to listOf(
            "     ",
            "     ",
            "     ",
            "     ",
            "█████"
        ),
        '~' to listOf(
            "     ",
            " █   ",
            "█ █ █",
            "   █ ",
            "     "
        )
    )
    
    /**
     * Render text as big letters.
     * @param text Text to render
     * @param spacing Spacing between characters
     * @return List of strings, one for each row
     */
    fun render(text: String, spacing: Int = 1): List<String> {
        val chars = text.uppercase().toList()
        val height = 5
        val result = MutableList(height) { StringBuilder() }
        
        for ((index, char) in chars.withIndex()) {
            val glyph = font5x5[char] ?: font5x5['?'] ?: List(height) { "????" }
            
            for (row in 0 until height) {
                result[row].append(glyph[row])
                if (index < chars.lastIndex) {
                    result[row].append(" ".repeat(spacing))
                }
            }
        }
        
        return result.map { it.toString() }
    }
    
    /**
     * Render text as a single joined string with newlines.
     */
    fun renderString(text: String, spacing: Int = 1): String {
        return render(text, spacing).joinToString("\n")
    }
    
    /**
     * Get the width of rendered text.
     */
    fun getWidth(text: String, spacing: Int = 1): Int {
        if (text.isEmpty()) return 0
        val charWidths = text.uppercase().sumOf { char ->
            (font5x5[char]?.firstOrNull()?.length ?: 5)
        }
        return charWidths + (text.length - 1) * spacing
    }
    
    /**
     * Get the height of rendered text.
     */
    fun getHeight(): Int = 5
    
    /**
     * Render with custom fill and empty characters.
     */
    fun renderCustom(
        text: String,
        fillChar: Char = '█',
        emptyChar: Char = ' ',
        spacing: Int = 1
    ): List<String> {
        return render(text, spacing).map { line ->
            line.map { if (it == '█') fillChar else emptyChar }.joinToString("")
        }
    }
    
    /**
     * Render with border around text.
     */
    fun renderBoxed(
        text: String,
        spacing: Int = 1,
        padding: Int = 1,
        borderChar: Char = '#'
    ): List<String> {
        val inner = render(text, spacing)
        val width = inner.maxOfOrNull { it.length } ?: 0
        val totalWidth = width + padding * 2 + 2
        
        val result = mutableListOf<String>()
        
        // Top border
        result.add(borderChar.toString().repeat(totalWidth))
        
        // Top padding
        repeat(padding) {
            result.add(borderChar + " ".repeat(totalWidth - 2) + borderChar)
        }
        
        // Content
        for (line in inner) {
            val paddedLine = " ".repeat(padding) + line.padEnd(width) + " ".repeat(padding)
            result.add(borderChar + paddedLine + borderChar)
        }
        
        // Bottom padding
        repeat(padding) {
            result.add(borderChar + " ".repeat(totalWidth - 2) + borderChar)
        }
        
        // Bottom border
        result.add(borderChar.toString().repeat(totalWidth))
        
        return result
    }
    
    /**
     * Render centered text for a given width.
     */
    fun renderCentered(text: String, totalWidth: Int, spacing: Int = 1): List<String> {
        val rendered = render(text, spacing)
        val textWidth = rendered.maxOfOrNull { it.length } ?: 0
        val leftPadding = (totalWidth - textWidth) / 2
        
        return rendered.map { line ->
            " ".repeat(maxOf(0, leftPadding)) + line
        }
    }
    
    /**
     * Create a scrolling text effect (returns list of frames).
     */
    fun createScrollFrames(text: String, visibleWidth: Int, spacing: Int = 1): List<List<String>> {
        val fullRender = render(text + "   ", spacing)
        val totalWidth = fullRender.maxOfOrNull { it.length } ?: 0
        
        val frames = mutableListOf<List<String>>()
        for (offset in 0 until totalWidth) {
            val frame = fullRender.map { line ->
                val extended = line + line
                extended.substring(offset % extended.length).take(visibleWidth)
            }
            frames.add(frame)
        }
        
        return frames
    }
    
    /**
     * Available characters in the font.
     */
    fun availableChars(): Set<Char> = font5x5.keys
    
    /**
     * Check if a character is supported.
     */
    fun isSupported(char: Char): Boolean = char.uppercaseChar() in font5x5
    
    /**
     * Add or override a character in the font.
     * @param char The character to add
     * @param glyph List of 5 strings representing the character
     */
    private val customFont = mutableMapOf<Char, List<String>>()
    
    fun addChar(char: Char, glyph: List<String>) {
        require(glyph.size == 5) { "Glyph must have exactly 5 rows" }
        customFont[char.uppercaseChar()] = glyph
    }
    
    /**
     * Render digital clock format (HH:MM:SS or HH:MM).
     */
    fun renderTime(hours: Int, minutes: Int, seconds: Int? = null): List<String> {
        val timeStr = if (seconds != null) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", hours, minutes)
        }
        return render(timeStr)
    }
    
    /**
     * Font glyphs for digits only (smaller, 3x5).
     */
    private val font3x5 = mapOf(
        '0' to listOf("███", "█ █", "█ █", "█ █", "███"),
        '1' to listOf(" █ ", "██ ", " █ ", " █ ", "███"),
        '2' to listOf("███", "  █", "███", "█  ", "███"),
        '3' to listOf("███", "  █", "███", "  █", "███"),
        '4' to listOf("█ █", "█ █", "███", "  █", "  █"),
        '5' to listOf("███", "█  ", "███", "  █", "███"),
        '6' to listOf("███", "█  ", "███", "█ █", "███"),
        '7' to listOf("███", "  █", "  █", "  █", "  █"),
        '8' to listOf("███", "█ █", "███", "█ █", "███"),
        '9' to listOf("███", "█ █", "███", "  █", "███"),
        ':' to listOf("   ", " █ ", "   ", " █ ", "   ")
    )
    
    /**
     * Render with smaller 3x5 font (digits only).
     */
    fun renderSmall(text: String, spacing: Int = 1): List<String> {
        val chars = text.toList()
        val height = 5
        val result = MutableList(height) { StringBuilder() }
        
        for ((index, char) in chars.withIndex()) {
            val glyph = font3x5[char] ?: listOf("???", "???", "???", "???", "???")
            
            for (row in 0 until height) {
                result[row].append(glyph[row])
                if (index < chars.lastIndex) {
                    result[row].append(" ".repeat(spacing))
                }
            }
        }
        
        return result.map { it.toString() }
    }
}

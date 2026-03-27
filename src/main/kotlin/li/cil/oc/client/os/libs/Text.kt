package li.cil.oc.client.os.libs

/**
 * Text manipulation library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Text.lua
 */
object Text {
    
    /**
     * Trim whitespace from both ends of a string.
     */
    fun trim(text: String): String = text.trim()
    
    /**
     * Remove leading whitespace.
     */
    fun trimLeft(text: String): String = text.trimStart()
    
    /**
     * Remove trailing whitespace.
     */
    fun trimRight(text: String): String = text.trimEnd()
    
    /**
     * Wrap text to specified width.
     */
    fun wrap(text: String, width: Int): List<String> {
        if (width <= 0) return listOf(text)
        
        val lines = mutableListOf<String>()
        
        for (paragraph in text.split("\n")) {
            if (paragraph.isEmpty()) {
                lines.add("")
                continue
            }
            
            var line = ""
            for (word in paragraph.split(" ")) {
                if (line.isEmpty()) {
                    line = word
                } else if (line.length + 1 + word.length <= width) {
                    line += " $word"
                } else {
                    lines.add(line)
                    line = word
                }
            }
            if (line.isNotEmpty()) {
                lines.add(line)
            }
        }
        
        return lines
    }
    
    /**
     * Pad string on the left to reach target length.
     */
    fun padLeft(text: String, length: Int, char: Char = ' '): String {
        return if (text.length >= length) text
               else char.toString().repeat(length - text.length) + text
    }
    
    /**
     * Pad string on the right to reach target length.
     */
    fun padRight(text: String, length: Int, char: Char = ' '): String {
        return if (text.length >= length) text
               else text + char.toString().repeat(length - text.length)
    }
    
    /**
     * Center string with padding on both sides.
     */
    fun center(text: String, length: Int, char: Char = ' '): String {
        if (text.length >= length) return text
        val totalPad = length - text.length
        val leftPad = totalPad / 2
        val rightPad = totalPad - leftPad
        return char.toString().repeat(leftPad) + text + char.toString().repeat(rightPad)
    }
    
    /**
     * Split string by delimiter.
     */
    fun split(text: String, delimiter: String = " "): List<String> {
        return text.split(delimiter)
    }
    
    /**
     * Count occurrences of substring.
     */
    fun count(text: String, substring: String): Int {
        if (substring.isEmpty()) return 0
        var count = 0
        var index = 0
        while (true) {
            index = text.indexOf(substring, index)
            if (index < 0) break
            count++
            index += substring.length
        }
        return count
    }
    
    /**
     * Check if string starts with prefix.
     */
    fun startsWith(text: String, prefix: String): Boolean = text.startsWith(prefix)
    
    /**
     * Check if string ends with suffix.
     */
    fun endsWith(text: String, suffix: String): Boolean = text.endsWith(suffix)
    
    /**
     * Check if string contains substring.
     */
    fun contains(text: String, substring: String): Boolean = text.contains(substring)
    
    /**
     * Replace all occurrences of pattern with replacement.
     */
    fun replace(text: String, pattern: String, replacement: String): String {
        return text.replace(pattern, replacement)
    }
    
    /**
     * Convert to uppercase.
     */
    fun upper(text: String): String = text.uppercase()
    
    /**
     * Convert to lowercase.
     */
    fun lower(text: String): String = text.lowercase()
    
    /**
     * Capitalize first letter.
     */
    fun capitalize(text: String): String {
        return text.replaceFirstChar { it.uppercase() }
    }
    
    /**
     * Capitalize first letter of each word.
     */
    fun title(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }
    
    /**
     * Reverse the string.
     */
    fun reverse(text: String): String = text.reversed()
    
    /**
     * Get character at index.
     */
    fun charAt(text: String, index: Int): Char? {
        return if (index in text.indices) text[index] else null
    }
    
    /**
     * Get substring.
     */
    fun substring(text: String, start: Int, end: Int = text.length): String {
        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(safeStart, text.length)
        return text.substring(safeStart, safeEnd)
    }
    
    /**
     * Truncate string with ellipsis if too long.
     */
    fun truncate(text: String, maxLength: Int, suffix: String = "..."): String {
        return if (text.length <= maxLength) text
               else text.take(maxLength - suffix.length) + suffix
    }
    
    /**
     * Remove all whitespace.
     */
    fun removeWhitespace(text: String): String = text.replace(Regex("\\s+"), "")
    
    /**
     * Normalize whitespace (replace multiple spaces with single space).
     */
    fun normalizeWhitespace(text: String): String = text.replace(Regex("\\s+"), " ").trim()
    
    /**
     * Check if string is blank (empty or only whitespace).
     */
    fun isBlank(text: String): Boolean = text.isBlank()
    
    /**
     * Check if string contains only digits.
     */
    fun isDigits(text: String): Boolean = text.all { it.isDigit() }
    
    /**
     * Check if string contains only letters.
     */
    fun isAlpha(text: String): Boolean = text.all { it.isLetter() }
    
    /**
     * Check if string contains only alphanumeric characters.
     */
    fun isAlphanumeric(text: String): Boolean = text.all { it.isLetterOrDigit() }
    
    /**
     * Get Unicode code point at index.
     */
    fun codePointAt(text: String, index: Int): Int? {
        return if (index in text.indices) text[index].code else null
    }
    
    /**
     * Create string from code points.
     */
    fun fromCodePoints(vararg codePoints: Int): String {
        return codePoints.map { it.toChar() }.joinToString("")
    }
    
    /**
     * Escape special characters for display.
     */
    fun escape(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\"", "\\\"")
    }
    
    /**
     * Unescape special characters.
     */
    fun unescape(text: String): String {
        return text
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
    
    /**
     * Format string with placeholders (%s, %d, etc.)
     */
    fun format(template: String, vararg args: Any?): String {
        return String.format(template, *args)
    }
    
    /**
     * Calculate Levenshtein distance between two strings.
     */
    fun distance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        
        if (m == 0) return n
        if (n == 0) return m
        
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[m][n]
    }
}

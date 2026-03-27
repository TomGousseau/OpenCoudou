package li.cil.oc.client.os.libs

/**
 * JSON parsing and serialization for SkibidiOS2.
 * Compatible with SkibidiLuaOS JSON.lua
 */
object JSON {
    
    /**
     * Parse a JSON string into a Kotlin object.
     */
    fun parse(json: String): Any? {
        return JsonParser(json).parse()
    }
    
    /**
     * Serialize a Kotlin object to JSON string.
     */
    fun stringify(value: Any?, pretty: Boolean = false): String {
        return JsonSerializer(pretty).serialize(value)
    }
    
    /**
     * Parse JSON and return as Map (for objects).
     */
    @Suppress("UNCHECKED_CAST")
    fun parseObject(json: String): Map<String, Any?>? {
        return parse(json) as? Map<String, Any?>
    }
    
    /**
     * Parse JSON and return as List (for arrays).
     */
    @Suppress("UNCHECKED_CAST")
    fun parseArray(json: String): List<Any?>? {
        return parse(json) as? List<Any?>
    }
    
    /**
     * Check if string is valid JSON.
     */
    fun isValid(json: String): Boolean {
        return try {
            parse(json)
            true
        } catch (e: Exception) {
            false
        }
    }
}

private class JsonParser(private val json: String) {
    private var pos = 0
    
    fun parse(): Any? {
        skipWhitespace()
        return parseValue()
    }
    
    private fun parseValue(): Any? {
        skipWhitespace()
        if (pos >= json.length) return null
        
        return when (json[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            else -> parseNumber()
        }
    }
    
    private fun parseObject(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        pos++ // skip '{'
        skipWhitespace()
        
        if (json[pos] == '}') {
            pos++
            return map
        }
        
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            skipWhitespace()
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            
            when (json[pos]) {
                ',' -> pos++
                '}' -> { pos++; return map }
                else -> throw JsonException("Expected ',' or '}'")
            }
        }
    }
    
    private fun parseArray(): List<Any?> {
        val list = mutableListOf<Any?>()
        pos++ // skip '['
        skipWhitespace()
        
        if (json[pos] == ']') {
            pos++
            return list
        }
        
        while (true) {
            skipWhitespace()
            list.add(parseValue())
            skipWhitespace()
            
            when (json[pos]) {
                ',' -> pos++
                ']' -> { pos++; return list }
                else -> throw JsonException("Expected ',' or ']'")
            }
        }
    }
    
    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        
        while (pos < json.length && json[pos] != '"') {
            if (json[pos] == '\\') {
                pos++
                when (json[pos]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        val hex = json.substring(pos + 1, pos + 5)
                        sb.append(hex.toInt(16).toChar())
                        pos += 4
                    }
                }
            } else {
                sb.append(json[pos])
            }
            pos++
        }
        
        expect('"')
        return sb.toString()
    }
    
    private fun parseNumber(): Number {
        val start = pos
        if (json[pos] == '-') pos++
        
        while (pos < json.length && json[pos].isDigit()) pos++
        
        if (pos < json.length && json[pos] == '.') {
            pos++
            while (pos < json.length && json[pos].isDigit()) pos++
        }
        
        if (pos < json.length && (json[pos] == 'e' || json[pos] == 'E')) {
            pos++
            if (json[pos] == '+' || json[pos] == '-') pos++
            while (pos < json.length && json[pos].isDigit()) pos++
        }
        
        val numStr = json.substring(start, pos)
        return if ('.' in numStr || 'e' in numStr || 'E' in numStr) {
            numStr.toDouble()
        } else {
            val long = numStr.toLong()
            if (long in Int.MIN_VALUE..Int.MAX_VALUE) long.toInt() else long
        }
    }
    
    private fun parseBoolean(): Boolean {
        return if (json.substring(pos).startsWith("true")) {
            pos += 4
            true
        } else if (json.substring(pos).startsWith("false")) {
            pos += 5
            false
        } else {
            throw JsonException("Expected boolean")
        }
    }
    
    private fun parseNull(): Any? {
        if (json.substring(pos).startsWith("null")) {
            pos += 4
            return null
        }
        throw JsonException("Expected null")
    }
    
    private fun skipWhitespace() {
        while (pos < json.length && json[pos].isWhitespace()) pos++
    }
    
    private fun expect(c: Char) {
        if (pos >= json.length || json[pos] != c) {
            throw JsonException("Expected '$c' at position $pos")
        }
        pos++
    }
}

private class JsonSerializer(private val pretty: Boolean) {
    private var indent = 0
    
    fun serialize(value: Any?): String {
        return serializeValue(value)
    }
    
    private fun serializeValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is Boolean -> value.toString()
            is Number -> serializeNumber(value)
            is String -> serializeString(value)
            is Map<*, *> -> serializeObject(value)
            is List<*> -> serializeArray(value)
            is Array<*> -> serializeArray(value.toList())
            else -> serializeString(value.toString())
        }
    }
    
    private fun serializeNumber(n: Number): String {
        return if (n is Double || n is Float) {
            if (n.toDouble().isInfinite() || n.toDouble().isNaN()) "null"
            else n.toString()
        } else n.toString()
    }
    
    private fun serializeString(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 32) {
                    sb.append("\\u%04x".format(c.code))
                } else sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }
    
    private fun serializeObject(map: Map<*, *>): String {
        if (map.isEmpty()) return "{}"
        
        val sb = StringBuilder("{")
        indent++
        
        val entries = map.entries.toList()
        for ((i, entry) in entries.withIndex()) {
            if (pretty) sb.append("\n").append("  ".repeat(indent))
            sb.append(serializeString(entry.key.toString()))
            sb.append(if (pretty) ": " else ":")
            sb.append(serializeValue(entry.value))
            if (i < entries.size - 1) sb.append(",")
        }
        
        indent--
        if (pretty) sb.append("\n").append("  ".repeat(indent))
        sb.append("}")
        return sb.toString()
    }
    
    private fun serializeArray(list: List<*>): String {
        if (list.isEmpty()) return "[]"
        
        val sb = StringBuilder("[")
        indent++
        
        for ((i, item) in list.withIndex()) {
            if (pretty) sb.append("\n").append("  ".repeat(indent))
            sb.append(serializeValue(item))
            if (i < list.size - 1) sb.append(",")
        }
        
        indent--
        if (pretty) sb.append("\n").append("  ".repeat(indent))
        sb.append("]")
        return sb.toString()
    }
}

class JsonException(message: String) : Exception(message)

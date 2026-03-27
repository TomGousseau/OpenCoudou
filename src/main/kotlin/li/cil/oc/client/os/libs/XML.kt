package li.cil.oc.client.os.libs

/**
 * XML parsing and serialization for SkibidiOS2.
 * Compatible with SkibidiLuaOS XML.lua
 */
object XML {
    
    /**
     * Parse XML string into an XmlNode tree.
     */
    fun parse(xml: String): XmlNode? {
        return try {
            XmlParser(xml).parse()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Serialize an XmlNode to string.
     */
    fun stringify(node: XmlNode, pretty: Boolean = false): String {
        return XmlSerializer(pretty).serialize(node)
    }
    
    /**
     * Create an XML element.
     */
    fun element(name: String, attributes: Map<String, String> = emptyMap(), vararg children: Any): XmlNode {
        val node = XmlNode(name, attributes.toMutableMap())
        for (child in children) {
            when (child) {
                is XmlNode -> node.children.add(child)
                is String -> node.text = (node.text ?: "") + child
            }
        }
        return node
    }
    
    /**
     * Escape special XML characters.
     */
    fun escape(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
    
    /**
     * Unescape XML entities.
     */
    fun unescape(text: String): String {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }
}

/**
 * Represents an XML node.
 */
data class XmlNode(
    val name: String,
    val attributes: MutableMap<String, String> = mutableMapOf(),
    val children: MutableList<XmlNode> = mutableListOf(),
    var text: String? = null
) {
    /**
     * Get attribute value.
     */
    operator fun get(attr: String): String? = attributes[attr]
    
    /**
     * Set attribute value.
     */
    operator fun set(attr: String, value: String) {
        attributes[attr] = value
    }
    
    /**
     * Find first child with given name.
     */
    fun find(childName: String): XmlNode? {
        return children.find { it.name == childName }
    }
    
    /**
     * Find all children with given name.
     */
    fun findAll(childName: String): List<XmlNode> {
        return children.filter { it.name == childName }
    }
    
    /**
     * Get text content (including nested text).
     */
    fun textContent(): String {
        val sb = StringBuilder()
        text?.let { sb.append(it) }
        children.forEach { sb.append(it.textContent()) }
        return sb.toString()
    }
    
    /**
     * Navigate using XPath-like syntax.
     * e.g., "root/child/grandchild"
     */
    fun query(path: String): XmlNode? {
        val parts = path.split("/").filter { it.isNotEmpty() }
        var current: XmlNode? = this
        
        for (part in parts) {
            current = current?.find(part) ?: return null
        }
        return current
    }
    
    /**
     * Add a child node.
     */
    fun addChild(child: XmlNode): XmlNode {
        children.add(child)
        return this
    }
    
    /**
     * Create and add a child with the given name.
     */
    fun addChild(name: String, text: String? = null): XmlNode {
        val child = XmlNode(name, mutableMapOf(), mutableListOf(), text)
        children.add(child)
        return child
    }
}

private class XmlParser(private val xml: String) {
    private var pos = 0
    
    fun parse(): XmlNode? {
        skipWhitespace()
        skipProlog()
        skipWhitespace()
        return parseElement()
    }
    
    private fun skipProlog() {
        // Skip XML declaration and doctype
        while (pos < xml.length) {
            skipWhitespace()
            if (xml.substring(pos).startsWith("<?")) {
                val end = xml.indexOf("?>", pos)
                if (end != -1) pos = end + 2
            } else if (xml.substring(pos).startsWith("<!")) {
                val end = xml.indexOf(">", pos)
                if (end != -1) pos = end + 1
            } else {
                break
            }
        }
    }
    
    private fun parseElement(): XmlNode? {
        skipWhitespace()
        if (pos >= xml.length || xml[pos] != '<') return null
        if (xml.getOrNull(pos + 1) == '/') return null
        
        pos++ // skip '<'
        val name = parseName()
        val attributes = mutableMapOf<String, String>()
        
        // Parse attributes
        while (true) {
            skipWhitespace()
            if (xml[pos] == '/' || xml[pos] == '>') break
            
            val attrName = parseName()
            skipWhitespace()
            expect('=')
            skipWhitespace()
            val attrValue = parseQuotedString()
            attributes[attrName] = XML.unescape(attrValue)
        }
        
        val node = XmlNode(name, attributes)
        
        // Self-closing tag
        if (xml[pos] == '/') {
            pos++
            expect('>')
            return node
        }
        
        expect('>')
        
        // Parse content
        while (pos < xml.length) {
            skipWhitespace()
            
            if (xml.substring(pos).startsWith("</")) {
                // Closing tag
                pos += 2
                val closingName = parseName()
                expect('>')
                if (closingName != name) {
                    throw XmlException("Mismatched tags: $name vs $closingName")
                }
                return node
            } else if (xml[pos] == '<') {
                // Child element
                val child = parseElement()
                if (child != null) {
                    node.children.add(child)
                }
            } else {
                // Text content
                val text = parseText()
                if (text.isNotBlank()) {
                    node.text = (node.text ?: "") + XML.unescape(text)
                }
            }
        }
        
        return node
    }
    
    private fun parseName(): String {
        val start = pos
        while (pos < xml.length && (xml[pos].isLetterOrDigit() || xml[pos] in ":-_")) {
            pos++
        }
        return xml.substring(start, pos)
    }
    
    private fun parseQuotedString(): String {
        val quote = xml[pos]
        if (quote != '"' && quote != '\'') {
            throw XmlException("Expected quote")
        }
        pos++
        val start = pos
        while (pos < xml.length && xml[pos] != quote) {
            pos++
        }
        val value = xml.substring(start, pos)
        pos++ // skip closing quote
        return value
    }
    
    private fun parseText(): String {
        val start = pos
        while (pos < xml.length && xml[pos] != '<') {
            pos++
        }
        return xml.substring(start, pos).trim()
    }
    
    private fun skipWhitespace() {
        while (pos < xml.length && xml[pos].isWhitespace()) {
            pos++
        }
    }
    
    private fun expect(c: Char) {
        if (pos >= xml.length || xml[pos] != c) {
            throw XmlException("Expected '$c' at $pos")
        }
        pos++
    }
}

private class XmlSerializer(private val pretty: Boolean) {
    private var indent = 0
    
    fun serialize(node: XmlNode): String {
        val sb = StringBuilder()
        if (indent == 0) {
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            if (pretty) sb.append("\n")
        }
        serializeNode(node, sb)
        return sb.toString()
    }
    
    private fun serializeNode(node: XmlNode, sb: StringBuilder) {
        if (pretty && indent > 0) {
            sb.append("  ".repeat(indent))
        }
        
        sb.append("<${node.name}")
        
        for ((key, value) in node.attributes) {
            sb.append(" $key=\"${XML.escape(value)}\"")
        }
        
        if (node.children.isEmpty() && node.text.isNullOrEmpty()) {
            sb.append("/>")
            if (pretty) sb.append("\n")
            return
        }
        
        sb.append(">")
        
        if (node.children.isNotEmpty()) {
            if (pretty) sb.append("\n")
            indent++
            for (child in node.children) {
                serializeNode(child, sb)
            }
            indent--
            if (pretty) sb.append("  ".repeat(indent))
        } else if (node.text != null) {
            sb.append(XML.escape(node.text!!))
        }
        
        sb.append("</${node.name}>")
        if (pretty) sb.append("\n")
    }
}

class XmlException(message: String) : Exception(message)

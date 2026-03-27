package li.cil.oc.client.os.browser

import li.cil.oc.common.blockentity.WebDisplayBlockEntity
import li.cil.oc.client.os.network.NetworkStack
import li.cil.oc.client.os.network.NetworkResult
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that bridges WebDisplay blocks with browser rendering capabilities.
 * This allows WebDisplay blocks to use the same rendering as the SkibidiOS2 browser.
 */
object WebDisplayService {
    
    private val activeDisplays = ConcurrentHashMap<BlockPos, WebDisplayState>()
    private val networkStack = NetworkStack()
    
    data class WebDisplayState(
        val pos: BlockPos,
        var url: String = "about:home",
        var title: String = "Web Display",
        var content: List<String> = listOf(""),
        var loading: Boolean = false,
        var scrollOffset: Int = 0,
        var lastUpdate: Long = 0
    )
    
    /**
     * Register a web display at the given position.
     */
    fun registerDisplay(pos: BlockPos): WebDisplayState {
        val state = WebDisplayState(pos)
        activeDisplays[pos] = state
        return state
    }
    
    /**
     * Unregister a web display.
     */
    fun unregisterDisplay(pos: BlockPos) {
        activeDisplays.remove(pos)
    }
    
    /**
     * Get the state of a display.
     */
    fun getDisplay(pos: BlockPos): WebDisplayState? {
        return activeDisplays[pos]
    }
    
    /**
     * Navigate a display to a URL.
     */
    fun navigateTo(pos: BlockPos, url: String) {
        val state = activeDisplays[pos] ?: registerDisplay(pos)
        var targetUrl = url.trim()
        
        // Handle special URLs
        when {
            targetUrl.isEmpty() || targetUrl == "about:blank" -> {
                state.url = "about:blank"
                state.title = "Blank"
                state.content = listOf("")
                state.loading = false
                syncToBlockEntity(pos, state)
                return
            }
            targetUrl == "about:home" -> {
                showHomePage(state)
                syncToBlockEntity(pos, state)
                return
            }
            !targetUrl.contains("://") -> {
                targetUrl = if (targetUrl.contains(".")) {
                    "https://$targetUrl"
                } else {
                    "https://www.google.com/search?q=${java.net.URLEncoder.encode(targetUrl, "UTF-8")}"
                }
            }
        }
        
        state.url = targetUrl
        state.loading = true
        state.title = "Loading..."
        state.content = listOf("Loading $targetUrl...")
        syncToBlockEntity(pos, state)
        
        // Fetch the page
        networkStack.get(targetUrl) { result ->
            state.loading = false
            
            when (result) {
                is NetworkResult.Success -> {
                    val html = result.data.bodyAsString()
                    state.title = extractTitle(html)
                    state.content = renderHtml(html)
                    state.scrollOffset = 0
                }
                is NetworkResult.Failure -> {
                    state.title = "Error"
                    state.content = listOf(
                        "═══════════════════════════════════════",
                        "           ⚠ Error Loading Page",
                        "═══════════════════════════════════════",
                        "",
                        "URL: $targetUrl",
                        "",
                        "Error: ${result.error}",
                        "",
                        "Please check:",
                        "  • The URL is correct",
                        "  • You have internet access"
                    )
                }
                else -> {}
            }
            
            syncToBlockEntity(pos, state)
        }
    }
    
    /**
     * Handle scroll input.
     */
    fun scroll(pos: BlockPos, delta: Int) {
        val state = activeDisplays[pos] ?: return
        val maxScroll = maxOf(0, state.content.size - WebDisplayBlockEntity.SCREEN_HEIGHT)
        state.scrollOffset = (state.scrollOffset - delta).coerceIn(0, maxScroll)
        syncToBlockEntity(pos, state)
    }
    
    /**
     * Handle click input.
     */
    fun click(pos: BlockPos, x: Float, y: Float, button: Int) {
        val state = activeDisplays[pos] ?: return
        
        // Calculate which line was clicked
        val lineIndex = ((y * WebDisplayBlockEntity.SCREEN_HEIGHT).toInt() + state.scrollOffset)
            .coerceIn(0, state.content.size - 1)
        
        if (lineIndex < state.content.size) {
            val line = state.content[lineIndex]
            
            // Check if clicking on a link (simple pattern: [text](url))
            val linkMatch = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)").find(line)
            if (linkMatch != null && button == 0) {
                val url = linkMatch.groupValues[2]
                navigateTo(pos, url)
            }
        }
    }
    
    /**
     * Handle key input.
     */
    fun keyPress(pos: BlockPos, keyCode: Int, char: Char) {
        val state = activeDisplays[pos] ?: return
        
        when (keyCode) {
            265 -> scroll(pos, 3)  // Up arrow
            264 -> scroll(pos, -3) // Down arrow
            266 -> scroll(pos, WebDisplayBlockEntity.SCREEN_HEIGHT - 2) // Page Up
            267 -> scroll(pos, -(WebDisplayBlockEntity.SCREEN_HEIGHT - 2)) // Page Down
            268 -> { // Home
                state.scrollOffset = 0
                syncToBlockEntity(pos, state)
            }
            269 -> { // End
                state.scrollOffset = maxOf(0, state.content.size - WebDisplayBlockEntity.SCREEN_HEIGHT)
                syncToBlockEntity(pos, state)
            }
        }
    }
    
    private fun showHomePage(state: WebDisplayState) {
        state.url = "about:home"
        state.title = "Home"
        state.loading = false
        state.scrollOffset = 0
        
        state.content = listOf(
            "",
            "    ╔═══════════════════════════════════════════════════════════════╗",
            "    ║                                                               ║",
            "    ║           🌐  SkibidiOS2 Web Display  🌐                      ║",
            "    ║                                                               ║",
            "    ╚═══════════════════════════════════════════════════════════════╝",
            "",
            "    Right-click this block to set a URL.",
            "    Use Laser Pointer to click links.",
            "    Use Remote Keyboard to type.",
            "",
            "    Quick Links:",
            "    ─────────────",
            "    [Google](https://www.google.com)",
            "    [Wikipedia](https://www.wikipedia.org)",
            "    [GitHub](https://www.github.com)",
            "",
            "    Controls:",
            "    ───────────",
            "    • Right-click block → Open URL config",
            "    • Laser Pointer left-click → Click on page",
            "    • Laser Pointer scroll → Scroll page",
            "    • Remote Keyboard → Type text"
        )
    }
    
    private fun extractTitle(html: String): String {
        val match = Regex("<title[^>]*>([^<]*)</title>", RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.get(1)?.trim()?.take(50) ?: "Untitled"
    }
    
    private fun renderHtml(html: String): List<String> {
        val lines = mutableListOf<String>()
        val width = WebDisplayBlockEntity.SCREEN_WIDTH - 2
        
        // Simple HTML to text conversion
        var text = html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>|</div>|</li>|</tr>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<h[1-6][^>]*>", RegexOption.IGNORE_CASE), "\n═══ ")
            .replace(Regex("</h[1-6]>", RegexOption.IGNORE_CASE), " ═══\n")
            .replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "  • ")
            .replace(Regex("<hr[^>]*>", RegexOption.IGNORE_CASE), "\n${"─".repeat(width)}\n")
            .replace(Regex("<a[^>]*href=\"([^\"]*)\"[^>]*>([^<]*)</a>", RegexOption.IGNORE_CASE)) { match ->
                "[${match.groupValues[2]}](${match.groupValues[1]})"
            }
            .replace(Regex("<img[^>]*alt=\"([^\"]*)\"[^>]*>", RegexOption.IGNORE_CASE)) { match ->
                "[Image: ${match.groupValues[1]}]"
            }
            .replace(Regex("<img[^>]*>", RegexOption.IGNORE_CASE), "[Image]")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("&#(\\d+);")) { match ->
                match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: ""
            }
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\n\\s*\n\\s*\n+"), "\n\n")
            .trim()
        
        // Word wrap
        for (paragraph in text.split("\n")) {
            if (paragraph.isEmpty()) {
                lines.add("")
                continue
            }
            
            var line = ""
            for (word in paragraph.split(" ")) {
                if (line.length + word.length + 1 > width) {
                    lines.add(line)
                    line = word
                } else {
                    line = if (line.isEmpty()) word else "$line $word"
                }
            }
            if (line.isNotEmpty()) {
                lines.add(line)
            }
        }
        
        return lines
    }
    
    /**
     * Sync the display state to the block entity.
     */
    private fun syncToBlockEntity(pos: BlockPos, state: WebDisplayState) {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return
        
        minecraft.execute {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is WebDisplayBlockEntity) {
                // Update block entity from service state
                // Note: This requires adding public setters or a sync method to WebDisplayBlockEntity
            }
        }
    }
    
    /**
     * Tick all active displays for animations, etc.
     */
    fun tickDisplays() {
        val currentTime = System.currentTimeMillis()
        activeDisplays.values.forEach { state ->
            if (state.loading && currentTime - state.lastUpdate > 500) {
                state.lastUpdate = currentTime
                // Animate loading indicator
            }
        }
    }
}

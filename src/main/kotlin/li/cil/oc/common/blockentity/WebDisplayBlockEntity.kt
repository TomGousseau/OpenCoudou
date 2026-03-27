package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.client.os.browser.WebBrowser
import li.cil.oc.client.os.core.FrameBuffer
import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.network.NetworkStack
import li.cil.oc.client.os.network.NetworkResult
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Block entity for WebDisplay block.
 * Handles web page rendering, user input, and SkibidiOS2 browser integration.
 */
class WebDisplayBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.WEB_DISPLAY.get(), pos, state) {
    
    companion object {
        const val DEFAULT_URL = "about:home"
        const val SCREEN_WIDTH = 160
        const val SCREEN_HEIGHT = 100
    }
    
    // Display state
    var currentUrl: String = DEFAULT_URL
        private set
    var pageTitle: String = "Web Display"
        private set
    var isOn: Boolean = true
    var isLoading: Boolean = false
    
    // Frame buffer for rendering
    val frameBuffer = FrameBuffer(SCREEN_WIDTH, SCREEN_HEIGHT)
    
    // Rendered page content (lines of text for character-based display)
    var renderedContent: List<String> = listOf("SkibidiOS2 Web Display", "", "Right-click to set URL")
        private set
    
    // Scroll position
    var scrollOffset: Int = 0
        private set
    
    // Network stack for HTTP requests
    private val networkStack = NetworkStack()
    
    // Input queue for processing
    private val inputQueue = ConcurrentLinkedQueue<InputEvent>()
    
    // Last update time for animations
    private var lastUpdate: Long = 0
    
    // Users currently looking at this display (for sync)
    private val viewers = mutableSetOf<UUID>()
    
    sealed class InputEvent {
        data class Click(val x: Float, val y: Float, val button: Int) : InputEvent()
        data class Scroll(val delta: Int) : InputEvent()
        data class KeyPress(val keyCode: Int, val char: Char) : InputEvent()
        data class KeyRelease(val keyCode: Int) : InputEvent()
    }
    
    /**
     * Server tick - process input and update state.
     */
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        
        // Process input events
        while (inputQueue.isNotEmpty()) {
            val event = inputQueue.poll()
            when (event) {
                is InputEvent.Click -> processClick(event.x, event.y, event.button)
                is InputEvent.Scroll -> processScroll(event.delta)
                is InputEvent.KeyPress -> processKeyPress(event.keyCode, event.char)
                is InputEvent.KeyRelease -> { /* Handle key release if needed */ }
            }
        }
        
        // Update display periodically
        val currentTime = level.gameTime
        if (currentTime - lastUpdate >= 20) { // Every second
            lastUpdate = currentTime
            syncToClients()
        }
    }
    
    /**
     * Navigate to a URL.
     */
    fun navigateTo(url: String) {
        var targetUrl = url.trim()
        
        // Handle special URLs
        when {
            targetUrl.isEmpty() || targetUrl == "about:blank" -> {
                currentUrl = "about:blank"
                pageTitle = "Blank"
                renderedContent = listOf("")
                isLoading = false
                syncToClients()
                return
            }
            targetUrl == "about:home" -> {
                showHomePage()
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
        
        currentUrl = targetUrl
        isLoading = true
        renderedContent = listOf("Loading $targetUrl...")
        syncToClients()
        
        // Fetch the page
        networkStack.get(targetUrl) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    val html = result.data.bodyAsString()
                    pageTitle = extractTitle(html)
                    renderedContent = renderHtml(html)
                    isLoading = false
                    scrollOffset = 0
                    syncToClients()
                }
                is NetworkResult.Failure -> {
                    pageTitle = "Error"
                    renderedContent = listOf(
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
                        "  • The server has internet access"
                    )
                    isLoading = false
                    syncToClients()
                }
                else -> {}
            }
        }
    }
    
    private fun showHomePage() {
        currentUrl = "about:home"
        pageTitle = "Home"
        isLoading = false
        
        renderedContent = listOf(
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
            "    • https://www.google.com",
            "    • https://www.wikipedia.org",
            "    • https://www.github.com",
            "",
            "    Controls:",
            "    ───────────",
            "    • Right-click block → Open URL config",
            "    • Laser Pointer left-click → Click on page",
            "    • Laser Pointer right-click → Right-click menu",
            "    • Laser Pointer scroll → Scroll page",
            "    • Remote Keyboard → Type text"
        )
        
        syncToClients()
    }
    
    private fun extractTitle(html: String): String {
        val match = Regex("<title[^>]*>([^<]*)</title>", RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.get(1)?.trim()?.take(50) ?: "Untitled"
    }
    
    private fun renderHtml(html: String): List<String> {
        val lines = mutableListOf<String>()
        val width = SCREEN_WIDTH - 2
        
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
                "[${match.groupValues[2]}]"
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
     * Handle click input.
     */
    fun handleClick(x: Float, y: Float, button: Int) {
        inputQueue.add(InputEvent.Click(x, y, button))
    }
    
    private fun processClick(x: Float, y: Float, button: Int) {
        // Calculate line clicked based on y position and scroll
        val lineIndex = ((y * (SCREEN_HEIGHT - 1)).toInt() + scrollOffset).coerceIn(0, renderedContent.size - 1)
        
        // For now, just log the click - could be extended to handle link clicks
        // In a full implementation, we'd detect if click is on a link and navigate
    }
    
    /**
     * Handle scroll input.
     */
    fun handleScroll(delta: Int) {
        inputQueue.add(InputEvent.Scroll(delta))
    }
    
    private fun processScroll(delta: Int) {
        scrollOffset = (scrollOffset - delta).coerceIn(0, maxOf(0, renderedContent.size - SCREEN_HEIGHT))
        syncToClients()
    }
    
    /**
     * Handle key press input.
     */
    fun handleKeyPress(keyCode: Int, char: Char) {
        inputQueue.add(InputEvent.KeyPress(keyCode, char))
    }
    
    private fun processKeyPress(keyCode: Int, char: Char) {
        // Handle special keys
        when (keyCode) {
            265 -> processScroll(3)  // Up arrow - scroll up
            264 -> processScroll(-3) // Down arrow - scroll down
            266 -> processScroll(SCREEN_HEIGHT - 2) // Page Up
            267 -> processScroll(-(SCREEN_HEIGHT - 2)) // Page Down
            268 -> { scrollOffset = 0; syncToClients() } // Home
            269 -> { scrollOffset = maxOf(0, renderedContent.size - SCREEN_HEIGHT); syncToClients() } // End
        }
    }
    
    /**
     * Handle key release input.
     */
    fun handleKeyRelease(keyCode: Int) {
        inputQueue.add(InputEvent.KeyRelease(keyCode))
    }
    
    /**
     * Open configuration screen for setting URL.
     */
    fun openConfigScreen(player: ServerPlayer) {
        // Send packet to client to open URL input screen
        // For now, we'll use a simple chat-based input
        player.sendSystemMessage(Component.literal("§b[Web Display]§r Current URL: $currentUrl"))
        player.sendSystemMessage(Component.literal("§7Use command: /webdisplay seturl <url>"))
    }
    
    /**
     * Add a viewer tracking.
     */
    fun addViewer(player: Player) {
        viewers.add(player.uuid)
    }
    
    /**
     * Remove a viewer.
     */
    fun removeViewer(player: Player) {
        viewers.remove(player.uuid)
    }
    
    /**
     * Sync state to clients.
     */
    private fun syncToClients() {
        setChanged()
        level?.sendBlockUpdated(blockPos, blockState, blockState, 3)
    }
    
    // ==================== NBT Persistence ====================
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putString("url", currentUrl)
        tag.putString("title", pageTitle)
        tag.putBoolean("on", isOn)
        tag.putInt("scroll", scrollOffset)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        currentUrl = tag.getString("url").ifEmpty { DEFAULT_URL }
        pageTitle = tag.getString("title").ifEmpty { "Web Display" }
        isOn = tag.getBoolean("on")
        scrollOffset = tag.getInt("scroll")
        
        // Re-load the page
        if (currentUrl != DEFAULT_URL && currentUrl != "about:blank") {
            navigateTo(currentUrl)
        } else {
            showHomePage()
        }
    }
    
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag, registries)
        
        // Include rendered content for client display
        val contentTag = CompoundTag()
        renderedContent.forEachIndexed { index, line ->
            contentTag.putString(index.toString(), line)
        }
        contentTag.putInt("count", renderedContent.size)
        tag.put("content", contentTag)
        
        return tag
    }
    
    override fun handleUpdateTag(tag: CompoundTag, registries: HolderLookup.Provider) {
        loadAdditional(tag, registries)
        
        // Load rendered content from tag
        if (tag.contains("content")) {
            val contentTag = tag.getCompound("content")
            val count = contentTag.getInt("count")
            val content = mutableListOf<String>()
            for (i in 0 until count) {
                content.add(contentTag.getString(i.toString()))
            }
            renderedContent = content
        }
    }
    
    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }
}

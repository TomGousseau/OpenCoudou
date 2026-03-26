package li.cil.oc.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.OpenComputers
import li.cil.oc.common.container.*
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

/**
 * Base screen class for OpenComputers GUIs.
 */
abstract class OCScreen<T : OCContainerMenu>(
    menu: T,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<T>(menu, playerInventory, title) {
    
    companion object {
        val GUI_COMPONENTS = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/components.png")
        val GUI_BACKGROUND = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/background.png")
        val GUI_SLOTS = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/slots.png")
        
        // Common colors
        const val COLOR_TEXT_DARK = 0x404040
        const val COLOR_TEXT_LIGHT = 0xE0E0E0
        const val COLOR_ENERGY_LOW = 0xFF4040
        const val COLOR_ENERGY_MED = 0xFFFF40
        const val COLOR_ENERGY_HIGH = 0x40FF40
    }
    
    protected var backgroundTexture: ResourceLocation = GUI_BACKGROUND
    
    override fun init() {
        super.init()
        // Center the title
        titleLabelX = (imageWidth - font.width(title)) / 2
        titleLabelY = 6
        // Standard inventory label position
        inventoryLabelX = 8
        inventoryLabelY = imageHeight - 94
    }
    
    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        graphics.blit(backgroundTexture, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        super.render(graphics, mouseX, mouseY, partialTick)
        renderTooltip(graphics, mouseX, mouseY)
    }
    
    /**
     * Renders an energy bar.
     */
    protected fun renderEnergyBar(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        current: Int,
        max: Int
    ) {
        val percentage = if (max > 0) current.toFloat() / max.toFloat() else 0f
        val filledWidth = (width * percentage).toInt()
        
        // Background
        graphics.fill(x, y, x + width, y + height, 0xFF202020.toInt())
        
        // Energy fill
        val color = when {
            percentage < 0.25f -> COLOR_ENERGY_LOW
            percentage < 0.5f -> COLOR_ENERGY_MED
            else -> COLOR_ENERGY_HIGH
        }
        graphics.fill(x, y, x + filledWidth, y + height, 0xFF000000.toInt() or color)
        
        // Border
        graphics.renderOutline(x, y, width, height, 0xFF000000.toInt())
    }
    
    /**
     * Renders a progress bar.
     */
    protected fun renderProgressBar(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        progress: Float,
        color: Int = 0x4080FF
    ) {
        val filledWidth = (width * progress.coerceIn(0f, 1f)).toInt()
        
        graphics.fill(x, y, x + width, y + height, 0xFF303030.toInt())
        graphics.fill(x, y, x + filledWidth, y + height, 0xFF000000.toInt() or color)
        graphics.renderOutline(x, y, width, height, 0xFF000000.toInt())
    }
}

/**
 * Computer case screen.
 */
class CaseScreen(
    menu: CaseMenu,
    playerInventory: Inventory,
    title: Component
) : OCScreen<CaseMenu>(menu, playerInventory, title) {
    
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/case.png")
    }
    
    init {
        imageWidth = 176
        imageHeight = 222
        backgroundTexture = TEXTURE
    }
    
    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        super.renderBg(graphics, partialTick, mouseX, mouseY)
        
        // Energy bar
        renderEnergyBar(
            graphics,
            leftPos + 8, 
            topPos + 128, 
            160, 
            8,
            menu.energy,
            menu.maxEnergy
        )
        
        // Running indicator
        val statusColor = if (menu.isRunning) 0xFF40FF40.toInt() else 0xFFFF4040.toInt()
        graphics.fill(leftPos + 163, topPos + 7, leftPos + 169, topPos + 13, statusColor)
    }
    
    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        super.renderLabels(graphics, mouseX, mouseY)
        
        // Tier label
        graphics.drawString(font, "Tier ${menu.tier}", 8, 110, COLOR_TEXT_DARK, false)
        
        // Energy label
        val energyText = "${menu.energy} / ${menu.maxEnergy}"
        graphics.drawString(font, energyText, 8, 118, COLOR_TEXT_DARK, false)
    }
}

/**
 * Screen display screen (showing the actual text/graphics buffer).
 */
class ScreenScreen(
    menu: ScreenMenu,
    playerInventory: Inventory,
    title: Component
) : OCScreen<ScreenMenu>(menu, playerInventory, title) {
    
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/screen.png")
        
        // Screen configuration
        const val CHAR_WIDTH = 6
        const val CHAR_HEIGHT = 9
        const val SCREEN_PADDING = 4
    }
    
    private var screenWidth = 50  // Characters
    private var screenHeight = 16 // Characters
    private var screenBuffer: Array<CharArray>? = null
    private var foregroundColors: Array<IntArray>? = null
    private var backgroundColors: Array<IntArray>? = null
    
    init {
        // Calculate dynamic size based on screen resolution
        imageWidth = screenWidth * CHAR_WIDTH + SCREEN_PADDING * 2
        imageHeight = screenHeight * CHAR_HEIGHT + SCREEN_PADDING * 2
        backgroundTexture = TEXTURE
    }
    
    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // Draw screen background (black)
        graphics.fill(
            leftPos, 
            topPos, 
            leftPos + imageWidth, 
            topPos + imageHeight, 
            0xFF000000.toInt()
        )
        
        // Draw screen content
        renderScreenContent(graphics)
        
        // Draw border
        graphics.renderOutline(
            leftPos - 1, 
            topPos - 1, 
            imageWidth + 2, 
            imageHeight + 2, 
            0xFF404040.toInt()
        )
    }
    
    private fun renderScreenContent(graphics: GuiGraphics) {
        val buffer = screenBuffer ?: return
        val fgColors = foregroundColors ?: return
        val bgColors = backgroundColors ?: return
        
        for (y in buffer.indices) {
            for (x in buffer[y].indices) {
                val char = buffer[y][x]
                val fg = fgColors[y][x]
                val bg = bgColors[y][x]
                
                val screenX = leftPos + SCREEN_PADDING + x * CHAR_WIDTH
                val screenY = topPos + SCREEN_PADDING + y * CHAR_HEIGHT
                
                // Draw background
                if (bg != 0x000000) {
                    graphics.fill(
                        screenX, screenY,
                        screenX + CHAR_WIDTH, screenY + CHAR_HEIGHT,
                        0xFF000000.toInt() or bg
                    )
                }
                
                // Draw character
                if (char != ' ') {
                    graphics.drawString(
                        font, 
                        char.toString(), 
                        screenX, 
                        screenY, 
                        0xFF000000.toInt() or fg,
                        false
                    )
                }
            }
        }
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // Forward keyboard input to the network
        // Don't consume Escape key
        if (keyCode == 256) return super.keyPressed(keyCode, scanCode, modifiers)
        
        // Send key event
        li.cil.oc.common.network.NetworkHandler.sendKeyboardInput(
            menu.levelAccess.evaluate({ _, pos -> pos }, net.minecraft.core.BlockPos.ZERO),
            li.cil.oc.common.network.KeyboardInputType.KEY_DOWN,
            Char(keyCode),
            scanCode
        )
        
        return true
    }
    
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        li.cil.oc.common.network.NetworkHandler.sendKeyboardInput(
            menu.levelAccess.evaluate({ _, pos -> pos }, net.minecraft.core.BlockPos.ZERO),
            li.cil.oc.common.network.KeyboardInputType.KEY_UP,
            Char(keyCode),
            scanCode
        )
        
        return true
    }
    
    override fun charTyped(char: Char, modifiers: Int): Boolean {
        // Handle text input
        return super.charTyped(char, modifiers)
    }
    
    fun updateScreenBuffer(width: Int, height: Int, buffer: String) {
        screenWidth = width
        screenHeight = height
        
        screenBuffer = Array(height) { CharArray(width) { ' ' } }
        foregroundColors = Array(height) { IntArray(width) { 0xFFFFFF } }
        backgroundColors = Array(height) { IntArray(width) { 0x000000 } }
        
        // Parse buffer string and populate arrays
        // Format: each character followed by 6 bytes for fg/bg colors
        var index = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (index < buffer.length) {
                    screenBuffer!![y][x] = buffer[index++]
                }
            }
        }
    }
}

/**
 * Disk drive screen.
 */
class DiskDriveScreen(
    menu: DiskDriveMenu,
    playerInventory: Inventory,
    title: Component
) : OCScreen<DiskDriveMenu>(menu, playerInventory, title) {
    
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/disk_drive.png")
    }
    
    init {
        imageWidth = 176
        imageHeight = 166
        backgroundTexture = TEXTURE
    }
}

/**
 * RAID screen.
 */
class RaidScreen(
    menu: RaidMenu,
    playerInventory: Inventory,
    title: Component
) : OCScreen<RaidMenu>(menu, playerInventory, title) {
    
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/raid.png")
    }
    
    init {
        imageWidth = 176
        imageHeight = 166
        backgroundTexture = TEXTURE
    }
}

/**
 * Server rack screen.
 */
class RackScreen(
    menu: RackMenu,
    playerInventory: Inventory,
    title: Component
) : OCScreen<RackMenu>(menu, playerInventory, title) {
    
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/rack.png")
    }
    
    init {
        imageWidth = 176
        imageHeight = 186
        backgroundTexture = TEXTURE
    }
}

/**
 * Assembler screen.
 */
class AssemblerScreen(
    menu: AssemblerMenu,
    playerInventory: Inventory,
    title: Component
) : OCScreen<AssemblerMenu>(menu, playerInventory, title) {
    
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/assembler.png")
    }
    
    init {
        imageWidth = 176
        imageHeight = 202
        backgroundTexture = TEXTURE
    }
}

/**
 * Disassembler screen.
 */
class DisassemblerScreen(
    menu: DisassemblerMenu,
    playerInventory: Inventory,
    title: Component
) : OCScreen<DisassemblerMenu>(menu, playerInventory, title) {
    
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/disassembler.png")
    }
    
    init {
        imageWidth = 176
        imageHeight = 166
        backgroundTexture = TEXTURE
    }
}

/**
 * Charger screen.
 */
class ChargerScreen(
    menu: ChargerMenu,
    playerInventory: Inventory,
    title: Component
) : OCScreen<ChargerMenu>(menu, playerInventory, title) {
    
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/charger.png")
    }
    
    init {
        imageWidth = 176
        imageHeight = 166
        backgroundTexture = TEXTURE
    }
}

/**
 * Transposer screen.
 */
class TransposerScreen(
    menu: TransposerMenu,
    playerInventory: Inventory,
    title: Component
) : OCScreen<TransposerMenu>(menu, playerInventory, title) {
    
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/transposer.png")
    }
    
    init {
        imageWidth = 176
        imageHeight = 166
        backgroundTexture = TEXTURE
    }
}

/**
 * Printer screen.
 */
class PrinterScreen(
    menu: PrinterMenu,
    playerInventory: Inventory,
    title: Component
) : OCScreen<PrinterMenu>(menu, playerInventory, title) {
    
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/printer.png")
    }
    
    init {
        imageWidth = 176
        imageHeight = 166
        backgroundTexture = TEXTURE
    }
}

/**
 * Robot screen.
 */
class RobotScreen(
    menu: RobotMenu,
    playerInventory: Inventory,
    title: Component
) : OCScreen<RobotMenu>(menu, playerInventory, title) {
    
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/robot.png")
    }
    
    init {
        imageWidth = 176
        imageHeight = 186
        backgroundTexture = TEXTURE
    }
}

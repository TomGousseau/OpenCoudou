package li.cil.oc.client

import li.cil.oc.OpenComputers
import li.cil.oc.client.gui.*
import li.cil.oc.client.renderer.*
import li.cil.oc.common.container.*
import li.cil.oc.common.init.ModEntities
import li.cil.oc.common.init.ModMenus
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.client.renderer.entity.EntityRenderers
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent

/**
 * Client-side initialization and registration.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ClientSetup {
    
    @SubscribeEvent
    @JvmStatic
    fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            // Client-side setup that needs to run on main thread
            setupBlockColors()
            setupItemColors()
        }
    }
    
    @SubscribeEvent
    @JvmStatic
    fun registerMenuScreens(event: RegisterMenuScreensEvent) {
        event.register(ModMenus.CASE.get(), ::CaseScreen)
        event.register(ModMenus.SCREEN.get(), ::ScreenScreen)
        event.register(ModMenus.DISK_DRIVE.get(), ::DiskDriveScreen)
        event.register(ModMenus.RAID.get(), ::RaidScreen)
        event.register(ModMenus.RACK.get(), ::RackScreen)
        event.register(ModMenus.ASSEMBLER.get(), ::AssemblerScreen)
        event.register(ModMenus.DISASSEMBLER.get(), ::DisassemblerScreen)
        event.register(ModMenus.CHARGER.get(), ::ChargerScreen)
        event.register(ModMenus.TRANSPOSER.get(), ::TransposerScreen)
        event.register(ModMenus.PRINTER.get(), ::PrinterScreen)
        event.register(ModMenus.ROBOT.get(), ::RobotScreen)
    }
    
    @SubscribeEvent
    @JvmStatic
    fun registerEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(ModEntities.ROBOT.get(), ::RobotRenderer)
        event.registerEntityRenderer(ModEntities.DRONE.get(), ::DroneRenderer)
    }
    
    @SubscribeEvent
    @JvmStatic
    fun registerBlockEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        // Register block entity renderers
        event.registerBlockEntityRenderer(li.cil.oc.common.init.ModBlockEntities.WEB_DISPLAY.get(), ::WebDisplayRenderer)
    }
    
    @SubscribeEvent
    @JvmStatic
    fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(RobotModel.LAYER_LOCATION, RobotModel::createBodyLayer)
        event.registerLayerDefinition(DroneModel.LAYER_LOCATION, DroneModel::createBodyLayer)
    }
    
    private fun setupBlockColors() {
        // Register custom block colors for cables, screens, etc.
    }
    
    private fun setupItemColors() {
        // Register custom item colors for tiered items
    }
}

/**
 * Client-side rendering utilities.
 */
object RenderUtils {
    
    /**
     * Converts an OC color (0-15) to RGB.
     */
    fun ocColorToRgb(color: Int): Int {
        return when (color) {
            0 -> 0x000000 // Black
            1 -> 0xCC4C4C // Red
            2 -> 0x57A64E // Green
            3 -> 0x7F664C // Brown
            4 -> 0x3366CC // Blue
            5 -> 0xB266E5 // Purple
            6 -> 0x4C99B2 // Cyan
            7 -> 0x999999 // Light Gray
            8 -> 0x4C4C4C // Gray
            9 -> 0xF2B2CC // Pink
            10 -> 0x7FCC19 // Lime
            11 -> 0xDEDE6C // Yellow
            12 -> 0x99B2F2 // Light Blue
            13 -> 0xE57FD8 // Magenta
            14 -> 0xF2B233 // Orange
            15 -> 0xF0F0F0 // White
            else -> 0xFFFFFF
        }
    }
    
    /**
     * Converts RGB to the nearest OC color index.
     */
    fun rgbToOcColor(rgb: Int): Int {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        
        val palette = listOf(
            0x000000, 0xCC4C4C, 0x57A64E, 0x7F664C,
            0x3366CC, 0xB266E5, 0x4C99B2, 0x999999,
            0x4C4C4C, 0xF2B2CC, 0x7FCC19, 0xDEDE6C,
            0x99B2F2, 0xE57FD8, 0xF2B233, 0xF0F0F0
        )
        
        var bestIndex = 0
        var bestDistance = Int.MAX_VALUE
        
        for ((index, color) in palette.withIndex()) {
            val pr = (color shr 16) and 0xFF
            val pg = (color shr 8) and 0xFF
            val pb = color and 0xFF
            
            val dr = r - pr
            val dg = g - pg
            val db = b - pb
            val distance = dr * dr + dg * dg + db * db
            
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        
        return bestIndex
    }
    
    /**
     * Interpolates between two colors.
     */
    fun lerpColor(color1: Int, color2: Int, t: Float): Int {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        
        val r = (r1 + (r2 - r1) * t).toInt().coerceIn(0, 255)
        val g = (g1 + (g2 - g1) * t).toInt().coerceIn(0, 255)
        val b = (b1 + (b2 - b1) * t).toInt().coerceIn(0, 255)
        
        return (r shl 16) or (g shl 8) or b
    }
}

/**
 * Client-side keyboard handling.
 */
object KeyboardHandler {
    
    // Key code mappings
    private val MINECRAFT_TO_LWJGL = mapOf(
        256 to 1,   // ESC
        257 to 28,  // ENTER
        258 to 15,  // TAB
        259 to 14,  // BACKSPACE
        262 to 205, // RIGHT
        263 to 203, // LEFT
        264 to 208, // DOWN
        265 to 200, // UP
        266 to 201, // PAGE_UP
        267 to 209, // PAGE_DOWN
        268 to 199, // HOME
        269 to 207, // END
        280 to 58,  // CAPS_LOCK
        281 to 70,  // SCROLL_LOCK
        282 to 69,  // NUM_LOCK
        283 to 210, // INSERT
        284 to 99,  // F15
        290 to 59,  // F1
        291 to 60,  // F2
        292 to 61,  // F3
        293 to 62,  // F4
        294 to 63,  // F5
        295 to 64,  // F6
        296 to 65,  // F7
        297 to 66,  // F8
        298 to 67,  // F9
        299 to 68,  // F10
        300 to 87,  // F11
        301 to 88,  // F12
        340 to 42,  // LEFT_SHIFT
        341 to 29,  // LEFT_CONTROL
        342 to 56,  // LEFT_ALT
        344 to 54,  // RIGHT_SHIFT
        345 to 157, // RIGHT_CONTROL
        346 to 184  // RIGHT_ALT
    )
    
    /**
     * Converts GLFW key codes to LWJGL2 key codes (for Lua compatibility).
     */
    fun glfwToLwjgl(glfwCode: Int): Int {
        // Direct mapping for ASCII letters
        if (glfwCode in 65..90) { // A-Z
            return glfwCode - 65 + 30 // A=30 in LWJGL2
        }
        
        // Numbers
        if (glfwCode in 48..57) { // 0-9
            return if (glfwCode == 48) 11 else glfwCode - 48 + 1
        }
        
        // Space
        if (glfwCode == 32) return 57
        
        // Special keys
        return MINECRAFT_TO_LWJGL[glfwCode] ?: glfwCode
    }
    
    /**
     * Gets the character for a key press.
     */
    fun getCharacter(keyCode: Int, shiftHeld: Boolean): Char {
        // This is simplified - actual implementation would need more logic
        if (keyCode in 65..90) {
            return if (shiftHeld) keyCode.toChar() else (keyCode + 32).toChar()
        }
        if (keyCode == 32) return ' '
        return Char.MIN_VALUE
    }
}

/**
 * Client-side sound handler.
 */
object SoundHandler {
    
    /**
     * Plays a computer beep sound.
     */
    fun playBeep(x: Double, y: Double, z: Double, frequency: Int, duration: Float) {
        val minecraft = net.minecraft.client.Minecraft.getInstance()
        val level = minecraft.level ?: return
        
        // Create a custom sound based on frequency
        // In a real implementation, this would generate or select an appropriate sound
    }
    
    /**
     * Plays keyboard typing sounds.
     */
    fun playKeyClick(x: Double, y: Double, z: Double) {
        // Play keyboard click sound
    }
    
    /**
     * Plays disk drive sounds.
     */
    fun playDiskInsert(x: Double, y: Double, z: Double) {
        // Play disk insert sound
    }
    
    fun playDiskEject(x: Double, y: Double, z: Double) {
        // Play disk eject sound
    }
    
    fun playDiskActivity(x: Double, y: Double, z: Double) {
        // Play disk read/write sound
    }
}

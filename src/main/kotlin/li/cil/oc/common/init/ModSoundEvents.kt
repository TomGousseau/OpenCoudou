package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Registration for OpenComputers sound events.
 */
object ModSoundEvents {
    private val SOUNDS: DeferredRegister<SoundEvent> = 
        DeferredRegister.create(Registries.SOUND_EVENT, OpenComputers.MOD_ID)
    
    // ========================================
    // Computer Sounds
    // ========================================
    
    val COMPUTER_RUNNING: DeferredHolder<SoundEvent, SoundEvent> = registerSound("computer.running")
    val COMPUTER_STARTUP: DeferredHolder<SoundEvent, SoundEvent> = registerSound("computer.startup")
    val COMPUTER_SHUTDOWN: DeferredHolder<SoundEvent, SoundEvent> = registerSound("computer.shutdown")
    val COMPUTER_BEEP: DeferredHolder<SoundEvent, SoundEvent> = registerSound("computer.beep")
    
    // ========================================
    // Assembler Sounds
    // ========================================
    
    val ASSEMBLER_ASSEMBLE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("assembler.assemble")
    val ASSEMBLER_SUCCESS: DeferredHolder<SoundEvent, SoundEvent> = registerSound("assembler.success")
    
    // ========================================
    // Disassembler Sounds
    // ========================================
    
    val DISASSEMBLER_DISASSEMBLE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("disassembler.disassemble")
    
    // ========================================
    // Printer Sounds
    // ========================================
    
    val PRINTER_PRINT: DeferredHolder<SoundEvent, SoundEvent> = registerSound("printer.print")
    
    // ========================================
    // Storage Sounds
    // ========================================
    
    val FLOPPY_INSERT: DeferredHolder<SoundEvent, SoundEvent> = registerSound("floppy.insert")
    val FLOPPY_EJECT: DeferredHolder<SoundEvent, SoundEvent> = registerSound("floppy.eject")
    val FLOPPY_ACCESS: DeferredHolder<SoundEvent, SoundEvent> = registerSound("floppy.access")
    
    val HDD_ACCESS: DeferredHolder<SoundEvent, SoundEvent> = registerSound("hdd.access")
    
    // ========================================
    // Robot Sounds
    // ========================================
    
    val ROBOT_MOVE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot.move")
    val ROBOT_TURN: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot.turn")
    val ROBOT_SWING: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot.swing")
    val ROBOT_USE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot.use")
    
    // ========================================
    // Drone Sounds
    // ========================================
    
    val DRONE_FLYING: DeferredHolder<SoundEvent, SoundEvent> = registerSound("drone.flying")
    val DRONE_DEPLOY: DeferredHolder<SoundEvent, SoundEvent> = registerSound("drone.deploy")
    
    // ========================================
    // Keyboard Sounds
    // ========================================
    
    val KEYBOARD_PRESS: DeferredHolder<SoundEvent, SoundEvent> = registerSound("keyboard.press")
    
    // ========================================
    // Screen Sounds
    // ========================================
    
    val SCREEN_CLICK: DeferredHolder<SoundEvent, SoundEvent> = registerSound("screen.click")
    
    // ========================================
    // Misc Sounds
    // ========================================
    
    val CHARGER_CHARGE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("charger.charge")
    val CHARGER_COMPLETE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("charger.complete")
    
    val HOLOGRAM_UPDATE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("hologram.update")
    
    val GEOLYZER_SCAN: DeferredHolder<SoundEvent, SoundEvent> = registerSound("geolyzer.scan")
    
    val MODEM_MESSAGE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("modem.message")
    
    val EFFECT_NANOMACHINES: DeferredHolder<SoundEvent, SoundEvent> = registerSound("effect.nanomachines")
    
    // ========================================
    // Helper Methods
    // ========================================
    
    private fun registerSound(name: String): DeferredHolder<SoundEvent, SoundEvent> {
        return SOUNDS.register(name.replace(".", "_")) {
            SoundEvent.createVariableRangeEvent(Settings.resource(name.replace(".", "/")))
        }
    }
    
    // ========================================
    // Registration
    // ========================================
    
    fun register(bus: IEventBus) {
        SOUNDS.register(bus)
        OpenComputers.LOGGER.debug("Registered sound events")
    }
}

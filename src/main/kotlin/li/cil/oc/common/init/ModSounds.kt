package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Sound event registration for OpenComputers.
 */
object ModSounds {
    val SOUNDS: DeferredRegister<SoundEvent> = DeferredRegister.create(
        Registries.SOUND_EVENT, OpenComputers.MOD_ID
    )
    
    // Computer sounds
    val COMPUTER_RUNNING: DeferredHolder<SoundEvent, SoundEvent> = registerSound("computer.running")
    val COMPUTER_STARTUP: DeferredHolder<SoundEvent, SoundEvent> = registerSound("computer.startup")
    val COMPUTER_SHUTDOWN: DeferredHolder<SoundEvent, SoundEvent> = registerSound("computer.shutdown")
    val COMPUTER_BEEP: DeferredHolder<SoundEvent, SoundEvent> = registerSound("computer.beep")
    
    // Robot sounds
    val ROBOT_MOVE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot.move")
    val ROBOT_TURN: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot.turn")
    val ROBOT_SWING: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot.swing")
    
    // Drone sounds
    val DRONE_FLY: DeferredHolder<SoundEvent, SoundEvent> = registerSound("drone.fly")
    
    // Disk drive sounds
    val DISK_INSERT: DeferredHolder<SoundEvent, SoundEvent> = registerSound("disk.insert")
    val DISK_EJECT: DeferredHolder<SoundEvent, SoundEvent> = registerSound("disk.eject")
    
    // Keyboard sounds
    val KEYBOARD_PRESS: DeferredHolder<SoundEvent, SoundEvent> = registerSound("keyboard.press")
    val KEYBOARD_RELEASE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("keyboard.release")
    
    // Printer sounds
    val PRINTER_WORKING: DeferredHolder<SoundEvent, SoundEvent> = registerSound("printer.working")
    val PRINTER_DONE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("printer.done")
    
    // Assembler sounds
    val ASSEMBLER_WORKING: DeferredHolder<SoundEvent, SoundEvent> = registerSound("assembler.working")
    val ASSEMBLER_DONE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("assembler.done")
    
    // Disassembler sounds
    val DISASSEMBLER_WORKING: DeferredHolder<SoundEvent, SoundEvent> = registerSound("disassembler.working")
    
    // Network sounds
    val NETWORK_ACTIVITY: DeferredHolder<SoundEvent, SoundEvent> = registerSound("network.activity")
    
    // Hologram sounds
    val HOLOGRAM_UPDATE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("hologram.update")
    
    private fun registerSound(name: String): DeferredHolder<SoundEvent, SoundEvent> {
        val location = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, name)
        return SOUNDS.register(name.replace(".", "_")) { 
            SoundEvent.createVariableRangeEvent(location) 
        }
    }
}

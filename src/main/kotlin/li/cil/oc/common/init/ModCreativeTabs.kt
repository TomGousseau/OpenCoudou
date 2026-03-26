package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Registration for OpenComputers creative tabs.
 */
object ModCreativeTabs {
    private val CREATIVE_TABS: DeferredRegister<CreativeModeTab> = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenComputers.MOD_ID)
    
    // ========================================
    // Main Tab
    // ========================================
    
    val MAIN: DeferredHolder<CreativeModeTab, CreativeModeTab> =
        CREATIVE_TABS.register("main") {
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.${OpenComputers.MOD_ID}.main"))
                .icon { ItemStack(ModBlocks.CASE_TIER2.get()) }
                .displayItems { _, output ->
                    // Blocks
                    output.accept(ModItems.CASE_TIER1_ITEM.get())
                    output.accept(ModItems.CASE_TIER2_ITEM.get())
                    output.accept(ModItems.CASE_TIER3_ITEM.get())
                    output.accept(ModItems.CASE_CREATIVE_ITEM.get())
                    
                    output.accept(ModItems.SCREEN_TIER1_ITEM.get())
                    output.accept(ModItems.SCREEN_TIER2_ITEM.get())
                    output.accept(ModItems.SCREEN_TIER3_ITEM.get())
                    
                    output.accept(ModItems.KEYBOARD_ITEM.get())
                    output.accept(ModItems.REDSTONE_IO_ITEM.get())
                    
                    output.accept(ModItems.CABLE_ITEM.get())
                    output.accept(ModItems.RELAY_ITEM.get())
                    output.accept(ModItems.ACCESS_POINT_ITEM.get())
                    
                    output.accept(ModItems.CAPACITOR_ITEM.get())
                    output.accept(ModItems.POWER_CONVERTER_ITEM.get())
                    output.accept(ModItems.POWER_DISTRIBUTOR_ITEM.get())
                    output.accept(ModItems.CHARGER_ITEM.get())
                    
                    output.accept(ModItems.ADAPTER_ITEM.get())
                    output.accept(ModItems.TRANSPOSER_ITEM.get())
                    output.accept(ModItems.DISK_DRIVE_ITEM.get())
                    output.accept(ModItems.RAID_ITEM.get())
                    
                    output.accept(ModItems.GEOLYZER_ITEM.get())
                    output.accept(ModItems.MOTION_SENSOR_ITEM.get())
                    output.accept(ModItems.WAYPOINT_ITEM.get())
                    
                    output.accept(ModItems.RACK_ITEM.get())
                    output.accept(ModItems.ASSEMBLER_ITEM.get())
                    output.accept(ModItems.DISASSEMBLER_ITEM.get())
                    output.accept(ModItems.PRINTER_ITEM.get())
                    
                    output.accept(ModItems.HOLOGRAM_TIER1_ITEM.get())
                    output.accept(ModItems.HOLOGRAM_TIER2_ITEM.get())
                    
                    output.accept(ModItems.MICROCONTROLLER_ITEM.get())
                    
                    // CPUs
                    output.accept(ModItems.CPU_TIER1.get())
                    output.accept(ModItems.CPU_TIER2.get())
                    output.accept(ModItems.CPU_TIER3.get())
                    
                    // Memory
                    output.accept(ModItems.MEMORY_TIER1.get())
                    output.accept(ModItems.MEMORY_TIER1_5.get())
                    output.accept(ModItems.MEMORY_TIER2.get())
                    output.accept(ModItems.MEMORY_TIER2_5.get())
                    output.accept(ModItems.MEMORY_TIER3.get())
                    output.accept(ModItems.MEMORY_TIER3_5.get())
                    
                    // GPUs
                    output.accept(ModItems.GPU_TIER1.get())
                    output.accept(ModItems.GPU_TIER2.get())
                    output.accept(ModItems.GPU_TIER3.get())
                    
                    // Storage
                    output.accept(ModItems.HDD_TIER1.get())
                    output.accept(ModItems.HDD_TIER2.get())
                    output.accept(ModItems.HDD_TIER3.get())
                    output.accept(ModItems.FLOPPY.get())
                    output.accept(ModItems.EEPROM.get())
                    
                    // Network cards
                    output.accept(ModItems.NETWORK_CARD.get())
                    output.accept(ModItems.WIRELESS_CARD_TIER1.get())
                    output.accept(ModItems.WIRELESS_CARD_TIER2.get())
                    output.accept(ModItems.INTERNET_CARD.get())
                    output.accept(ModItems.LINKED_CARD.get())
                    
                    // Other cards
                    output.accept(ModItems.REDSTONE_CARD_TIER1.get())
                    output.accept(ModItems.REDSTONE_CARD_TIER2.get())
                    output.accept(ModItems.DATA_CARD_TIER1.get())
                    output.accept(ModItems.DATA_CARD_TIER2.get())
                    output.accept(ModItems.DATA_CARD_TIER3.get())
                    output.accept(ModItems.WORLD_SENSOR_CARD.get())
                    
                    // Upgrades
                    output.accept(ModItems.UPGRADE_ANGEL.get())
                    output.accept(ModItems.UPGRADE_BATTERY_TIER1.get())
                    output.accept(ModItems.UPGRADE_BATTERY_TIER2.get())
                    output.accept(ModItems.UPGRADE_BATTERY_TIER3.get())
                    output.accept(ModItems.UPGRADE_CHUNKLOADER.get())
                    output.accept(ModItems.UPGRADE_CRAFTING.get())
                    output.accept(ModItems.UPGRADE_DATABASE_TIER1.get())
                    output.accept(ModItems.UPGRADE_DATABASE_TIER2.get())
                    output.accept(ModItems.UPGRADE_DATABASE_TIER3.get())
                    output.accept(ModItems.UPGRADE_EXPERIENCE.get())
                    output.accept(ModItems.UPGRADE_GENERATOR.get())
                    output.accept(ModItems.UPGRADE_HOVER_TIER1.get())
                    output.accept(ModItems.UPGRADE_HOVER_TIER2.get())
                    output.accept(ModItems.UPGRADE_INVENTORY.get())
                    output.accept(ModItems.UPGRADE_INVENTORY_CONTROLLER.get())
                    output.accept(ModItems.UPGRADE_LEASH.get())
                    output.accept(ModItems.UPGRADE_MFU.get())
                    output.accept(ModItems.UPGRADE_NAVIGATION.get())
                    output.accept(ModItems.UPGRADE_PISTON.get())
                    output.accept(ModItems.UPGRADE_SIGN.get())
                    output.accept(ModItems.UPGRADE_SOLAR_GENERATOR.get())
                    output.accept(ModItems.UPGRADE_TANK.get())
                    output.accept(ModItems.UPGRADE_TANK_CONTROLLER.get())
                    output.accept(ModItems.UPGRADE_TRACTOR_BEAM.get())
                    output.accept(ModItems.UPGRADE_TRADING.get())
                    
                    // Containers
                    output.accept(ModItems.CARD_CONTAINER_TIER1.get())
                    output.accept(ModItems.CARD_CONTAINER_TIER2.get())
                    output.accept(ModItems.CARD_CONTAINER_TIER3.get())
                    output.accept(ModItems.UPGRADE_CONTAINER_TIER1.get())
                    output.accept(ModItems.UPGRADE_CONTAINER_TIER2.get())
                    output.accept(ModItems.UPGRADE_CONTAINER_TIER3.get())
                    
                    // Special items
                    output.accept(ModItems.TABLET.get())
                    output.accept(ModItems.DRONE_CASE_TIER1.get())
                    output.accept(ModItems.DRONE_CASE_TIER2.get())
                    output.accept(ModItems.DRONE_CASE_CREATIVE.get())
                    output.accept(ModItems.HOVER_BOOTS.get())
                    output.accept(ModItems.NANOMACHINES.get())
                    output.accept(ModItems.TERMINAL.get())
                    output.accept(ModItems.ANALYZER.get())
                    output.accept(ModItems.MANUAL.get())
                    
                    // Tools
                    output.accept(ModItems.WRENCH.get())
                    output.accept(ModItems.DEBUG_CARD.get())
                    
                    // Materials
                    output.accept(ModItems.CHAMELIUM.get())
                    output.accept(ModItems.CHAMELIUM_BLOCK_ITEM.get())
                    output.accept(ModItems.CIRCUIT_TIER1.get())
                    output.accept(ModItems.CIRCUIT_TIER2.get())
                    output.accept(ModItems.CIRCUIT_TIER3.get())
                    output.accept(ModItems.CIRCUIT_TIER4.get())
                    output.accept(ModItems.TRANSISTOR.get())
                    output.accept(ModItems.MICROCHIP_TIER1.get())
                    output.accept(ModItems.MICROCHIP_TIER2.get())
                    output.accept(ModItems.MICROCHIP_TIER3.get())
                    output.accept(ModItems.ALU.get())
                    output.accept(ModItems.CONTROL_UNIT.get())
                    output.accept(ModItems.DISK_PLATTER.get())
                    output.accept(ModItems.CARD_BASE.get())
                    output.accept(ModItems.CUTTING_WIRE.get())
                    output.accept(ModItems.ACID.get())
                    output.accept(ModItems.RAW_CIRCUIT_BOARD.get())
                    output.accept(ModItems.INK_CARTRIDGE.get())
                    output.accept(ModItems.INK_CARTRIDGE_EMPTY.get())
                    output.accept(ModItems.INTERWEB.get())
                    output.accept(ModItems.COMPONENT_BUS_TIER1.get())
                    output.accept(ModItems.COMPONENT_BUS_TIER2.get())
                    output.accept(ModItems.COMPONENT_BUS_TIER3.get())
                }
                .build()
        }
    
    // ========================================
    // Registration
    // ========================================
    
    fun register(bus: IEventBus) {
        CREATIVE_TABS.register(bus)
        OpenComputers.LOGGER.debug("Registered creative tabs")
    }
}

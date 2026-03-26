package li.cil.oc.datagen

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModItems
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper
import java.util.function.Supplier

/**
 * Item model provider for OpenComputers.
 * Generates item models for all items.
 */
class ModItemModelProvider(
    output: PackOutput,
    helper: ExistingFileHelper
) : ItemModelProvider(output, OpenComputers.MOD_ID, helper) {
    
    override fun registerModels() {
        // === Materials ===
        simpleItem(ModItems.RAW_CIRCUIT_BOARD)
        simpleItem(ModItems.CIRCUIT_BOARD)
        simpleItem(ModItems.PRINTED_CIRCUIT_BOARD)
        simpleItem(ModItems.TRANSISTOR)
        simpleItem(ModItems.MICROCHIP_TIER1)
        simpleItem(ModItems.MICROCHIP_TIER2)
        simpleItem(ModItems.MICROCHIP_TIER3)
        simpleItem(ModItems.ALU)
        simpleItem(ModItems.CONTROL_UNIT)
        simpleItem(ModItems.DISK_PLATTER)
        simpleItem(ModItems.INTERWEB)
        simpleItem(ModItems.CHAMELIUM)
        simpleItem(ModItems.INK_CARTRIDGE)
        simpleItem(ModItems.INK_CARTRIDGE_COLOR)
        simpleItem(ModItems.ACID)
        simpleItem(ModItems.CUTTING_WIRE)
        simpleItem(ModItems.CAPACITOR)
        simpleItem(ModItems.BUTTON_GROUP)
        simpleItem(ModItems.ARROW_KEYS)
        simpleItem(ModItems.NUMPAD)
        simpleItem(ModItems.CARD_BASE)
        simpleItem(ModItems.CABLE)
        
        // === CPUs ===
        simpleItem(ModItems.CPU_TIER1)
        simpleItem(ModItems.CPU_TIER2)
        simpleItem(ModItems.CPU_TIER3)
        
        // === Memory ===
        simpleItem(ModItems.RAM_TIER1)
        simpleItem(ModItems.RAM_TIER15)
        simpleItem(ModItems.RAM_TIER2)
        simpleItem(ModItems.RAM_TIER25)
        simpleItem(ModItems.RAM_TIER3)
        simpleItem(ModItems.RAM_TIER35)
        
        // === Storage ===
        simpleItem(ModItems.EEPROM)
        simpleItem(ModItems.FLOPPY_DISK)
        simpleItem(ModItems.HDD_TIER1)
        simpleItem(ModItems.HDD_TIER2)
        simpleItem(ModItems.HDD_TIER3)
        
        // === Cards ===
        simpleItem(ModItems.GRAPHICS_CARD_TIER1)
        simpleItem(ModItems.GRAPHICS_CARD_TIER2)
        simpleItem(ModItems.GRAPHICS_CARD_TIER3)
        simpleItem(ModItems.NETWORK_CARD)
        simpleItem(ModItems.WIRELESS_CARD_TIER1)
        simpleItem(ModItems.WIRELESS_CARD_TIER2)
        simpleItem(ModItems.INTERNET_CARD)
        simpleItem(ModItems.LINKED_CARD)
        simpleItem(ModItems.REDSTONE_CARD_TIER1)
        simpleItem(ModItems.REDSTONE_CARD_TIER2)
        simpleItem(ModItems.DATA_CARD_TIER1)
        simpleItem(ModItems.DATA_CARD_TIER2)
        simpleItem(ModItems.DATA_CARD_TIER3)
        simpleItem(ModItems.WORLD_SENSOR_CARD)
        
        // === Upgrades ===
        simpleItem(ModItems.ANGEL_UPGRADE)
        simpleItem(ModItems.BATTERY_UPGRADE_TIER1)
        simpleItem(ModItems.BATTERY_UPGRADE_TIER2)
        simpleItem(ModItems.BATTERY_UPGRADE_TIER3)
        simpleItem(ModItems.CHUNKLOADER_UPGRADE)
        simpleItem(ModItems.CRAFTING_UPGRADE)
        simpleItem(ModItems.DATABASE_UPGRADE_TIER1)
        simpleItem(ModItems.DATABASE_UPGRADE_TIER2)
        simpleItem(ModItems.DATABASE_UPGRADE_TIER3)
        simpleItem(ModItems.EXPERIENCE_UPGRADE)
        simpleItem(ModItems.GENERATOR_UPGRADE)
        simpleItem(ModItems.HOVER_UPGRADE_TIER1)
        simpleItem(ModItems.HOVER_UPGRADE_TIER2)
        simpleItem(ModItems.INVENTORY_UPGRADE)
        simpleItem(ModItems.INVENTORY_CONTROLLER_UPGRADE)
        simpleItem(ModItems.NAVIGATION_UPGRADE)
        simpleItem(ModItems.PISTON_UPGRADE)
        simpleItem(ModItems.SIGN_UPGRADE)
        simpleItem(ModItems.SOLAR_GENERATOR_UPGRADE)
        simpleItem(ModItems.TANK_UPGRADE)
        simpleItem(ModItems.TANK_CONTROLLER_UPGRADE)
        simpleItem(ModItems.TRACTOR_BEAM_UPGRADE)
        simpleItem(ModItems.TRADING_UPGRADE)
        simpleItem(ModItems.LEASH_UPGRADE)
        
        // === Tools ===
        simpleItem(ModItems.ANALYZER)
        simpleItem(ModItems.TERMINAL)
        simpleItem(ModItems.REMOTE_TERMINAL)
        simpleItem(ModItems.MANUAL)
        simpleItem(ModItems.NANOMACHINES)
        simpleItem(ModItems.WRENCH)
        simpleItem(ModItems.HOVER_BOOTS)
        
        // === APUs ===
        simpleItem(ModItems.APU_TIER1)
        simpleItem(ModItems.APU_TIER2)
        
        // === Component Bus ===
        simpleItem(ModItems.COMPONENT_BUS_TIER1)
        simpleItem(ModItems.COMPONENT_BUS_TIER2)
        simpleItem(ModItems.COMPONENT_BUS_TIER3)
        
        // === Server ===
        simpleItem(ModItems.SERVER_TIER1)
        simpleItem(ModItems.SERVER_TIER2)
        simpleItem(ModItems.SERVER_TIER3)
        simpleItem(ModItems.SERVER_CREATIVE)
        simpleItem(ModItems.TERMINAL_SERVER)
        
        // === Tablet Case ===
        simpleItem(ModItems.TABLET_CASE_TIER1)
        simpleItem(ModItems.TABLET_CASE_TIER2)
        simpleItem(ModItems.TABLET_CASE_CREATIVE)
        
        // === Microcontroller Cases ===
        simpleItem(ModItems.MICROCONTROLLER_CASE_TIER1)
        simpleItem(ModItems.MICROCONTROLLER_CASE_TIER2)
        simpleItem(ModItems.MICROCONTROLLER_CASE_CREATIVE)
        
        // === Drone Cases ===
        simpleItem(ModItems.DRONE_CASE_TIER1)
        simpleItem(ModItems.DRONE_CASE_TIER2)
        simpleItem(ModItems.DRONE_CASE_CREATIVE)
    }
    
    private fun simpleItem(item: Supplier<out Item>) {
        val name = item.get().let { 
            // Get registration name from registry
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it).path
        }
        withExistingParent(name, mcLoc("item/generated"))
            .texture("layer0", modLoc("item/$name"))
    }
    
    private fun modLoc(path: String): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, path)
}

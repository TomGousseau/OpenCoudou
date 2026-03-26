package li.cil.oc.datagen

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModBlocks
import li.cil.oc.common.init.ModItems
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.ItemTagsProvider
import net.minecraft.data.tags.TagsProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.BlockTagsProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

/**
 * Custom tags for OpenComputers
 */
object ModTags {
    object Blocks {
        val COMPUTERS = tag("computers")
        val SCREENS = tag("screens")
        val CABLES = tag("cables")
        val POWER_STORAGE = tag("power_storage")
        val NETWORK_DEVICES = tag("network_devices")
        val ROBOT_PLACEABLE = tag("robot_placeable")
        val WRENCH_BREAKABLE = tag("wrench_breakable")
        
        private fun tag(name: String): TagKey<Block> =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, name))
    }
    
    object Items {
        val COMPONENTS = tag("components")
        val CPUS = tag("cpus")
        val MEMORY = tag("memory")
        val STORAGE = tag("storage")
        val CARDS = tag("cards") 
        val UPGRADES = tag("upgrades")
        val MATERIALS = tag("materials")
        val MICROCHIPS = tag("microchips")
        val CASES = tag("cases")
        val SERVERS = tag("servers")
        
        // Forge tags
        val WRENCHES = Tags.Items.TOOLS_WRENCH
        
        private fun tag(name: String): TagKey<Item> =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, name))
    }
}

/**
 * Block tag provider for OpenComputers
 */
class ModBlockTagsProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>,
    existingFileHelper: ExistingFileHelper?
) : BlockTagsProvider(output, lookupProvider, OpenComputers.MOD_ID, existingFileHelper) {
    
    override fun addTags(provider: HolderLookup.Provider) {
        // Mineable with pickaxe
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(ModBlocks.CASE_TIER1.get())
            .add(ModBlocks.CASE_TIER2.get())
            .add(ModBlocks.CASE_TIER3.get())
            .add(ModBlocks.CASE_CREATIVE.get())
            .add(ModBlocks.SCREEN_TIER1.get())
            .add(ModBlocks.SCREEN_TIER2.get())
            .add(ModBlocks.SCREEN_TIER3.get())
            .add(ModBlocks.KEYBOARD.get())
            .add(ModBlocks.CAPACITOR.get())
            .add(ModBlocks.CHARGER.get())
            .add(ModBlocks.POWER_CONVERTER.get())
            .add(ModBlocks.POWER_DISTRIBUTOR.get())
            .add(ModBlocks.RELAY.get())
            .add(ModBlocks.SWITCH.get())
            .add(ModBlocks.ACCESS_POINT.get())
            .add(ModBlocks.NET_SPLITTER.get())
            .add(ModBlocks.DISK_DRIVE.get())
            .add(ModBlocks.RAID.get())
            .add(ModBlocks.ADAPTER.get())
            .add(ModBlocks.ASSEMBLER.get())
            .add(ModBlocks.DISASSEMBLER.get())
            .add(ModBlocks.GEOLYZER.get())
            .add(ModBlocks.HOLOGRAM_TIER1.get())
            .add(ModBlocks.HOLOGRAM_TIER2.get())
            .add(ModBlocks.MOTION_SENSOR.get())
            .add(ModBlocks.PRINTER.get())
            .add(ModBlocks.RACK.get())
            .add(ModBlocks.REDSTONE_IO.get())
            .add(ModBlocks.TRANSPOSER.get())
            .add(ModBlocks.WAYPOINT.get())
            .add(ModBlocks.ROBOT.get())
            .add(ModBlocks.MICROCONTROLLER.get())
            .add(ModBlocks.CABLE.get())
            .add(ModBlocks.CHAMELIUM_BLOCK.get())
        
        // Needs iron tool
        tag(BlockTags.NEEDS_IRON_TOOL)
            .add(ModBlocks.CASE_TIER2.get())
            .add(ModBlocks.CASE_TIER3.get())
            .add(ModBlocks.SCREEN_TIER2.get())
            .add(ModBlocks.SCREEN_TIER3.get())
        
        // Custom tags
        tag(ModTags.Blocks.COMPUTERS)
            .add(ModBlocks.CASE_TIER1.get())
            .add(ModBlocks.CASE_TIER2.get())
            .add(ModBlocks.CASE_TIER3.get())
            .add(ModBlocks.CASE_CREATIVE.get())
            .add(ModBlocks.ROBOT.get())
            .add(ModBlocks.MICROCONTROLLER.get())
        
        tag(ModTags.Blocks.SCREENS)
            .add(ModBlocks.SCREEN_TIER1.get())
            .add(ModBlocks.SCREEN_TIER2.get())
            .add(ModBlocks.SCREEN_TIER3.get())
        
        tag(ModTags.Blocks.CABLES)
            .add(ModBlocks.CABLE.get())
        
        tag(ModTags.Blocks.POWER_STORAGE)
            .add(ModBlocks.CAPACITOR.get())
            .add(ModBlocks.CHARGER.get())
            .add(ModBlocks.POWER_CONVERTER.get())
            .add(ModBlocks.POWER_DISTRIBUTOR.get())
        
        tag(ModTags.Blocks.NETWORK_DEVICES)
            .add(ModBlocks.RELAY.get())
            .add(ModBlocks.SWITCH.get())
            .add(ModBlocks.ACCESS_POINT.get())
            .add(ModBlocks.NET_SPLITTER.get())
        
        tag(ModTags.Blocks.WRENCH_BREAKABLE)
            .addTag(ModTags.Blocks.COMPUTERS)
            .addTag(ModTags.Blocks.SCREENS)
            .addTag(ModTags.Blocks.CABLES)
            .addTag(ModTags.Blocks.POWER_STORAGE)
            .addTag(ModTags.Blocks.NETWORK_DEVICES)
    }
}

/**
 * Item tag provider for OpenComputers
 */
class ModItemTagsProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>,
    blockTags: CompletableFuture<TagsProvider.TagLookup<Block>>,
    existingFileHelper: ExistingFileHelper?
) : ItemTagsProvider(output, lookupProvider, blockTags, OpenComputers.MOD_ID, existingFileHelper) {
    
    override fun addTags(provider: HolderLookup.Provider) {
        // CPUs
        tag(ModTags.Items.CPUS)
            .add(ModItems.CPU_TIER1.get())
            .add(ModItems.CPU_TIER2.get())
            .add(ModItems.CPU_TIER3.get())
        
        // Memory
        tag(ModTags.Items.MEMORY)
            .add(ModItems.RAM_TIER1.get())
            .add(ModItems.RAM_TIER15.get())
            .add(ModItems.RAM_TIER2.get())
            .add(ModItems.RAM_TIER25.get())
            .add(ModItems.RAM_TIER3.get())
            .add(ModItems.RAM_TIER35.get())
        
        // Storage
        tag(ModTags.Items.STORAGE)
            .add(ModItems.EEPROM.get())
            .add(ModItems.FLOPPY_DISK.get())
            .add(ModItems.HDD_TIER1.get())
            .add(ModItems.HDD_TIER2.get())
            .add(ModItems.HDD_TIER3.get())
        
        // Cards
        tag(ModTags.Items.CARDS)
            .add(ModItems.GRAPHICS_CARD_TIER1.get())
            .add(ModItems.GRAPHICS_CARD_TIER2.get())
            .add(ModItems.GRAPHICS_CARD_TIER3.get())
            .add(ModItems.NETWORK_CARD.get())
            .add(ModItems.WIRELESS_CARD_TIER1.get())
            .add(ModItems.WIRELESS_CARD_TIER2.get())
            .add(ModItems.INTERNET_CARD.get())
            .add(ModItems.LINKED_CARD.get())
            .add(ModItems.REDSTONE_CARD_TIER1.get())
            .add(ModItems.REDSTONE_CARD_TIER2.get())
            .add(ModItems.DATA_CARD_TIER1.get())
            .add(ModItems.DATA_CARD_TIER2.get())
            .add(ModItems.DATA_CARD_TIER3.get())
            .add(ModItems.WORLD_SENSOR_CARD.get())
        
        // Upgrades
        tag(ModTags.Items.UPGRADES)
            .add(ModItems.ANGEL_UPGRADE.get())
            .add(ModItems.BATTERY_UPGRADE_TIER1.get())
            .add(ModItems.BATTERY_UPGRADE_TIER2.get())
            .add(ModItems.BATTERY_UPGRADE_TIER3.get())
            .add(ModItems.CHUNKLOADER_UPGRADE.get())
            .add(ModItems.CRAFTING_UPGRADE.get())
            .add(ModItems.DATABASE_UPGRADE_TIER1.get())
            .add(ModItems.DATABASE_UPGRADE_TIER2.get())
            .add(ModItems.DATABASE_UPGRADE_TIER3.get())
            .add(ModItems.EXPERIENCE_UPGRADE.get())
            .add(ModItems.GENERATOR_UPGRADE.get())
            .add(ModItems.HOVER_UPGRADE_TIER1.get())
            .add(ModItems.HOVER_UPGRADE_TIER2.get())
            .add(ModItems.INVENTORY_UPGRADE.get())
            .add(ModItems.INVENTORY_CONTROLLER_UPGRADE.get())
            .add(ModItems.NAVIGATION_UPGRADE.get())
            .add(ModItems.PISTON_UPGRADE.get())
            .add(ModItems.SIGN_UPGRADE.get())
            .add(ModItems.SOLAR_GENERATOR_UPGRADE.get())
            .add(ModItems.TANK_UPGRADE.get())
            .add(ModItems.TANK_CONTROLLER_UPGRADE.get())
            .add(ModItems.TRACTOR_BEAM_UPGRADE.get())
            .add(ModItems.TRADING_UPGRADE.get())
            .add(ModItems.LEASH_UPGRADE.get())
        
        // Materials
        tag(ModTags.Items.MATERIALS)
            .add(ModItems.RAW_CIRCUIT_BOARD.get())
            .add(ModItems.CIRCUIT_BOARD.get())
            .add(ModItems.PRINTED_CIRCUIT_BOARD.get())
            .add(ModItems.TRANSISTOR.get())
            .add(ModItems.ALU.get())
            .add(ModItems.CONTROL_UNIT.get())
            .add(ModItems.DISK_PLATTER.get())
            .add(ModItems.INTERWEB.get())
            .add(ModItems.CHAMELIUM.get())
            .add(ModItems.INK_CARTRIDGE.get())
            .add(ModItems.INK_CARTRIDGE_COLOR.get())
            .add(ModItems.ACID.get())
            .add(ModItems.CUTTING_WIRE.get())
            .add(ModItems.CAPACITOR.get())
            .add(ModItems.BUTTON_GROUP.get())
            .add(ModItems.ARROW_KEYS.get())
            .add(ModItems.NUMPAD.get())
            .add(ModItems.CARD_BASE.get())
            .add(ModItems.CABLE.get())
        
        // Microchips
        tag(ModTags.Items.MICROCHIPS)
            .add(ModItems.MICROCHIP_TIER1.get())
            .add(ModItems.MICROCHIP_TIER2.get())
            .add(ModItems.MICROCHIP_TIER3.get())
        
        // Cases
        tag(ModTags.Items.CASES)
            .add(ModItems.TABLET_CASE_TIER1.get())
            .add(ModItems.TABLET_CASE_TIER2.get())
            .add(ModItems.TABLET_CASE_CREATIVE.get())
            .add(ModItems.MICROCONTROLLER_CASE_TIER1.get())
            .add(ModItems.MICROCONTROLLER_CASE_TIER2.get())
            .add(ModItems.MICROCONTROLLER_CASE_CREATIVE.get())
            .add(ModItems.DRONE_CASE_TIER1.get())
            .add(ModItems.DRONE_CASE_TIER2.get())
            .add(ModItems.DRONE_CASE_CREATIVE.get())
        
        // Servers
        tag(ModTags.Items.SERVERS)
            .add(ModItems.SERVER_TIER1.get())
            .add(ModItems.SERVER_TIER2.get())
            .add(ModItems.SERVER_TIER3.get())
            .add(ModItems.SERVER_CREATIVE.get())
            .add(ModItems.TERMINAL_SERVER.get())
        
        // All components
        tag(ModTags.Items.COMPONENTS)
            .addTag(ModTags.Items.CPUS)
            .addTag(ModTags.Items.MEMORY)
            .addTag(ModTags.Items.STORAGE)
            .addTag(ModTags.Items.CARDS)
            .addTag(ModTags.Items.UPGRADES)
        
        // Forge wrench tag
        tag(ModTags.Items.WRENCHES)
            .add(ModItems.WRENCH.get())
    }
}
